package ai.wenjuanpro.app.domain.validation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Tests for Story 2.1 — StudentIdValidator (正则 `^[A-Za-z0-9_-]{1,64}$`).
 *
 * Scenario IDs map to docs/qa/assessments/2.1-test-design-20260417.md.
 */
class StudentIdValidatorTest {
    @Test
    fun `2_1-UNIT-001 valid S001 returns Valid`() {
        assertEquals(
            StudentIdValidator.ValidationResult.Valid,
            StudentIdValidator.validate("S001"),
        )
    }

    @Test
    fun `2_1-UNIT-002 empty string returns Invalid (BLIND-BOUNDARY-001 Null Empty)`() {
        val result = StudentIdValidator.validate("")
        assertTrue(result is StudentIdValidator.ValidationResult.Invalid)
        assertTrue((result as StudentIdValidator.ValidationResult.Invalid).reason.isNotBlank())
    }

    @Test
    fun `2_1-UNIT-003 single char A returns Valid (BLIND-BOUNDARY-002 Min)`() {
        assertEquals(
            StudentIdValidator.ValidationResult.Valid,
            StudentIdValidator.validate("A"),
        )
    }

    @Test
    fun `2_1-UNIT-004 64 chars returns Valid (BLIND-BOUNDARY-003 Max)`() {
        assertEquals(
            StudentIdValidator.ValidationResult.Valid,
            StudentIdValidator.validate("A".repeat(64)),
        )
    }

    @Test
    fun `2_1-UNIT-005 65 chars returns Invalid (BLIND-BOUNDARY-004 Just Beyond)`() {
        assertTrue(
            StudentIdValidator.validate("A".repeat(65)) is
                StudentIdValidator.ValidationResult.Invalid,
        )
    }

    @Test
    fun `2_1-UNIT-006 contains space returns Invalid`() {
        assertTrue(
            StudentIdValidator.validate("S 001") is StudentIdValidator.ValidationResult.Invalid,
        )
    }

    @Test
    fun `2_1-UNIT-007 contains chinese returns Invalid (BLIND-BOUNDARY-005 Type Mismatch)`() {
        assertTrue(
            StudentIdValidator.validate("学生001") is StudentIdValidator.ValidationResult.Invalid,
        )
    }

    @Test
    fun `2_1-UNIT-008 underscore and hyphen only returns Valid`() {
        assertEquals(
            StudentIdValidator.ValidationResult.Valid,
            StudentIdValidator.validate("_-"),
        )
    }
}
