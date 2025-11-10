package com.example.personalfinancialmanagement;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import java.io.File;

public class ManageDataFragment extends Fragment {

    public static ManageDataFragment newInstance() { return new ManageDataFragment(); }

    private View trackContainer; private View segDb; private View segCache; private View segOther; private TextView tvUsed; private TextView tvDb; private TextView tvCache; private TextView tvOther;

    @Nullable @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.activity_manage_data, container, false);
        final int baseL = root.getPaddingLeft();
        final int baseT = root.getPaddingTop();
        final int baseR = root.getPaddingRight();
        final int baseB = root.getPaddingBottom();
        androidx.core.view.ViewCompat.setOnApplyWindowInsetsListener(root, (v, insets) -> {
            androidx.core.graphics.Insets bars = insets.getInsets(androidx.core.view.WindowInsetsCompat.Type.systemBars());
            v.setPadding(baseL, baseT + bars.top, baseR, baseB);
            return insets;
        });
        trackContainer = root.findViewById(R.id.storage_track);
        segDb = root.findViewById(R.id.storage_seg_db);
        segCache = root.findViewById(R.id.storage_seg_cache);
        segOther = root.findViewById(R.id.storage_seg_other);
        tvUsed = root.findViewById(R.id.tv_used_total);
        tvDb = root.findViewById(R.id.tv_used_db);
        tvCache = root.findViewById(R.id.tv_used_cache);
        tvOther = root.findViewById(R.id.tv_used_other);

        View btnBack = root.findViewById(R.id.btn_back);
        if (btnBack != null) btnBack.setOnClickListener(v -> requireActivity().getSupportFragmentManager().popBackStack());

        View btnClear = root.findViewById(R.id.btn_clear_data);
        if (btnClear != null) btnClear.setOnClickListener(v -> confirmClear());

        refreshSizes();
        return root;
    }

    private void refreshSizes() {
        if (getContext() == null) return;
        long db = 0, cache = 0, other = 0;
        try {
            File dbFile = requireContext().getDatabasePath("campus_expense_db");
            if (dbFile != null && dbFile.exists()) db = dbFile.length();
        } catch (Throwable ignored) {}
        try { cache += dirSize(requireContext().getCacheDir()); } catch (Throwable ignored) {}
        try { File ec = requireContext().getExternalCacheDir(); cache += dirSize(ec); } catch (Throwable ignored) {}
        try {
            File files = requireContext().getFilesDir(); other += dirSize(files);
            File prefs = new File(requireContext().getApplicationInfo().dataDir, "shared_prefs"); other += dirSize(prefs);
            // exclude DB directory from other size
            File dbDir = new File(requireContext().getApplicationInfo().dataDir, "databases");
            long otherMinusDb = dirSize(dbDir) - db; if (otherMinusDb > 0) other += otherMinusDb;
        } catch (Throwable ignored) {}

        long total = Math.max(1, db + cache + other);
        final String usedStr = human(total);
        final String dbStr = human(db);
        final String cacheStr = human(cache);
        final String otherStr = human(other);
        final long fDb = db, fCache = cache, fOther = other;

        if (!isAdded()) return;
        trackContainer.post(() -> {
            int width = trackContainer.getWidth();
            if (width <= 0) width = trackContainer.getMeasuredWidth();
            if (width <= 0) width = trackContainer.getLayoutParams().width;
            int dbW = (int) Math.round(width * (fDb / (double) (fDb + fCache + fOther + 0.00001)));
            int cacheW = (int) Math.round(width * (fCache / (double) (fDb + fCache + fOther + 0.00001)));
            int otherW = Math.max(0, width - dbW - cacheW);
            setWidth(segDb, dbW);
            setWidth(segCache, cacheW);
            setWidth(segOther, otherW);

            if (tvUsed != null) tvUsed.setText("Used: " + usedStr);
            if (tvDb != null) tvDb.setText("Database: " + dbStr);
            if (tvCache != null) tvCache.setText("Cache: " + cacheStr);
            if (tvOther != null) tvOther.setText("Other: " + otherStr);
        });
    }

    private void confirmClear() {
        new com.google.android.material.dialog.MaterialAlertDialogBuilder(requireContext())
                .setTitle("Clear App Data")
                .setMessage("This will delete database records and cached files. Continue?")
                .setPositiveButton("Clear", (d, w) -> clearData())
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void clearData() {
        final long userId = requireActivity().getIntent().getLongExtra("userId", -1);
        Async.runIo(() -> {
            try { AppDatabase.getInstance(requireContext()).clearUserData(userId); } catch (Throwable ignored) {}
            Async.runMain(() -> android.widget.Toast.makeText(requireContext(), "Cleared data for this account", android.widget.Toast.LENGTH_SHORT).show());
            Async.runMain(this::refreshSizes);
        });
    }

    private static long dirSize(File f) {
        if (f == null || !f.exists()) return 0;
        if (f.isFile()) return f.length();
        long s = 0; File[] list = f.listFiles(); if (list == null) return 0;
        for (File c : list) s += dirSize(c);
        return s;
    }

    private static void deleteDir(File f) {
        if (f == null || !f.exists()) return; if (f.isFile()) { f.delete(); return; }
        File[] list = f.listFiles(); if (list != null) for (File c : list) deleteDir(c); f.delete();
    }

    private static void setWidth(View v, int w) { ViewGroup.LayoutParams lp = v.getLayoutParams(); lp.width = Math.max(0, w); v.setLayoutParams(lp); }
    private static String human(long b) {
        double kb = b / 1024.0; double mb = kb / 1024.0; double gb = mb / 1024.0;
        if (gb >= 1) return String.format(java.util.Locale.getDefault(), "%.2f GB", gb);
        if (mb >= 1) return String.format(java.util.Locale.getDefault(), "%.1f MB", mb);
        if (kb >= 1) return String.format(java.util.Locale.getDefault(), "%.0f KB", kb);
        return b + " B";
    }
}
