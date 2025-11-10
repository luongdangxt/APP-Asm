package com.example.personalfinancialmanagement;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.viewpager2.widget.ViewPager2;
import androidx.activity.EdgeToEdge;
import androidx.core.view.WindowCompat;
import android.graphics.Color;
import android.view.WindowInsetsController;

import java.util.Arrays;
import java.util.List;

public class OnboardingActivity extends AppCompatActivity {
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
        setContentView(R.layout.activity_onboarding);

        ViewPager2 pager = findViewById(R.id.pager);
        // Disable swipe to keep onboarding fixed on a single screen
        pager.setUserInputEnabled(false);
        Button btn = findViewById(R.id.btn_get_started);

        List<OnboardingPage> pages = Arrays.asList(
                new OnboardingPage(R.drawable.ic_launcher_foreground, "Note Down Expenses", "Daily note your expenses to help manage money"),
                new OnboardingPage(R.drawable.ic_launcher_foreground, "Simple Money Management", "Get notifications when you overspend"),
                new OnboardingPage(R.drawable.ic_launcher_foreground, "Easy to Track and Analyze", "Tracking helps ensure you don't overspend")
        );
        pager.setAdapter(new OnboardingAdapter(pages));

        btn.setOnClickListener(v -> {
            SharedPreferences sp = getSharedPreferences("prefs", MODE_PRIVATE);
            sp.edit().putBoolean("onboarding_done", true).apply();
            startActivity(new Intent(this, LoginActivity.class));
            finish();
        });
    }
}
