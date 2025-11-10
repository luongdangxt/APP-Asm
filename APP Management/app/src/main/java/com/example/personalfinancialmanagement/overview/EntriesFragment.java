package com.example.personalfinancialmanagement;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.graphics.Insets;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class EntriesFragment extends Fragment {
    public static final String ARG_USER_ID = "userId";
    private long userId;

    public static EntriesFragment newInstance(long userId) {
        EntriesFragment f = new EntriesFragment();
        Bundle b = new Bundle(); b.putLong(ARG_USER_ID, userId); f.setArguments(b); return f;
    }

    @Nullable @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.activity_entries, container, false);
        userId = getArguments() != null ? getArguments().getLong(ARG_USER_ID, -1) : -1;

        final int baseL = root.getPaddingLeft();
        final int baseT = root.getPaddingTop();
        final int baseR = root.getPaddingRight();
        final int baseB = root.getPaddingBottom();
        ViewCompat.setOnApplyWindowInsetsListener(root, (v, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(baseL, baseT, baseR, baseB + bars.bottom);
            return insets;
        });
        RecyclerView rv = root.findViewById(R.id.rv_entries);
        CombinedEntryAdapter adapter = new CombinedEntryAdapter();
        rv.setLayoutManager(new LinearLayoutManager(requireContext()));
        rv.setHasFixedSize(true);
        rv.setItemViewCacheSize(24);
        rv.setAdapter(adapter);

        reload(adapter);
        TextView title = root.findViewById(R.id.tv_title);
        if (title != null) title.setText("Entries");
        return root;
    }

    @Override public void onResume() {
        super.onResume();
        RecyclerView rv = getView() != null ? getView().findViewById(R.id.rv_entries) : null;
        if (rv != null && rv.getAdapter() instanceof CombinedEntryAdapter) reload((CombinedEntryAdapter) rv.getAdapter());
    }

    private void reload(CombinedEntryAdapter adapter) {
        ExpenseRepository exRepo = new ExpenseRepository(requireContext());
        IncomeRepository inRepo = new IncomeRepository(requireContext());
        Async.runIo(() -> {
            List<Expense> ex = exRepo.latest(userId, 100);
            List<Income> in = inRepo.latest(userId, 100);
            java.util.ArrayList<CombinedEntryAdapter.Item> items = new java.util.ArrayList<>();
            for (Expense e : ex) items.add(new CombinedEntryAdapter.Item(false, e.dateUtc, e.description, e.amount, e.category));
            for (Income ic : in) items.add(new CombinedEntryAdapter.Item(true, ic.dateUtc, ic.title, ic.amount, ic.category));
            items.sort((a,b)-> Long.compare(b.time, a.time));
            Async.runMain(() -> adapter.submit(items));
        });
    }
}
