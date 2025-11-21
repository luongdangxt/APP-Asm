package com.example.personalfinancialmanagement;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.LinearLayout;
import android.graphics.drawable.GradientDrawable;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.chip.ChipGroup;

import java.text.DateFormatSymbols;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

/**
 * Spending report screen with donut chart and category list.
 */
public class ExpenseReportFragment extends Fragment {
    public static final String ARG_USER_ID = "userId";
    private static final String STATE_DAILY_MODE = "state_daily_mode";
    private static final String STATE_CAL_TIME = "state_cal_time";
    public static ExpenseReportFragment newInstance(long userId){
        ExpenseReportFragment f = new ExpenseReportFragment();
        Bundle b = new Bundle(); b.putLong(ARG_USER_ID, userId); f.setArguments(b); return f;
    }

    private long userId;
    private final Calendar monthCal = Calendar.getInstance();
    private ExpenseRepository expenseRepository; private IncomeRepository incomeRepository;
    private TextView tvTitle, tvPeriod, tvExpense, tvIncome, tvChange;
    private CategoryDonutView donut;
    private LinearLayout legendContainer;
    private RecyclerView rvCats; private CategorySummaryAdapter catAdapter;
    private boolean dailyMode = false;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (savedInstanceState != null) {
            dailyMode = savedInstanceState.getBoolean(STATE_DAILY_MODE, false);
            long restoredTime = savedInstanceState.getLong(STATE_CAL_TIME, System.currentTimeMillis());
            monthCal.setTimeInMillis(restoredTime);
        }
    }

    @Nullable @Override public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_expense_report, container, false);
        userId = getArguments()!=null? getArguments().getLong(ARG_USER_ID, -1) : -1;
        expenseRepository = new ExpenseRepository(requireContext());
        incomeRepository = new IncomeRepository(requireContext());

        tvTitle = root.findViewById(R.id.tv_title);
        tvPeriod = root.findViewById(R.id.tv_period);
        tvExpense = root.findViewById(R.id.tv_expense_total);
        tvIncome = root.findViewById(R.id.tv_income_total);
        tvChange = root.findViewById(R.id.tv_change_text);
        donut = root.findViewById(R.id.donut);
        legendContainer = root.findViewById(R.id.legend_container);
        rvCats = root.findViewById(R.id.rv_categories);

        View scroll = root.findViewById(R.id.scroll_report);
        if (scroll != null) {
            final int baseBottom = scroll.getPaddingBottom();
            ViewCompat.setOnApplyWindowInsetsListener(scroll, (v, insets) -> {
                Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
                v.setPadding(v.getPaddingLeft(), v.getPaddingTop(), v.getPaddingRight(), baseBottom + bars.bottom);
                return insets;
            });
            ViewCompat.requestApplyInsets(scroll);
        }

        if (tvTitle!=null) tvTitle.setText(R.string.expense_report_title);
        ImageButton back = root.findViewById(R.id.btn_back);
        if (back!=null) back.setOnClickListener(v-> requireActivity().getSupportFragmentManager().popBackStack());
        ImageButton prev = root.findViewById(R.id.btn_prev_month);
        ImageButton next = root.findViewById(R.id.btn_next_month);
        if (prev!=null) prev.setOnClickListener(v->{ monthCal.add(dailyMode ? Calendar.DAY_OF_MONTH : Calendar.MONTH,-1); refresh(); });
        if (next!=null) next.setOnClickListener(v->{ monthCal.add(dailyMode ? Calendar.DAY_OF_MONTH : Calendar.MONTH, 1); refresh(); });
        if (tvPeriod != null) {
            tvPeriod.setOnClickListener(v -> {
                if (!dailyMode) return;
                Calendar c = (Calendar) monthCal.clone();
                DatePickerDialog dialog = new DatePickerDialog(requireContext(),
                        (view, year, month, dayOfMonth) -> {
                            monthCal.set(Calendar.YEAR, year);
                            monthCal.set(Calendar.MONTH, month);
                            monthCal.set(Calendar.DAY_OF_MONTH, dayOfMonth);
                            refresh();
                        },
                        c.get(Calendar.YEAR),
                        c.get(Calendar.MONTH),
                        c.get(Calendar.DAY_OF_MONTH));
                dialog.show();
            });
            tvPeriod.setClickable(true);
            tvPeriod.setFocusable(true);
        }

        ChipGroup intervals = root.findViewById(R.id.group_interval);
        if (intervals != null) {
            intervals.setOnCheckedStateChangeListener((group, checkedIds) -> {
                boolean daySelected = checkedIds != null && checkedIds.contains(R.id.chip_interval_day);
                if (daySelected != dailyMode) {
                    dailyMode = daySelected;
                    refresh();
                }
            });
            intervals.check(dailyMode ? R.id.chip_interval_day : R.id.chip_interval_month);
        }

        rvCats.setLayoutManager(new LinearLayoutManager(requireContext()));
        catAdapter = new CategorySummaryAdapter();
        rvCats.setAdapter(catAdapter);

        // Tabs are visual only at the moment; real grouping can be wired later
        refresh();
        return root;
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean(STATE_DAILY_MODE, dailyMode);
        outState.putLong(STATE_CAL_TIME, monthCal.getTimeInMillis());
    }

    private void refresh(){
        String label = dailyMode ? dayLabel(monthCal) : monthLabel(monthCal);
        if (tvPeriod!=null) {
            tvPeriod.setText(label);
            tvPeriod.setEnabled(dailyMode);
            tvPeriod.setAlpha(dailyMode ? 1f : 0.7f);
        }

        long time = monthCal.getTimeInMillis();
        final int positiveColor = ContextCompat.getColor(requireContext(), R.color.reportPositive);
        final int negativeColor = ContextCompat.getColor(requireContext(), R.color.reportNegative);
        Async.runIo(() -> {
            // Totals
            List<Expense> spendsMonth = dailyMode ? expenseRepository.listForDay(userId, time) : expenseRepository.listForCurrentMonth(userId, time);
            double totalExpense = 0; for (Expense e : spendsMonth) totalExpense += e.amount;
            List<Income> incomesMonth = dailyMode ? incomeRepository.listForDay(userId, time) : incomeRepository.listForCurrentMonth(userId, time);
            double totalIncome = 0; for (Income i : incomesMonth) totalIncome += i.amount;

            // Prev month delta
            Calendar prev = (Calendar) monthCal.clone();
            prev.add(dailyMode ? Calendar.DAY_OF_MONTH : Calendar.MONTH, -1);
            List<Expense> prevSpends = dailyMode ? expenseRepository.listForDay(userId, prev.getTimeInMillis())
                    : expenseRepository.listForCurrentMonth(userId, prev.getTimeInMillis());
            double prevTotal = 0; for (Expense e : prevSpends) prevTotal += e.amount;
            double diff = Math.abs(totalExpense - prevTotal);
            boolean decreased = totalExpense < prevTotal;

            // Categories
            java.util.Map<String, Double> map = new java.util.HashMap<>();
            for (Expense e : spendsMonth){ String k = e.category==null? "Other" : e.category; map.put(k, map.getOrDefault(k, 0.0)+e.amount); }
            ArrayList<CategorySummaryAdapter.Item> cats = new ArrayList<>();
            for (java.util.Map.Entry<String,Double> en : map.entrySet()) cats.add(new CategorySummaryAdapter.Item(en.getKey(), en.getValue(), 0));
            cats.sort((a,b)-> Double.compare(b.total, a.total));
            double sum = Math.max(1.0, totalExpense);
            for (CategorySummaryAdapter.Item it : cats) it.percent = (int) Math.round((it.total / sum) * 100.0);

            // Donut data
            ArrayList<CategoryDonutView.Slice> slices = new ArrayList<>();
            int[] palette = new int[]{ 0xFF6C63FF, 0xFFFF9F40, 0xFF00C2FF, 0xFF34D399, 0xFFEC4899, 0xFFF59E0B, 0xFF9CA3AF };
            for (int i=0;i<cats.size();i++) slices.add(new CategoryDonutView.Slice((float) cats.get(i).total, palette[i%palette.length]));

            String compareLabel = dailyMode ? "yesterday" : "last month";
            String changeText = (decreased? "Decreased " : "Increased ") + formatCurrency(diff) + " vs " + compareLabel;

            final double fTotalExpense = totalExpense;
            final double fTotalIncome = totalIncome;
            final String fChangeText = changeText;
            final ArrayList<CategoryDonutView.Slice> fSlices = slices;
            final ArrayList<CategorySummaryAdapter.Item> fCats = cats;
            Async.runMain(() -> {
                if (!isAdded()) return;
                tvExpense.setText(formatCurrency(fTotalExpense));
                tvIncome.setText(formatCurrency(fTotalIncome));
                tvChange.setText(fChangeText);
                tvChange.setTextColor(decreased? positiveColor : negativeColor);
                donut.setData(fSlices);
                donut.setCenterTexts(getString(R.string.expense_report_total_label), formatCurrency(fTotalExpense));
                catAdapter.submit(fCats);
                updateLegend(fCats, fSlices);
            });
        });
    }

    private void updateLegend(List<CategorySummaryAdapter.Item> cats, List<CategoryDonutView.Slice> slices) {
        if (legendContainer == null) return;
        legendContainer.removeAllViews();
        if (cats == null || cats.isEmpty()) {
            legendContainer.setVisibility(View.GONE);
            return;
        }
        if (!isAdded()) return;
        LayoutInflater inflater = LayoutInflater.from(requireContext());
        int count = Math.min(cats.size(), slices != null ? slices.size() : 0);
        if (count == 0) {
            legendContainer.setVisibility(View.GONE);
            return;
        }
        for (int i = 0; i < count; i++) {
            View row = inflater.inflate(R.layout.item_report_legend, legendContainer, false);
            View dot = row.findViewById(R.id.legend_dot);
            TextView label = row.findViewById(R.id.legend_label);
            TextView percent = row.findViewById(R.id.legend_percent);
            CategorySummaryAdapter.Item item = cats.get(i);
            label.setText(item.name);
            percent.setText(item.percent + "%");
            int color = slices.get(i).color;
            if (dot.getBackground() instanceof GradientDrawable) {
                GradientDrawable bg = (GradientDrawable) dot.getBackground().mutate();
                bg.setColor(color);
                dot.setBackground(bg);
            } else {
                dot.setBackgroundColor(color);
            }
            legendContainer.addView(row);
        }
        legendContainer.setVisibility(View.VISIBLE);
    }

    private static String monthLabel(Calendar c){
        Calendar now = Calendar.getInstance();
        boolean same = now.get(Calendar.YEAR)==c.get(Calendar.YEAR) && now.get(Calendar.MONTH)==c.get(Calendar.MONTH);
        if (same) return "This month";
        return String.format(Locale.getDefault(), "%s %d", new DateFormatSymbols().getMonths()[c.get(Calendar.MONTH)], c.get(Calendar.YEAR));
    }

    private static String dayLabel(Calendar c){
        Calendar now = Calendar.getInstance();
        boolean same = now.get(Calendar.YEAR)==c.get(Calendar.YEAR)
                && now.get(Calendar.MONTH)==c.get(Calendar.MONTH)
                && now.get(Calendar.DAY_OF_MONTH)==c.get(Calendar.DAY_OF_MONTH);
        if (same) return "Today";
        SimpleDateFormat fmt = new SimpleDateFormat("MMM d, yyyy", Locale.getDefault());
        return fmt.format(c.getTime());
    }

    private static String formatCurrency(double v){
        return String.format(Locale.getDefault(), "$%,.2f", v);
    }
}
