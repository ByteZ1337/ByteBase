package xyz.xenondevs.bytebase.patch.annotation

/**
 * Annotation used to "proxy" a field in the target class. Also has the ability to make the field accessible and mutable.
 *
 * **Note:** Changing the fields access can only be done if the field is defined in the target class (for now).
 *
 * @property name the name of the target field.
 * @property desc the descriptor of the target field.
 */
@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.PROPERTY, AnnotationTarget.FIELD)
annotation class FieldAccessor(
    val name: String = "",
    val desc: String = "",
)
