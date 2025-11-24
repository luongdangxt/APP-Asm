package com.example.personalfinancialmanagement;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import android.util.TypedValue;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;

import com.google.android.material.switchmaterial.SwitchMaterial;
import com.example.personalfinancialmanagement.auth.LoginActivity;
import com.example.personalfinancialmanagement.auth.PasswordHasher;
import com.example.personalfinancialmanagement.data.user.User;
import com.example.personalfinancialmanagement.data.user.UserDao;
import com.example.personalfinancialmanagement.data.user.UserRepository;

import java.util.Locale;
import java.util.List;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;

public class ProfileFragment extends Fragment {
    public static final String ARG_USER_ID = "userId";
    private ActivityResultLauncher<Intent> pickImageLauncher;
    private ImageView ivAvatar;
    private long currentUserId = -1;

    public static ProfileFragment newInstance(long userId) {
        ProfileFragment f = new ProfileFragment();
        Bundle b = new Bundle();
        b.putLong(ARG_USER_ID, userId);
        f.setArguments(b);
        return f;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        pickImageLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() != android.app.Activity.RESULT_OK || result.getData() == null) return;
                    Uri uri = result.getData().getData();
                    if (uri == null || currentUserId <= 0) return;
                    Async.runIo(() -> {
                        boolean ok = saveAvatarFromUri(uri, currentUserId);
                        Async.runMain(() -> {
                            if (!isAdded()) return;
                            if (ok) {
                                loadAvatar(currentUserId);
                            } else {
                                Toast.makeText(requireContext(), "Failed to save image", Toast.LENGTH_SHORT).show();
                            }
                        });
                    });
                }
        );
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.activity_profile, container, false);

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

        long userId = getArguments() != null ? getArguments().getLong(ARG_USER_ID, -1) : -1;
        TextView tvName = root.findViewById(R.id.tv_name);
        TextView tvEmailSubtitle = root.findViewById(R.id.tv_sub);
        TextView tvMemberSince = root.findViewById(R.id.tv_profile_member_since);
        TextView tvIncomeMonth = root.findViewById(R.id.tv_profile_income_month);
        TextView tvExpenseMonth = root.findViewById(R.id.tv_profile_expense_month);
        TextView tvBudgetSummary = root.findViewById(R.id.tv_profile_budget_summary);
        ivAvatar = root.findViewById(R.id.iv_profile_avatar);

        EditText edUsername = root.findViewById(R.id.ed_username);
        EditText edFullName = root.findViewById(R.id.ed_full_name);
        EditText edEmail = root.findViewById(R.id.ed_email);
        EditText edPhone = root.findViewById(R.id.ed_phone);
        EditText edNewPass = root.findViewById(R.id.ed_new_password);
        EditText edConfirm = root.findViewById(R.id.ed_confirm_password);
        UserDao userDao = AppDatabase.getInstance(requireContext()).userDao();
        UserRepository userRepo = new UserRepository(requireContext());
        final User[] holder = new User[1];
        currentUserId = userId;
        loadAvatar(userId);
        if (ivAvatar != null) {
            ivAvatar.setOnClickListener(v -> openImagePicker());
        }
        if (userId > 0) {
            Async.runIo(() -> {
                User u = userDao.findById(userId);
                if (u == null) {
                    // Fallback to server data if local cache is missing
                    User remote = userRepo.fetchMeViaToken();
                    if (remote == null) {
                        remote = userRepo.findById(userId);
                    }
                    if (remote != null) {
                        // Upsert into local DB using server-provided id for consistency
                        User existing = userDao.findByUsername(remote.username);
                        if (existing == null) {
                            long insertedId = userDao.insert(remote);
                            if (insertedId > 0) remote.id = insertedId;
                        } else {
                            remote.id = existing.id;
                            userDao.update(remote);
                        }
                        u = remote;
                    }
                }
                holder[0] = u;
                final User uiUser = u;
                Async.runMain(() -> {
                    if (uiUser != null) {
                        tvName.setText(uiUser.fullName != null && !uiUser.fullName.trim().isEmpty() ? uiUser.fullName : uiUser.username);
                        edUsername.setText(uiUser.username);
                        if (uiUser.fullName != null) edFullName.setText(uiUser.fullName);
                        if (uiUser.email != null) edEmail.setText(uiUser.email);
                        if (uiUser.phone != null) edPhone.setText(uiUser.phone);
                        if (tvEmailSubtitle != null) tvEmailSubtitle.setText(uiUser.email != null ? uiUser.email : "Tap edit to add your email");
                        if (tvMemberSince != null) tvMemberSince.setText(String.format(Locale.getDefault(), "User ID #%d", uiUser.id));
                    }
                });
            });
        }

        setEditable(root, false, edUsername, edFullName, edEmail, edPhone, edNewPass, edConfirm);
        root.findViewById(R.id.btn_edit).setOnClickListener(v -> {
            boolean nowEditable = !edUsername.isEnabled();
            setEditable(root, nowEditable, edUsername, edFullName, edEmail, edPhone, edNewPass, edConfirm);
        });

        root.findViewById(R.id.btn_save).setOnClickListener(v -> {
            final User current = holder[0];
            if (current == null) return;
            String newName = edUsername.getText().toString().trim();
            String newPass = edNewPass.getText().toString();
            String confirm = edConfirm.getText().toString();
            String fullName = edFullName.getText().toString().trim();
            String email = edEmail.getText().toString().trim();
            String phone = edPhone.getText().toString().trim();

            if (newName.isEmpty()) { Toast.makeText(requireContext(), "Username required", Toast.LENGTH_SHORT).show(); return; }
            final String newPasswordForServer = newPass;
            Async.runIo(() -> {
                User existing = userDao.findByUsername(newName);
                if (existing != null && existing.id != current.id) {
                    Async.runMain(() -> Toast.makeText(requireContext(), "Username already exists", Toast.LENGTH_SHORT).show());
                    return;
                }
                User working = current;
                working.username = newName;
                working.fullName = fullName.isEmpty()? null : fullName;
                working.email = email.isEmpty()? null : email;
                working.phone = phone.isEmpty()? null : phone;
                if (!newPass.isEmpty()) {
                    if (!newPass.equals(confirm)) {
                        Async.runMain(() -> Toast.makeText(requireContext(), "Passwords do not match", Toast.LENGTH_SHORT).show());
                        return;
                    }
                }
                User updatedRemote = userRepo.updateProfile(working, newPasswordForServer);
                if (updatedRemote == null) {
                    final String err = userRepo.getLastError() != null ? userRepo.getLastError() : "Unable to update profile on server";
                    Async.runMain(() -> Toast.makeText(requireContext(), err, Toast.LENGTH_SHORT).show());
                    return;
                }
                if (!newPass.isEmpty()) {
                    updatedRemote.passwordHash = PasswordHasher.sha256(newPass);
                }
                working = updatedRemote;
                holder[0] = working;
                userDao.update(working);
                User finalU = working;
                Async.runMain(() -> {
                    tvName.setText(finalU.fullName != null && !finalU.fullName.trim().isEmpty() ? finalU.fullName : finalU.username);
                    if (tvEmailSubtitle != null) tvEmailSubtitle.setText(finalU.email != null ? finalU.email : "Tap edit to add your email");
                    Toast.makeText(requireContext(), "Saved", Toast.LENGTH_SHORT).show();
                    setEditable(root, false, edUsername, edFullName, edEmail, edPhone, edNewPass, edConfirm);
                });
            });
        });

        SwitchMaterial swNotif = root.findViewById(R.id.sw_profile_notifications);
        SwitchMaterial swBudget = root.findViewById(R.id.sw_profile_budget_alerts);
        SettingsRepository prefs = new SettingsRepository(requireContext());
        if (swNotif != null) {
            swNotif.setChecked(prefs.notificationsEnabled());
            swNotif.setOnCheckedChangeListener((button, checked) -> prefs.setNotificationsEnabled(checked));
        }
        if (swBudget != null) {
            swBudget.setChecked(prefs.budgetAlertsEnabled());
            swBudget.setOnCheckedChangeListener((button, checked) -> prefs.setBudgetAlertsEnabled(checked));
        }

        View btnManageData = root.findViewById(R.id.btn_profile_manage_data);
        if (btnManageData != null) {
            btnManageData.setOnClickListener(v -> {
                if (requireActivity() instanceof MainActivity) {
                    ((MainActivity) requireActivity()).showFragment(ManageDataFragment.newInstance());
                }
            });
        }

        View btnContact = root.findViewById(R.id.btn_profile_contact);
        if (btnContact != null) {
            btnContact.setOnClickListener(v -> openSupportEmail());
        }

        View btnLogout = root.findViewById(R.id.btn_profile_logout);
        if (btnLogout != null) {
            btnLogout.setOnClickListener(v -> {
                new SessionManager(requireContext()).clear();
                Intent i = new Intent(requireContext(), LoginActivity.class);
                i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(i);
            });
        }

        if (userId > 0) {
            loadStats(userId, tvIncomeMonth, tvExpenseMonth, tvBudgetSummary);
        }

        return root;
    }

    private void setEditable(View root, boolean enabled, EditText... edits) {
        for (EditText e : edits) if (e != null) e.setEnabled(enabled);
        View save = root.findViewById(R.id.btn_save);
        if (save != null) save.setEnabled(enabled);
        TextView edit = root.findViewById(R.id.btn_edit);
        if (edit != null) edit.setText(enabled ? "Done" : "Edit");
    }

    private void loadStats(long userId, TextView tvIncome, TextView tvExpense, TextView tvBudget) {
        if (userId <= 0) return;
        final android.content.Context ctx = requireContext().getApplicationContext();
        Async.runIo(() -> {
            long now = System.currentTimeMillis();
            double incomeTotal = 0;
            double expenseTotal = 0;
            double budgetLimit = 0;
            double budgetUsed = 0;
            try {
                IncomeRepository incomeRepo = new IncomeRepository(ctx);
                for (Income income : incomeRepo.listForCurrentMonth(userId, now)) incomeTotal += income.amount;
            } catch (Throwable ignored) {}
            try {
                ExpenseRepository expenseRepo = new ExpenseRepository(ctx);
                for (Expense expense : expenseRepo.listForCurrentMonth(userId, now)) expenseTotal += expense.amount;
            } catch (Throwable ignored) {}
            try {
                int monthKey = MonthUtils.monthKey(now);
                BudgetRepository budgetRepo = new BudgetRepository(ctx);
                List<Budget> budgets = budgetRepo.listForMonth(userId, monthKey);
                for (Budget b : budgets) {
                    budgetLimit += b.limitAmount;
                }
                budgetUsed = expenseTotal;
            } catch (Throwable ignored) {}

            final double fIncome = incomeTotal;
            final double fExpense = expenseTotal;
            final double fBudgetLimit = budgetLimit;
            final double fBudgetUsed = budgetUsed;

            Async.runMain(() -> {
                if (!isAdded()) return;
                if (tvIncome != null) tvIncome.setText(String.format(Locale.getDefault(), "$%,.2f", fIncome));
                if (tvExpense != null) tvExpense.setText(String.format(Locale.getDefault(), "$%,.2f", fExpense));
                if (tvBudget != null) {
                    if (fBudgetLimit > 0) {
                        tvBudget.setText(String.format(Locale.getDefault(), "Budgets: $%,.2f of $%,.2f used", fBudgetUsed, fBudgetLimit));
                    } else {
                        tvBudget.setText("No budgets configured yet");
                    }
                }
            });
        });
    }

    private void openSupportEmail() {
        Intent email = new Intent(Intent.ACTION_SENDTO);
        email.setData(Uri.parse("mailto:"));
        email.putExtra(Intent.EXTRA_EMAIL, new String[]{"support@example.com"});
        email.putExtra(Intent.EXTRA_SUBJECT, "PFM Support");
        try {
            startActivity(Intent.createChooser(email, "Contact support"));
        } catch (Exception ignored) {
            Toast.makeText(requireContext(), "No email app installed", Toast.LENGTH_SHORT).show();
        }
    }

    private void openImagePicker() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("image/*");
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        pickImageLauncher.launch(Intent.createChooser(intent, "Choose profile photo"));
    }

    private void loadAvatar(long userId) {
        if (ivAvatar == null || userId <= 0) return;
        File f = getAvatarFile(userId);
        if (!f.exists()) {
            ivAvatar.setImageResource(R.drawable.ic_avatar);
            return;
        }
        Async.runIo(() -> {
            Bitmap bmp = BitmapFactory.decodeFile(f.getAbsolutePath());
            if (bmp != null) {
                Async.runMain(() -> {
                    if (isAdded() && ivAvatar != null) {
                        ivAvatar.setImageBitmap(bmp);
                    }
                });
            }
        });
    }

    private boolean saveAvatarFromUri(Uri uri, long userId) {
        try (InputStream is = requireContext().getContentResolver().openInputStream(uri)) {
            if (is == null) return false;
            Bitmap src = BitmapFactory.decodeStream(is);
            if (src == null) return false;
            Bitmap bmp = src;
            int max = 512;
            if (src.getWidth() > max || src.getHeight() > max) {
                float scale = Math.min((float) max / src.getWidth(), (float) max / src.getHeight());
                int w = Math.max(1, Math.round(src.getWidth() * scale));
                int h = Math.max(1, Math.round(src.getHeight() * scale));
                bmp = Bitmap.createScaledBitmap(src, w, h, true);
                if (bmp != src) src.recycle();
            }
            File out = getAvatarFile(userId);
            try (FileOutputStream fos = new FileOutputStream(out)) {
                bmp.compress(Bitmap.CompressFormat.JPEG, 90, fos);
                fos.flush();
            }
            if (bmp != src) bmp.recycle();
            return true;
        } catch (Exception e) {
            Log.e("ProfileFragment", "saveAvatarFromUri error", e);
            return false;
        }
    }

    private File getAvatarFile(long userId) {
        File dir = requireContext().getFilesDir();
        return new File(dir, "avatar_" + userId + ".jpg");
    }
}
