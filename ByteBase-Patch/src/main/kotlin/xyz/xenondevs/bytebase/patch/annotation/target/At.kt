package xyz.xenondevs.bytebase.patch.annotation.target

annotation class At(
    val value: String,
    val position: Position = Position.BEFORE,
    val offset: Int = 0,
    val ignoreLineNumbers: Boolean = true
)


enum class Position(val offset: Int) {
    BEFORE(0),
    AFTER(1),
    REPLACE(0)
}