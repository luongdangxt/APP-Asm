package com.example.personalfinancialmanagement;

import android.graphics.Color;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowInsetsController;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.widget.NestedScrollView;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.personalfinancialmanagement.Async;
import com.example.personalfinancialmanagement.Budget;
import com.example.personalfinancialmanagement.BudgetAlertManager;
import com.example.personalfinancialmanagement.BudgetAlertTracker;
import com.example.personalfinancialmanagement.BudgetRepository;
import com.example.personalfinancialmanagement.CategoryPreferences;
import com.example.personalfinancialmanagement.ExpenseRepository;
import com.example.personalfinancialmanagement.MonthUtils;
import com.example.personalfinancialmanagement.R;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.MaterialAutoCompleteTextView;
import com.google.android.material.textfield.TextInputEditText;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;

public class ManageBudgetsActivity extends AppCompatActivity {
    public static final String EXTRA_USER_ID = "userId";

    private BudgetRepository budgetRepository;
    private ExpenseRepository expenseRepository;
    private CategoryPreferences categoryPreferences;
    private long userId;
    private int monthKey;

    private MaterialAutoCompleteTextView inputCategory;
    private TextInputEditText inputLimit;
    private RecyclerView rvBudgets;
    private TextView tvEmpty;
    private BudgetAdapter adapter;
    private View topBar;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        getWindow().setStatusBarColor(Color.TRANSPARENT);
        getWindow().setNavigationBarColor(Color.TRANSPARENT);
        WindowInsetsController controller = getWindow().getInsetsController();
        if (controller != null) {
            controller.setSystemBarsAppearance(0, WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS);
            controller.setSystemBarsAppearance(0, WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS);
        }
        setContentView(R.layout.activity_budget);

        userId = getIntent().getLongExtra(EXTRA_USER_ID, -1);
        if (userId <= 0) {
            Toast.makeText(this, getString(R.string.budget_missing_account), Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        monthKey = MonthUtils.monthKey(System.currentTimeMillis());
        budgetRepository = new BudgetRepository(this);
        expenseRepository = new ExpenseRepository(this);
        categoryPreferences = new CategoryPreferences(this);

        topBar = findViewById(R.id.top_bar);
        ImageButton btnBack = findViewById(R.id.btn_back);
        TextView tvTitle = findViewById(R.id.tv_budget_title);
        if (tvTitle != null) tvTitle.setText(R.string.budget_manage_title);
        if (btnBack != null) btnBack.setOnClickListener(v -> finish());
        inputCategory = findViewById(R.id.input_budget_category);
        inputLimit = findViewById(R.id.ed_limit);
        rvBudgets = findViewById(R.id.rv_budgets);
        tvEmpty = findViewById(R.id.tv_budget_empty);
        MaterialButton btnSave = findViewById(R.id.btn_save_budget);

        setupCategoryDropdown();

        if (rvBudgets != null) {
            rvBudgets.setLayoutManager(new LinearLayoutManager(this));
            adapter = new BudgetAdapter();
            rvBudgets.setAdapter(adapter);
            rvBudgets.setNestedScrollingEnabled(false);
        }

        if (btnSave != null) {
            btnSave.setOnClickListener(v -> saveBudget());
        }

        setupInsets();
    }

    private void setupInsets() {
        View root = findViewById(R.id.budget_root);
        NestedScrollView scroll = findViewById(R.id.budget_scroll);
        if (root != null && topBar != null && scroll != null) {
            final int topPaddingStart = topBar.getPaddingStart();
            final int topPaddingTop = topBar.getPaddingTop();
            final int topPaddingEnd = topBar.getPaddingEnd();
            final int topPaddingBottom = topBar.getPaddingBottom();
            final int scrollPadStart = scroll.getPaddingStart();
            final int scrollPadTop = scroll.getPaddingTop();
            final int scrollPadEnd = scroll.getPaddingEnd();
            final int scrollPadBottom = scroll.getPaddingBottom();
            ViewCompat.setOnApplyWindowInsetsListener(root, (v, insets) -> {
                Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
                topBar.setPadding(
                        topPaddingStart + bars.left,
                        topPaddingTop + bars.top,
                        topPaddingEnd + bars.right,
                        topPaddingBottom);
                scroll.setPadding(
                        scrollPadStart + bars.left,
                        scrollPadTop + bars.top,
                        scrollPadEnd + bars.right,
                        scrollPadBottom + bars.bottom);
                return insets;
            });
            ViewCompat.requestApplyInsets(root);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadBudgets();
    }

    private void setupCategoryDropdown() {
        if (inputCategory == null) return;
        LinkedHashSet<String> categories = new LinkedHashSet<>();
        String[] defaults = getResources().getStringArray(R.array.default_budget_categories);
        for (String item : defaults) {
            categories.add(item);
        }
        List<String> custom = categoryPreferences.getCustomExpenseCategories();
        for (String item : custom) categories.add(item);
        ArrayList<String> data = new ArrayList<>(categories);
        android.widget.ArrayAdapter<String> adapter = new android.widget.ArrayAdapter<>(this, android.R.layout.simple_list_item_1, data);
        inputCategory.setAdapter(adapter);
        inputCategory.setText(null, false);
    }

    private void saveBudget() {
        final String category = CategoryPreferences.sanitizeLabel(inputCategory != null ? inputCategory.getText().toString() : "");
        if (TextUtils.isEmpty(category)) {
            if (inputCategory != null) inputCategory.setError("Enter a category");
            return;
        }
        String raw = "";
        if (inputLimit != null && inputLimit.getText() != null) {
            raw = inputLimit.getText().toString().replace(",", "").trim();
        }
        double limit = 0;
        try {
            limit = Double.parseDouble(raw);
        } catch (Exception ignored) {}
        if (limit <= 0) {
            if (inputLimit != null) inputLimit.setError("Enter a positive amount");
            return;
        }
        final double fLimit = limit;
        Async.runIo(() -> {
            budgetRepository.upsert(userId, monthKey, category, fLimit);
            BudgetAlertTracker tracker = new BudgetAlertTracker(this);
            tracker.reset(userId, monthKey, category);
            BudgetAlertManager.reEvaluate(this, userId, category, monthKey);
            Async.runMain(() -> {
                if (inputLimit != null) inputLimit.setText("");
                Toast.makeText(this, getString(R.string.budget_saved), Toast.LENGTH_SHORT).show();
                loadBudgets();
            });
        });
    }

    private void loadBudgets() {
        Async.runIo(() -> {
            List<Budget> budgets = budgetRepository.listForMonth(userId, monthKey);
            ArrayList<BudgetItem> display = new ArrayList<>();
            for (Budget b : budgets) {
                double spent = expenseRepository.sumForCategory(userId, b.category, monthKey);
                display.add(new BudgetItem(b, spent));
            }
            Async.runMain(() -> {
                if (adapter != null) adapter.submit(display);
                updateEmptyState(display.isEmpty());
            });
        });
    }

    private void updateEmptyState(boolean empty) {
        if (tvEmpty != null) {
            tvEmpty.setVisibility(empty ? View.VISIBLE : View.GONE);
        }
    }

    private class BudgetAdapter extends RecyclerView.Adapter<BudgetAdapter.VH> {
        private final List<BudgetItem> items = new ArrayList<>();
        private final NumberFormat currency = NumberFormat.getCurrencyInstance(Locale.getDefault());

        void submit(List<BudgetItem> data) {
            items.clear();
            if (data != null) items.addAll(data);
            notifyDataSetChanged();
        }

        @Override
        public VH onCreateViewHolder(ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_budget_entry, parent, false);
            return new VH(v);
        }

        @Override
        public void onBindViewHolder(VH holder, int position) {
            BudgetItem item = items.get(position);
            holder.category.setText(item.category);
            holder.amounts.setText(String.format(Locale.getDefault(), "%s / %s",
                    currency.format(item.spent), currency.format(item.limit)));
            double remaining = item.limit - item.spent;
            String status = remaining >= 0
                    ? String.format(Locale.getDefault(), "Remaining %s", currency.format(remaining))
                    : String.format(Locale.getDefault(), "Over by %s", currency.format(Math.abs(remaining)));
            holder.status.setText(status);
            holder.delete.setOnClickListener(v -> deleteBudget(item));
        }

        @Override
        public int getItemCount() {
            return items.size();
        }

        private void deleteBudget(BudgetItem item) {
            Async.runIo(() -> {
                budgetRepository.delete(userId, item.monthKey, item.category);
                new BudgetAlertTracker(ManageBudgetsActivity.this).reset(userId, item.monthKey, item.category);
                Async.runMain(() -> {
                    Toast.makeText(ManageBudgetsActivity.this, getString(R.string.budget_removed), Toast.LENGTH_SHORT).show();
                    loadBudgets();
                });
            });
        }

        class VH extends RecyclerView.ViewHolder {
            final TextView category;
            final TextView amounts;
            final TextView status;
            final ImageButton delete;
            VH(View itemView) {
                super(itemView);
                category = itemView.findViewById(R.id.tv_budget_category);
                amounts = itemView.findViewById(R.id.tv_budget_amounts);
                status = itemView.findViewById(R.id.tv_budget_status);
                delete = itemView.findViewById(R.id.btn_delete_budget);
            }
        }
    }

    private static class BudgetItem {
        final long id;
        final String category;
        final double limit;
        final double spent;
        final int monthKey;

        BudgetItem(Budget budget, double spent) {
            this.id = budget.id;
            this.category = budget.category;
            this.limit = budget.limitAmount;
            this.spent = spent;
            this.monthKey = budget.monthKey;
        }
    }
}
