package com.firstapp.kidredpawpaws;

import android.content.Intent;
import android.os.Bundle;
import android.provider.CalendarContract;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class AppointmentConfirmationActivity extends AppCompatActivity {

    private static final String TAG = "ApptConfirmActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_appointment_confirmation);

        // Get extras from intent
        String petName = getIntent().getStringExtra("pet_name");
        String service = getIntent().getStringExtra("service");
        String staffName = getIntent().getStringExtra("staff_name");
        String displayDate = getIntent().getStringExtra("display_date");
        String displayTime = getIntent().getStringExtra("display_time");
        String location = getIntent().getStringExtra("location");
        String scheduledAt = getIntent().getStringExtra("scheduled_at");
        int durationMinutes = getIntent().getIntExtra("duration_minutes", 30);

        // Fallbacks
        if (petName == null) petName = "Selected pet";
        if (service == null) service = "Scheduled service";
        if (displayDate == null) displayDate = "Selected date";
        if (displayTime == null) displayTime = "Selected time";
        if (location == null) location = "Kindred Paws Pet Care Center";

        // Update UI
        TextView tvSubtitle = findViewById(R.id.tv_subtitle);
        TextView tvConfirmPet = findViewById(R.id.tv_confirm_pet);
        TextView tvConfirmService = findViewById(R.id.tv_confirm_service);
        TextView tvConfirmStaff = findViewById(R.id.tv_confirm_staff);
        TextView tvConfirmDate = findViewById(R.id.tv_confirm_date);
        TextView tvConfirmTime = findViewById(R.id.tv_confirm_time);
        TextView tvConfirmLocation = findViewById(R.id.tv_confirm_location);

        if (tvSubtitle != null) {
            tvSubtitle.setText("Your appointment for " + petName + " has been scheduled successfully.");
        }
        if (tvConfirmPet != null) {
            tvConfirmPet.setText("Pet: " + petName);
        }
        if (tvConfirmService != null) {
            tvConfirmService.setText("Service: " + service);
        }
        if (tvConfirmStaff != null) {
            if (staffName != null && !staffName.isEmpty()) {
                tvConfirmStaff.setText("Staff: " + staffName);
                tvConfirmStaff.setVisibility(View.VISIBLE);
            } else {
                tvConfirmStaff.setVisibility(View.GONE);
            }
        }
        if (tvConfirmDate != null) {
            tvConfirmDate.setText("Date: " + displayDate);
        }
        if (tvConfirmTime != null) {
            tvConfirmTime.setText("Time: " + displayTime);
        }
        if (tvConfirmLocation != null) {
            tvConfirmLocation.setText("Location: " + location);
        }

        Button btnAddCalendar = findViewById(R.id.btn_add_calendar);
        Button btnBackHome = findViewById(R.id.btn_back_home);

        String finalService = service;
        String finalPetName = petName;
        String finalLocation = location;
        
        if (btnAddCalendar != null) {
            btnAddCalendar.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    addToCalendar(finalService, finalPetName, finalLocation, scheduledAt, durationMinutes);
                }
            });
        }

        if (btnBackHome != null) {
            btnBackHome.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent intent = new Intent(AppointmentConfirmationActivity.this, MainActivity.class);
                    intent.putExtra("SELECT_TAB", R.id.nav_home);
                    intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                    startActivity(intent);
                    finish();
                }
            });
        }
    }

    private void addToCalendar(String service, String petName, String location, String scheduledAt, int duration) {
        Intent intent = new Intent(Intent.ACTION_INSERT)
                .setData(CalendarContract.Events.CONTENT_URI)
                .putExtra(CalendarContract.Events.TITLE, "Kindred Paws - " + service)
                .putExtra(CalendarContract.Events.DESCRIPTION, "Pet: " + petName + "\nService: " + service + "\nLocation: " + location)
                .putExtra(CalendarContract.Events.EVENT_LOCATION, location);

        if (scheduledAt != null) {
            try {
                // scheduled_at format: YYYY-MM-DDTHH:mm:ss
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault());
                Date startDate = sdf.parse(scheduledAt);
                if (startDate != null) {
                    long startMillis = startDate.getTime();
                    long endMillis = startMillis + (duration * 60 * 1000L);
                    intent.putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, startMillis);
                    intent.putExtra(CalendarContract.EXTRA_EVENT_END_TIME, endMillis);
                }
            } catch (Exception e) {
                Log.e(TAG, "Error parsing scheduled_at for calendar", e);
            }
        }

        startActivity(intent);
    }
}
