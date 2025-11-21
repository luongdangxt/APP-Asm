package com.example.personalfinancialmanagement;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import java.util.ArrayList;

public class FeedbackFragment extends Fragment {
    public static final String ARG_USER_ID = "userId";

    public static FeedbackFragment newInstance(long userId) {
        FeedbackFragment f = new FeedbackFragment();
        Bundle b = new Bundle();
        b.putLong(ARG_USER_ID, userId);
        f.setArguments(b);
        return f;
    }

    private long userId;
    private FeedbackDao dao;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.activity_feedback, container, false);
        userId = getArguments() != null ? getArguments().getLong(ARG_USER_ID, -1) : -1;
        dao = AppDatabase.getInstance(requireContext()).feedbackDao();

        EditText input = root.findViewById(R.id.ed_feedback);
        Button send = root.findViewById(R.id.btn_send_feedback);
        ListView list = root.findViewById(R.id.lv_feedback);

        send.setOnClickListener(v -> {
            String msg = input.getText().toString().trim();
            if (!msg.isEmpty() && userId > 0) {
                Async.runIo(() -> {
                    dao.insert(new Feedback(userId, msg, System.currentTimeMillis()));
                    Async.runMain(() -> { input.setText(""); refresh(list); });
                });
            }
        });

        refresh(list);
        return root;
    }

    private void refresh(ListView list) {
        Async.runIo(() -> {
            java.util.List<Feedback> items = dao.list(userId);
            ArrayList<String> rows = new ArrayList<>();
            for (Feedback f : items) rows.add(f.message);
            Async.runMain(() -> {
                if (!isAdded() || getContext() == null) return;
                list.setAdapter(new ArrayAdapter<>(requireContext(), android.R.layout.simple_list_item_1, rows));
            });
        });
    }
}
