package com.example.weighttracker;

/**
 * Wraps a value that should only be consumed once by a LiveData observer.
 * Prevents stale events from replaying after configuration changes like
 * screen rotation. Without this, a successful login result would re-fire
 * on rotation, causing a duplicate navigation.
 */
public class SingleEvent<T> {

    private final T content;
    private boolean consumed = false;

    public SingleEvent(T content) {
        this.content = content;
    }

    /**
     * Returns the content if it has not been consumed yet, or null
     * if it has already been handled. Calling this marks the event
     * as consumed.
     */
    public T getContentIfNotConsumed() {
        if (consumed) {
            return null;
        }
        consumed = true;
        return content;
    }

    /** Returns the content regardless of consumed state (for logging/debug). */
    public T peekContent() {
        return content;
    }
}
