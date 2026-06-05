package com.firstapp.kidredpawpaws;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.FrameLayout;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;

import com.firstapp.kidredpawpaws.utils.SessionManager;

public class SplashActivity extends AppCompatActivity {

    private FrameLayout flLogo;
    private TextView tvAppName;
    private SessionManager sessionManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Apply dark mode preference before super.onCreate
        applyDarkModePreference();
        
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        sessionManager = new SessionManager(this);

        flLogo = findViewById(R.id.fl_logo);
        tvAppName = findViewById(R.id.tv_app_name);

        // Initial states
        flLogo.setAlpha(0f);
        flLogo.setScaleX(0.7f);
        flLogo.setScaleY(0.7f);
        
        // Start Logo Animation
        flLogo.animate()
                .alpha(1f)
                .scaleX(1f)
                .scaleY(1f)
                .setDuration(1000)
                .setInterpolator(new AccelerateDecelerateInterpolator())
                .withEndAction(new Runnable() {
                    @Override
                    public void run() {
                        // Fade in App Name
                        tvAppName.animate()
                                .alpha(1f)
                                .setDuration(800)
                                .start();
                    }
                })
                .start();

        // Navigate after total animation time
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                handleNavigation();
            }
        }, 2200);
    }

    private void applyDarkModePreference() {
        SharedPreferences prefs = getSharedPreferences("app_preferences", MODE_PRIVATE);
        boolean isDarkMode = prefs.getBoolean("dark_mode_enabled", false);
        if (isDarkMode) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        }
    }

    private void handleNavigation() {
        SharedPreferences prefs = getSharedPreferences("app_preferences", MODE_PRIVATE);
        boolean onboardingCompleted = prefs.getBoolean("onboarding_completed", false);

        if (!onboardingCompleted) {
            startActivity(new Intent(SplashActivity.this, OnboardingActivity.class));
        } else {
            if (sessionManager.isLoggedIn()) {
                startActivity(new Intent(SplashActivity.this, MainActivity.class));
            } else {
                startActivity(new Intent(SplashActivity.this, LoginActivity.class));
            }
        }
        finish();
    }
}