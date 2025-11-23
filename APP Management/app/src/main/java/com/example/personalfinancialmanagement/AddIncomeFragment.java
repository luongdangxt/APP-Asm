package com.example.personalfinancialmanagement;

import android.os.Bundle;
import android.text.Editable;
import android.text.InputType;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.core.content.ContextCompat;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.graphics.Insets;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.personalfinancialmanagement.CategoryPreferences;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.example.personalfinancialmanagement.network.FinanceRemoteRepository;
import com.example.personalfinancialmanagement.network.FinanceRemoteRepository.SourceItem;

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
    private ChipGroup groupSource;
    private Chip chipOtherSource;
    private Chip templateSourceChip;
    private CategoryPreferences categoryPreferences;
    private FinanceRemoteRepository remoteRepo;
    private int lastCheckedChipId = View.NO_ID;
    private String selectedCategory = "Wallet";
    private final List<SourceItem> remoteSources = new ArrayList<>();

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
        groupSource = root.findViewById(R.id.group_income_source);
        chipOtherSource = root.findViewById(R.id.chip_income_other);
        templateSourceChip = root.findViewById(R.id.chip_income_wallet);
        categoryPreferences = new CategoryPreferences(requireContext());
        remoteRepo = new FinanceRemoteRepository(requireContext());

        dayAdapter = new WeekDayAdapter();
        rvDays.setLayoutManager(new GridLayoutManager(requireContext(), 7));
        rvDays.setAdapter(dayAdapter);
        dayAdapter.setListener(d -> { cal.setTimeInMillis(d.timeMillis); refreshCalendar(); });
        root.findViewById(R.id.btn_prev_month).setOnClickListener(v -> { cal.add(Calendar.MONTH, -1); cal.set(Calendar.DAY_OF_MONTH, 1); refreshCalendar(); });
        root.findViewById(R.id.btn_next_month).setOnClickListener(v -> { cal.add(Calendar.MONTH, 1); cal.set(Calendar.DAY_OF_MONTH, 1); refreshCalendar(); });

        addSavedSourceChips();
        fetchRemoteSources();
        bindChipGroup(groupSource, chipOtherSource, this::showCustomIncomeDialog, value -> selectedCategory = value);

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
        return root;
    }

    private void fetchRemoteSources() {
        Async.runIo(() -> {
            List<SourceItem> list = remoteRepo.listSources("income");
            if (list != null) {
                remoteSources.clear();
                remoteSources.addAll(list);
            }
            Async.runMain(() -> {
                if (!isAdded()) return;
                if (list != null) {
                    for (SourceItem item : list) {
                        addCustomSourceChip(item);
                    }
                }
            });
        });
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

    private void addSavedSourceChips() {
        if (groupSource == null || categoryPreferences == null) return;
        List<String> saved = categoryPreferences.getCustomIncomeSources();
        for (String label : saved) {
            addCustomSourceChip(new SourceItem(null, label, "income"));
        }
    }

    private void showCustomIncomeDialog() {
        if (!isAdded() || groupSource == null) return;
        final int previousSelection = lastCheckedChipId;
        if (chipOtherSource != null) chipOtherSource.setChecked(false);

        final EditText input = new EditText(requireContext());
        input.setHint("Enter source");
        input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_WORDS);

        FrameLayout container = new FrameLayout(requireContext());
        int pad = (int) dp(16);
        container.setPadding(pad, pad, pad, 0);
        FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        container.addView(input, lp);

        AlertDialog dialog = new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Custom income source")
                .setView(container)
                .setNegativeButton("Cancel", (d, w) -> restorePreviousIncomeSelection(previousSelection))
                .setPositiveButton("Save", null)
                .create();
        dialog.setOnShowListener(d -> {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
                String value = CategoryPreferences.sanitizeLabel(input.getText().toString());
                if (TextUtils.isEmpty(value)) {
                    input.setError("Please enter a source");
                    return;
                }
                saveNewIncomeSource(value, dialog);
            });
        });
        dialog.show();
    }

    private void restorePreviousIncomeSelection(int previousSelection) {
        if (groupSource == null) return;
        if (previousSelection == View.NO_ID && groupSource.getChildCount() > 0) {
            View first = groupSource.getChildAt(0);
            if (first instanceof Chip) ((Chip) first).setChecked(true);
            return;
        }
        if (previousSelection != View.NO_ID) {
            Chip prev = groupSource.findViewById(previousSelection);
            if (prev != null) prev.setChecked(true);
        }
    }

    private Chip addCustomSourceChip(SourceItem source) {
        if (groupSource == null || source == null) return null;
        String label = source.label;
        Chip existing = findChipByLabel(groupSource, label);
        if (existing != null) {
            if (source.id != null && !source.id.isEmpty()) {
                existing.setTag(R.id.tag_source_object, source);
            }
            return existing;
        }
        Chip chip = createChoiceChip(label);
        chip.setTag(R.id.tag_source_object, source);
        chip.setOnLongClickListener(v -> {
            showSourceActions(chip, source);
            return true;
        });
        int index = chipOtherSource != null ? groupSource.indexOfChild(chipOtherSource) : groupSource.getChildCount();
        if (index < 0) index = groupSource.getChildCount();
        groupSource.addView(chip, index);
        return chip;
    }

    private Chip createChoiceChip(String label) {
        Chip chip = (Chip) LayoutInflater.from(requireContext()).inflate(R.layout.view_choice_chip, groupSource, false);
        chip.setText(label);
        chip.setTag(label);
        chip.setEnsureMinTouchTargetSize(false);
        applyTemplateLayout(chip);
        return chip;
    }

    private void applyTemplateLayout(Chip chip) {
        if (groupSource == null) return;
        Chip template = templateSourceChip != null ? templateSourceChip
                : (chipOtherSource != null ? chipOtherSource : findFirstChip(groupSource));
        if (template == null) return;
        ViewGroup.LayoutParams lp = template.getLayoutParams();
        if (lp instanceof ViewGroup.MarginLayoutParams) {
            ViewGroup.MarginLayoutParams newLp = new ViewGroup.MarginLayoutParams((ViewGroup.MarginLayoutParams) lp);
            chip.setLayoutParams(newLp);
        }
        chip.setTextColor(template.getTextColors());
        chip.setChipBackgroundColor(template.getChipBackgroundColor());
        chip.setRippleColor(template.getRippleColor());
        chip.setChipStrokeColor(template.getChipStrokeColor());
        chip.setChipStrokeWidth(template.getChipStrokeWidth());
        chip.setChipCornerRadius(template.getChipCornerRadius());
        chip.setChipMinHeight(template.getChipMinHeight());
        chip.setChipStartPadding(template.getChipStartPadding());
        chip.setChipEndPadding(template.getChipEndPadding());
        chip.setTextStartPadding(template.getTextStartPadding());
        chip.setTextEndPadding(template.getTextEndPadding());
    }

    private Chip findFirstChip(ChipGroup group) {
        if (group == null) return null;
        for (int i = 0; i < group.getChildCount(); i++) {
            View child = group.getChildAt(i);
            if (child instanceof Chip) return (Chip) child;
        }
        return null;
    }

    private float dp(float value) {
        return value * requireContext().getResources().getDisplayMetrics().density;
    }

    private Chip findChipByLabel(ChipGroup group, String tag) {
        if (group == null || TextUtils.isEmpty(tag)) return null;
        for (int i = 0; i < group.getChildCount(); i++) {
            View child = group.getChildAt(i);
            if (child instanceof Chip) {
                String label = resolveChipLabel((Chip) child);
                if (tag.equals(label)) {
                    return (Chip) child;
                }
            }
        }
        return null;
    }

    private void bindChipGroup(ChipGroup group, Chip otherChip, Runnable onOtherSelected, ChipSelectionListener listener) {
        if (group == null || listener == null) return;
        group.setOnCheckedStateChangeListener((chipGroup, checkedIds) -> {
            if (checkedIds == null || checkedIds.isEmpty()) return;
            Chip chip = chipGroup.findViewById(checkedIds.get(0));
            if (chip == null) return;
            if (otherChip != null && chip.getId() == otherChip.getId()) {
                chip.setChecked(false);
                if (onOtherSelected != null) onOtherSelected.run();
                return;
            }
            lastCheckedChipId = chip.getId();
            listener.onValueSelected(resolveChipLabel(chip));
        });
        int checkedId = group.getCheckedChipId();
        if (checkedId != View.NO_ID) {
            Chip chip = group.findViewById(checkedId);
            if (chip != null) {
                lastCheckedChipId = chip.getId();
                listener.onValueSelected(resolveChipLabel(chip));
            }
        } else if (group.getChildCount() > 0 && group.getChildAt(0) instanceof Chip) {
            Chip chip = (Chip) group.getChildAt(0);
            chip.setChecked(true);
            lastCheckedChipId = chip.getId();
            listener.onValueSelected(resolveChipLabel(chip));
        }
    }

    private String resolveChipLabel(Chip chip) {
        Object tag = chip.getTag();
        if (tag instanceof SourceItem) return ((SourceItem) tag).label;
        Object sourceObj = chip.getTag(R.id.tag_source_object);
        if (sourceObj instanceof SourceItem) return ((SourceItem) sourceObj).label;
        return tag != null ? tag.toString() : chip.getText().toString();
    }

    private void saveNewIncomeSource(String value, AlertDialog dialog) {
        Async.runIo(() -> {
            SourceItem created = remoteRepo.createSource("income", value);
            boolean remoteOk = created != null;
            if (!remoteOk) {
                if (categoryPreferences != null) categoryPreferences.addCustomIncomeSource(value);
                created = new SourceItem(null, value, "income");
            } else if (categoryPreferences != null) {
                categoryPreferences.addCustomIncomeSource(created.label);
            }
            SourceItem finalCreated = created;
            Async.runMain(() -> {
                if (!isAdded()) return;
                Chip chip = addCustomSourceChip(finalCreated);
                if (chip != null) chip.setChecked(true);
                Toast.makeText(requireContext(), remoteOk ? "Saved" : "Saved offline", Toast.LENGTH_SHORT).show();
                dialog.dismiss();
            });
        });
    }

    private void showSourceActions(Chip chip, SourceItem source) {
        if (chip == null || source == null) return;
        String[] options = new String[]{"Rename", "Delete"};
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(source.label)
                .setItems(options, (d, which) -> {
                    if (which == 0) {
                        promptRename(chip, source);
                    } else if (which == 1) {
                        confirmDelete(chip, source);
                    }
                })
                .show();
    }

    private void promptRename(Chip chip, SourceItem source) {
        final EditText input = new EditText(requireContext());
        input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_WORDS);
        input.setText(source.label);
        FrameLayout container = new FrameLayout(requireContext());
        int pad = (int) dp(16);
        container.setPadding(pad, pad, pad, 0);
        container.addView(input, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Rename source")
                .setView(container)
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Save", (dialog, which) -> {
                    String newLabel = CategoryPreferences.sanitizeLabel(input.getText().toString());
                    if (TextUtils.isEmpty(newLabel)) {
                        input.setError("Please enter a source");
                        return;
                    }
                    renameSource(chip, source, newLabel);
                })
                .show();
    }

    private void renameSource(Chip chip, SourceItem source, String newLabel) {
        String oldLabel = source.label;
        Async.runIo(() -> {
            SourceItem updated = source.id != null ? remoteRepo.updateSource(source.id, newLabel, source.type) : null;
            boolean remoteOk = updated != null;
            if (!remoteOk) {
                updated = new SourceItem(source.id, newLabel, source.type);
            }
            if (categoryPreferences != null) {
                categoryPreferences.renameCustomIncomeSource(oldLabel, newLabel);
            }
            SourceItem finalUpdated = updated;
            Async.runMain(() -> {
                chip.setText(finalUpdated.label);
                chip.setTag(finalUpdated.label);
                chip.setTag(R.id.tag_source_object, finalUpdated);
                if (chip.isChecked()) selectedCategory = finalUpdated.label;
                Toast.makeText(requireContext(), remoteOk ? "Updated" : "Updated offline", Toast.LENGTH_SHORT).show();
            });
        });
    }

    private void confirmDelete(Chip chip, SourceItem source) {
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Delete source")
                .setMessage("Remove \"" + source.label + "\"?")
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Delete", (d, w) -> deleteSource(chip, source))
                .show();
    }

    private void deleteSource(Chip chip, SourceItem source) {
        Async.runIo(() -> {
            boolean remoteOk = source.id != null && remoteRepo.deleteSource(source.id);
            if (categoryPreferences != null) categoryPreferences.removeCustomIncomeSource(source.label);
            Async.runMain(() -> {
                if (groupSource != null) groupSource.removeView(chip);
                if (chip.isChecked()) {
                    lastCheckedChipId = View.NO_ID;
                    bindChipGroup(groupSource, chipOtherSource, this::showCustomIncomeDialog, value -> selectedCategory = value);
                }
                Toast.makeText(requireContext(), remoteOk ? "Deleted" : "Deleted offline", Toast.LENGTH_SHORT).show();
            });
        });
    }

    private interface ChipSelectionListener {
        void onValueSelected(String value);
    }
}
