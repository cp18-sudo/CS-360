package com.example.weighttracker;

import org.junit.Test;
import static org.junit.Assert.*;

/**
 * Verifies that SingleEvent delivers its content exactly once
 * and returns null on subsequent reads.
 */
public class SingleEventTest {

    @Test
    public void firstRead_returnsContent() {
        SingleEvent<String> event = new SingleEvent<>("hello");
        assertEquals("hello", event.getContentIfNotConsumed());
    }

    @Test
    public void secondRead_returnsNull() {
        SingleEvent<String> event = new SingleEvent<>("hello");
        event.getContentIfNotConsumed(); // consume
        assertNull(event.getContentIfNotConsumed());
    }

    @Test
    public void peekContent_doesNotConsume() {
        SingleEvent<String> event = new SingleEvent<>("hello");
        assertEquals("hello", event.peekContent());
        assertEquals("hello", event.getContentIfNotConsumed()); // still available
    }

    @Test
    public void peekContent_alwaysReturnsValue() {
        SingleEvent<String> event = new SingleEvent<>("hello");
        event.getContentIfNotConsumed(); // consume
        assertEquals("hello", event.peekContent()); // still accessible via peek
    }

    @Test
    public void nullContent_isHandled() {
        SingleEvent<String> event = new SingleEvent<>(null);
        assertNull(event.getContentIfNotConsumed());
        // After consuming null, second call also returns null (same behavior)
        assertNull(event.getContentIfNotConsumed());
    }
}
