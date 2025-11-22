package com.example.personalfinancialmanagement.auth;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import androidx.activity.EdgeToEdge;
import androidx.core.view.WindowCompat;
import android.graphics.Color;
import android.view.WindowInsetsController;
import android.os.Build;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.checkbox.MaterialCheckBox;

import com.example.personalfinancialmanagement.Async;
import com.example.personalfinancialmanagement.MainActivity;
import com.example.personalfinancialmanagement.SessionManager;
import com.example.personalfinancialmanagement.data.user.User;
import com.example.personalfinancialmanagement.data.user.UserRepository;
import com.example.personalfinancialmanagement.R;

public class LoginActivity extends AppCompatActivity {
    private UserRepository userRepository;
    private SessionManager sessionManager;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        getWindow().setStatusBarColor(Color.TRANSPARENT);
        getWindow().setNavigationBarColor(Color.TRANSPARENT);
        if (Build.VERSION.SDK_INT >= 30) {
            WindowInsetsController c = getWindow().getInsetsController();
            if (c != null) {
                c.setSystemBarsAppearance(0, WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS);
                c.setSystemBarsAppearance(0, WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS);
            }
        }
        setContentView(R.layout.activity_login);

        userRepository = new UserRepository(this);
        sessionManager = new SessionManager(this);

        EditText username = findViewById(R.id.username);
        EditText password = findViewById(R.id.password);
        MaterialCheckBox rememberLogin = findViewById(R.id.cb_remember_login);
        Button login = findViewById(R.id.btn_login_button);
        TextView toRegister = findViewById(R.id.to_register);

        if (rememberLogin != null) {
            rememberLogin.setChecked(sessionManager.isRememberEnabled());
        }
        String rememberedUsername = sessionManager.getRememberedUsername();
        if (rememberedUsername != null && !rememberedUsername.isEmpty()) {
            username.setText(rememberedUsername);
        }
        attemptAutoLoginIfNeeded();

        login.setOnClickListener(v -> {
            final String u = username.getText().toString().trim();
            final String p = password.getText().toString();
            if (u.isEmpty() || p.isEmpty()) {
                Toast.makeText(this, "Enter username and password", Toast.LENGTH_SHORT).show();
                return;
            }
            Async.runIo(() -> {
                try {
                    User user = userRepository.login(u, p);
                    Async.runMain(() -> {
                        if (user != null) {
                            boolean remember = rememberLogin != null && rememberLogin.isChecked();
                            sessionManager.updateRememberedUser(remember, user.id, user.username);
                            openMain(user.id);
                        } else {
                            Toast.makeText(this, "Invalid credentials", Toast.LENGTH_SHORT).show();
                        }
                    });
                } catch (Throwable t) {
                    Async.runMain(() -> Toast.makeText(this, "Login error: " + t.getMessage(), Toast.LENGTH_LONG).show());
                }
            });
        });

        toRegister.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(LoginActivity.this, RegisterActivity.class));
            }
        });
    }

    private void attemptAutoLoginIfNeeded() {
        if (sessionManager == null || !sessionManager.shouldAutoLogin()) return;
        final long userId = sessionManager.getRememberedUserId();
        Async.runIo(() -> {
            // Try token-based auto login first
            User user = userRepository.fetchMeViaToken();
            if (user == null && userId > 0) {
                user = userRepository.findById(userId);
            }
            User finalUser = user;
            Async.runMain(() -> {
                if (finalUser != null && finalUser.id > 0) {
                    openMain(finalUser.id);
                } else {
                    sessionManager.clear();
                }
            });
        });
    }

    private void openMain(long userId) {
        Intent i = new Intent(this, MainActivity.class);
        i.putExtra("userId", userId);
        startActivity(i);
        finish();
    }
}
