package com.example.personalfinancialmanagement;

import android.os.Bundle;
import android.widget.Button;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.activity.EdgeToEdge;
import androidx.core.view.WindowCompat;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.graphics.Insets;
import android.graphics.Color;
import android.view.WindowInsetsController;

public class RegisterActivity extends AppCompatActivity {
    private UserRepository userRepository;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        getWindow().setStatusBarColor(Color.TRANSPARENT);
        getWindow().setNavigationBarColor(Color.TRANSPARENT);
        WindowInsetsController insetsController = getWindow().getInsetsController();
        if (insetsController != null) {
            insetsController.setSystemBarsAppearance(0, WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS);
            insetsController.setSystemBarsAppearance(0, WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS);
        }
        setContentView(R.layout.activity_register);
        View root = findViewById(R.id.register_root);
        if (root != null) {
            ViewCompat.setOnApplyWindowInsetsListener(root, (v, insets) -> {
                Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
                // Apply only top padding to avoid vertical jump when IME shows
                v.setPadding(v.getPaddingLeft(), bars.top + 0, v.getPaddingRight(), v.getPaddingBottom());
                return insets;
            });
        }
        userRepository = new UserRepository(this);

        EditText username = findViewById(R.id.reg_username);
        EditText email = findViewById(R.id.reg_email);
        EditText phone = findViewById(R.id.reg_phone);
        com.google.android.material.textfield.MaterialAutoCompleteTextView dropCountry = findViewById(R.id.drop_country);
        if (dropCountry != null) {
            final String[] items = new String[]{"+84 (Vietnam)", "+1 (US/CA)", "+81 (Japan)", "+82 (Korea)", "+86 (China)", "+65 (Singapore)", "+60 (Malaysia)"};
            final String[] codesOnly = new String[]{"+84", "+1", "+81", "+82", "+86", "+65", "+60"};
            dropCountry.setAdapter(new android.widget.ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, items));
            dropCountry.setText(codesOnly[0], false);
            dropCountry.setOnItemClickListener((parent, v1, position, id) -> dropCountry.setText(codesOnly[position], false));
            dropCountry.setOnClickListener(v -> dropCountry.showDropDown());
        }
        EditText password = findViewById(R.id.reg_password);
        EditText confirm = findViewById(R.id.reg_confirm);
        Button register = findViewById(R.id.btn_register_button);
        TextView toLogin = findViewById(R.id.tv_to_login);

        register.setOnClickListener(v -> {
            String u = username.getText().toString().trim();
            String e = email.getText().toString().trim();
            String phLocal = phone.getText().toString().trim();
            String cc = dropCountry != null ? dropCountry.getText().toString() : "+84";
            String codeDigits = cc.replaceAll("[^0-9]", "");
            String ph = (codeDigits.isEmpty()?"":codeDigits) + phLocal.replaceAll("[^0-9]", "");
            String p = password.getText().toString();
            String c = confirm.getText().toString();
            android.widget.CheckBox cb = findViewById(R.id.cb_terms);

            if (u.isEmpty() || e.isEmpty() || ph.isEmpty() || p.isEmpty() || c.isEmpty()) {
                Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show();
                return;
            }
            if (!android.util.Patterns.EMAIL_ADDRESS.matcher(e).matches()) {
                Toast.makeText(this, "Invalid email", Toast.LENGTH_SHORT).show();
                return;
            }
            if (!isValidInternationalPhone(ph)) {
                Toast.makeText(this, "Invalid phone number or country code", Toast.LENGTH_SHORT).show();
                return;
            }
            if (!isStrongPassword(p)) {
                Toast.makeText(this, "Password must be 8+ chars with upper, lower, digit, special", Toast.LENGTH_LONG).show();
                return;
            }
            if (!p.equals(c)) {
                Toast.makeText(this, "Passwords do not match", Toast.LENGTH_SHORT).show();
                return;
            }
            if (cb != null && !cb.isChecked()) {
                Toast.makeText(this, "You must accept the terms", Toast.LENGTH_SHORT).show();
                return;
            }
            // Simulated email verification: prompt one-time code (demo: 123456)
            android.widget.EditText codeInput = new android.widget.EditText(this);
            codeInput.setHint("Enter 6-digit code");
            codeInput.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
            new com.google.android.material.dialog.MaterialAlertDialogBuilder(this)
                    .setTitle("Verify email")
                    .setMessage("A verification code has been sent to " + e + ". (Demo code: 123456)")
                    .setView(codeInput)
                    .setPositiveButton("Verify", (d,w)->{
                        String code = codeInput.getText().toString().trim();
                        if (!"123456".equals(code)) { Toast.makeText(this, "Invalid code", Toast.LENGTH_SHORT).show(); return; }
                        Async.runIo(() -> {
                            long id = userRepository.register(u, p);
                            if (id > 0) {
                                // Save additional profile fields
                                UserDao dao = AppDatabase.getInstance(this).userDao();
                                User newUser = dao.findByUsername(u);
                                if (newUser != null) {
                                    newUser.fullName = ((EditText)findViewById(R.id.reg_fullname)).getText().toString().trim();
                                    newUser.email = e;
                                    newUser.phone = "+" + ph; // store with +
                                    dao.update(newUser);
                                }
                                Async.runMain(() -> { Toast.makeText(this, "Account created. Please login.", Toast.LENGTH_SHORT).show(); finish(); });
                            } else {
                                Async.runMain(() -> Toast.makeText(this, "Username already exists", Toast.LENGTH_SHORT).show());
                            }
                        });
                    })
                    .setNegativeButton("Cancel", null)
                    .show();
        });

        toLogin.setOnClickListener(v -> {
            finish();
        });
    }

    // Very lightweight international phone validation with country-length map
    private boolean isValidInternationalPhone(String raw) {
        String s = raw.replaceAll("\\s+", "");
        if (s.startsWith("+")) s = s.substring(1);
        if (!s.matches("\\d{6,15}")) return false; // E.164 bounds
        // Extract country code by known prefixes
        int[] codes = new int[]{84,1,81,82,86,65,60};
        int code = -1; String rest = null;
        for (int c : codes) {
            String cs = String.valueOf(c);
            if (s.startsWith(cs)) { code = c; rest = s.substring(cs.length()); break; }
        }
        if (code == -1 || rest == null) return false;
        // length heuristics per country code
        int len = rest.length();
        switch (code) {
            case 84: return len>=9 && len<=10; // Vietnam
            case 1: return len==10; // US/CA
            case 81: return len==10; // Japan
            case 82: return len>=9 && len<=10; // Korea
            case 86: return len==11; // China
            case 65: return len==8;  // Singapore
            case 60: return len>=9 && len<=10; // Malaysia
            default: return len>=6 && len<=12;
        }
    }

    private boolean isStrongPassword(String p) {
        if (p == null || p.length() < 8) return false;
        boolean up=false, low=false, dig=false, sp=false;
        for (char ch : p.toCharArray()) {
            if (Character.isUpperCase(ch)) up=true; else if (Character.isLowerCase(ch)) low=true; else if (Character.isDigit(ch)) dig=true; else sp=true;
        }
        return up && low && dig && sp;
    }
}
