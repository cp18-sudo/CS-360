package com.example.weighttracker;

import org.junit.Test;
import static org.junit.Assert.*;

/**
 * Unit tests for InputValidator. Checks that validation rules catch
 * empty input, boundary violations, and bad numeric formats, and that
 * valid input passes cleanly.
 *
 * Tests check for 0 (valid) vs non-zero (error resource ID) since
 * actual string resolution requires an Android context.
 */
public class InputValidatorTest {

    // ---- Username validation ----

    @Test
    public void username_null_returnsError() {
        assertNotEquals(0, InputValidator.validateUsername(null));
    }

    @Test
    public void username_empty_returnsError() {
        assertNotEquals(0, InputValidator.validateUsername(""));
    }

    @Test
    public void username_onlyWhitespace_returnsError() {
        assertNotEquals(0, InputValidator.validateUsername("   "));
    }

    @Test
    public void username_tooShort_returnsError() {
        assertNotEquals(0, InputValidator.validateUsername("ab"));
    }

    @Test
    public void username_exactMinLength_isValid() {
        assertEquals(0, InputValidator.validateUsername("abc"));
    }

    @Test
    public void username_tooLong_returnsError() {
        String longName = "a".repeat(InputValidator.MAX_USERNAME_LENGTH + 1);
        assertNotEquals(0, InputValidator.validateUsername(longName));
    }

    @Test
    public void username_exactMaxLength_isValid() {
        String maxName = "a".repeat(InputValidator.MAX_USERNAME_LENGTH);
        assertEquals(0, InputValidator.validateUsername(maxName));
    }

    @Test
    public void username_typicalInput_isValid() {
        assertEquals(0, InputValidator.validateUsername("chris"));
    }

    // ---- Password validation ----

    @Test
    public void password_null_returnsError() {
        assertNotEquals(0, InputValidator.validatePassword(null));
    }

    @Test
    public void password_empty_returnsError() {
        assertNotEquals(0, InputValidator.validatePassword(""));
    }

    @Test
    public void password_tooShort_returnsError() {
        assertNotEquals(0, InputValidator.validatePassword("abc"));
    }

    @Test
    public void password_exactMinLength_isValid() {
        String minPass = "a".repeat(InputValidator.MIN_PASSWORD_LENGTH);
        assertEquals(0, InputValidator.validatePassword(minPass));
    }

    @Test
    public void password_typicalInput_isValid() {
        assertEquals(0, InputValidator.validatePassword("securepass123"));
    }

    // ---- Weight parsing and validation ----

    @Test
    public void weight_null_returnsError() {
        WeightParseResult r = InputValidator.parseAndValidateWeight(null);
        assertFalse(r.isValid());
    }

    @Test
    public void weight_empty_returnsError() {
        WeightParseResult r = InputValidator.parseAndValidateWeight("");
        assertFalse(r.isValid());
    }

    @Test
    public void weight_whitespaceOnly_returnsError() {
        WeightParseResult r = InputValidator.parseAndValidateWeight("   ");
        assertFalse(r.isValid());
    }

    @Test
    public void weight_nonNumeric_returnsError() {
        WeightParseResult r = InputValidator.parseAndValidateWeight("abc");
        assertFalse(r.isValid());
    }

    @Test
    public void weight_negative_returnsError() {
        WeightParseResult r = InputValidator.parseAndValidateWeight("-10");
        assertFalse(r.isValid());
    }

    @Test
    public void weight_zero_returnsError() {
        WeightParseResult r = InputValidator.parseAndValidateWeight("0");
        assertFalse(r.isValid());
    }

    @Test
    public void weight_belowMin_returnsError() {
        WeightParseResult r = InputValidator.parseAndValidateWeight("0.5");
        assertFalse(r.isValid());
    }

    @Test
    public void weight_aboveMax_returnsError() {
        WeightParseResult r = InputValidator.parseAndValidateWeight("1501");
        assertFalse(r.isValid());
    }

    @Test
    public void weight_atMin_isValid() {
        WeightParseResult r = InputValidator.parseAndValidateWeight("1.0");
        assertTrue(r.isValid());
        assertEquals(1.0, r.getWeight(), 0.001);
    }

    @Test
    public void weight_atMax_isValid() {
        WeightParseResult r = InputValidator.parseAndValidateWeight("1500");
        assertTrue(r.isValid());
        assertEquals(1500.0, r.getWeight(), 0.001);
    }

    @Test
    public void weight_typicalValue_isValid() {
        WeightParseResult r = InputValidator.parseAndValidateWeight("185.5");
        assertTrue(r.isValid());
        assertEquals(185.5, r.getWeight(), 0.001);
    }

    @Test
    public void weight_withLeadingWhitespace_isValid() {
        WeightParseResult r = InputValidator.parseAndValidateWeight("  200  ");
        assertTrue(r.isValid());
        assertEquals(200.0, r.getWeight(), 0.001);
    }

    @Test
    public void weight_decimalPrecision_isValid() {
        WeightParseResult r = InputValidator.parseAndValidateWeight("150.75");
        assertTrue(r.isValid());
        assertEquals(150.75, r.getWeight(), 0.001);
    }
}
