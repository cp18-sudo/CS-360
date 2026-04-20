package com.example.weighttracker;

import org.junit.Test;
import static org.junit.Assert.*;

/** Verifies the success/error factory methods on WeightParseResult. */
public class WeightParseResultTest {

    @Test
    public void success_isValid() {
        WeightParseResult r = WeightParseResult.success(175.0);
        assertTrue(r.isValid());
        assertEquals(175.0, r.getWeight(), 0.001);
        assertEquals(0, r.getErrorResId());
    }

    @Test
    public void error_isNotValid() {
        WeightParseResult r = WeightParseResult.error(42);
        assertFalse(r.isValid());
        assertEquals(42, r.getErrorResId());
    }
}
