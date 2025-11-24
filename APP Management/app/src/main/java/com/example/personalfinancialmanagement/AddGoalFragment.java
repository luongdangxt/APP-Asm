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
import android.widget.TextView;

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
    public static final String ARG_GOAL_TITLE = "goalTitle";
    public static final String ARG_GOAL_AMOUNT = "goalAmount";
    public static final String ARG_GOAL_DEADLINE = "goalDeadline";
    public static final String ARG_GOAL_CADENCE = "goalCadence";
    private long userId;
    private final Calendar deadline = Calendar.getInstance();
    private long editingGoalId = -1;
    private String editingOriginalTitle;
    private double editingOriginalAmount = 0;
    private int editingOriginalCadence = 0;
    private long editingOriginalDeadline = -1;

    public static AddGoalFragment newInstance(long userId) {
        AddGoalFragment f = new AddGoalFragment();
        Bundle b = new Bundle(); b.putLong(ARG_USER_ID, userId); f.setArguments(b); return f;
    }

    public static AddGoalFragment editInstance(long userId, SavingsGoal goal) {
        AddGoalFragment f = new AddGoalFragment();
        Bundle b = new Bundle();
        b.putLong(ARG_USER_ID, userId);
        if (goal != null) {
            b.putLong(ARG_GOAL_ID, goal.id);
            b.putString(ARG_GOAL_TITLE, goal.title);
            b.putDouble(ARG_GOAL_AMOUNT, goal.targetAmount);
            b.putInt(ARG_GOAL_CADENCE, goal.cadence);
            b.putLong(ARG_GOAL_DEADLINE, goal.deadlineUtc);
        }
        f.setArguments(b);
        return f;
    }

    @Nullable @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.activity_add_goal, container, false);
        Bundle args = getArguments();
        userId = args != null ? args.getLong(ARG_USER_ID, -1) : -1;
        final android.content.Context appCtx = requireContext().getApplicationContext();
        editingGoalId = args != null ? args.getLong(ARG_GOAL_ID, -1) : -1;
        editingOriginalTitle = args != null ? args.getString(ARG_GOAL_TITLE) : null;
        editingOriginalAmount = args != null ? args.getDouble(ARG_GOAL_AMOUNT, 0) : 0;
        editingOriginalCadence = args != null ? args.getInt(ARG_GOAL_CADENCE, 0) : 0;
        editingOriginalDeadline = args != null ? args.getLong(ARG_GOAL_DEADLINE, -1) : -1;
        final boolean isEditing = editingGoalId > 0 || (editingOriginalTitle != null && !editingOriginalTitle.isEmpty());

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
        TextView header = root.findViewById(R.id.header_title);

        String[] items = new String[] {"Daily", "Weekly", "Monthly", "Yearly"};
        drop.setAdapter(new ArrayAdapter<>(requireContext(), android.R.layout.simple_dropdown_item_1line, items));
        int cadenceIdx = editingOriginalCadence;
        if (cadenceIdx < 0 || cadenceIdx >= items.length) cadenceIdx = 0;
        drop.setText(items[cadenceIdx], false);
        drop.setOnClickListener(v -> drop.showDropDown());

        SimpleDateFormat df = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
        if (editingOriginalDeadline > 0) {
            deadline.setTimeInMillis(editingOriginalDeadline);
        }
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

        // Prefill when editing
        if (isEditing) {
            if (header != null) header.setText("Edit Goal");
            if (editingOriginalTitle != null) etTitle.setText(editingOriginalTitle);
            if (editingOriginalAmount > 0) {
                String amtText = editingOriginalAmount % 1 == 0 ? String.format(Locale.getDefault(), "%.0f", editingOriginalAmount) : String.valueOf(editingOriginalAmount);
                etAmount.setText(amtText);
            }
        }

        Button save = root.findViewById(R.id.btn_save);
        if (isEditing) save.setText("UPDATE GOAL");
        save.setOnClickListener(v -> {
            String t = etTitle.getText().toString().trim();
            double amt = 0; try { amt = Double.parseDouble(etAmount.getText().toString().trim()); } catch (Exception ignored) {}
            int cadence = 0; String s = drop.getText().toString(); if ("Weekly".equals(s)) cadence = 1; else if ("Monthly".equals(s)) cadence = 2; else if ("Yearly".equals(s)) cadence = 3;

            if (userId > 0 && !t.isEmpty() && amt > 0) {
                final String fTitle = t;
                final double fAmt = amt;
                final int fCadence = cadence;
                final long fDeadline = deadline.getTimeInMillis();
                final boolean editing = isEditing;
                final String originalTitle = editingOriginalTitle;
                final double originalAmount = editingOriginalAmount;
                final int originalCadence = editingOriginalCadence;
                final long originalDeadline = editingOriginalDeadline;
                Async.runIo(() -> {
                    SavingsRepository repo = new SavingsRepository(appCtx);
                    boolean ok;
                    if (editing) {
                        if (originalTitle != null && !originalTitle.trim().isEmpty()) {
                            repo.deleteGoalByTitle(userId, originalTitle);
                        }
                        ok = repo.addGoal(userId, fTitle, fAmt, "bag", fDeadline > 0 ? fDeadline : null, fCadence);
                        if (!ok && originalTitle != null && !originalTitle.trim().isEmpty()) {
                            repo.addGoal(userId, originalTitle, originalAmount, "bag", originalDeadline > 0 ? originalDeadline : null, originalCadence);
                        }
                    } else {
                        ok = repo.addGoal(userId, fTitle, fAmt, "bag", fDeadline > 0 ? fDeadline : null, fCadence);
                    }
                    Async.runMain(() -> {
                        if (!isAdded()) return;
                        if (ok && getActivity()!=null) getActivity().getSupportFragmentManager().popBackStack();
                        // else keep screen so user can retry
                    });
                });
            }
        });

        return root;
    }
}
