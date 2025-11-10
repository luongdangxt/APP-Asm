package com.example.personalfinancialmanagement;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.graphics.Insets;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.text.DateFormatSymbols;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class AddIncomeFragment extends Fragment {
    public static final String ARG_USER_ID = "userId";
    private static final String ARG_PRESET_AMOUNT = "presetAmount";
    private long userId;
    private IncomeRepository incomeRepository;

    private final Calendar cal = Calendar.getInstance();
    private TextView tvMonthLabel;
    private RecyclerView rvDays;
    private WeekDayAdapter dayAdapter;
    private EditText edTitle, edAmount;
    private TextView chipSalary, chipRewards, chipCustom;
    private String selectedCategory = "Salary";

    public static AddIncomeFragment newInstance(long userId) {
        return newInstance(userId, -1);
    }

    public static AddIncomeFragment newInstance(long userId, double presetAmount) {
        AddIncomeFragment f = new AddIncomeFragment();
        Bundle b = new Bundle();
        b.putLong(ARG_USER_ID, userId);
        if (presetAmount > 0) b.putDouble(ARG_PRESET_AMOUNT, presetAmount);
        f.setArguments(b);
        return f;
    }

    @Nullable @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.activity_add_income, container, false);
        Bundle args = getArguments();
        userId = args != null ? args.getLong(ARG_USER_ID, -1) : -1;
        double presetAmount = args != null ? args.getDouble(ARG_PRESET_AMOUNT, -1) : -1;
        incomeRepository = new IncomeRepository(requireContext());

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

        tvMonthLabel = root.findViewById(R.id.tv_month_label);
        rvDays = root.findViewById(R.id.rv_days);
        edTitle = root.findViewById(R.id.ed_title);
        edAmount = root.findViewById(R.id.ed_amount);
        chipSalary = root.findViewById(R.id.chip_salary);
        chipRewards = root.findViewById(R.id.chip_rewards);
        chipCustom = root.findViewById(R.id.chip_custom);

        dayAdapter = new WeekDayAdapter();
        rvDays.setLayoutManager(new GridLayoutManager(requireContext(), 7));
        rvDays.setAdapter(dayAdapter);
        dayAdapter.setListener(d -> { cal.setTimeInMillis(d.timeMillis); refreshCalendar(); });
        root.findViewById(R.id.btn_prev_month).setOnClickListener(v -> { cal.add(Calendar.MONTH, -1); cal.set(Calendar.DAY_OF_MONTH, 1); refreshCalendar(); });
        root.findViewById(R.id.btn_next_month).setOnClickListener(v -> { cal.add(Calendar.MONTH, 1); cal.set(Calendar.DAY_OF_MONTH, 1); refreshCalendar(); });

        View.OnClickListener catClick = v -> setCategory(((TextView)v).getText().toString());
        chipSalary.setOnClickListener(catClick);
        chipRewards.setOnClickListener(catClick);
        chipCustom.setOnClickListener(v -> {
            Toast.makeText(requireContext(), "Tap 'Salary' or 'Rewards' or type in field below to change.", Toast.LENGTH_SHORT).show();
        });

        root.findViewById(R.id.btn_save_income).setOnClickListener(v -> save());

        if (presetAmount > 0) {
            String formatted = NumberFormat.getNumberInstance(Locale.getDefault()).format(presetAmount);
            edAmount.setText(formatted);
            edAmount.setSelection(formatted.length());
        }

        edAmount.addTextChangedListener(new TextWatcher() {
            boolean editing;
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override public void afterTextChanged(Editable s) {
                if (editing) return; editing = true;
                try {
                    String raw = s.toString().replace(",", "").replace("$", "").trim();
                    if (raw.isEmpty()) { editing=false; return; }
                    double v = Double.parseDouble(raw);
                    s.replace(0, s.length(), NumberFormat.getNumberInstance(Locale.getDefault()).format(v));
                } catch (Exception ignored) {} finally { editing = false; }
            }
        });

        refreshCalendar();
        setCategory(selectedCategory);
        return root;
    }

    private void setCategory(String name) {
        selectedCategory = name;
        chipSalary.setBackgroundResource(name.equals("Salary") ? R.drawable.shape_pill_blue : R.drawable.shape_pill_white);
        chipSalary.setTextColor(name.equals("Salary") ? 0xFFFFFFFF : 0xFF424242);
        chipRewards.setBackgroundResource(name.equals("Rewards") ? R.drawable.shape_pill_blue : R.drawable.shape_pill_white);
        chipRewards.setTextColor(name.equals("Rewards") ? 0xFFFFFFFF : 0xFF424242);
    }

    private void refreshCalendar() {
        String monthName = new DateFormatSymbols(Locale.getDefault()).getMonths()[cal.get(Calendar.MONTH)];
        tvMonthLabel.setText(monthName + " - " + cal.get(Calendar.YEAR));
        Calendar first = (Calendar) cal.clone(); first.set(Calendar.DAY_OF_MONTH, 1); first.set(Calendar.HOUR_OF_DAY,0); first.set(Calendar.MINUTE,0); first.set(Calendar.SECOND,0); first.set(Calendar.MILLISECOND,0);
        int dow1 = first.get(Calendar.DAY_OF_WEEK); int offsetStart = (dow1 + 5) % 7; Calendar start = (Calendar) first.clone(); start.add(Calendar.DAY_OF_MONTH, -offsetStart);
        Calendar last = (Calendar) cal.clone(); last.set(Calendar.DAY_OF_MONTH, last.getActualMaximum(Calendar.DAY_OF_MONTH)); int dowLast = last.get(Calendar.DAY_OF_WEEK); int offsetEnd = (7-((dowLast+6)%7))%7; int totalCells = offsetStart + last.get(Calendar.DAY_OF_MONTH) + offsetEnd; if (totalCells < 42) totalCells = 42;
        ArrayList<WeekDayAdapter.Day> days = new ArrayList<>(totalCells);
        for (int i=0;i<totalCells;i++) { Calendar d=(Calendar)start.clone(); d.add(Calendar.DAY_OF_MONTH,i); boolean inMonth=d.get(Calendar.MONTH)==cal.get(Calendar.MONTH) && d.get(Calendar.YEAR)==cal.get(Calendar.YEAR); boolean selected=d.get(Calendar.YEAR)==cal.get(Calendar.YEAR)&&d.get(Calendar.DAY_OF_YEAR)==cal.get(Calendar.DAY_OF_YEAR); days.add(new WeekDayAdapter.Day(d.get(Calendar.DAY_OF_MONTH), inMonth, selected, d.getTimeInMillis())); }
        dayAdapter.submit(days);
    }

    private void save() {
        String title = edTitle.getText().toString().trim();
        double amount = 0;
        try { String raw = edAmount.getText().toString().replace(","," ").replace(" ",""); amount = Double.parseDouble(raw); } catch (Exception ignored) {}
        if (userId <= 0 || title.isEmpty() || amount <= 0) {
            Toast.makeText(requireContext(), "Please enter title and amount", Toast.LENGTH_SHORT).show();
            return;
        }
        final String fTitle = title;
        final double fAmount = amount;
        final long fWhen = cal.getTimeInMillis();
        final String fCategory = selectedCategory;
        Async.runIo(() -> {
            incomeRepository.add(new Income(userId, fTitle, fWhen, fAmount, fCategory));
            Async.runMain(() -> {
                if (!isAdded()) return;
                Toast.makeText(requireContext(), "Income added", Toast.LENGTH_SHORT).show();
                if (getActivity() instanceof MainActivity) {
                    ((MainActivity) getActivity()).refreshOverview(userId);
                }
                if (getActivity()!=null) getActivity().getSupportFragmentManager().popBackStack();
            });
        });
    }
}
