package com.firstapp.kidredpawpaws;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.FrameLayout;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

public class SplashActivity extends AppCompatActivity {

    private FrameLayout flLogo;
    private TextView tvAppName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

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
                navigateToLogin();
            }
        }, 2200);
    }

    private void navigateToLogin() {
        startActivity(new Intent(SplashActivity.this, LoginActivity.class));
        finish();
    }
}