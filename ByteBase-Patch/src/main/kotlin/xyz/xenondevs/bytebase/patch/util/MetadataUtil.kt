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
import xyz.xenondevs.bytebase.jvm.ClassWrapper
import xyz.xenondevs.bytebase.util.toMap
import kotlin.reflect.KProperty

internal val KmProperty.desc: String
    get() = fieldSignature?.descriptor ?: getterSignature!!.descriptor.drop(2)

internal fun KmProperty.resolveAnnotations(clazz: Class<*>): List<Annotation> {
    if (!hasAnnotations) return emptyList()
    val method = clazz.getMethod(syntheticMethodForAnnotations!!.name)
    return method.annotations.toList()
}

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

