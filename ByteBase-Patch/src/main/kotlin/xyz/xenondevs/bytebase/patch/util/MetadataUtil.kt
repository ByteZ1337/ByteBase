package xyz.xenondevs.bytebase.patch.util

import kotlinx.metadata.KmClass
import kotlinx.metadata.KmFunction
import kotlinx.metadata.KmProperty
import kotlinx.metadata.hasAnnotations
import kotlinx.metadata.jvm.KotlinClassMetadata
import kotlinx.metadata.jvm.Metadata
import kotlinx.metadata.jvm.fieldSignature
import kotlinx.metadata.jvm.getterSignature
import kotlinx.metadata.jvm.signature
import kotlinx.metadata.jvm.syntheticMethodForAnnotations
import xyz.xenondevs.bytebase.RuntimeUtils
import xyz.xenondevs.bytebase.jvm.ClassWrapper
import xyz.xenondevs.bytebase.patch.util.StringUtils.dropBi
import xyz.xenondevs.bytebase.util.toMap
import kotlin.reflect.KClass
import kotlin.reflect.KProperty

internal fun KmProperty.resolveAnnotations(clazz: ClassWrapper): List<Annotation> {
    if (!hasAnnotations) return emptyList()
    return resolveAnnotations(clazz, syntheticMethodForAnnotations!!.toString(), "Could not find synthetic method for annotations")
}

internal fun KmFunction.resolveAnnotations(clazz: ClassWrapper): List<Annotation> {
    if (!hasAnnotations) return emptyList()
    return resolveAnnotations(clazz, signature!!.toString(), "Could not find method for annotations")
}

@Suppress("UNCHECKED_CAST")
internal fun resolveAnnotations(
    clazz: ClassWrapper,
    methodSignature: String,
    error: String
): List<Annotation> {
    val method = clazz.getMethod(methodSignature, true)
        ?: throw IllegalStateException(error)
    return method.visibleAnnotations.map {
        val annotClass = Class.forName(it.desc.dropBi(1).replace('/', '.')).kotlin as KClass<Annotation>
        return@map RuntimeUtils.constructAnnotation(annotClass, it.toMap())
    }
}

operator fun ClassWrapper.get(func: KmFunction) =
    getMethod(func.signature!!.toString(), includesDesc = true)

internal val KmProperty.desc: String
    get() = fieldSignature?.descriptor ?: getterSignature!!.descriptor.drop(2)

internal val KmFunction.desc: String
    get() = signature!!.descriptor.drop(2)

internal object MetadataUtil {
    
    @Suppress("UNCHECKED_CAST")
    fun getMetadata(clazz: ClassWrapper): KotlinClassMetadata? {
        val metadataAnnotation = clazz.visibleAnnotations.find { it.desc == "Lkotlin/Metadata;" } ?: return null
        val metadataMap = metadataAnnotation.toMap()
        val mv = (metadataMap["mv"] as ArrayList<Int>).toIntArray()
        val d1 = (metadataMap["d1"] as ArrayList<String>).toTypedArray()
        val d2 = (metadataMap["d2"] as ArrayList<String>).toTypedArray()
        val header = Metadata(
            kind = metadataMap["k"] as Int,
            metadataVersion = mv,
            data1 = d1,
            data2 = d2,
            extraString = metadataMap["xs"] as String?,
            packageName = metadataMap["pn"] as String?,
            extraInt = metadataMap["xi"] as Int?
        )
        return KotlinClassMetadata.read(header)
    }
    
    internal fun isKotlinClass(clazz: ClassWrapper) =
        clazz.sourceFile.endsWith(".kt") || clazz.visibleAnnotations.any { it.desc == "Lkotlin/Metadata;" }
    
    internal fun KProperty<*>.findIn(clazz: KmClass): KmProperty? {
        return clazz.properties.find { it.name == this.name }
    }
    
}

class KmClassWrapper(
    val kmClass: KmClass
) {
    
    constructor(clazz: ClassWrapper) : this((MetadataUtil.getMetadata(clazz) as KotlinClassMetadata.Class).kmClass)
    
    val properties: Map<String, KmProperty> = kmClass.properties.associateBy { it.fieldSignature.toString() }
    val functions: Map<String, KmFunction> = kmClass.functions.associateBy { it.signature.toString() }
    
}

