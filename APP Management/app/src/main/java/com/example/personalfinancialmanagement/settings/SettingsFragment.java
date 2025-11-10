package com.example.personalfinancialmanagement;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;

import com.google.android.material.switchmaterial.SwitchMaterial;
import com.google.android.material.textfield.MaterialAutoCompleteTextView;

public class SettingsFragment extends Fragment {
    private void openUrl(String url) {
        try { startActivity(new android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(url))); } catch (Exception ignored) {}
    }

    private void showManageAppData() {
        new com.google.android.material.dialog.MaterialAlertDialogBuilder(requireContext())
                .setTitle("Manage App Data")
                .setMessage("Clear this account's data (expenses, incomes, budgets, savings). Your account stays signed in.")
                .setPositiveButton("Clear All", (d, w) -> {
                    Async.runIo(() -> {
                        long uid = requireActivity().getIntent().getLongExtra("userId", -1);
                        try { AppDatabase.getInstance(requireContext()).clearUserData(uid); } catch (Throwable ignored) {}
                        Async.runMain(() -> android.widget.Toast.makeText(requireContext(), "Cleared data for this account", android.widget.Toast.LENGTH_SHORT).show());
                    });
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.activity_settings, container, false);

        View inner = root.findViewById(R.id.settings_root);
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
        if (back != null) back.setVisibility(View.GONE);

        TextView tvThemeSummary = root.findViewById(R.id.tv_theme_summary);
        TextView tvNotificationSummary = root.findViewById(R.id.tv_notification_summary);

        View feedback = root.findViewById(R.id.row_feedback);
        if (feedback != null) feedback.setOnClickListener(v -> {
            if (requireActivity() instanceof MainActivity) {
                long uid = requireActivity().getIntent().getLongExtra("userId", -1);
                ((MainActivity) requireActivity()).showFragment(FeedbackFragment.newInstance(uid));
            }
        });

        SettingsRepository prefs = new SettingsRepository(requireContext());

        SwitchMaterial swNotif = root.findViewById(R.id.sw_notifications);
        if (swNotif != null) {
            swNotif.setChecked(prefs.notificationsEnabled());
            if (tvNotificationSummary != null) {
                tvNotificationSummary.setText(swNotif.isChecked() ? "Stay informed about budgets and saving tips." : "Turn on alerts to receive budgeting reminders.");
            }
            swNotif.setOnCheckedChangeListener((b, v) -> {
                prefs.setNotificationsEnabled(v);
                if (tvNotificationSummary != null) {
                    tvNotificationSummary.setText(v ? "Stay informed about budgets and saving tips." : "Turn on alerts to receive budgeting reminders.");
                }
            });
        }

        View rowDark = root.findViewById(R.id.row_dark_mode);
        if (rowDark != null) {
            if (tvThemeSummary != null) {
                tvThemeSummary.setText(themeSummaryText(prefs.themeMode()));
            }
            rowDark.setOnClickListener(v -> {
                String[] items = new String[]{"System", "Light", "Dark"};
                int mode = Math.max(0, Math.min(2, prefs.themeMode()));
                new com.google.android.material.dialog.MaterialAlertDialogBuilder(requireContext())
                        .setTitle("Dark Mode")
                        .setSingleChoiceItems(items, mode, (d, which) -> {
                            prefs.setThemeMode(which);
                            if (tvThemeSummary != null) {
                                tvThemeSummary.setText(themeSummaryText(which));
                            }
                            int appMode = AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM;
                            if (which == 1) appMode = AppCompatDelegate.MODE_NIGHT_NO;
                            else if (which == 2) appMode = AppCompatDelegate.MODE_NIGHT_YES;
                            AppCompatDelegate.setDefaultNightMode(appMode);
                            try { requireActivity().getIntent().putExtra("openSettingsOnCreate", true); } catch (Throwable ignored) {}
                            d.dismiss();
                            requireActivity().recreate();
                        })
                        .setNegativeButton("Cancel", null)
                        .show();
            });
        }

        View rowRate = root.findViewById(R.id.row_rate);
        if (rowRate != null) rowRate.setOnClickListener(v -> {
            String pkg = requireContext().getPackageName();
            android.content.Intent i = new android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse("market://details?id=" + pkg));
            try { startActivity(i); } catch (Exception e) {
                startActivity(new android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse("https://play.google.com/store/apps/details?id=" + pkg)));
            }
        });
        View rowShare = root.findViewById(R.id.row_share);
        if (rowShare != null) rowShare.setOnClickListener(v -> {
            android.content.Intent s = new android.content.Intent(android.content.Intent.ACTION_SEND);
            s.setType("text/plain");
            s.putExtra(android.content.Intent.EXTRA_TEXT, "Check out this app!");
            startActivity(android.content.Intent.createChooser(s, "Share App"));
        });
        View rowPrivacy = root.findViewById(R.id.row_privacy);
        if (rowPrivacy != null) rowPrivacy.setOnClickListener(v -> openUrl("https://example.com/privacy"));
        View rowTerms = root.findViewById(R.id.row_terms);
        if (rowTerms != null) rowTerms.setOnClickListener(v -> openUrl("https://example.com/terms"));
        View rowCookies = root.findViewById(R.id.row_cookies);
        if (rowCookies != null) rowCookies.setOnClickListener(v -> {
            if (requireActivity() instanceof MainActivity) {
                ((MainActivity) requireActivity()).showFragment(ManageDataFragment.newInstance());
            }
        });
        View rowContact = root.findViewById(R.id.row_contact);
        if (rowContact != null) rowContact.setOnClickListener(v -> {
            android.content.Intent email = new android.content.Intent(android.content.Intent.ACTION_SENDTO);
            email.setData(android.net.Uri.parse("mailto:"));
            email.putExtra(android.content.Intent.EXTRA_EMAIL, new String[]{"support@example.com"});
            email.putExtra(android.content.Intent.EXTRA_SUBJECT, "PFM Support");
            startActivity(email);
        });

        View logout = root.findViewById(R.id.btn_logout);
        if (logout != null) logout.setOnClickListener(v -> {
            android.content.Intent i = new android.content.Intent(requireContext(), LoginActivity.class);
            i.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK | android.content.Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(i);
        });

        return root;
    }

    private String themeSummaryText(int mode) {
        switch (mode) {
            case 1: return "Light appearance";
            case 2: return "Always dark";
            default: return "Follow system";
        }
    }
}
