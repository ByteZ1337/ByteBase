package xyz.xenondevs.bytebase.patch.patcher.kotlin.remapper.impl

import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap
import it.unimi.dsi.fastutil.objects.Object2IntMap
import it.unimi.dsi.fastutil.objects.ObjectArrayList
import kotlinx.metadata.KmProperty
import kotlinx.metadata.isVar
import org.objectweb.asm.Handle
import org.objectweb.asm.Opcodes.*
import org.objectweb.asm.Type
import org.objectweb.asm.tree.FieldInsnNode
import org.objectweb.asm.tree.FieldNode
import org.objectweb.asm.tree.InvokeDynamicInsnNode
import org.objectweb.asm.tree.LabelNode
import xyz.xenondevs.bytebase.asm.InsnBuilder
import xyz.xenondevs.bytebase.asm.buildInsnList
import xyz.xenondevs.bytebase.asm.insnListOf
import xyz.xenondevs.bytebase.jvm.ClassWrapper
import xyz.xenondevs.bytebase.patch.PatchMode
import xyz.xenondevs.bytebase.patch.Patcher
import xyz.xenondevs.bytebase.patch.Patcher.LoadedPatch
import xyz.xenondevs.bytebase.patch.patcher.kotlin.remapper.MappingsContainer
import xyz.xenondevs.bytebase.patch.patcher.kotlin.remapper.NonAnnotatedPropertyRemapper
import xyz.xenondevs.bytebase.patch.util.StringUtils.between
import xyz.xenondevs.bytebase.patch.util.StringUtils.capitalize
import xyz.xenondevs.bytebase.patch.util.StringUtils.possessive
import xyz.xenondevs.bytebase.patch.util.desc
import xyz.xenondevs.bytebase.patch.util.weak.IntWeakIdentityHashMap
import xyz.xenondevs.bytebase.util.InsnUtils
import xyz.xenondevs.bytebase.util.MethodNode
import xyz.xenondevs.bytebase.util.TypeUtils
import xyz.xenondevs.bytebase.util.accessWrapper
import xyz.xenondevs.bytebase.util.arrayLoadInsn
import xyz.xenondevs.bytebase.util.arrayStoreInsn
import xyz.xenondevs.bytebase.util.internalName
import xyz.xenondevs.bytebase.util.loadInsn
import xyz.xenondevs.bytebase.util.newArrayInsn
import xyz.xenondevs.bytebase.util.returnInsn
import kotlin.random.Random

private const val FIELD_HOLDER_NAME = "ByteBaseFieldHolder"

/**
 * TODO: Also insert default field values
 */
internal class NewFieldRemapper(
    patcher: Patcher,
    patch: LoadedPatch,
    mappings: MappingsContainer,
    newDefinitions: MutableMap<String, ClassWrapper>
) : NonAnnotatedPropertyRemapper(patcher, patch, mappings, newDefinitions) {
    
    private val logger get() = patcher.logger
    
    private val fieldHolder by lazy {
        FieldHolderBuilder(newDefinitions.getOrElse(FIELD_HOLDER_NAME) {
            ClassWrapper("$FIELD_HOLDER_NAME.class").apply { access = ACC_PUBLIC }
        }).apply {
            if (FIELD_HOLDER_NAME !in newDefinitions) {
                init()
                addNewClass(this.clazz)
            }
        }
    }
    
    override fun processProperty(prop: KmProperty) {
        if (patch.patchMode == PatchMode.CLASSLOADER) {
            addField(prop)
        } else {
            proxyField(prop)
        }
    }
    
    override fun finish() {
        if (patch.patchMode != PatchMode.INSTRUMENTATION)
            return
        fieldHolder.finishClass()
    }
    
    private fun addField(prop: KmProperty) {
        val target = patch.target
        var name = prop.name
        val desc = prop.desc
        logger.debug("- Adding field $name.$desc to ${patch.target.name}.")
        
        if (target.inheritanceTree.resolveField(name, desc) != null) {
            name = "${name}_" + Random.nextInt(1337)
            logger.warn("- Field $name already exists in ${target.name.possessive()} InheritanceTree, renamed to $name")
        }
        
        val field = FieldNode(ACC_PUBLIC, name, desc, null, null)
        target.fields.add(field)
        
        if (!prop.isVar) {
            field.accessWrapper.setFinal(true)
            mappings.addRemap(prop, insnListOf(FieldInsnNode(GETFIELD, target.name, name, desc)))
        } else {
            mappings.addRemap(prop, insnListOf(FieldInsnNode(GETFIELD, target.name, name, desc)), insnListOf(FieldInsnNode(PUTFIELD, target.name, name, desc)))
        }
    }
    
    private fun proxyField(prop: KmProperty) {
        val key = prop.name + prop.desc
        logger.debug("- Redirecting field calls of $key to ByteBaseFieldHolder.")
        fieldHolder.addNeededType(prop)
    }
    
    inner class FieldHolderBuilder(
        val clazz: ClassWrapper
    ) {
        
        private val propByType = Int2ObjectOpenHashMap<MutableList<KmProperty>>()
        
        fun init() {
            val identityMap = FieldNode(ACC_PRIVATE or ACC_FINAL or ACC_STATIC, "IDENTITY_MAP", "L${Object2IntMap::class.internalName};", null, null)
            clazz.fields.add(identityMap)
            val onRemove = clazz.getOrCreateMethod("onRemove", "(I)V", ACC_PRIVATE or ACC_STATIC)
            clazz.getOrCreateClassInit().instructions = buildInsnList {
                addLabel()
                new(IntWeakIdentityHashMap::class)
                dup()
                add(
                    InvokeDynamicInsnNode(
                        "accept",
                        "()Ljava/util/function/Consumer;",
                        InsnUtils.LAMBDA_METAFACTORY_HANDLE,
                        Type.getType("(I)V"),
                        Handle(H_INVOKESTATIC, clazz.name, onRemove.name, onRemove.desc, false),
                        Type.getType("(I)V")
                    )
                )
                invokeSpecial(IntWeakIdentityHashMap::class.internalName, "<init>", "(Ljava/util/function/IntConsumer;)V")
                putStatic(clazz.name, identityMap.name, identityMap.desc)
                
                addLabel()
                _return()
            }
        }
        
        fun addNeededType(prop: KmProperty) {
            val type = prop.desc
            val sort = Type.getType(type).getHolderSort()
            propByType.getOrPut(sort, ::ObjectArrayList).add(prop)
        }
        
        fun finishClass() {
            propByType.forEach { (sort, props) ->
                val actualType = TypeUtils.getTypeForSort(sort, arrayToObject = true)
                val actualDesc = actualType.descriptor
                if (props.size == 1) {
                    val (name, desc) = addMap(actualType, isSingleMap = true)
                    val prop = props.first()
                    val getter = buildInsnList {
                        getStatic(clazz.name, name, desc)
                        swap()
                        invokeStatic(System::identityHashCode)
                        invokeVirtual(desc.between('L', ';'), "get", "(I)$actualDesc")
                        if (actualType.sort >= Type.ARRAY)
                            checkCast(actualType.internalName)
                    }
                    if (!prop.isVar) {
                        mappings.addRemap(prop, getter)
                        return@forEach
                    }
                    val setter = buildInsnList {
                        swap()
                        pop()
                        getStatic(clazz.name, name, desc)
                        swap()
                        aLoad(0)
                        invokeStatic(System::identityHashCode)
                        swap()
                        invokeVirtual(desc.between('L', ';'), "put", "(I$actualDesc)$actualDesc")
                    }
                    mappings.addRemap(prop, getter, setter)
                } else {
                    val (name, desc) = addMap(actualType, isSingleMap = false)
                    val (getName, getDesc) = addMultiGetMethod(name, desc, actualType)
                    var setName: String? = null
                    var setDesc: String? = null
                    props.forEachIndexed { i, prop ->
                        
                        val getter = buildInsnList {
                            invokeStatic(System::identityHashCode)
                            ldc(i)
                            ldc(props.size)
                            invokeStatic(clazz.name, getName, getDesc)
                        }
                        if (!prop.isVar) {
                            mappings.addRemap(prop, getter)
                            return@forEach
                        }
                        
                        if (setName == null) {
                            val ref = addMultiSetMethod(name, desc, actualType)
                            setName = ref.first
                            setDesc = ref.second
                        }
                        val setter = buildInsnList {
                            swap()
                            invokeStatic(System::identityHashCode) // stack: [value, hash]
                            swap() // stack: [hash, value]
                            ldc(i) // stack: [hash, value, i]
                            swap() // stack: [hash, i, value]
                            ldc(props.size) // stack: [hash, i, value, size]
                            swap() // stack: [hash, i, size, value]
                            invokeStatic(clazz.name, setName!!, setDesc!!)
                        }
                        mappings.addRemap(prop, getter, setter)
                    }
                }
            }
        }
        
        private fun addMap(type: Type, isSingleMap: Boolean): Pair<String, String> {
            val typeName = type.className.substringAfterLast('.').capitalize()
            val mapName = if (isSingleMap) "SINGLE_${typeName.uppercase()}" else "MULTI_${typeName.uppercase()}"
            val mapType = if (isSingleMap) "Lit/unimi/dsi/fastutil/ints/Int2${typeName}OpenHashMap;" else "L" + Int2ObjectOpenHashMap::class.internalName + ";"
            
            if (clazz.getField(mapName, mapType) != null)
                return mapName to mapType
            
            val mapField = FieldNode(ACC_PRIVATE or ACC_FINAL or ACC_STATIC, mapName, mapType, null, null)
            clazz.fields.add(mapField)
            
            val insns = clazz.getOrCreateClassInit().instructions
            insns.insertBefore(insns.last, buildInsnList {
                val internalName = if (isSingleMap) mapType.substring(1, mapType.length - 1) else Int2ObjectOpenHashMap::class.internalName
                new(internalName)
                dup()
                invokeSpecial(internalName, "<init>", "()V")
                putStatic(clazz.name, mapName, mapType)
                addLabel()
            })
            addToOnRemove(mapName, mapType)
            return mapName to mapType
        }
        
        
        private fun addToOnRemove(name: String, desc: String) {
            val onRemove = clazz.getMethod("onRemove", "(I)V")!!
            val insns = onRemove.instructions
            insns.insertBefore(insns.last, buildInsnList {
                addLabel()
                getStatic(clazz.name, name, desc)
                iLoad(0)
                invokeVirtual(desc.substring(1, desc.length - 1), "remove", "(I)Z", false)
                pop()
                addLabel()
            })
        }
        
        private fun addMultiGetMethod(mapName: String, mapDesc: String, valueType: Type): Pair<String, String> {
            val valueDesc = valueType.descriptor
            val name = "getMulti" + valueType.className.substringAfterLast('.').capitalize()
            val desc = "(III)$valueDesc"
            
            if (clazz.getMethod(name, desc) != null)
                return name to desc
            
            val method = MethodNode(ACC_PUBLIC or ACC_STATIC, name, desc) {
                getOrAddArray(mapName, mapDesc, valueType) {
                    aLoad(4)
                    iLoad(1)
                    add(valueType.arrayLoadInsn())
                    add(valueType.returnInsn())
                }
            }
            clazz.methods.add(method)
            return method.name to method.desc
        }
        
        private fun addMultiSetMethod(mapName: String, mapDesc: String, valueType: Type): Pair<String, String> {
            val valueDesc = valueType.descriptor
            val name = "setMulti" + valueType.className.substringAfterLast('.').capitalize()
            val desc = "(III$valueDesc)V"
            
            if (clazz.getMethod(name, desc) != null)
                return name to desc
            
            val method = MethodNode(ACC_PUBLIC or ACC_STATIC, name, desc) {
                getOrAddArray(mapName, mapDesc, valueType) {
                    aLoad(4)
                    iLoad(1)
                    add(valueType.loadInsn(3))
                    add(valueType.arrayStoreInsn())
                    _return()
                }
            }
            clazz.methods.add(method)
            return method.name to method.desc
        }
        
        private fun InsnBuilder.getOrAddArray(mapName: String, mapDesc: String, valueType: Type, then: InsnBuilder.() -> Unit) {
            val valueDesc = valueType.descriptor
            val addLabel = LabelNode()
            val getLabel = LabelNode()
            
            addLabel()
            getStatic(clazz.name, mapName, mapDesc)
            iLoad(0)
            invokeVirtual(mapDesc.between('L', ';'), "get", "(I)Ljava/lang/Object;", false)
            checkCast("[$valueDesc")
            dup()
            aStore(4)
            ifnull(addLabel)
            
            add(getLabel)
            then()
            
            add(addLabel)
            iLoad(2)
            add(valueType.newArrayInsn())
            aStore(4)
            getStatic(clazz.name, mapName, mapDesc)
            iLoad(0)
            aLoad(4)
            invokeVirtual(mapDesc.between('L', ';'), "put", "(ILjava/lang/Object;)Ljava/lang/Object;", false)
            pop()
            goto(getLabel)
        }
        
    }
}

private fun Type.getHolderSort(): Int {
    return if (sort < Type.ARRAY) sort else Type.OBJECT
}