package com.example.personalfinancialmanagement;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.content.Intent;
import android.net.Uri;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;
import android.util.TypedValue;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;

import com.google.android.material.switchmaterial.SwitchMaterial;

import java.util.Locale;
import java.util.List;

public class ProfileFragment extends Fragment {
    public static final String ARG_USER_ID = "userId";

    public static ProfileFragment newInstance(long userId) {
        ProfileFragment f = new ProfileFragment();
        Bundle b = new Bundle();
        b.putLong(ARG_USER_ID, userId);
        f.setArguments(b);
        return f;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.activity_profile, container, false);

        final int baseL = root.getPaddingLeft();
        final int baseT = root.getPaddingTop();
        final int baseR = root.getPaddingRight();
        final int baseB = root.getPaddingBottom();
        ViewCompat.setOnApplyWindowInsetsListener(root, (v, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(baseL, baseT, baseR, baseB + bars.bottom);
            return insets;
        });

        ImageButton back = root.findViewById(R.id.btn_back);
        if (back != null) back.setOnClickListener(v -> requireActivity().getSupportFragmentManager().popBackStack());

        long userId = getArguments() != null ? getArguments().getLong(ARG_USER_ID, -1) : -1;
        TextView tvName = root.findViewById(R.id.tv_name);
        TextView tvEmailSubtitle = root.findViewById(R.id.tv_sub);
        TextView tvMemberSince = root.findViewById(R.id.tv_profile_member_since);
        TextView tvIncomeMonth = root.findViewById(R.id.tv_profile_income_month);
        TextView tvExpenseMonth = root.findViewById(R.id.tv_profile_expense_month);
        TextView tvBudgetSummary = root.findViewById(R.id.tv_profile_budget_summary);

        EditText edUsername = root.findViewById(R.id.ed_username);
        EditText edFullName = root.findViewById(R.id.ed_full_name);
        EditText edEmail = root.findViewById(R.id.ed_email);
        EditText edPhone = root.findViewById(R.id.ed_phone);
        EditText edNewPass = root.findViewById(R.id.ed_new_password);
        EditText edConfirm = root.findViewById(R.id.ed_confirm_password);
        UserDao userDao = AppDatabase.getInstance(requireContext()).userDao();
        final User[] holder = new User[1];
        if (userId > 0) {
            Async.runIo(() -> {
                User u = userDao.findById(userId);
                holder[0] = u;
                Async.runMain(() -> {
                    if (u != null) {
                        tvName.setText(u.fullName != null && !u.fullName.trim().isEmpty() ? u.fullName : u.username);
                        edUsername.setText(u.username);
                        if (u.fullName != null) edFullName.setText(u.fullName);
                        if (u.email != null) edEmail.setText(u.email);
                        if (u.phone != null) edPhone.setText(u.phone);
                        if (tvEmailSubtitle != null) tvEmailSubtitle.setText(u.email != null ? u.email : "Tap edit to add your email");
                        if (tvMemberSince != null) tvMemberSince.setText(String.format(Locale.getDefault(), "User ID #%d", u.id));
                    }
                });
            });
        }

        setEditable(root, false, edUsername, edFullName, edEmail, edPhone, edNewPass, edConfirm);
        root.findViewById(R.id.btn_edit).setOnClickListener(v -> {
            boolean nowEditable = !edUsername.isEnabled();
            setEditable(root, nowEditable, edUsername, edFullName, edEmail, edPhone, edNewPass, edConfirm);
        });

        root.findViewById(R.id.btn_save).setOnClickListener(v -> {
            User u = holder[0];
            if (u == null) return;
            String newName = edUsername.getText().toString().trim();
            String newPass = edNewPass.getText().toString();
            String confirm = edConfirm.getText().toString();
            String fullName = edFullName.getText().toString().trim();
            String email = edEmail.getText().toString().trim();
            String phone = edPhone.getText().toString().trim();

            if (newName.isEmpty()) { Toast.makeText(requireContext(), "Username required", Toast.LENGTH_SHORT).show(); return; }
            Async.runIo(() -> {
                User existing = userDao.findByUsername(newName);
                if (existing != null && existing.id != u.id) {
                    Async.runMain(() -> Toast.makeText(requireContext(), "Username already exists", Toast.LENGTH_SHORT).show());
                    return;
                }
                u.username = newName;
                u.fullName = fullName.isEmpty()? null : fullName;
                u.email = email.isEmpty()? null : email;
                u.phone = phone.isEmpty()? null : phone;
                if (!newPass.isEmpty()) {
                    if (!newPass.equals(confirm)) {
                        Async.runMain(() -> Toast.makeText(requireContext(), "Passwords do not match", Toast.LENGTH_SHORT).show());
                        return;
                    }
                    u.passwordHash = PasswordHasher.sha256(newPass);
                }
                userDao.update(u);
                Async.runMain(() -> {
                    tvName.setText(u.fullName != null && !u.fullName.trim().isEmpty() ? u.fullName : u.username);
                    if (tvEmailSubtitle != null) tvEmailSubtitle.setText(u.email != null ? u.email : "Tap edit to add your email");
                    Toast.makeText(requireContext(), "Saved", Toast.LENGTH_SHORT).show();
                    setEditable(root, false, edUsername, edFullName, edEmail, edPhone, edNewPass, edConfirm);
                });
            });
        });

        SwitchMaterial swNotif = root.findViewById(R.id.sw_profile_notifications);
        SwitchMaterial swBudget = root.findViewById(R.id.sw_profile_budget_alerts);
        SettingsRepository prefs = new SettingsRepository(requireContext());
        if (swNotif != null) {
            swNotif.setChecked(prefs.notificationsEnabled());
            swNotif.setOnCheckedChangeListener((button, checked) -> prefs.setNotificationsEnabled(checked));
        }
        if (swBudget != null) {
            swBudget.setChecked(prefs.budgetAlertsEnabled());
            swBudget.setOnCheckedChangeListener((button, checked) -> prefs.setBudgetAlertsEnabled(checked));
        }

        View btnManageData = root.findViewById(R.id.btn_profile_manage_data);
        if (btnManageData != null) {
            btnManageData.setOnClickListener(v -> {
                if (requireActivity() instanceof MainActivity) {
                    ((MainActivity) requireActivity()).showFragment(ManageDataFragment.newInstance());
                }
            });
        }

        View btnContact = root.findViewById(R.id.btn_profile_contact);
        if (btnContact != null) {
            btnContact.setOnClickListener(v -> openSupportEmail());
        }

        View btnLogout = root.findViewById(R.id.btn_profile_logout);
        if (btnLogout != null) {
            btnLogout.setOnClickListener(v -> {
                Intent i = new Intent(requireContext(), LoginActivity.class);
                i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(i);
            });
        }

        if (userId > 0) {
            loadStats(userId, tvIncomeMonth, tvExpenseMonth, tvBudgetSummary);
        }

        return root;
    }

    private void setEditable(View root, boolean enabled, EditText... edits) {
        for (EditText e : edits) if (e != null) e.setEnabled(enabled);
        View save = root.findViewById(R.id.btn_save);
        if (save != null) save.setEnabled(enabled);
        TextView edit = root.findViewById(R.id.btn_edit);
        if (edit != null) edit.setText(enabled ? "Done" : "Edit");
    }

    private void loadStats(long userId, TextView tvIncome, TextView tvExpense, TextView tvBudget) {
        if (userId <= 0) return;
        final android.content.Context ctx = requireContext().getApplicationContext();
        Async.runIo(() -> {
            long now = System.currentTimeMillis();
            double incomeTotal = 0;
            double expenseTotal = 0;
            double budgetLimit = 0;
            double budgetUsed = 0;
            try {
                IncomeRepository incomeRepo = new IncomeRepository(ctx);
                for (Income income : incomeRepo.listForCurrentMonth(userId, now)) incomeTotal += income.amount;
            } catch (Throwable ignored) {}
            try {
                ExpenseRepository expenseRepo = new ExpenseRepository(ctx);
                for (Expense expense : expenseRepo.listForCurrentMonth(userId, now)) expenseTotal += expense.amount;
            } catch (Throwable ignored) {}
            try {
                int monthKey = MonthUtils.monthKey(now);
                BudgetRepository budgetRepo = new BudgetRepository(ctx);
                List<Budget> budgets = budgetRepo.listForMonth(userId, monthKey);
                for (Budget b : budgets) {
                    budgetLimit += b.limitAmount;
                }
                budgetUsed = expenseTotal;
            } catch (Throwable ignored) {}

            final double fIncome = incomeTotal;
            final double fExpense = expenseTotal;
            final double fBudgetLimit = budgetLimit;
            final double fBudgetUsed = budgetUsed;

            Async.runMain(() -> {
                if (!isAdded()) return;
                if (tvIncome != null) tvIncome.setText(String.format(Locale.getDefault(), "$%,.2f", fIncome));
                if (tvExpense != null) tvExpense.setText(String.format(Locale.getDefault(), "$%,.2f", fExpense));
                if (tvBudget != null) {
                    if (fBudgetLimit > 0) {
                        tvBudget.setText(String.format(Locale.getDefault(), "Budgets: $%,.2f of $%,.2f used", fBudgetUsed, fBudgetLimit));
                    } else {
                        tvBudget.setText("No budgets configured yet");
                    }
                }
            });
        });
    }

    private void openSupportEmail() {
        Intent email = new Intent(Intent.ACTION_SENDTO);
        email.setData(Uri.parse("mailto:"));
        email.putExtra(Intent.EXTRA_EMAIL, new String[]{"support@example.com"});
        email.putExtra(Intent.EXTRA_SUBJECT, "PFM Support");
        try {
            startActivity(Intent.createChooser(email, "Contact support"));
        } catch (Exception ignored) {
            Toast.makeText(requireContext(), "No email app installed", Toast.LENGTH_SHORT).show();
        }
    }
}
