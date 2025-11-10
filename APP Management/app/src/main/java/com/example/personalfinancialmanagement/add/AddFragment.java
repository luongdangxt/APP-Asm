package com.example.personalfinancialmanagement;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.core.view.ViewCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class AddFragment extends Fragment {
    public static final String ARG_USER_ID = "userId";
    private long userId;
    private ExpenseRepository expenseRepository;
    private View cardAddIncome;
    private View cardAddExpense;
    private ImageView iconAddIncome;
    private ImageView iconAddExpense;
    private TextView tvAddIncomeTitle;
    private TextView tvAddIncomeSubtitle;
    private TextView tvAddExpenseTitle;
    private TextView tvAddExpenseSubtitle;
    private Action currentAction = Action.EXPENSE;

    private enum Action { INCOME, EXPENSE }

    public static AddFragment newInstance(long userId) {
        AddFragment f = new AddFragment();
        Bundle b = new Bundle();
        b.putLong(ARG_USER_ID, userId);
        f.setArguments(b);
        return f;
    }

    @Nullable @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.activity_add, container, false);
        userId = getArguments() != null ? getArguments().getLong(ARG_USER_ID, -1) : -1;
        expenseRepository = new ExpenseRepository(requireContext());

        ImageButton back = root.findViewById(R.id.btn_back);
        if (back != null) back.setVisibility(View.GONE);

        final int baseL = root.getPaddingLeft();
        final int baseT = root.getPaddingTop();
        final int baseR = root.getPaddingRight();
        final int baseB = root.getPaddingBottom();
        ViewCompat.setOnApplyWindowInsetsListener(root, (v, insets) -> {
            androidx.core.graphics.Insets bars = insets.getInsets(androidx.core.view.WindowInsetsCompat.Type.systemBars());
            v.setPadding(baseL, baseT, baseR, baseB + bars.bottom);
            return insets;
        });

        cardAddIncome = root.findViewById(R.id.btn_add_income);
        cardAddExpense = root.findViewById(R.id.btn_add_expense);
        iconAddIncome = root.findViewById(R.id.icon_add_income);
        iconAddExpense = root.findViewById(R.id.icon_add_expense);
        tvAddIncomeTitle = root.findViewById(R.id.tv_add_income_title);
        tvAddIncomeSubtitle = root.findViewById(R.id.tv_add_income_subtitle);
        tvAddExpenseTitle = root.findViewById(R.id.tv_add_expense_title);
        tvAddExpenseSubtitle = root.findViewById(R.id.tv_add_expense_subtitle);

        if (cardAddIncome != null) {
            cardAddIncome.setOnClickListener(v -> {
                if (currentAction == Action.INCOME) {
                    openAddIncome(-1);
                } else {
                    setActiveAction(Action.INCOME);
                }
            });
        }
        if (cardAddExpense != null) {
            cardAddExpense.setOnClickListener(v -> {
                if (currentAction == Action.EXPENSE) {
                    openAddExpense(-1);
                } else {
                    setActiveAction(Action.EXPENSE);
                }
            });
        }

        View quick50 = root.findViewById(R.id.chip_quick_50);
        if (quick50 != null) quick50.setOnClickListener(v -> triggerQuickAmount(50));
        View quick100 = root.findViewById(R.id.chip_quick_100);
        if (quick100 != null) quick100.setOnClickListener(v -> triggerQuickAmount(100));
        View quickCustom = root.findViewById(R.id.chip_quick_custom);
        if (quickCustom != null) quickCustom.setOnClickListener(v -> triggerQuickAmount(-1));

        RecyclerView rv = root.findViewById(R.id.rv_latest);
        CombinedEntryAdapter adapter = new CombinedEntryAdapter();
        rv.setLayoutManager(new LinearLayoutManager(requireContext()));
        rv.setAdapter(adapter);
        refreshLatest(adapter);
        TextView title = root.findViewById(R.id.tv_latest_title);
        if (title != null) title.setText("Latest activity");

        ImageButton more = root.findViewById(R.id.btn_more);
        if (more != null) more.setOnClickListener(v -> {
            if (requireActivity() instanceof MainActivity) ((MainActivity) requireActivity()).showFragment(EntriesFragment.newInstance(userId));
        });

        setActiveAction(Action.EXPENSE);
        return root;
    }

    @Override public void onResume() {
        super.onResume();
        View root = getView();
        if (root != null) {
            RecyclerView rv = root.findViewById(R.id.rv_latest);
            if (rv != null && rv.getAdapter() instanceof CombinedEntryAdapter) {
                refreshLatest((CombinedEntryAdapter) rv.getAdapter());
            }
            updateActionStyles();
        }
    }

    private void refreshLatest(CombinedEntryAdapter adapter) {
        final android.content.Context ctx = getContext();
        if (ctx == null) return;
        final android.content.Context appCtx = ctx.getApplicationContext();
        Async.runIo(() -> {
            List<Expense> ex = expenseRepository.latest(userId, 20);
            List<Income> in = new IncomeRepository(appCtx).latest(userId, 20);
            java.util.ArrayList<CombinedEntryAdapter.Item> items = new java.util.ArrayList<>();
            for (Expense e : ex) items.add(new CombinedEntryAdapter.Item(false, e.dateUtc, e.description, e.amount, e.category));
            for (Income ic : in) items.add(new CombinedEntryAdapter.Item(true, ic.dateUtc, ic.title, ic.amount, ic.category));
            items.sort((a,b) -> Long.compare(b.time, a.time));
            if (items.size() > 20) items = new java.util.ArrayList<>(items.subList(0, 20));
            java.util.ArrayList<CombinedEntryAdapter.Item> finalItems = items;
            Async.runMain(() -> {
                if (!isAdded()) return;
                adapter.submit(finalItems);
                View rootView = getView();
                if (rootView != null) {
                    TextView empty = rootView.findViewById(R.id.tv_empty_state);
                    if (empty != null) {
                        empty.setVisibility(finalItems.isEmpty() ? View.VISIBLE : View.GONE);
                    }
                }
            });
        });
    }

    private void triggerQuickAmount(int amount) {
        if (!(requireActivity() instanceof MainActivity)) return;
        boolean isExpense = currentAction == Action.EXPENSE;
        if (amount < 0) {
            if (isExpense) openAddExpense(-1); else openAddIncome(-1);
            return;
        }
        String label = isExpense ? "expense" : "income";
        Toast.makeText(requireContext(), "Quick add $" + amount + " " + label, Toast.LENGTH_SHORT).show();
        if (isExpense) openAddExpense(amount); else openAddIncome(amount);
    }

    private void openAddExpense(double presetAmount) {
        if (!(requireActivity() instanceof MainActivity)) return;
        MainActivity main = (MainActivity) requireActivity();
        AddExpenseFragment fragment = presetAmount > 0 ? AddExpenseFragment.newInstance(userId, presetAmount) : AddExpenseFragment.newInstance(userId);
        main.showFragment(fragment);
    }

    private void openAddIncome(double presetAmount) {
        if (!(requireActivity() instanceof MainActivity)) return;
        MainActivity main = (MainActivity) requireActivity();
        AddIncomeFragment fragment = presetAmount > 0 ? AddIncomeFragment.newInstance(userId, presetAmount) : AddIncomeFragment.newInstance(userId);
        main.showFragment(fragment);
    }

    private void setActiveAction(Action action) {
        currentAction = action;
        updateActionStyles();
    }

    private void updateActionStyles() {
        if (!isAdded()) return;
        int primaryBlue = ContextCompat.getColor(requireContext(), R.color.primaryBlue);
        int textPrimary = ContextCompat.getColor(requireContext(), R.color.textPrimary);
        int textSecondary = ContextCompat.getColor(requireContext(), R.color.textSecondary);
        int white = ContextCompat.getColor(requireContext(), android.R.color.white);

        if (cardAddExpense != null) {
            boolean selected = currentAction == Action.EXPENSE;
            cardAddExpense.setBackgroundResource(selected ? R.drawable.shape_pill_blue : R.drawable.shape_pill_white);
            if (tvAddExpenseTitle != null) tvAddExpenseTitle.setTextColor(selected ? white : textPrimary);
            if (tvAddExpenseSubtitle != null) tvAddExpenseSubtitle.setTextColor(selected ? white : textSecondary);
            if (iconAddExpense != null) iconAddExpense.setColorFilter(selected ? white : primaryBlue);
        }

        if (cardAddIncome != null) {
            boolean selected = currentAction == Action.INCOME;
            cardAddIncome.setBackgroundResource(selected ? R.drawable.shape_pill_blue : R.drawable.shape_pill_white);
            if (tvAddIncomeTitle != null) tvAddIncomeTitle.setTextColor(selected ? white : textPrimary);
            if (tvAddIncomeSubtitle != null) tvAddIncomeSubtitle.setTextColor(selected ? white : textSecondary);
            if (iconAddIncome != null) iconAddIncome.setColorFilter(selected ? white : primaryBlue);
        }
    }
}
