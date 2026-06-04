package com.firstapp.kidredpawpaws;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.viewpager2.widget.ViewPager2;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;
import java.util.ArrayList;
import java.util.List;

public class OnboardingActivity extends AppCompatActivity {

    private OnboardingAdapter onboardingAdapter;
    private ViewPager2 viewPager;
    private Button btnNext;
    private TextView tvBack;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_onboarding);

        viewPager = findViewById(R.id.viewPagerOnboarding);
        btnNext = findViewById(R.id.btn_next);
        tvBack = findViewById(R.id.tv_back);

        setupOnboardingItems();

        viewPager.setAdapter(onboardingAdapter);

        TabLayout tabLayout = findViewById(R.id.tab_layout_indicator);
        new TabLayoutMediator(tabLayout, viewPager, (tab, position) -> {}).attach();

        viewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                super.onPageSelected(position);
                if (position == onboardingAdapter.getItemCount() - 1) {
                    btnNext.setText("Get Started");
                } else {
                    btnNext.setText("Next");
                }

                if (position == 0) {
                    tvBack.setVisibility(View.INVISIBLE);
                } else {
                    tvBack.setVisibility(View.VISIBLE);
                }
            }
        });

        btnNext.setOnClickListener(v -> {
            if (viewPager.getCurrentItem() + 1 < onboardingAdapter.getItemCount()) {
                viewPager.setCurrentItem(viewPager.getCurrentItem() + 1);
            } else {
                completeOnboarding();
            }
        });

        tvBack.setOnClickListener(v -> {
            if (viewPager.getCurrentItem() > 0) {
                viewPager.setCurrentItem(viewPager.getCurrentItem() - 1);
            }
        });
    }

    private void setupOnboardingItems() {
        List<OnboardingItem> onboardingItems = new ArrayList<>();

        onboardingItems.add(new OnboardingItem(
                "Welcome to Kindred Paws",
                "Manage your pet’s health, grooming, and appointments in one place.",
                android.R.drawable.ic_menu_gallery
        ));

        onboardingItems.add(new OnboardingItem(
                "Book Services Easily",
                "Schedule check-ups, vaccinations, and grooming in just a few taps.",
                android.R.drawable.ic_menu_agenda
        ));

        onboardingItems.add(new OnboardingItem(
                "Track Pet History",
                "View your pet’s records, upcoming visits, and care updates anytime.",
                android.R.drawable.ic_menu_report_image
        ));

        onboardingAdapter = new OnboardingAdapter(onboardingItems);
    }

    private void completeOnboarding() {
        SharedPreferences sharedPreferences = getSharedPreferences("kindred_paws_prefs", MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putBoolean("onboarding_complete", true);
        editor.apply();

        startActivity(new Intent(OnboardingActivity.this, LoginActivity.class));
        finish();
    }
}