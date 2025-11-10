package com.example.personalfinancialmanagement;

import android.app.Application;
import android.os.Looper;

import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class PfmApp extends Application {
    @Override public void onCreate() {
        super.onCreate();
        Thread.setDefaultUncaughtExceptionHandler((t, e) -> {
            try {
                File dir = new File(getFilesDir(), "crash");
                if (!dir.exists()) dir.mkdirs();
                String ts = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
                File f = new File(dir, "crash_" + ts + ".log");
                try (FileWriter fw = new FileWriter(f); PrintWriter pw = new PrintWriter(fw)) {
                    pw.println("Thread: " + t.getName());
                    e.printStackTrace(pw);
                }
            } catch (Throwable ignored) {}
            if (Looper.getMainLooper() != null && Thread.currentThread() == Looper.getMainLooper().getThread()) {
                android.widget.Toast.makeText(getApplicationContext(), "App error captured. Restarting...", android.widget.Toast.LENGTH_SHORT).show();
            }
            // Let the system handle termination to avoid undefined state
            android.os.Process.killProcess(android.os.Process.myPid());
            System.exit(10);
        });
    }
}
