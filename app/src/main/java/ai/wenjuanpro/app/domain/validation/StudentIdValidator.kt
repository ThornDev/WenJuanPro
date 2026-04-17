package ai.wenjuanpro.app.domain.validation

object StudentIdValidator {
    private val STUDENT_ID_REGEX = Regex("^[A-Za-z0-9_-]{1,64}$")
    private const val INVALID_REASON = "学号格式非法"

    fun validate(input: String): ValidationResult =
        if (STUDENT_ID_REGEX.matches(input)) {
            ValidationResult.Valid
        } else {
            ValidationResult.Invalid(INVALID_REASON)
        }

    sealed interface ValidationResult {
        data object Valid : ValidationResult

        data class Invalid(val reason: String) : ValidationResult
    }
}
