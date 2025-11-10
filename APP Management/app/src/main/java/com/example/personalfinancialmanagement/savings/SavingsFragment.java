package com.example.personalfinancialmanagement;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.text.DateFormatSymbols;
import java.util.Calendar;
import java.util.List;

public class SavingsFragment extends Fragment {
    public static final String ARG_USER_ID = "userId";

    private SavingsRepository repo;
    private long userId;

    private TextView tvCurrentMonthSavings;
    private TextView tvMonthLabel;
    private TextView tvGoalAmount;
    private TextView tvGoalTarget;
    private TextView tvMonthIncome;
    private TextView tvMonthExpense;
    private TextView tvAutoContributions;
    private TextView tvLeftoverMessage;
    private TextView tvLeftoverHint;
    private TextView tvGoalGuidance;
    private RecyclerView rvGoals;
    private double leftoverSuggestion = 0;
    private View goalBarContainer; private View goalBarFill; private int lastGoalFillW = -1;

    public static SavingsFragment newInstance(long userId) {
        SavingsFragment f = new SavingsFragment();
        Bundle b = new Bundle();
        b.putLong(ARG_USER_ID, userId);
        f.setArguments(b);
        return f;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.activity_savings, container, false);

        if (root instanceof androidx.coordinatorlayout.widget.CoordinatorLayout) {
            View scrollView = ((androidx.coordinatorlayout.widget.CoordinatorLayout) root).getChildAt(0);
            if (scrollView != null) {
                final int baseL = scrollView.getPaddingLeft();
                final int baseT = scrollView.getPaddingTop();
                final int baseR = scrollView.getPaddingRight();
                final int baseB = scrollView.getPaddingBottom();
                ViewCompat.setOnApplyWindowInsetsListener(scrollView, (v, insets) -> {
                    Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
                    v.setPadding(baseL, baseT, baseR, baseB + bars.bottom);
                    return insets;
                });
            }
        }

        Bundle args = getArguments();
        userId = args != null ? args.getLong(ARG_USER_ID, -1) : -1;
        repo = new SavingsRepository(requireContext());

        tvCurrentMonthSavings = root.findViewById(R.id.tv_current_month_savings);
        tvMonthLabel = root.findViewById(R.id.tv_month_label);
        tvGoalAmount = root.findViewById(R.id.tv_goal_amount);
        tvGoalTarget = root.findViewById(R.id.tv_goal_target);
        tvMonthIncome = root.findViewById(R.id.tv_month_income);
        tvMonthExpense = root.findViewById(R.id.tv_month_expense);
        tvAutoContributions = root.findViewById(R.id.tv_auto_contributions);
        tvLeftoverMessage = root.findViewById(R.id.tv_leftover_message);
        tvLeftoverHint = root.findViewById(R.id.tv_leftover_hint);
        tvGoalGuidance = root.findViewById(R.id.tv_goal_guidance);
        rvGoals = root.findViewById(R.id.rv_goals);
        rvGoals.setLayoutManager(new LinearLayoutManager(requireContext()));
        rvGoals.setHasFixedSize(true);
        rvGoals.setItemViewCacheSize(20);

        SavingsGoalAdapter adapter = new SavingsGoalAdapter(goal -> showGoalActions(goal));
        rvGoals.setAdapter(adapter);

        View back = root.findViewById(R.id.btn_back);
        if (back != null) back.setVisibility(View.GONE);
        View addGoal = root.findViewById(R.id.btn_add_goal);
        if (addGoal != null) addGoal.setOnClickListener(v -> {
            if (requireActivity() instanceof MainActivity) {
                ((MainActivity) requireActivity()).showFragment(AddGoalFragment.newInstance(userId));
            }
        });

        goalBarContainer = root.findViewById(R.id.goal_bar_container);
        goalBarFill = root.findViewById(R.id.goal_bar_fill);
        View edit = root.findViewById(R.id.btn_goal_edit);
        if (edit != null) edit.setOnClickListener(v -> promptSetMonthlyGoal());
        View manage = root.findViewById(R.id.btn_goals_more);
        if (manage != null) manage.setOnClickListener(v -> showManageGoals());
        View quickAdjust = root.findViewById(R.id.btn_quick_adjust_goal);
        if (quickAdjust != null) quickAdjust.setOnClickListener(v -> showManageGoals());
        View quickContribution = root.findViewById(R.id.btn_add_quick_contribution);
        if (quickContribution != null) quickContribution.setOnClickListener(v -> promptQuickContribution());

        // First-time seed: create two example goals if user exists and has none
        Async.runIo(() -> {
            boolean userExists = false;
            try { userExists = AppDatabase.getInstance(requireContext()).userDao().findById(userId) != null; } catch (Throwable ignored) {}
            if (userExists && repo.listGoals(userId).isEmpty()) {
                try { repo.addGoal(userId, "New Bike", 600, "bike"); } catch (Throwable ignored) {}
                try { repo.addGoal(userId, "Iphone 15 Pro", 1000, "phone"); } catch (Throwable ignored) {}
            }
            Async.runMain(this::refresh);
        });
        // Trigger initial refresh (runs queries off UI thread inside refresh())
        refresh();
        return root;
    }

    @Override public void onResume() {
        super.onResume();
        refresh();
    }

    private void refresh() {
        final android.content.Context ctx0 = getContext();
        if (ctx0 == null) return;
        final android.content.Context appCtx = ctx0.getApplicationContext();
        Async.runIo(() -> {
            long now = System.currentTimeMillis();
            double monthSaved = repo.monthSavings(userId, now);
            double autoAllocated = repo.monthAutoAllocated(userId, now);

            int monthKey = MonthUtils.monthKey(now);
            SavingsMonthlyGoal mg;
            try {
                boolean userExists = AppDatabase.getInstance(appCtx).userDao().findById(userId) != null;
                if (userExists) {
                    mg = repo.getOrCreateMonthlyGoal(userId, monthKey, 200);
                } else {
                    mg = new SavingsMonthlyGoal(userId, monthKey, 200);
                }
            } catch (Throwable t) {
                mg = new SavingsMonthlyGoal(userId, monthKey, 200);
            }
            double totalIncome = 0, totalExpense = 0;
            for (Income i : new IncomeRepository(appCtx).listForCurrentMonth(userId, now)) totalIncome += i.amount;
            for (Expense e : new ExpenseRepository(appCtx).listForCurrentMonth(userId, now)) totalExpense += e.amount;
            double newLeftover = Math.max(0, totalIncome - totalExpense);
            final double displaySavings = Math.max(0, Math.max(monthSaved, newLeftover) - autoAllocated);
            final double savedForProgress = Math.max(0, Math.max(monthSaved, newLeftover) - autoAllocated);
            final double mgTarget = mg.targetAmount;
            final double fTotalIncome = totalIncome;
            final double fTotalExpense = totalExpense;
            final double fAutoAllocated = autoAllocated;
            final double fLeftover = newLeftover;
            final double fMonthSaved = monthSaved;

            List<SavingsGoal> goals = repo.listGoals(userId);
            java.util.ArrayList<SavingsGoalAdapter.Item> items = new java.util.ArrayList<>();
            for (SavingsGoal g : goals) items.add(new SavingsGoalAdapter.Item(g, repo.goalProgress(userId, g.id)));

            Async.runMain(() -> {
                if (!isAdded()) return;
                leftoverSuggestion = fLeftover;
                if (tvCurrentMonthSavings != null) tvCurrentMonthSavings.setText(String.format(java.util.Locale.getDefault(), "$%,.0f", displaySavings));
                if (tvGoalTarget != null) tvGoalTarget.setText(String.format(java.util.Locale.getDefault(), "Target: $%,.0f", mgTarget));
                if (tvMonthIncome != null) tvMonthIncome.setText(String.format(java.util.Locale.getDefault(), "$%,.0f", fTotalIncome));
                if (tvMonthExpense != null) tvMonthExpense.setText(String.format(java.util.Locale.getDefault(), "$%,.0f", fTotalExpense));
                if (tvAutoContributions != null) tvAutoContributions.setText(String.format(java.util.Locale.getDefault(), "$%,.0f", fAutoAllocated));
                if (tvLeftoverMessage != null) tvLeftoverMessage.setText(String.format(java.util.Locale.getDefault(), "$%,.0f available", fLeftover));
                if (tvLeftoverHint != null) {
                    double suggested = Math.min(fLeftover, Math.max(0, mgTarget - savedForProgress));
                    if (suggested <= 0) {
                        tvLeftoverHint.setText("Great job! You're on pace with your monthly goal.");
                    } else {
                        tvLeftoverHint.setText(String.format(java.util.Locale.getDefault(), "Tip: Move %s today to stay on track.", String.format(java.util.Locale.getDefault(), "$%,.0f", suggested)));
                    }
                }
                // Compute and show progress percent
                final double ratio = mgTarget <= 0 ? 0 : Math.max(0, Math.min(1, savedForProgress / mgTarget));
                if (tvGoalAmount != null) {
                    int pct = (int) Math.round(ratio * 100);
                    tvGoalAmount.setText(String.format(java.util.Locale.getDefault(), "$%,.0f  •  %d%%", savedForProgress, pct));
                    int nightMask = getResources().getConfiguration().uiMode & android.content.res.Configuration.UI_MODE_NIGHT_MASK;
                    boolean isNight = nightMask == android.content.res.Configuration.UI_MODE_NIGHT_YES;
                    int white = androidx.core.content.ContextCompat.getColor(requireContext(), android.R.color.white);
                    int primary = androidx.core.content.ContextCompat.getColor(requireContext(), R.color.textPrimary);
                    tvGoalAmount.setTextColor(isNight ? white : (ratio >= 0.55 ? white : primary));
                }
                if (tvGoalGuidance != null) {
                    if (ratio >= 1) {
                        tvGoalGuidance.setText("Amazing! You've reached your monthly goal.");
                    } else if (ratio >= 0.6) {
                        tvGoalGuidance.setText("You're on pace. Keep contributing to finish strong.");
                    } else if (fMonthSaved <= 0) {
                        tvGoalGuidance.setText("Start with a small transfer today to kick off your savings momentum.");
                    } else {
                        tvGoalGuidance.setText("Increase contributions slightly to catch up with your monthly target.");
                    }
                }

                // Update progress bar to reflect saved vs target
                if (goalBarContainer != null && goalBarFill != null) {
                    goalBarContainer.post(() -> {
                        int w = goalBarContainer.getWidth();
                        int fillW = (int) Math.round(w * ratio);
                        if (lastGoalFillW < 0) lastGoalFillW = 0;
                        final int start = lastGoalFillW;
                        final int end = fillW;
                        if (start == end) {
                            ViewGroup.LayoutParams lp = goalBarFill.getLayoutParams();
                            lp.width = end;
                            goalBarFill.setLayoutParams(lp);
                        } else {
                            android.animation.ValueAnimator va = android.animation.ValueAnimator.ofInt(start, end);
                            va.setDuration(400);
                            va.addUpdateListener(anim -> {
                                int cur = (Integer) anim.getAnimatedValue();
                                ViewGroup.LayoutParams lp2 = goalBarFill.getLayoutParams();
                                lp2.width = cur;
                                goalBarFill.setLayoutParams(lp2);
                            });
                            va.start();
                        }
                        lastGoalFillW = end;
                    });
                }

                Calendar c = Calendar.getInstance();
                c.setTimeInMillis(now);
                String monthName = new DateFormatSymbols().getMonths()[c.get(Calendar.MONTH)];
                if (tvMonthLabel != null) tvMonthLabel.setText(monthName);

                if (rvGoals != null && rvGoals.getAdapter() instanceof SavingsGoalAdapter) {
                    ((SavingsGoalAdapter) rvGoals.getAdapter()).submit(items);
                }
            });
        });
    }

    private void setMonthlyGoal(double value) {
        Async.runIo(() -> {
            long now = System.currentTimeMillis();
            SavingsMonthlyGoal mg = repo.getOrCreateMonthlyGoal(userId, MonthUtils.monthKey(now), 200);
            mg.targetAmount = value;
            AppDatabase.getInstance(requireContext()).savingsMonthlyGoalDao().upsert(mg);
        });
    }

    private void promptSetMonthlyGoal() {
        View v = getLayoutInflater().inflate(R.layout.dialog_set_monthly_goal, null, false);
        final EditText input = v.findViewById(R.id.et_monthly_goal);
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Set monthly savings goal")
                .setView(v)
                .setPositiveButton("Save", (d, w) -> {
                    double val = 0; try { val = Double.parseDouble(input.getText().toString().trim()); } catch (Exception ignored) {}
                    if (val >= 0) {
                        setMonthlyGoal(val);
                        refresh();
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showGoalActions(SavingsGoal goal) {
        String[] opts = new String[] {"Add $50", "Add $100", "Edit", "Delete"};
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(goal.title)
                .setItems(opts, (d, which) -> {
                    if (which == 0 || which == 1) {
                        double amount = which == 0 ? 50 : 100;
                        Async.runIo(() -> { repo.addContribution(userId, goal.id, amount, System.currentTimeMillis()); Async.runMain(this::refresh); });
                    } else if (which == 2) {
                        if (requireActivity() instanceof MainActivity) {
                            ((MainActivity) requireActivity()).showFragment(AddGoalFragment.editInstance(userId, goal.id));
                        }
                    } else if (which == 3) {
                        new MaterialAlertDialogBuilder(requireContext())
                                .setTitle("Delete goal?")
                                .setMessage("This will remove the goal. Existing contributions remain.")
                                .setPositiveButton("Delete", (dd, w) -> Async.runIo(() -> { AppDatabase.getInstance(requireContext()).savingsGoalDao().delete(goal); Async.runMain(this::refresh); }))
                                .setNegativeButton("Cancel", null)
                                .show();
                    }
                })
                .show();
    }

    private void showManageGoals() {
        Async.runIo(() -> {
            List<SavingsGoal> goals = repo.listGoals(userId);
            if (goals.isEmpty()) return;
            String[] titles = new String[goals.size()];
            for (int i = 0; i < goals.size(); i++) titles[i] = goals.get(i).title;
            Async.runMain(() -> new MaterialAlertDialogBuilder(requireContext())
                    .setTitle("Manage goals")
                    .setItems(titles, (d, idx) -> {
                        SavingsGoal g = goals.get(idx);
                        String[] actions = new String[]{"Edit", "Delete"};
                        new MaterialAlertDialogBuilder(requireContext())
                                .setTitle(g.title)
                                .setItems(actions, (d2, which) -> {
                                    if (which == 0) {
                                        if (requireActivity() instanceof MainActivity) {
                                            ((MainActivity) requireActivity()).showFragment(AddGoalFragment.editInstance(userId, g.id));
                                        }
                                    } else {
                                        new MaterialAlertDialogBuilder(requireContext())
                                                .setTitle("Delete goal?")
                                                .setMessage("This will remove the goal. Existing contributions remain.")
                                                .setPositiveButton("Delete", (dd, w) -> Async.runIo(() -> { AppDatabase.getInstance(requireContext()).savingsGoalDao().delete(g); Async.runMain(this::refresh); }))
                                                .setNegativeButton("Cancel", null)
                                                .show();
                                    }
                                })
                                .show();
                    })
                    .show());
        });
    }

    private void promptQuickContribution() {
        Async.runIo(() -> {
            List<SavingsGoal> goals = repo.listGoals(userId);
            if (goals.isEmpty()) {
                Async.runMain(() -> {
                    if (!isAdded()) return;
                    Toast.makeText(requireContext(), "Create a savings goal first", Toast.LENGTH_SHORT).show();
                });
                return;
            }
            String[] titles = new String[goals.size()];
            for (int i = 0; i < goals.size(); i++) titles[i] = goals.get(i).title;
            Async.runMain(() -> {
                if (!isAdded()) return;
                new MaterialAlertDialogBuilder(requireContext())
                        .setTitle("Add $50 to")
                        .setItems(titles, (d, which) -> {
                            SavingsGoal target = goals.get(which);
                            Async.runIo(() -> {
                                repo.addContribution(userId, target.id, 50, System.currentTimeMillis());
                                Async.runMain(() -> {
                                    if (!isAdded()) return;
                                    Toast.makeText(requireContext(), "Added $50 to " + target.title, Toast.LENGTH_SHORT).show();
                                    refresh();
                                });
                            });
                        })
                        .setNegativeButton("Cancel", null)
                        .show();
            });
        });
    }
}
