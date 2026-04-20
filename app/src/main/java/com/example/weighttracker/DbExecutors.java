package com.example.weighttracker;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Shared single-threaded executor for off-main-thread database work.
 * All classes use this instead of creating private executors.
 */
public final class DbExecutors {

    private static final ExecutorService IO = Executors.newSingleThreadExecutor();

    private DbExecutors() {}

    public static ExecutorService io() { return IO; }
}
