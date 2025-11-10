package com.example.personalfinancialmanagement;

import android.os.Bundle;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.work.Data;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.ListenableWorker;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;

import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

public class MainActivity extends AppCompatActivity {
    private ExpenseRepository expenseRepository;
    private IncomeRepository incomeRepository;
    private BudgetRepository budgetRepository;
    private final java.util.concurrent.ExecutorService io = java.util.concurrent.Executors.newSingleThreadExecutor();
    private View btnHome, btnSavingsTab, btnNotify, btnSettings;
    private int systemBarsTop = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        SettingsRepository prefs = new SettingsRepository(this);
        int themeMode = prefs.themeMode();
        int appMode = androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM;
        if (themeMode == 1) appMode = androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_NO;
        else if (themeMode == 2) appMode = androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_YES;
        androidx.appcompat.app.AppCompatDelegate.setDefaultNightMode(appMode);

        View root = findViewById(R.id.main);
        ViewCompat.setOnApplyWindowInsetsListener(root, (v, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            systemBarsTop = bars.top;
            boolean inFragment = getSupportFragmentManager().getBackStackEntryCount() > 0;
            v.setPadding(bars.left, inFragment ? 0 : bars.top, bars.right, 0);
            View bottom = findViewById(R.id.bottom_bar);
            if (bottom != null) {
                bottom.setPadding(bottom.getPaddingLeft(), bottom.getPaddingTop(), bottom.getPaddingRight(), bars.bottom);
            }
            View fragmentContainer = findViewById(R.id.fragment_container);
            if (fragmentContainer != null) {
                fragmentContainer.setPadding(bars.left, bars.top, bars.right, 0);
            }
            return insets;
        });

        getSupportFragmentManager().addOnBackStackChangedListener(() -> {
            if (getSupportFragmentManager().getBackStackEntryCount() == 0) {
                View sc = findViewById(R.id.scroll);
                View cont = findViewById(R.id.fragment_container);
                View top = findViewById(R.id.top_bar);
                if (cont != null) cont.setVisibility(View.GONE);
                if (sc != null) sc.setVisibility(View.VISIBLE);
                if (top != null) top.setVisibility(View.VISIBLE);
                setNavSelected("home");
                applyRootTopInset();
                long uid = getIntent().getLongExtra("userId", -1);
                refreshOverview(uid);
            } else {
                View sc = findViewById(R.id.scroll);
                View cont = findViewById(R.id.fragment_container);
                View top = findViewById(R.id.top_bar);
                if (sc != null) sc.setVisibility(View.GONE);
                if (cont != null) cont.setVisibility(View.VISIBLE);
                if (top != null) top.setVisibility(View.GONE);
                applyRootTopInset();
            }
        });

        long userId = getIntent().getLongExtra("userId", -1);
        boolean openSettings = getIntent().getBooleanExtra("openSettingsOnCreate", false);

        expenseRepository = new ExpenseRepository(this);
        incomeRepository = new IncomeRepository(this);
        budgetRepository = new BudgetRepository(this);

        RecyclerView rv = findViewById(R.id.rv_latest);
        if (rv != null) {
            rv.setLayoutManager(new LinearLayoutManager(this));
            rv.setHasFixedSize(true);
            rv.setItemViewCacheSize(24);
            rv.setAdapter(new CombinedEntryAdapter());
        }

        if (openSettings) {
            showFragment(new SettingsFragment());
            try { getIntent().removeExtra("openSettingsOnCreate"); } catch (Throwable ignored) {}
        } else {
            refreshOverview(userId);
            setNavSelected("home");
        }

        View fab = findViewById(R.id.btn_add);
        if (fab != null) {
            fab.setOnClickListener(v -> showFragment(AddFragment.newInstance(userId)));
        }

        View btnLatestOverflow = findViewById(R.id.btn_more);
        if (btnLatestOverflow != null) {
            long finalUserId = userId;
            btnLatestOverflow.setOnClickListener(v -> showFragment(EntriesFragment.newInstance(finalUserId)));
        }

        btnHome = findViewById(R.id.btn_home);
        if (btnHome != null) {
            btnHome.setOnClickListener(v -> {
                FragmentManager fm = getSupportFragmentManager();
                if (fm.getBackStackEntryCount() > 0) {
                    fm.popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE);
                }
                View cont = findViewById(R.id.fragment_container);
                View sc = findViewById(R.id.scroll);
                View top = findViewById(R.id.top_bar);
                if (cont != null) cont.setVisibility(View.GONE);
                if (top != null) top.setVisibility(View.VISIBLE);
                if (sc != null) {
                    sc.setVisibility(View.VISIBLE);
                    if (sc instanceof androidx.core.widget.NestedScrollView) {
                        ((androidx.core.widget.NestedScrollView) sc).smoothScrollTo(0, 0);
                    }
                }
                setNavSelected("home");
                applyRootTopInset();
                long uid = getIntent().getLongExtra("userId", -1);
                refreshOverview(uid);
            });
        }

        btnNotify = findViewById(R.id.btn_notify);
        if (btnNotify != null) {
            btnNotify.setOnClickListener(v -> {
                long uid = getIntent().getLongExtra("userId", -1);
                showFragment(NotificationsFragment.newInstance(uid));
            });
        }

        btnSettings = findViewById(R.id.btn_settings);
        if (btnSettings != null) {
            btnSettings.setOnClickListener(v -> showFragment(new SettingsFragment()));
        }

        btnSavingsTab = findViewById(R.id.btn_savings_tab);
        if (btnSavingsTab != null) {
            long finalUserId = userId;
            btnSavingsTab.setOnClickListener(v -> showFragment(SavingsFragment.newInstance(finalUserId)));
        }

        scheduleBudgetWork(userId);
        scheduleDailyNotifications(userId);
    }

    private void scheduleBudgetWork(long userId) {
        Data data = new Data.Builder().putLong(BudgetCheckerWorker.KEY_USER_ID, userId).build();
        PeriodicWorkRequest work = new PeriodicWorkRequest.Builder(BudgetCheckerWorker.class, 12, TimeUnit.HOURS)
                .setInputData(data)
                .build();
        WorkManager.getInstance(this).enqueueUniquePeriodicWork("budget-check", ExistingPeriodicWorkPolicy.UPDATE, work);

        Data recurData = new Data.Builder().putLong(RecurringApplierWorker.KEY_USER_ID, userId).build();
        PeriodicWorkRequest recur = new PeriodicWorkRequest.Builder(RecurringApplierWorker.class, 24, TimeUnit.HOURS)
                .setInputData(recurData)
                .build();
        WorkManager.getInstance(this).enqueueUniquePeriodicWork("recurring-apply", ExistingPeriodicWorkPolicy.UPDATE, recur);

        Data goalData = new Data.Builder().putLong(GoalAutoContributionWorker.KEY_USER_ID, userId).build();
        PeriodicWorkRequest goalWork = new PeriodicWorkRequest.Builder(GoalAutoContributionWorker.class, 24, TimeUnit.HOURS)
                .setInputData(goalData)
                .build();
        WorkManager.getInstance(this).enqueueUniquePeriodicWork("goal-auto-contrib", ExistingPeriodicWorkPolicy.UPDATE, goalWork);
    }

    public void showFragment(Fragment f) {
        View scroll = findViewById(R.id.scroll);
        View container = findViewById(R.id.fragment_container);
        View top = findViewById(R.id.top_bar);
        if (scroll != null) scroll.setVisibility(View.GONE);
        if (container != null) {
            container.setVisibility(View.VISIBLE);
            container.setPadding(container.getPaddingLeft(), systemBarsTop, container.getPaddingRight(), container.getPaddingBottom());
        }
        if (top != null) top.setVisibility(View.GONE);
        try {
            getSupportFragmentManager()
                    .beginTransaction()
                    .setCustomAnimations(R.anim.fade_in, R.anim.fade_out, R.anim.fade_in, R.anim.fade_out)
                    .replace(R.id.fragment_container, f)
                    .addToBackStack(null)
                    .commitAllowingStateLoss();
        } catch (IllegalStateException ignored) {}
        if (f instanceof SavingsFragment) setNavSelected("savings");
        else if (f instanceof SettingsFragment) setNavSelected("settings");
        else if (f instanceof AddFragment) setNavSelected("add");
        else if (f instanceof NotificationsFragment) setNavSelected("notify");
        applyRootTopInset();
        container.post(() -> {
            if (container != null) {
                container.setPadding(container.getPaddingLeft(), systemBarsTop, container.getPaddingRight(), container.getPaddingBottom());
            }
        });
    }

    private void setNavSelected(String key) {
        int active = 0xFF2962FF;
        try { active = getResources().getColor(R.color.primaryBlue, getTheme()); } catch (Exception ignored) {}
        int inactive = 0xFF8A8A8A;
        try { inactive = getResources().getColor(R.color.textSecondary, getTheme()); } catch (Exception ignored) {}

        android.content.res.ColorStateList on = android.content.res.ColorStateList.valueOf(active);
        android.content.res.ColorStateList off = android.content.res.ColorStateList.valueOf(inactive);

        if (btnHome instanceof android.widget.ImageButton) ((android.widget.ImageButton) btnHome).setImageTintList("home".equals(key) ? on : off);
        if (btnSavingsTab instanceof android.widget.ImageButton) ((android.widget.ImageButton) btnSavingsTab).setImageTintList("savings".equals(key) ? on : off);
        if (btnSettings instanceof android.widget.ImageButton) ((android.widget.ImageButton) btnSettings).setImageTintList("settings".equals(key) ? on : off);
        if (btnNotify instanceof android.widget.ImageButton) ((android.widget.ImageButton) btnNotify).setImageTintList("notify".equals(key) ? on : off);
    }

    private void applyRootTopInset() {
        View root = findViewById(R.id.main);
        if (root == null) return;
        boolean inFragment = getSupportFragmentManager().getBackStackEntryCount() > 0;
        int topPad = inFragment ? 0 : systemBarsTop;
        root.setPadding(root.getPaddingLeft(), topPad, root.getPaddingRight(), 0);
        View fragmentContainer = findViewById(R.id.fragment_container);
        if (fragmentContainer != null) {
            if (inFragment) {
                fragmentContainer.setPadding(fragmentContainer.getPaddingLeft(), systemBarsTop, fragmentContainer.getPaddingRight(), fragmentContainer.getPaddingBottom());
            } else {
                fragmentContainer.setPadding(fragmentContainer.getPaddingLeft(), 0, fragmentContainer.getPaddingRight(), fragmentContainer.getPaddingBottom());
            }
        }
    }

    @Override protected void onResume() {
        super.onResume();
        long userId = getIntent().getLongExtra("userId", -1);
        refreshOverview(userId);
    }

    public void refreshOverview(long userId) {
        io.execute(() -> {
            double totalExpense = 0;
            double totalIncome = 0;
            double budgetTotal = 0;
            List<Expense> expenseList = java.util.Collections.emptyList();
            List<Income> incomeList = java.util.Collections.emptyList();
            long now = System.currentTimeMillis();
            if (userId > 0) {
                expenseList = expenseRepository.listForCurrentMonth(userId, now);
                for (Expense e : expenseList) totalExpense += e.amount;
                incomeList = incomeRepository.listForCurrentMonth(userId, now);
                for (Income i : incomeList) totalIncome += i.amount;
                int monthKey = MonthUtils.monthKey(now);
                for (Budget b : budgetRepository.listForMonth(userId, monthKey)) budgetTotal += b.limitAmount;
            }

            final double fTotalExpense = totalExpense;
            final double fTotalIncome = totalIncome;
            final double fBudgetTotal = budgetTotal;
            final List<Expense> fExp = expenseList;
            final List<Income> fInc = incomeList;
            runOnUiThread(() -> {
                Locale locale = Locale.getDefault();
                java.text.SimpleDateFormat monthFmt = new java.text.SimpleDateFormat("MMMM yyyy", locale);
                java.text.SimpleDateFormat timeFmt = new java.text.SimpleDateFormat("MMM d, h:mm a", locale);
                double net = fTotalIncome - fTotalExpense;

                View avatar = findViewById(R.id.iv_avatar);
                if (avatar != null) {
                    long uid = userId;
                    avatar.setOnClickListener(v -> showFragment(ProfileFragment.newInstance(uid)));
                }

                TextView tvMonth = findViewById(R.id.tv_overview_month_label);
                if (tvMonth != null) tvMonth.setText(monthFmt.format(new java.util.Date(now)));

                TextView tvBalance = findViewById(R.id.tv_overview_balance_amount);
                if (tvBalance != null) tvBalance.setText(String.format(locale, "%s$%,.2f", net < 0 ? "-" : "", Math.abs(net)));

                TextView tvBalanceHint = findViewById(R.id.tv_overview_balance_hint);
                if (tvBalanceHint != null) {
                    tvBalanceHint.setText(net >= 0
                            ? "Great! Income exceeds expense this month."
                            : "Spending is higher than income. Review recent expenses.");
                }

                TextView tvIncome = findViewById(R.id.tv_overview_income_amount);
                if (tvIncome != null) tvIncome.setText(String.format(locale, "$%,.2f", fTotalIncome));

                TextView tvExpense = findViewById(R.id.tv_overview_expense_amount);
                if (tvExpense != null) tvExpense.setText(String.format(locale, "$%,.2f", fTotalExpense));

                TextView tvLastRefresh = findViewById(R.id.tv_overview_last_refresh);
                if (tvLastRefresh != null) tvLastRefresh.setText("Last updated " + timeFmt.format(new java.util.Date(now)));

                ProgressBar progressBudget = findViewById(R.id.progress_budget);
                TextView tvBudgetSummary = findViewById(R.id.tv_budget_summary);
                TextView tvBudgetRemaining = findViewById(R.id.tv_budget_remaining);
                if (progressBudget != null) {
                    int progress = 0;
                    if (fBudgetTotal > 0) {
                        progress = (int) Math.max(0, Math.min(100, Math.round((fTotalExpense / fBudgetTotal) * 100)));
                    }
                    progressBudget.setProgress(progress);
                }
                if (tvBudgetSummary != null) {
                    if (fBudgetTotal > 0) {
                        tvBudgetSummary.setText(String.format(locale, "$%,.2f of $%,.2f used", fTotalExpense, fBudgetTotal));
                    } else {
                        tvBudgetSummary.setText("No budgets set");
                    }
                }
                if (tvBudgetRemaining != null) {
                    if (fBudgetTotal > 0) {
                        double remaining = fBudgetTotal - fTotalExpense;
                        String label = remaining >= 0 ? "Remaining" : "Over by";
                        tvBudgetRemaining.setText(String.format(locale, "%s $%,.2f", label, Math.abs(remaining)));
                    } else {
                        tvBudgetRemaining.setText("Create budgets to track limits.");
                    }
                }

                TextView tvActivitySummary = findViewById(R.id.tv_activity_summary);
                TextView tvActivityAmount = findViewById(R.id.tv_activity_amount);
                TextView tvActivityHint = findViewById(R.id.tv_activity_hint);

                RecyclerView rv2 = findViewById(R.id.rv_latest);
                TextView tvEmpty = findViewById(R.id.tv_overview_empty);
                if (rv2 != null && rv2.getAdapter() instanceof CombinedEntryAdapter) {
                    java.util.ArrayList<CombinedEntryAdapter.Item> items = new java.util.ArrayList<>();
                    for (Expense e : fExp) items.add(new CombinedEntryAdapter.Item(false, e.dateUtc, e.description, e.amount, e.category));
                    for (Income i : fInc) items.add(new CombinedEntryAdapter.Item(true, i.dateUtc, i.title, i.amount, i.category));
                    items.sort((a, b) -> Long.compare(b.time, a.time));
                    if (items.size() > 15) items = new java.util.ArrayList<>(items.subList(0, 15));
                    ((CombinedEntryAdapter) rv2.getAdapter()).submit(items);
                    if (tvEmpty != null) tvEmpty.setVisibility(items.isEmpty() ? View.VISIBLE : View.GONE);

                    if (tvActivitySummary != null && tvActivityAmount != null && tvActivityHint != null) {
                        if (!items.isEmpty()) {
                            CombinedEntryAdapter.Item top = items.get(0);
                            tvActivitySummary.setText(top.income ? "Latest income" : "Latest expense");
                            tvActivityAmount.setText(String.format(locale, "%s $%,.2f", top.income ? "+" : "-", top.amount));
                            String description = (top.title == null || top.title.trim().isEmpty()) ? "Unnamed entry" : top.title;
                            tvActivityHint.setText(String.format(locale, "%s • %s",
                                    description,
                                    timeFmt.format(new java.util.Date(top.time))));
                        } else {
                            tvActivitySummary.setText("No recent entries");
                            tvActivityAmount.setText("$0.00");
                            tvActivityHint.setText("Add an income or expense to build your history.");
                        }
                    }
                }
            });
        });
    }

    private void scheduleDailyNotifications(long userId) {
        if (userId <= 0) return;
        Data morningData = new Data.Builder().putLong(DailyGreetingWorker.KEY_USER_ID, userId).build();
        enqueueDailyWorker("daily-morning-greeting", DailyGreetingWorker.class, 6, 0, morningData);

        Data eveningData = new Data.Builder().putLong(DailySummaryWorker.KEY_USER_ID, userId).build();
        enqueueDailyWorker("daily-evening-summary", DailySummaryWorker.class, 20, 0, eveningData);
    }

    private void enqueueDailyWorker(String uniqueName, Class<? extends ListenableWorker> workerClass, int hourOfDay, int minute, Data data) {
        long now = System.currentTimeMillis();
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(now);
        calendar.set(Calendar.HOUR_OF_DAY, hourOfDay);
        calendar.set(Calendar.MINUTE, minute);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        long triggerAt = calendar.getTimeInMillis();
        if (triggerAt <= now) {
            triggerAt += TimeUnit.DAYS.toMillis(1);
        }
        long initialDelay = triggerAt - now;

        PeriodicWorkRequest request = new PeriodicWorkRequest.Builder(workerClass, 24, TimeUnit.HOURS)
                .setInputData(data)
                .setInitialDelay(initialDelay, TimeUnit.MILLISECONDS)
                .build();

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(uniqueName, ExistingPeriodicWorkPolicy.UPDATE, request);
    }

    @Override protected void onDestroy() {
        super.onDestroy();
        io.shutdownNow();
    }
}
