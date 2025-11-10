package com.example.personalfinancialmanagement;

import android.os.Handler;
import android.os.Looper;

import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Lightweight threading helper to run work off the UI thread and post results back.
 */
public final class Async {
    private static final ExecutorService IO = Executors.newFixedThreadPool(2);
    private static final Handler MAIN = new Handler(Looper.getMainLooper());

    private Async() {}

    public static ExecutorService io() { return IO; }
    public static void runIo(Runnable r) { IO.execute(r); }
    public static void runMain(Runnable r) { MAIN.post(r); }
}
