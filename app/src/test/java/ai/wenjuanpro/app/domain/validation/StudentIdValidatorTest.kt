package ai.wenjuanpro.app.domain.validation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Tests for StudentIdValidator — accepts QR payloads of the form
 * `<id>@beihai` and extracts the bare id portion.
 */
class StudentIdValidatorTest {
    @Test
    fun `valid id with required suffix returns Valid carrying the id only`() {
        val result = StudentIdValidator.validate("S001@beihai")
        assertEquals(
            StudentIdValidator.ValidationResult.Valid(studentId = "S001"),
            result,
        )
    }

    @Test
    fun `valid numeric id`() {
        val result = StudentIdValidator.validate("123456@beihai")
        assertEquals(
            StudentIdValidator.ValidationResult.Valid(studentId = "123456"),
            result,
        )
    }

    @Test
    fun `suffix is matched case-insensitively`() {
        val result = StudentIdValidator.validate("S001@BeiHai")
        assertEquals(
            StudentIdValidator.ValidationResult.Valid(studentId = "S001"),
            result,
        )
    }

    @Test
    fun `missing suffix returns Invalid`() {
        val result = StudentIdValidator.validate("S001")
        assertTrue(result is StudentIdValidator.ValidationResult.Invalid)
    }

    @Test
    fun `empty string returns Invalid`() {
        val result = StudentIdValidator.validate("")
        assertTrue(result is StudentIdValidator.ValidationResult.Invalid)
    }

    @Test
    fun `wrong suffix returns Invalid`() {
        val result = StudentIdValidator.validate("S001@other")
        assertTrue(result is StudentIdValidator.ValidationResult.Invalid)
    }

    @Test
    fun `empty id part with valid suffix returns Invalid`() {
        val result = StudentIdValidator.validate("@beihai")
        assertTrue(result is StudentIdValidator.ValidationResult.Invalid)
    }

    @Test
    fun `id part exceeding 64 chars returns Invalid`() {
        val result = StudentIdValidator.validate("A".repeat(65) + "@beihai")
        assertTrue(result is StudentIdValidator.ValidationResult.Invalid)
    }

    @Test
    fun `id part at 64 chars returns Valid`() {
        val id = "A".repeat(64)
        val result = StudentIdValidator.validate("$id@beihai")
        assertEquals(
            StudentIdValidator.ValidationResult.Valid(studentId = id),
            result,
        )
    }

    @Test
    fun `id part with space returns Invalid`() {
        val result = StudentIdValidator.validate("S 001@beihai")
        assertTrue(result is StudentIdValidator.ValidationResult.Invalid)
    }

    @Test
    fun `id part with chinese returns Invalid`() {
        val result = StudentIdValidator.validate("学生001@beihai")
        assertTrue(result is StudentIdValidator.ValidationResult.Invalid)
    }

    @Test
    fun `underscore and hyphen in id part are valid`() {
        val result = StudentIdValidator.validate("_-@beihai")
        assertEquals(
            StudentIdValidator.ValidationResult.Valid(studentId = "_-"),
            result,
        )
    }
}
