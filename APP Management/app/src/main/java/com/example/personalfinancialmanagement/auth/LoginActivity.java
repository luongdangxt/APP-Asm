package com.example.personalfinancialmanagement;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import androidx.activity.EdgeToEdge;
import androidx.core.view.WindowCompat;
import android.graphics.Color;
import android.view.WindowInsetsController;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

public class LoginActivity extends AppCompatActivity {
    private UserRepository userRepository;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        getWindow().setStatusBarColor(Color.TRANSPARENT);
        getWindow().setNavigationBarColor(Color.TRANSPARENT);
        WindowInsetsController c = getWindow().getInsetsController();
        if (c != null) {
            c.setSystemBarsAppearance(0, WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS);
            c.setSystemBarsAppearance(0, WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS);
        }
        setContentView(R.layout.activity_login);

        userRepository = new UserRepository(this);

        EditText username = findViewById(R.id.username);
        EditText password = findViewById(R.id.password);
        Button login = findViewById(R.id.btn_login_button);
        TextView toRegister = findViewById(R.id.to_register);

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
                            Intent i = new Intent(this, MainActivity.class);
                            i.putExtra("userId", user.id);
                            startActivity(i);
                            finish();
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
}
