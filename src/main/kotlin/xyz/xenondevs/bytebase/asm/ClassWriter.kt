package xyz.xenondevs.bytebase.asm

import org.objectweb.asm.ClassWriter as AsmClassWriter

class ClassWriter(flags: Int = COMPUTE_FRAMES) : AsmClassWriter(flags) {
}