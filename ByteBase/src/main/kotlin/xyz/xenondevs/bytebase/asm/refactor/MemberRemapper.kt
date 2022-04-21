package xyz.xenondevs.bytebase.asm.refactor

import org.objectweb.asm.commons.SimpleRemapper

/**
 * [SimpleRemapper] implementation that also allows remapping of field names and local variable names
 */
class MemberRemapper(mappings: Map<String, String>) : SimpleRemapper(mappings) {
    
    override fun mapFieldName(owner: String, name: String, descriptor: String) =
        map("$owner.$name.$descriptor") ?: name
    
    fun mapLocalVariableName(owner: String, methodName: String, methodDesc: String, varName: String, varDesc: String) =
        map("$owner.$methodName$methodDesc.$varName.$varDesc") ?: varName
}