package com.example.personalfinancialmanagement;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.chip.ChipGroup;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.switchmaterial.SwitchMaterial;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

public class NotificationsFragment extends Fragment {
    public static final String ARG_USER_ID = "userId";

    private long userId;
    private NotificationRepository repository;
    private NotificationsAdapter adapter;

    private TextView tvUnreadCount;
    private TextView tvLastSync;
    private TextView tvEmptyState;
    private String currentFilter = "all";
    private boolean unreadOnly = false;
    private long lastRefreshedAt = 0;

    public static NotificationsFragment newInstance(long userId) {
        NotificationsFragment fragment = new NotificationsFragment();
        Bundle args = new Bundle();
        args.putLong(ARG_USER_ID, userId);
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.activity_notifications, container, false);
        userId = getArguments() != null ? getArguments().getLong(ARG_USER_ID, -1) : -1;
        repository = new NotificationRepository(requireContext());

        View inner = ((androidx.coordinatorlayout.widget.CoordinatorLayout) root).getChildAt(0);
        if (inner != null) {
            final int baseL = inner.getPaddingLeft();
            final int baseT = inner.getPaddingTop();
            final int baseR = inner.getPaddingRight();
            final int baseB = inner.getPaddingBottom();
            ViewCompat.setOnApplyWindowInsetsListener(inner, (v, insets) -> {
                Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
                v.setPadding(baseL, baseT, baseR, baseB + bars.bottom);
                return insets;
            });
        }

        ImageButton back = root.findViewById(R.id.btn_back);
        if (back != null) {
            back.setVisibility(View.VISIBLE);
            back.setOnClickListener(v -> {
                if (requireActivity() != null) requireActivity().onBackPressed();
            });
        }

        RecyclerView rv = root.findViewById(R.id.rv_notifications);
        rv.setLayoutManager(new LinearLayoutManager(requireContext()));
        adapter = new NotificationsAdapter(new AdapterListener());
        rv.setAdapter(adapter);

        tvUnreadCount = root.findViewById(R.id.tv_unread_count);
        tvLastSync = root.findViewById(R.id.tv_last_sync);
        tvEmptyState = root.findViewById(R.id.tv_empty_state);

        View btnMarkAll = root.findViewById(R.id.btn_mark_all_read);
        if (btnMarkAll != null) {
            btnMarkAll.setOnClickListener(v -> markAllRead());
        }

        View btnClear = root.findViewById(R.id.btn_clear_all);
        if (btnClear != null) {
            btnClear.setOnClickListener(v -> confirmClearAll());
        }

        View btnRefresh = root.findViewById(R.id.btn_refresh);
        if (btnRefresh != null) {
            btnRefresh.setOnClickListener(v -> refresh(false));
        }

        SwitchMaterial swUnread = root.findViewById(R.id.switch_unread_only);
        if (swUnread != null) {
            swUnread.setOnCheckedChangeListener((buttonView, isChecked) -> {
                unreadOnly = isChecked;
                refresh(false);
            });
        }

        ChipGroup chips = root.findViewById(R.id.chip_group_filters);
        if (chips != null) {
            chips.setOnCheckedStateChangeListener((group, checkedIds) -> {
                if (checkedIds == null || checkedIds.isEmpty()) return;
                int id = checkedIds.get(0);
                if (id == R.id.chip_budget) currentFilter = NotificationRepository.TYPE_BUDGET;
                else if (id == R.id.chip_goals) currentFilter = NotificationRepository.TYPE_GOAL;
                else if (id == R.id.chip_reminders) currentFilter = NotificationRepository.TYPE_REMINDER;
                else if (id == R.id.chip_system) currentFilter = NotificationRepository.TYPE_SYSTEM;
                else currentFilter = "all";
                refresh(false);
            });
        }

        ImageButton settingsShortcut = root.findViewById(R.id.btn_settings_shortcut);
        if (settingsShortcut != null) {
            settingsShortcut.setOnClickListener(v -> {
                if (requireActivity() instanceof MainActivity) {
                    ((MainActivity) requireActivity()).showFragment(new SettingsFragment());
                }
            });
        }

        refresh(true);
        return root;
    }

    private void refresh(boolean ensureSeed) {
        Async.runIo(() -> {
            if (ensureSeed) {
                try {
                    repository.ensureSeeded(userId);
                } catch (Throwable ignored) {
                }
            }
            List<AppNotification> items = repository.list(userId, currentFilter, unreadOnly);
            long unread = repository.unreadCount(userId);
            long total = repository.totalCount(userId);
            lastRefreshedAt = System.currentTimeMillis();
            Async.runMain(() -> updateUi(items, unread, total));
        });
    }

    private void updateUi(List<AppNotification> items, long unread, long total) {
        if (!isAdded()) return;
        adapter.submit(items);
        tvUnreadCount.setText(String.valueOf(unread));
        String lastSyncText = lastRefreshedAt == 0 ? "Never" : new SimpleDateFormat("MMM d, h:mm a", Locale.getDefault()).format(lastRefreshedAt);
        tvLastSync.setText("Updated " + lastSyncText);
        boolean empty = items == null || items.isEmpty();
        tvEmptyState.setVisibility(empty ? View.VISIBLE : View.GONE);
        tvEmptyState.setText(empty ? (unreadOnly ? "No unread notifications." : "No notifications yet.") : "");
    }

    private void markAllRead() {
        Async.runIo(() -> {
            repository.markAllRead(userId);
            refresh(false);
        });
    }

    private void confirmClearAll() {
        if (getContext() == null) return;
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Clear notifications")
                .setMessage("Remove all notifications from the inbox?")
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Clear", (dialog, which) -> clearAll())
                .show();
    }

    private void clearAll() {
        Async.runIo(() -> {
            repository.clear(userId);
            refresh(true);
        });
    }

    private class AdapterListener implements NotificationsAdapter.Listener {
        @Override
        public void onToggleRead(AppNotification item, boolean markRead) {
            Async.runIo(() -> {
                repository.markRead(item.id, markRead);
                refresh(false);
            });
        }

        @Override
        public void onDelete(AppNotification item) {
            Async.runIo(() -> {
                repository.delete(item.id);
                refresh(false);
            });
        }

        @Override
        public void onAction(AppNotification item) {
            if (!isAdded()) return;
            Async.runIo(() -> repository.markRead(item.id, true));
            refresh(false);
            if (requireActivity() instanceof MainActivity) {
                MainActivity main = (MainActivity) requireActivity();
                switch (item.actionTarget != null ? item.actionTarget : "") {
                    case "budget_overview":
                        main.showFragment(TotalExpensesFragment.newInstance(userId, System.currentTimeMillis()));
                        break;
                    case "goals":
                        main.showFragment(SavingsFragment.newInstance(userId));
                        break;
                    case "recurring":
                        main.showFragment(AddExpenseFragment.newInstance(userId)); // fallback to expenses addition
                        break;
                    case "feedback":
                        main.showFragment(FeedbackFragment.newInstance(userId));
                        break;
                    default:
                        break;
                }
            }
        }
    }
}
