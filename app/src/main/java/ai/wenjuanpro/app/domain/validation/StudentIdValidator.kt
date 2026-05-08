package ai.wenjuanpro.app.domain.validation

/**
 * Validates a scanned QR payload of the form `<studentId>@beihai` and
 * extracts the bare student id (the part before the @beihai suffix).
 *
 * - The suffix `@beihai` is mandatory and matched case-insensitively.
 * - The id part must match `[A-Za-z0-9_-]{1,64}`.
 * - Downstream code (result file naming, welcome page, session) uses
 *   only the extracted student id, never the raw QR string.
 */
object StudentIdValidator {
    const val REQUIRED_SUFFIX = "@beihai"
    private val ID_PART_REGEX = Regex("^[A-Za-z0-9_-]{1,64}$")

    private const val INVALID_REASON = "二维码格式非法"

    fun validate(input: String): ValidationResult {
        if (!input.endsWith(REQUIRED_SUFFIX, ignoreCase = true)) {
            return ValidationResult.Invalid(INVALID_REASON)
        }
        val idPart = input.substring(0, input.length - REQUIRED_SUFFIX.length)
        if (!ID_PART_REGEX.matches(idPart)) {
            return ValidationResult.Invalid(INVALID_REASON)
        }
        return ValidationResult.Valid(studentId = idPart)
    }

    sealed interface ValidationResult {
        data class Valid(val studentId: String) : ValidationResult

        data class Invalid(val reason: String) : ValidationResult
    }
}
