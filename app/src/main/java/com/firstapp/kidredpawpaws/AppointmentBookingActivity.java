package com.firstapp.kidredpawpaws;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.firstapp.kidredpawpaws.models.supabase.AppointmentCreateRequest;
import com.firstapp.kidredpawpaws.models.supabase.AppointmentDto;
import com.firstapp.kidredpawpaws.models.supabase.PetCreateRequest;
import com.firstapp.kidredpawpaws.models.supabase.PetDto;
import com.firstapp.kidredpawpaws.models.supabase.StaffDto;
import com.firstapp.kidredpawpaws.repositories.ClientRepository;
import com.firstapp.kidredpawpaws.utils.SessionManager;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class AppointmentBookingActivity extends AppCompatActivity {

    private static final String TAG = "BookingActivity";
    private ClientRepository clientRepository;
    private SessionManager sessionManager;

    private TextView tvHeaderTitle, tvBookingProgress, tvBookingError;
    private TextView tvStepPetLabel, tvServiceLabel, tvStaffLabel;
    private LinearLayout llPetsContainer, llServicesContainer, llStaffContainer, llNoPetsContainer;
    private View hsvPets;
    private TextView tvBookingDate, tvBookingTime;
    private Button btnConfirm;

    private String bookingType; // "vet" or "grooming"
    private boolean isVetFlow = true; 
    private String selectedPetId, selectedPetName;
    private String selectedServiceDisplayName; 
    private String selectedStaffId, selectedStaffName;
    private String selectedDate, selectedTime;
    private String displayDate, displayTime;
    
    private String dbType, dbCategory, dbService, dbTitle;
    private int durationMinutes = 30;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_appointment_booking);

        clientRepository = new ClientRepository();
        sessionManager = new SessionManager(this);

        // Get flow type from intent
        bookingType = getIntent().getStringExtra("booking_type");
        if (bookingType == null) bookingType = "vet";
        isVetFlow = "vet".equals(bookingType);

        Log.d(TAG, "booking_type: " + bookingType);

        // Bind Views
        tvHeaderTitle = findViewById(R.id.tv_booking_header_title);
        tvBookingProgress = findViewById(R.id.tv_booking_progress);
        tvBookingError = findViewById(R.id.tv_booking_error);
        
        tvStepPetLabel = findViewById(R.id.tv_step_pet_label);
        tvServiceLabel = findViewById(R.id.tv_service_label);
        tvStaffLabel = findViewById(R.id.tv_staff_label);
        
        llPetsContainer = findViewById(R.id.ll_booking_pets);
        llServicesContainer = findViewById(R.id.ll_booking_services);
        llStaffContainer = findViewById(R.id.ll_booking_staff);
        llNoPetsContainer = findViewById(R.id.ll_no_pets_container);
        hsvPets = findViewById(R.id.hsv_pets);
        
        tvBookingDate = findViewById(R.id.tv_booking_date);
        tvBookingTime = findViewById(R.id.tv_booking_time);
        btnConfirm = findViewById(R.id.btn_confirm_booking);

        findViewById(R.id.btn_back_booking).setOnClickListener(v -> finish());
        findViewById(R.id.btn_add_pet_empty).setOnClickListener(v -> showAddPetDialog());

        tvBookingDate.setOnClickListener(v -> showDatePicker());
        tvBookingTime.setOnClickListener(v -> showTimePicker());

        setupFlowUI();
        loadPets();
        updateServices();
        loadStaff();

        btnConfirm.setOnClickListener(v -> handleConfirmBooking());

        updateProgress();
    }

    private void setupFlowUI() {
        if (isVetFlow) {
            tvHeaderTitle.setText("Vet Appointment");
            tvServiceLabel.setText("2. Select Service");
            tvStaffLabel.setText("3. Select Veterinarian");
        } else {
            tvHeaderTitle.setText("Grooming Appointment");
            tvServiceLabel.setText("2. Select Grooming Service");
            tvStaffLabel.setText("3. Select Groomer");
        }
    }

    private void loadPets() {
        String ownerId = sessionManager.getOwnerId();
        String token = sessionManager.getAccessToken();
        clientRepository.getPetsByOwnerId(token, ownerId, new Callback<List<PetDto>>() {
            @Override
            public void onResponse(@NonNull Call<List<PetDto>> call, @NonNull Response<List<PetDto>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    displayPets(response.body());
                }
            }
            @Override
            public void onFailure(@NonNull Call<List<PetDto>> call, @NonNull Throwable t) {
                Log.e(TAG, "Error loading pets", t);
            }
        });
    }

    private void displayPets(List<PetDto> pets) {
        llPetsContainer.removeAllViews();
        if (pets.isEmpty()) {
            llNoPetsContainer.setVisibility(View.VISIBLE);
            hsvPets.setVisibility(View.GONE);
            return;
        }

        llNoPetsContainer.setVisibility(View.GONE);
        hsvPets.setVisibility(View.VISIBLE);

        for (PetDto pet : pets) {
            View view = getLayoutInflater().inflate(R.layout.item_pet_select_book, llPetsContainer, false);
            TextView tv = view.findViewById(R.id.tv_pet_name_book);
            tv.setText(pet.getName());
            view.setTag(pet.getId());
            view.setOnClickListener(v -> {
                selectedPetId = pet.getId();
                selectedPetName = pet.getName();
                updatePetUI();
                updateProgress();
            });
            llPetsContainer.addView(view);
        }
        if (selectedPetId == null) {
            selectedPetId = pets.get(0).getId();
            selectedPetName = pets.get(0).getName();
        }
        updatePetUI();
        updateProgress();
    }

    private void updatePetUI() {
        for (int i = 0; i < llPetsContainer.getChildCount(); i++) {
            View v = llPetsContainer.getChildAt(i);
            View root = v.findViewById(R.id.ll_pet_root_book);
            if (root != null && v.getTag() != null) {
                if (v.getTag().equals(selectedPetId)) root.setBackgroundResource(R.drawable.bg_card_selected);
                else root.setBackgroundResource(R.drawable.bg_card_rounded);
            }
        }
    }

    private void updateServices() {
        llServicesContainer.removeAllViews();
        String[] services;
        if (isVetFlow) {
            services = new String[]{"Routine Check-up", "Vaccination", "Surgery"};
        } else {
            services = new String[]{"Full Grooming", "Nail Trimming", "Bath & Brush", "Haircut & Style", "De-shedding"};
        }

        for (String service : services) {
            TextView tv = new TextView(this);
            tv.setText(service);
            tv.setPadding(40, 20, 40, 20);
            tv.setGravity(android.view.Gravity.CENTER);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            lp.setMargins(0, 0, 16, 0);
            tv.setLayoutParams(lp);
            tv.setTextSize(13);
            tv.setTypeface(null, android.graphics.Typeface.BOLD);
            
            updateServiceChip(tv, service);
            
            tv.setOnClickListener(v -> {
                selectedServiceDisplayName = service;
                for (int i = 0; i < llServicesContainer.getChildCount(); i++) {
                    updateServiceChip((TextView) llServicesContainer.getChildAt(i), ((TextView) llServicesContainer.getChildAt(i)).getText().toString());
                }
                updateProgress();
            });
            llServicesContainer.addView(tv);
        }
    }

    private void updateServiceChip(TextView tv, String service) {
        if (service.equals(selectedServiceDisplayName)) {
            tv.setBackgroundResource(R.drawable.bg_button_primary);
            tv.setTextColor(ContextCompat.getColor(this, R.color.white));
        } else {
            tv.setBackgroundResource(R.drawable.bg_chip_teal);
            tv.setTextColor(ContextCompat.getColor(this, R.color.primary_teal));
        }
    }

    private void loadStaff() {
        llStaffContainer.removeAllViews();
        String token = sessionManager.getAccessToken();
        clientRepository.getStaff(token, new Callback<List<StaffDto>>() {
            @Override
            public void onResponse(@NonNull Call<List<StaffDto>> call, @NonNull Response<List<StaffDto>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    displayStaff(response.body());
                }
            }
            @Override
            public void onFailure(@NonNull Call<List<StaffDto>> call, @NonNull Throwable t) {
                Log.e(TAG, "Error loading staff", t);
            }
        });
    }

    private void displayStaff(List<StaffDto> staffList) {
        llStaffContainer.removeAllViews();
        String filter = isVetFlow ? "veterinarian" : "groomer";
        
        for (StaffDto s : staffList) {
            String role = s.getRole() != null ? s.getRole().toLowerCase() : "";
            if (!role.contains(filter)) continue;

            TextView tv = new TextView(this);
            String name = s.getFullName() != null ? s.getFullName() : s.getName();
            tv.setText(name);
            tv.setPadding(40, 20, 40, 20);
            tv.setGravity(android.view.Gravity.CENTER);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            lp.setMargins(0, 0, 16, 0);
            tv.setLayoutParams(lp);
            tv.setTextSize(13);
            tv.setTypeface(null, android.graphics.Typeface.BOLD);
            tv.setTag(s.getId());
            
            updateStaffChip(tv, s.getId());
            
            tv.setOnClickListener(click -> {
                selectedStaffId = s.getId();
                selectedStaffName = name;
                for (int i = 0; i < llStaffContainer.getChildCount(); i++) {
                    View child = llStaffContainer.getChildAt(i);
                    if (child instanceof TextView) {
                        updateStaffChip((TextView) child, (String) child.getTag());
                    }
                }
                updateProgress();
            });
            llStaffContainer.addView(tv);
        }
    }

    private void updateStaffChip(TextView tv, String id) {
        if (id != null && id.equals(selectedStaffId)) {
            tv.setBackgroundResource(R.drawable.bg_button_primary);
            tv.setTextColor(ContextCompat.getColor(this, R.color.white));
        } else {
            tv.setBackgroundResource(R.drawable.bg_chip_teal);
            tv.setTextColor(ContextCompat.getColor(this, R.color.primary_teal));
        }
    }

    private void showDatePicker() {
        Calendar c = Calendar.getInstance();
        new DatePickerDialog(this, (view, year, month, day) -> {
            selectedDate = String.format(Locale.getDefault(), "%04d-%02d-%02d", year, month + 1, day);
            Calendar temp = Calendar.getInstance();
            temp.set(year, month, day);
            displayDate = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(temp.getTime());
            tvBookingDate.setText(displayDate);
            tvBookingDate.setBackgroundResource(R.drawable.bg_button_primary);
            tvBookingDate.setTextColor(ContextCompat.getColor(this, R.color.white));
            updateProgress();
        }, c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH)).show();
    }

    private void showTimePicker() {
        Calendar c = Calendar.getInstance();
        new TimePickerDialog(this, (view, hour, min) -> {
            selectedTime = String.format(Locale.getDefault(), "%02d:%02d:00", hour, min);
            String ampm = hour < 12 ? "AM" : "PM";
            int h = hour > 12 ? hour - 12 : (hour == 0 ? 12 : hour);
            displayTime = String.format(Locale.getDefault(), "%02d:%02d %s", h, min, ampm);
            tvBookingTime.setText(displayTime);
            tvBookingTime.setBackgroundResource(R.drawable.bg_button_primary);
            tvBookingTime.setTextColor(ContextCompat.getColor(this, R.color.white));
            updateProgress();
        }, c.get(Calendar.HOUR_OF_DAY), c.get(Calendar.MINUTE), false).show();
    }

    private void updateProgress() {
        int step = 1;
        if (selectedPetId != null) step = 2;
        if (selectedPetId != null && selectedServiceDisplayName != null) step = 3;
        if (selectedPetId != null && selectedServiceDisplayName != null && selectedDate != null && selectedTime != null) step = 4;
        tvBookingProgress.setText("Step " + step + " of 4");
        btnConfirm.setText("5. Confirm Booking");
    }

    private void showAddPetDialog() {
        View v = getLayoutInflater().inflate(R.layout.dialog_add_pet, null);
        EditText etName = v.findViewById(R.id.et_pet_name);
        EditText etSpecies = v.findViewById(R.id.et_pet_species);
        EditText etBreed = v.findViewById(R.id.et_pet_breed);
        EditText etAge = v.findViewById(R.id.et_pet_age);
        TextView tvErr = v.findViewById(R.id.tv_add_pet_error);
        
        AlertDialog dialog = new AlertDialog.Builder(this).setView(v).create();
        if (dialog.getWindow() != null) dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);

        v.findViewById(R.id.btn_cancel_pet).setOnClickListener(view -> dialog.dismiss());
        v.findViewById(R.id.btn_save_pet).setOnClickListener(view -> {
            String name = etName.getText().toString().trim();
            String species = etSpecies.getText().toString().trim();
            String breed = etBreed.getText().toString().trim();
            String ageStr = etAge.getText().toString().trim();

            if (TextUtils.isEmpty(name) || TextUtils.isEmpty(species) || TextUtils.isEmpty(breed)) {
                tvErr.setText("Please fill required fields.");
                tvErr.setVisibility(View.VISIBLE);
                return;
            }

            Integer age = null;
            try { age = Integer.parseInt(ageStr); } catch (Exception ignored) {}

            String ownerId = sessionManager.getOwnerId();
            String token = sessionManager.getAccessToken();
            clientRepository.createPet(token, new PetCreateRequest(ownerId, name, species, breed, age, "Healthy"), new Callback<List<PetDto>>() {
                @Override
                public void onResponse(@NonNull Call<List<PetDto>> call, @NonNull Response<List<PetDto>> response) {
                    if (response.isSuccessful() && response.body() != null && !response.body().isEmpty()) {
                        dialog.dismiss();
                        loadPets();
                    }
                }
                @Override
                public void onFailure(@NonNull Call<List<PetDto>> call, @NonNull Throwable t) {
                    tvErr.setText("Failed to add pet.");
                    tvErr.setVisibility(View.VISIBLE);
                }
            });
        });
        dialog.show();
    }

    private void handleConfirmBooking() {
        if (selectedPetId == null || selectedServiceDisplayName == null || selectedDate == null || selectedTime == null) {
            showInlineError("Please complete appointment details.");
            return;
        }

        mapServiceData();

        String scheduledAt = selectedDate + "T" + selectedTime;
        
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault());
            Date start = sdf.parse(scheduledAt);
            if (start != null && start.before(new Date())) {
                showInlineError("Please select a future date and time.");
                return;
            }

            Calendar cal = Calendar.getInstance();
            if (start != null) cal.setTime(start);
            int startMins = cal.get(Calendar.HOUR_OF_DAY) * 60 + cal.get(Calendar.MINUTE);
            int endMins = startMins + durationMinutes;

            if (startMins < 480 || endMins > 1020) {
                showInlineError("Clinic hours are from 8:00 AM to 5:00 PM.");
                return;
            }

            checkAvailabilityAndInsert(scheduledAt, start);

        } catch (Exception e) {
            showInlineError("Invalid date or time format.");
        }
    }

    private void mapServiceData() {
        if (isVetFlow) {
            if ("Routine Check-up".equals(selectedServiceDisplayName)) {
                dbType = "checkup"; dbCategory = "Routine"; dbService = null; dbTitle = "Routine Check-up"; durationMinutes = 30;
            } else if ("Vaccination".equals(selectedServiceDisplayName)) {
                dbType = "checkup"; dbCategory = "Vaccination"; dbService = null; dbTitle = "Vaccination"; durationMinutes = 30;
            } else if ("Surgery".equals(selectedServiceDisplayName)) {
                dbType = "surgery"; dbCategory = "Surgery"; dbService = null; dbTitle = "Surgery"; durationMinutes = 120;
            }
        } else {
            dbType = "grooming";
            dbCategory = selectedServiceDisplayName;
            dbService = selectedServiceDisplayName;
            dbTitle = "Grooming Session";
            if ("Full Grooming".equals(selectedServiceDisplayName)) durationMinutes = 60;
            else if ("Nail Trimming".equals(selectedServiceDisplayName)) durationMinutes = 30;
            else if ("Bath & Brush".equals(selectedServiceDisplayName)) durationMinutes = 45;
            else if ("Haircut & Style".equals(selectedServiceDisplayName)) durationMinutes = 60;
            else if ("De-shedding".equals(selectedServiceDisplayName)) durationMinutes = 60;
        }
    }

    private void checkAvailabilityAndInsert(String scheduledAt, Date start) {
        String startDay = selectedDate + "T00:00:00";
        String token = sessionManager.getAccessToken();

        tvBookingError.setText("Checking availability...");
        tvBookingError.setVisibility(View.VISIBLE);

        clientRepository.getAppointmentsForConflictCheck(token, startDay, selectedDate + "T23:59:59", new Callback<List<AppointmentDto>>() {
            @Override
            public void onResponse(@NonNull Call<List<AppointmentDto>> call, @NonNull Response<List<AppointmentDto>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    boolean conflict = false;
                    long newStart = start.getTime();
                    long newEnd = newStart + (durationMinutes * 60000L);
                    
                    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault());
                    for (AppointmentDto appt : response.body()) {
                        try {
                            String s = appt.getScheduledAt().split("\\.")[0].replace("Z", "");
                            Date d = sdf.parse(s);
                            if (d != null) {
                                int dur = appt.getDurationMinutes() != null ? appt.getDurationMinutes() : 30;
                                long es = d.getTime();
                                long ee = es + (dur * 60000L);
                                if (newStart < ee && newEnd > es) {
                                    conflict = true; break;
                                }
                            }
                        } catch (Exception ignored) {}
                    }

                    if (conflict) {
                        showInlineError("This time slot is already booked. Please choose another time.");
                    } else {
                        insertAppointment(scheduledAt);
                    }
                } else {
                    showInlineError("Error checking availability.");
                }
            }
            @Override
            public void onFailure(@NonNull Call<List<AppointmentDto>> call, @NonNull Throwable t) {
                showInlineError("Network error.");
            }
        });
    }

    private void insertAppointment(String scheduledAt) {
        String token = sessionManager.getAccessToken();
        
        Log.d(TAG, "booking_type: " + bookingType);
        Log.d(TAG, "selected pet: " + selectedPetName);
        Log.d(TAG, "selected service: " + selectedServiceDisplayName);
        Log.d(TAG, "selected staff: " + selectedStaffName);
        Log.d(TAG, "type: " + dbType);
        Log.d(TAG, "category: " + dbCategory);
        Log.d(TAG, "scheduled_at: " + scheduledAt);
        Log.d(TAG, "duration_minutes: " + durationMinutes);

        AppointmentCreateRequest req = new AppointmentCreateRequest(
                selectedPetId, dbType, dbCategory,
                dbTitle, dbService, scheduledAt, durationMinutes, "scheduled", 
                "Booked from mobile app", selectedStaffId
        );

        clientRepository.createAppointment(token, req, new Callback<List<AppointmentDto>>() {
            @Override
            public void onResponse(@NonNull Call<List<AppointmentDto>> call, @NonNull Response<List<AppointmentDto>> response) {
                if (response.isSuccessful()) {
                    Intent intent = new Intent(AppointmentBookingActivity.this, AppointmentConfirmationActivity.class);
                    intent.putExtra("pet_name", selectedPetName);
                    intent.putExtra("appointment_type", dbType);
                    intent.putExtra("appointment_category", dbCategory);
                    intent.putExtra("service", dbService != null ? dbService : selectedServiceDisplayName);
                    intent.putExtra("display_date", displayDate);
                    intent.putExtra("display_time", displayTime);
                    intent.putExtra("scheduled_at", scheduledAt);
                    intent.putExtra("duration_minutes", durationMinutes);
                    intent.putExtra("location", "Kindred Paws Pet Care Center");
                    intent.putExtra("staff_name", selectedStaffName);
                    if (response.body() != null && !response.body().isEmpty()) {
                        intent.putExtra("appointment_id", response.body().get(0).getId());
                    }
                    startActivity(intent);
                    finish();
                } else {
                    showInlineError("Failed to book. Code: " + response.code());
                }
            }
            @Override
            public void onFailure(@NonNull Call<List<AppointmentDto>> call, @NonNull Throwable t) {
                showInlineError("Network error.");
            }
        });
    }

    private void showInlineError(String msg) {
        tvBookingError.setText(msg);
        tvBookingError.setVisibility(View.VISIBLE);
    }
}
