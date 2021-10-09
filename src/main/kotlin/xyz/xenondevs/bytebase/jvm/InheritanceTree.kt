package xyz.xenondevs.bytebase.jvm

class InheritanceTree(val wrapper: ClassWrapper) {
    val superClasses = HashSet<ClassWrapper>()
    val subClasses = HashSet<ClassWrapper>()
}