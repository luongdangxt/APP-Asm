package com.example.personalfinancialmanagement;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.text.DateFormatSymbols;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class TotalExpensesFragment extends Fragment {
    public static final String ARG_USER_ID = "userId";
    public static final String ARG_TIME_MILLIS = "timeMillis";

    public static TotalExpensesFragment newInstance(long userId, long timeMillis) {
        TotalExpensesFragment f = new TotalExpensesFragment();
        Bundle b = new Bundle();
        b.putLong(ARG_USER_ID, userId);
        b.putLong(ARG_TIME_MILLIS, timeMillis);
        f.setArguments(b);
        return f;
    }

    private long userId;
    private final Calendar monthCal = Calendar.getInstance();
    private ExpenseRepository expenseRepository;
    private BudgetRepository budgetRepository;

    private TextView tvMonth;
    private TextView tvTotal;
    private TextView tvPercent;
    private RecyclerView rv;
    private RecyclerView rvWeek;
    private WeekDayAdapter weekAdapter;
    private OverviewEntryAdapter spendsAdapter;
    private CategoryStatAdapter categoriesAdapter;
    private boolean showSpends = true;
    private TextView tvTabSpends, tvTabCategories;
    private View categoriesHeader; private DialProgressView dialView; private TextView tvDialPercent;
    private View legendDot1, legendDot2, legendDot3; private TextView legendText1, legendText2, legendText3;
    private View pulse1, pulse2; private android.animation.AnimatorSet pulseAnim1, pulseAnim2;
    private View vHeader; private com.google.android.material.bottomsheet.BottomSheetBehavior<View> sheetBehavior; private View scrim;

    @Nullable @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.activity_total_expenses, container, false);
        final int rootBaseL = root.getPaddingLeft();
        final int rootBaseT = root.getPaddingTop();
        final int rootBaseR = root.getPaddingRight();
        final int rootBaseB = root.getPaddingBottom();

        userId = getArguments() != null ? getArguments().getLong(ARG_USER_ID, -1) : -1;
        long time = getArguments() != null ? getArguments().getLong(ARG_TIME_MILLIS, System.currentTimeMillis()) : System.currentTimeMillis();
        monthCal.setTimeInMillis(time);

        expenseRepository = new ExpenseRepository(requireContext());
        budgetRepository = new BudgetRepository(requireContext());

        ImageButton btnBack = root.findViewById(R.id.btn_back);
        if (btnBack != null) btnBack.setOnClickListener(v -> requireActivity().getSupportFragmentManager().popBackStack());

        tvMonth = root.findViewById(R.id.tv_month_label);
        tvTotal = root.findViewById(R.id.tv_total);
        tvPercent = root.findViewById(R.id.tv_percent);
        rv = root.findViewById(R.id.rv_list);
        rvWeek = root.findViewById(R.id.rv_week_days);
        tvTabSpends = root.findViewById(R.id.tv_tab_spends);
        tvTabCategories = root.findViewById(R.id.tv_tab_categories);
        categoriesHeader = root.findViewById(R.id.categories_header);
        dialView = root.findViewById(R.id.dial_view);
        tvDialPercent = root.findViewById(R.id.tv_dial_percent);
        vHeader = root.findViewById(R.id.header_container);
        scrim = root.findViewById(R.id.sheet_scrim);
        legendDot1 = root.findViewById(R.id.legend_dot1); legendDot2 = root.findViewById(R.id.legend_dot2); legendDot3 = root.findViewById(R.id.legend_dot3);
        legendText1 = root.findViewById(R.id.legend_text1); legendText2 = root.findViewById(R.id.legend_text2); legendText3 = root.findViewById(R.id.legend_text3);

        // Apply system bar insets: pad header by status bar, sheet by nav bar
        if (vHeader != null) {
            final int hBaseL = vHeader.getPaddingLeft();
            final int hBaseT = vHeader.getPaddingTop();
            final int hBaseR = vHeader.getPaddingRight();
            final int hBaseB = vHeader.getPaddingBottom();
            androidx.core.view.ViewCompat.setOnApplyWindowInsetsListener(root, (v, insets) -> {
                androidx.core.graphics.Insets bars = insets.getInsets(androidx.core.view.WindowInsetsCompat.Type.systemBars());
                v.setPadding(rootBaseL, rootBaseT, rootBaseR, rootBaseB);
                vHeader.setPadding(hBaseL, hBaseT + bars.top, hBaseR, hBaseB);
                View sheet2 = v.findViewById(R.id.bottom_sheet);
                if (sheet2 != null) {
                    int sL = sheet2.getPaddingLeft(); int sT = sheet2.getPaddingTop(); int sR = sheet2.getPaddingRight(); int sB = sheet2.getPaddingBottom();
                    sheet2.setPadding(sL, sT, sR, sB + bars.bottom);
                }
                return insets;
            });
        }

        spendsAdapter = new OverviewEntryAdapter();
        categoriesAdapter = new CategoryStatAdapter();
        if (rv != null) {
            rv.setLayoutManager(new LinearLayoutManager(requireContext()));
            rv.setHasFixedSize(true);
            rv.setItemViewCacheSize(24);
            rv.setAdapter(spendsAdapter);
            rv.setNestedScrollingEnabled(true);
        }

        View sheet = root.findViewById(R.id.bottom_sheet);
        if (sheet != null) {
            sheetBehavior = com.google.android.material.bottomsheet.BottomSheetBehavior.from(sheet);
            sheet.post(() -> {
                try {
                    int screenH = ((View) sheet.getParent()).getHeight();
                    int headerH = vHeader != null ? vHeader.getHeight() : 0;
                    int overlap = (int) (getResources().getDisplayMetrics().density * 12);
                    int collapsedTop = Math.max(0, headerH - overlap);
                    int peek = Math.max(200, screenH - collapsedTop);
                    sheetBehavior.setPeekHeight(peek, true);
                    sheetBehavior.setState(com.google.android.material.bottomsheet.BottomSheetBehavior.STATE_COLLAPSED);
                } catch (Throwable ignored) {}
            });
            sheetBehavior.addBottomSheetCallback(new com.google.android.material.bottomsheet.BottomSheetBehavior.BottomSheetCallback() {
            @Override public void onStateChanged(@NonNull View bottomSheet, int newState) {}
            @Override public void onSlide(@NonNull View bottomSheet, float slideOffset) {
                float t = Math.max(0f, Math.min(1f, slideOffset));
                float headerAlpha = 1f - 0.6f * t;
                if (vHeader != null) vHeader.setAlpha(headerAlpha);
                if (scrim != null) scrim.setAlpha(0.15f * t);
            }
            });
        }

        if (vHeader != null && rv != null) {
            vHeader.setOnTouchListener(new View.OnTouchListener() {
                float lastY;
                @Override public boolean onTouch(View v, MotionEvent event) {
                    try {
                        if (event.getAction() == MotionEvent.ACTION_DOWN) {
                            lastY = event.getRawY();
                        } else if (event.getAction() == MotionEvent.ACTION_MOVE) {
                            float dy = event.getRawY() - lastY;
                            lastY = event.getRawY();
                            if (rv != null) {
                                MotionEvent me = MotionEvent.obtain(event.getDownTime(), event.getEventTime(), MotionEvent.ACTION_MOVE, event.getX(), event.getY() + dy, 0);
                                rv.dispatchTouchEvent(me);
                                me.recycle();
                            }
                        }
                    } catch (Throwable ignored) {}
                    return true;
                }
            });
        }

        weekAdapter = new WeekDayAdapter();
        weekAdapter.setListener(day -> { monthCal.setTimeInMillis(day.timeMillis); refresh(); });
        if (rvWeek != null) {
            rvWeek.setAdapter(weekAdapter);
            rvWeek.setLayoutManager(new androidx.recyclerview.widget.GridLayoutManager(requireContext(), 7));
        }

        tvTabSpends.setOnClickListener(v -> { showSpends = true; updateTabs(); });
        tvTabCategories.setOnClickListener(v -> { showSpends = false; updateTabs(); });

        ImageButton prev = root.findViewById(R.id.btn_prev_month);
        ImageButton next = root.findViewById(R.id.btn_next_month);
        if (prev != null) prev.setOnClickListener(v -> { monthCal.add(Calendar.MONTH, -1); refresh(); });
        if (next != null) next.setOnClickListener(v -> { monthCal.add(Calendar.MONTH, 1); refresh(); });

        refresh();
        return root;
    }

    private void refresh() {
        if (tvMonth != null) tvMonth.setText(new DateFormatSymbols().getMonths()[monthCal.get(Calendar.MONTH)] + " " + monthCal.get(Calendar.YEAR));
        final long currentMonthTime = monthCal.getTimeInMillis();
        Async.runIo(() -> {
            // Monthly data for header/percent and categories
            List<Expense> spendsMonth = expenseRepository.listForCurrentMonth(userId, currentMonthTime);
            double totalMonth = 0; for (Expense e : spendsMonth) totalMonth += e.amount;
            // Day-specific data for the bottom list
            List<Expense> spendsDay = expenseRepository.listForDay(userId, currentMonthTime);

            java.util.ArrayList<CategoryStatAdapter.Item> cats = null;
            double[] vals = null; int[] cols = null; String[] names = null; int n = 0;
            if (!showSpends) {
                java.util.Map<String, double[]> map = new java.util.HashMap<>();
                for (Expense e : spendsMonth) {
                    String key = e.category == null ? "Other" : e.category;
                    double[] val = map.getOrDefault(key, new double[]{0,0});
                    val[0] += e.amount; val[1] += 1; map.put(key, val);
                }
                cats = new java.util.ArrayList<>();
                for (java.util.Map.Entry<String,double[]> en : map.entrySet()) cats.add(new CategoryStatAdapter.Item(en.getKey(), en.getValue()[0], (int)en.getValue()[1]));
                cats.sort((a,b)-> Double.compare(b.total, a.total));
                n = cats.size(); vals = new double[n]; cols = new int[n]; names = new String[n];
                int[] palette = new int[]{0xFF2860FF, 0xFF6FA8FF, 0xFFBFD3FF};
                for (int i=0;i<n;i++){ vals[i]=cats.get(i).total; cols[i]=palette[i%palette.length]; names[i]=cats.get(i).category; }
            }

            int monthKey = MonthUtils.monthKey(currentMonthTime);
            double budgetTotal = 0; for (Budget b : budgetRepository.listForMonth(userId, monthKey)) budgetTotal += b.limitAmount;
            int percent = (budgetTotal <= 0) ? 0 : (int) Math.min(100, Math.round((totalMonth / budgetTotal) * 100));

            java.util.ArrayList<CategoryStatAdapter.Item> finalCats = cats;
            double[] finalVals = vals; int[] finalCols = cols; String[] finalNames = names; int finalN = n; double finalTotalMonth = totalMonth;
            List<Expense> finalSpendsDay = spendsDay;
            Async.runMain(() -> {
                if (!isAdded()) return;
                if (getView() == null) return; // View might be destroyed during theme change
                if (spendsAdapter != null) spendsAdapter.submit(finalSpendsDay);
                if (!showSpends) {
                    if (categoriesHeader != null) categoriesHeader.setVisibility(View.VISIBLE);
                    if (categoriesAdapter != null) categoriesAdapter.submit(finalCats);
                    try {
                        if (dialView != null) {
                            float share = 0f;
                            if (finalTotalMonth > 0 && finalCats != null && !finalCats.isEmpty()) {
                                share = (float) Math.max(0, Math.min(1, finalCats.get(0).total / finalTotalMonth));
                            }
                            // animate dial from 0 -> share
                            android.animation.ValueAnimator va = android.animation.ValueAnimator.ofFloat(0f, share);
                            va.setDuration(1200);
                            va.setInterpolator(new android.view.animation.AccelerateDecelerateInterpolator());
                            va.addUpdateListener(a -> {
                                float p = (float) a.getAnimatedValue();
                                dialView.setProgress(p);
                                if (tvDialPercent != null) tvDialPercent.setText(String.format(java.util.Locale.getDefault(), "%d%%", Math.round(p * 100)));
                            });
                            va.start();
                        }
                    } catch (Throwable ignored) {}
                    if (finalN>0){ legendDot1.setBackgroundTintList(android.content.res.ColorStateList.valueOf(finalCols[0])); legendText1.setText(finalNames[0]); legendDot1.setVisibility(View.VISIBLE); legendText1.setVisibility(View.VISIBLE);} else { legendDot1.setVisibility(View.GONE); legendText1.setVisibility(View.GONE);} 
                    if (finalN>1){ legendDot2.setBackgroundTintList(android.content.res.ColorStateList.valueOf(finalCols[1])); legendText2.setText(finalNames[1]); legendDot2.setVisibility(View.VISIBLE); legendText2.setVisibility(View.VISIBLE);} else { legendDot2.setVisibility(View.GONE); legendText2.setVisibility(View.GONE);} 
                    if (finalN>2){ legendDot3.setBackgroundTintList(android.content.res.ColorStateList.valueOf(finalCols[2])); legendText3.setText(finalNames[2]); legendDot3.setVisibility(View.VISIBLE); legendText3.setVisibility(View.VISIBLE);} else { legendDot3.setVisibility(View.GONE); legendText3.setVisibility(View.GONE);} 
                } else {
                    if (categoriesHeader != null) categoriesHeader.setVisibility(View.GONE);
                }
                if (tvTotal != null) tvTotal.setText(String.format(Locale.getDefault(), "$%,.0f", finalTotalMonth));
                if (tvPercent != null) tvPercent.setText("You have Spend total\n" + percent + "% of your budget");

                // Build calendar grid on UI thread (cheap)
                Calendar first = (Calendar) monthCal.clone();
                first.set(Calendar.DAY_OF_MONTH, 1);
                first.set(Calendar.HOUR_OF_DAY,0); first.set(Calendar.MINUTE,0); first.set(Calendar.SECOND,0); first.set(Calendar.MILLISECOND,0);
                int dow1 = first.get(Calendar.DAY_OF_WEEK);
                int offsetStart = (dow1 + 5) % 7;
                Calendar start = (Calendar) first.clone();
                start.add(Calendar.DAY_OF_MONTH, -offsetStart);

                Calendar last = (Calendar) monthCal.clone();
                last.set(Calendar.DAY_OF_MONTH, last.getActualMaximum(Calendar.DAY_OF_MONTH));
                int dowLast = last.get(Calendar.DAY_OF_WEEK);
                int offsetEnd = (7 - ((dowLast + 6) % 7)) % 7;
                int totalCells = offsetStart + last.get(Calendar.DAY_OF_MONTH) + offsetEnd;
                if (totalCells < 42) totalCells = 42;

                java.util.ArrayList<WeekDayAdapter.Day> days = new java.util.ArrayList<>(totalCells);
                for (int i=0;i<totalCells;i++) {
                    Calendar d = (Calendar) start.clone();
                    d.add(Calendar.DAY_OF_MONTH, i);
                    boolean inMonth = d.get(Calendar.MONTH)==monthCal.get(Calendar.MONTH) && d.get(Calendar.YEAR)==monthCal.get(Calendar.YEAR);
                    boolean selected = d.get(Calendar.YEAR)==monthCal.get(Calendar.YEAR) && d.get(Calendar.DAY_OF_YEAR)==monthCal.get(Calendar.DAY_OF_YEAR);
                    days.add(new WeekDayAdapter.Day(d.get(Calendar.DAY_OF_MONTH), inMonth, selected, d.getTimeInMillis()));
                }
                if (weekAdapter != null) weekAdapter.submit(days);

                View vroot = getView();
                if (pulse1 == null && vroot != null) { pulse1 = vroot.findViewById(R.id.pulse1); }
                if (pulse2 == null && vroot != null) { pulse2 = vroot.findViewById(R.id.pulse2); }
                startPulse();
            });
        });
    }

    private void updateTabs() {
        if (showSpends) {
            tvTabSpends.setBackgroundResource(R.drawable.shape_pill_blue);
            tvTabSpends.setTextColor(0xFFFFFFFF);
            tvTabCategories.setBackgroundResource(R.drawable.shape_pill_white);
            tvTabCategories.setTextColor(0xFF424242);
        } else {
            tvTabSpends.setBackgroundResource(R.drawable.shape_pill_white);
            tvTabSpends.setTextColor(0xFF424242);
            tvTabCategories.setBackgroundResource(R.drawable.shape_pill_blue);
            tvTabCategories.setTextColor(0xFFFFFFFF);
        }
        refresh();
    }

    @Override public void onPause() {
        super.onPause();
        if (pulseAnim1 != null) pulseAnim1.cancel();
        if (pulseAnim2 != null) pulseAnim2.cancel();
    }

    @Override public void onDestroyView() {
        super.onDestroyView();
        try { if (pulseAnim1 != null) pulseAnim1.cancel(); } catch (Throwable ignored) {}
        try { if (pulseAnim2 != null) pulseAnim2.cancel(); } catch (Throwable ignored) {}
        pulseAnim1 = null; pulseAnim2 = null; pulse1 = null; pulse2 = null;
    }

    @Override public void onResume() {
        super.onResume();
        // Ensure pulse is running after configuration/theme changes
        startPulse();
    }

    private void startPulse() {
        View root = getView();
        if (root == null) return;
        if (pulse1 == null) { pulse1 = root.findViewById(R.id.pulse1); }
        if (pulse2 == null) { pulse2 = root.findViewById(R.id.pulse2); }
        if (pulse1 == null || pulse2 == null) return;
        // If already running, avoid stopping/restarting (prevents cancel() loops)
        if (pulseAnim1 != null && pulseAnim1.isStarted() && pulseAnim2 != null && pulseAnim2.isStarted()) {
            return;
        }
        if (pulseAnim1 != null) { pulseAnim1.cancel(); }
        if (pulseAnim2 != null) { pulseAnim2.cancel(); }
        pulseAnim1 = createPulse(pulse1, 0);
        pulseAnim2 = createPulse(pulse2, 600);
        pulseAnim1.start();
        pulseAnim2.start();
    }

    private android.animation.AnimatorSet createPulse(View target, long delay) {
        target.setScaleX(0.8f); target.setScaleY(0.8f); target.setAlpha(0f);
        android.animation.ObjectAnimator sX = android.animation.ObjectAnimator.ofFloat(target, View.SCALE_X, 0.8f, 1.4f);
        android.animation.ObjectAnimator sY = android.animation.ObjectAnimator.ofFloat(target, View.SCALE_Y, 0.8f, 1.4f);
        android.animation.ObjectAnimator a = android.animation.ObjectAnimator.ofFloat(target, View.ALPHA, 0f, 0.35f, 0f);
        // Repeat indefinitely to avoid manual restart loops
        sX.setRepeatCount(android.animation.ValueAnimator.INFINITE);
        sY.setRepeatCount(android.animation.ValueAnimator.INFINITE);
        a.setRepeatCount(android.animation.ValueAnimator.INFINITE);
        sX.setRepeatMode(android.animation.ValueAnimator.RESTART);
        sY.setRepeatMode(android.animation.ValueAnimator.RESTART);
        a.setRepeatMode(android.animation.ValueAnimator.RESTART);
        android.animation.AnimatorSet set = new android.animation.AnimatorSet();
        set.setDuration(1800);
        set.setStartDelay(delay);
        set.playTogether(sX, sY, a);
        set.setInterpolator(new android.view.animation.AccelerateDecelerateInterpolator());
        return set;
    }
}
