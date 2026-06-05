package com.firstapp.kidredpawpaws;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;

public class OnboardingActivity extends AppCompatActivity {

    private Button btnGetStarted;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_onboarding);

        btnGetStarted = findViewById(R.id.btn_get_started);

        btnGetStarted.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                completeOnboarding();
            }
        });
    }

    private void completeOnboarding() {
        SharedPreferences prefs = getSharedPreferences("app_preferences", MODE_PRIVATE);
        prefs.edit().putBoolean("onboarding_completed", true).apply();

        startActivity(new Intent(OnboardingActivity.this, LoginActivity.class));
        finish();
    }
}
