package com.example.personalfinancialmanagement;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.graphics.Insets;

import com.google.android.material.textfield.TextInputLayout;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

public class AddGoalFragment extends Fragment {
    public static final String ARG_USER_ID = "userId";
    public static final String ARG_GOAL_ID = "goalId";
    private long userId;
    private final Calendar deadline = Calendar.getInstance();
    private long editingGoalId = -1;

    public static AddGoalFragment newInstance(long userId) {
        AddGoalFragment f = new AddGoalFragment();
        Bundle b = new Bundle(); b.putLong(ARG_USER_ID, userId); f.setArguments(b); return f;
    }

    public static AddGoalFragment editInstance(long userId, long goalId) {
        AddGoalFragment f = new AddGoalFragment();
        Bundle b = new Bundle(); b.putLong(ARG_USER_ID, userId); b.putLong(ARG_GOAL_ID, goalId); f.setArguments(b); return f;
    }

    @Nullable @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.activity_add_goal, container, false);
        userId = getArguments() != null ? getArguments().getLong(ARG_USER_ID, -1) : -1;
        final android.content.Context appCtx = requireContext().getApplicationContext();
        editingGoalId = getArguments() != null ? getArguments().getLong(ARG_GOAL_ID, -1) : -1;

        final int baseL = root.getPaddingLeft();
        final int baseT = root.getPaddingTop();
        final int baseR = root.getPaddingRight();
        final int baseB = root.getPaddingBottom();
        ViewCompat.setOnApplyWindowInsetsListener(root, (v, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(baseL, baseT, baseR, baseB + bars.bottom);
            return insets;
        });

        EditText etTitle = root.findViewById(R.id.et_title);
        EditText etAmount = root.findViewById(R.id.et_amount);
        AutoCompleteTextView drop = root.findViewById(R.id.drop_cadence);
        EditText etDeadline = root.findViewById(R.id.et_deadline);

        String[] items = new String[] {"Daily", "Weekly", "Monthly", "Yearly"};
        drop.setAdapter(new ArrayAdapter<>(requireContext(), android.R.layout.simple_dropdown_item_1line, items));
        drop.setText(items[0], false);

        SimpleDateFormat df = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
        etDeadline.setText(df.format(deadline.getTime()));
        Runnable openPicker = () -> {
            DatePickerDialog dlg = new DatePickerDialog(requireContext(), (vp, y, m, d) -> {
                deadline.set(Calendar.YEAR, y);
                deadline.set(Calendar.MONTH, m);
                deadline.set(Calendar.DAY_OF_MONTH, d);
                etDeadline.setText(df.format(deadline.getTime()));
            }, deadline.get(Calendar.YEAR), deadline.get(Calendar.MONTH), deadline.get(Calendar.DAY_OF_MONTH));
            dlg.show();
        };
        etDeadline.setOnClickListener(v -> openPicker.run());
        try {
            TextInputLayout til = (TextInputLayout) etDeadline.getParent().getParent();
            til.setEndIconOnClickListener(v -> openPicker.run());
        } catch (Exception ignored) {}

        if (editingGoalId > 0) {
            Async.runIo(() -> {
                SavingsGoal g = AppDatabase.getInstance(appCtx).savingsGoalDao().findById(editingGoalId);
                Async.runMain(() -> {
                    if (!isAdded()) return;
                    if (g != null) {
                        etTitle.setText(g.title);
                        etAmount.setText(String.valueOf((long) g.targetAmount));
                        deadline.setTimeInMillis(g.deadlineUtc > 0 ? g.deadlineUtc : System.currentTimeMillis());
                        etDeadline.setText(df.format(deadline.getTime()));
                        int idx = Math.max(0, Math.min(3, g.cadence));
                        drop.setText(items[idx], false);
                    }
                });
            });
        }

        Button save = root.findViewById(R.id.btn_save);
        save.setOnClickListener(v -> {
            String t = etTitle.getText().toString().trim();
            double amt = 0; try { amt = Double.parseDouble(etAmount.getText().toString().trim()); } catch (Exception ignored) {}
            int cadence = 0; String s = drop.getText().toString(); if ("Weekly".equals(s)) cadence = 1; else if ("Monthly".equals(s)) cadence = 2; else if ("Yearly".equals(s)) cadence = 3;

            if (userId > 0 && !t.isEmpty() && amt > 0) {
                final String fTitle = t;
                final double fAmt = amt;
                final int fCadence = cadence;
                final long fDeadline = deadline.getTimeInMillis();
                Async.runIo(() -> {
                    SavingsGoalDao dao = AppDatabase.getInstance(appCtx).savingsGoalDao();
                    if (editingGoalId > 0) {
                        SavingsGoal g = dao.findById(editingGoalId);
                        if (g != null) {
                            g.title = fTitle; g.targetAmount = fAmt; g.deadlineUtc = fDeadline; g.cadence = fCadence; dao.update(g);
                        }
                    } else {
                        SavingsRepository repo = new SavingsRepository(appCtx);
                        long id = repo.addGoal(userId, fTitle, fAmt, "bag");
                        SavingsGoal g = dao.findById(id);
                        if (g != null) { g.deadlineUtc = fDeadline; g.cadence = fCadence; dao.update(g); }
                    }
                    Async.runMain(() -> { if (getActivity()!=null) getActivity().getSupportFragmentManager().popBackStack(); });
                });
            }
        });

        return root;
    }
}
