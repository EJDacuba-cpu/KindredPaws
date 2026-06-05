package com.firstapp.kidredpawpaws;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.cardview.widget.CardView;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.firstapp.kidredpawpaws.models.supabase.AppointmentCreateRequest;
import com.firstapp.kidredpawpaws.models.supabase.AppointmentDto;
import com.firstapp.kidredpawpaws.models.supabase.PetCreateRequest;
import com.firstapp.kidredpawpaws.models.supabase.PetDto;
import com.firstapp.kidredpawpaws.repositories.ClientRepository;
import com.firstapp.kidredpawpaws.utils.SessionManager;

import java.io.IOException;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class BookFragment extends Fragment {

    private static final String TAG = "BookFragment";
    private ClientRepository clientRepository;
    private SessionManager sessionManager;

    private LinearLayout llPetsContainer;
    private LinearLayout llCategoriesContainer;
    private TextView tvBookMessage;
    private TextView tvSelectDate, tvSelectTime;
    private Button btnContinue;

    private String selectedPetId = null;
    private String selectedPetName = null;
    private String selectedType = "Check-up"; 
    private String selectedCategory = null;
    private String selectedDate = null;
    private String selectedTime = null;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_book, container, false);

        clientRepository = new ClientRepository();
        sessionManager = new SessionManager(requireContext());

        llPetsContainer = view.findViewById(R.id.ll_pets_container_book);
        llCategoriesContainer = view.findViewById(R.id.ll_categories_container);
        tvBookMessage = view.findViewById(R.id.tv_book_message);
        btnContinue = view.findViewById(R.id.btn_continue);
        btnContinue.setEnabled(false);
        btnContinue.setAlpha(0.5f);

        btnContinue.setOnClickListener(v -> handleBooking());

        view.findViewById(R.id.tv_add_new_pet).setOnClickListener(v -> showAddPetDialog());

        tvSelectDate = view.findViewById(R.id.tv_select_date);
        tvSelectTime = view.findViewById(R.id.tv_select_time);

        tvSelectDate.setOnClickListener(v -> showDatePicker());
        tvSelectTime.setOnClickListener(v -> showTimePicker());

        setupTypeSelection(view);
        loadPets();

        return view;
    }

    private void loadPets() {
        String ownerId = sessionManager.getOwnerId();
        String accessToken = sessionManager.getAccessToken();
        if (ownerId == null || ownerId.isEmpty() || accessToken == null) {
            showInlineMessage("Please login again.", true);
            btnContinue.setEnabled(false);
            return;
        }

        clientRepository.getPetsByOwnerId(accessToken, ownerId, new Callback<List<PetDto>>() {
            @Override
            public void onResponse(@NonNull Call<List<PetDto>> call, @NonNull Response<List<PetDto>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    List<PetDto> pets = response.body();
                    if (pets.isEmpty()) {
                        showInlineMessage("No pets found.", true);
                        btnContinue.setEnabled(false);
                    } else {
                        displayPets(pets);
                    }
                } else {
                    showInlineMessage("Error loading pets.", true);
                }
            }

            @Override
            public void onFailure(@NonNull Call<List<PetDto>> call, @NonNull Throwable t) {
                Log.e(TAG, "Network error loading pets", t);
                showInlineMessage("Network error.", true);
            }
        });
    }

    private void displayPets(List<PetDto> pets) {
        if (llPetsContainer == null) return;
        llPetsContainer.removeAllViews();
        
        int limit = Math.min(pets.size(), 2);
        for (int i = 0; i < limit; i++) {
            PetDto pet = pets.get(i);
            View petItem = getLayoutInflater().inflate(R.layout.item_pet_select_book, llPetsContainer, false);
            TextView tvName = petItem.findViewById(R.id.tv_pet_name_book);
            LinearLayout root = petItem.findViewById(R.id.ll_pet_root_book);

            tvName.setText(pet.getName());
            petItem.setTag(pet.getId());

            petItem.setOnClickListener(v -> {
                selectedPetId = pet.getId();
                selectedPetName = pet.getName();
                updatePetSelectionUI();
                validateContinueButton();
            });

            llPetsContainer.addView(petItem);
        }

        if (!pets.isEmpty()) {
            boolean found = false;
            if (selectedPetId != null) {
                for (PetDto p : pets) {
                    if (p.getId().equals(selectedPetId)) {
                        selectedPetName = p.getName();
                        found = true;
                        break;
                    }
                }
            }
            
            if (!found) {
                selectedPetId = pets.get(0).getId();
                selectedPetName = pets.get(0).getName();
            }
            updatePetSelectionUI();
        }
        validateContinueButton();
    }

    private void showAddPetDialog() {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_add_pet, null);
        EditText etName = dialogView.findViewById(R.id.et_pet_name);
        EditText etSpecies = dialogView.findViewById(R.id.et_pet_species);
        EditText etBreed = dialogView.findViewById(R.id.et_pet_breed);
        EditText etAge = dialogView.findViewById(R.id.et_pet_age);
        TextView tvError = dialogView.findViewById(R.id.tv_add_pet_error);
        Button btnCancel = dialogView.findViewById(R.id.btn_cancel_pet);
        Button btnSave = dialogView.findViewById(R.id.btn_save_pet);

        AlertDialog dialog = new AlertDialog.Builder(requireContext())
                .setView(dialogView)
                .setCancelable(true)
                .create();

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }

        btnCancel.setOnClickListener(v -> dialog.dismiss());

        btnSave.setOnClickListener(v -> {
            String name = etName.getText().toString().trim();
            String species = etSpecies.getText().toString().trim();
            String breed = etBreed.getText().toString().trim();
            String ageStr = etAge.getText().toString().trim();

            if (TextUtils.isEmpty(name)) {
                tvError.setText("Pet name is required.");
                tvError.setVisibility(View.VISIBLE);
                return;
            }
            if (TextUtils.isEmpty(species)) {
                tvError.setText("Species is required.");
                tvError.setVisibility(View.VISIBLE);
                return;
            }
            if (TextUtils.isEmpty(breed)) {
                tvError.setText("Breed is required.");
                tvError.setVisibility(View.VISIBLE);
                return;
            }

            Integer age = null;
            if (!TextUtils.isEmpty(ageStr)) {
                try {
                    age = Integer.parseInt(ageStr);
                } catch (NumberFormatException ignored) {}
            }

            savePet(name, species, breed, age, dialog);
        });

        dialog.show();
    }

    private void savePet(String name, String species, String breed, Integer age, AlertDialog dialog) {
        String ownerId = sessionManager.getOwnerId();
        String accessToken = sessionManager.getAccessToken();

        if (ownerId == null || ownerId.isEmpty()) {
            showInlineMessage("Please login again.", true);
            dialog.dismiss();
            return;
        }

        if (accessToken == null || accessToken.isEmpty()) {
            showInlineMessage("Session expired. Please login again.", true);
            dialog.dismiss();
            return;
        }

        PetCreateRequest request = new PetCreateRequest(ownerId, name, species, breed, age, "Healthy");

        clientRepository.createPet(accessToken, request, new Callback<List<PetDto>>() {
            @Override
            public void onResponse(@NonNull Call<List<PetDto>> call, @NonNull Response<List<PetDto>> response) {
                if (response.isSuccessful() && response.body() != null && !response.body().isEmpty()) {
                    Log.d(TAG, "Pet added successfully.");
                    selectedPetId = response.body().get(0).getId();
                    selectedPetName = response.body().get(0).getName();
                    dialog.dismiss();
                    loadPets(); 
                } else {
                    Log.e(TAG, "Failed to add pet. Code: " + response.code());
                    try {
                        if (response.errorBody() != null) {
                            Log.e(TAG, "Error Body: " + response.errorBody().string());
                        }
                    } catch (IOException ignored) {}
                    showInlineMessage("Failed to add pet. Please try again.", true);
                }
            }

            @Override
            public void onFailure(@NonNull Call<List<PetDto>> call, @NonNull Throwable t) {
                Log.e(TAG, "Network error adding pet", t);
                showInlineMessage("Failed to add pet. Please try again.", true);
            }
        });
    }

    private void validateContinueButton() {
        boolean isValid = selectedPetId != null && selectedType != null && selectedCategory != null && selectedDate != null && selectedTime != null;
        btnContinue.setEnabled(isValid);
        btnContinue.setAlpha(isValid ? 1.0f : 0.5f);
    }

    private void updatePetSelectionUI() {
        if (llPetsContainer == null) return;
        for (int i = 0; i < llPetsContainer.getChildCount(); i++) {
            View child = llPetsContainer.getChildAt(i);
            LinearLayout root = child.findViewById(R.id.ll_pet_root_book);
            if (child.getTag().equals(selectedPetId)) {
                root.setBackgroundResource(R.drawable.bg_card_selected);
            } else {
                root.setBackgroundResource(R.drawable.bg_card_rounded);
            }
        }
    }

    private void setupTypeSelection(View view) {
        CardView cvCheckup = view.findViewById(R.id.card_type_checkup);
        CardView cvGrooming = view.findViewById(R.id.card_type_grooming);
        CardView cvSurgery = view.findViewById(R.id.card_type_surgery);

        View.OnClickListener listener = v -> {
            int id = v.getId();
            if (id == R.id.card_type_checkup) selectedType = "Check-up";
            else if (id == R.id.card_type_grooming) selectedType = "Grooming";
            else if (id == R.id.card_type_surgery) selectedType = "Surgery";

            selectedCategory = null;
            updateTypeSelectionUI(view);
            loadCategories();
            validateContinueButton();
        };

        cvCheckup.setOnClickListener(listener);
        cvGrooming.setOnClickListener(listener);
        cvSurgery.setOnClickListener(listener);

        updateTypeSelectionUI(view);
        loadCategories();
    }

    private void updateTypeSelectionUI(View view) {
        int teal = ContextCompat.getColor(requireContext(), R.color.primary_teal);
        int black = ContextCompat.getColor(requireContext(), R.color.text_primary);
        int white = ContextCompat.getColor(requireContext(), R.color.white);

        CardView cvCheckup = view.findViewById(R.id.card_type_checkup);
        CardView cvGrooming = view.findViewById(R.id.card_type_grooming);
        CardView cvSurgery = view.findViewById(R.id.card_type_surgery);

        TextView tvCheckup = view.findViewById(R.id.tv_type_checkup);
        TextView tvGrooming = view.findViewById(R.id.tv_type_grooming);
        TextView tvSurgery = view.findViewById(R.id.tv_type_surgery);

        cvCheckup.setCardBackgroundColor(selectedType.equals("Check-up") ? teal : white);
        tvCheckup.setTextColor(selectedType.equals("Check-up") ? white : black);

        cvGrooming.setCardBackgroundColor(selectedType.equals("Grooming") ? teal : white);
        tvGrooming.setTextColor(selectedType.equals("Grooming") ? white : black);

        cvSurgery.setCardBackgroundColor(selectedType.equals("Surgery") ? teal : white);
        tvSurgery.setTextColor(selectedType.equals("Surgery") ? white : black);
    }

    private void loadCategories() {
        if (llCategoriesContainer == null) return;
        llCategoriesContainer.removeAllViews();

        String[] categories;
        switch (selectedType) {
            case "Grooming":
                categories = new String[]{"Bath", "Bath & Brush", "Full Grooming", "Nail Trimming"};
                break;
            case "Surgery":
                categories = new String[]{"Surgery", "Dental Surgery", "Minor Surgery"};
                break;
            case "Check-up":
            default:
                categories = new String[]{"Routine", "Vaccination", "Follow-up"};
                break;
        }

        for (String cat : categories) {
            TextView chip = new TextView(requireContext());
            chip.setText(cat);
            chip.setPadding(40, 24, 40, 24);
            chip.setTypeface(null, android.graphics.Typeface.BOLD);
            chip.setTextSize(13);
            
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            params.setMargins(0, 0, 16, 0);
            chip.setLayoutParams(params);

            updateCategoryChipStyle(chip, cat);

            chip.setOnClickListener(v -> {
                selectedCategory = cat;
                for (int i = 0; i < llCategoriesContainer.getChildCount(); i++) {
                    View child = llCategoriesContainer.getChildAt(i);
                    if (child instanceof TextView) {
                        updateCategoryChipStyle((TextView) child, ((TextView) child).getText().toString());
                    }
                }
                validateContinueButton();
            });

            llCategoriesContainer.addView(chip);
        }
    }

    private void updateCategoryChipStyle(TextView chip, String category) {
        if (category.equals(selectedCategory)) {
            chip.setBackgroundResource(R.drawable.bg_button_primary);
            chip.setTextColor(ContextCompat.getColor(requireContext(), R.color.white));
        } else {
            chip.setBackgroundResource(R.drawable.bg_chip_teal);
            chip.setTextColor(ContextCompat.getColor(requireContext(), R.color.primary_teal));
        }
    }

    private void showDatePicker() {
        final Calendar c = Calendar.getInstance();
        int year = c.get(Calendar.YEAR);
        int month = c.get(Calendar.MONTH);
        int day = c.get(Calendar.DAY_OF_MONTH);

        DatePickerDialog datePickerDialog = new DatePickerDialog(requireContext(),
                (view, year1, monthOfYear, dayOfMonth) -> {
                    selectedDate = String.format(Locale.getDefault(), "%04d-%02d-%02d", year1, monthOfYear + 1, dayOfMonth);
                    String displayDate = String.format(Locale.getDefault(), "%s %02d, %04d",
                            getMonthName(monthOfYear), dayOfMonth, year1);
                    tvSelectDate.setText(displayDate);
                    tvSelectDate.setTextColor(ContextCompat.getColor(requireContext(), R.color.white));
                    tvSelectDate.setBackgroundResource(R.drawable.bg_button_primary);
                    validateContinueButton();
                }, year, month, day);
        datePickerDialog.getDatePicker().setMinDate(System.currentTimeMillis() - 1000);
        datePickerDialog.show();
    }

    private void showTimePicker() {
        final Calendar c = Calendar.getInstance();
        int hour = c.get(Calendar.HOUR_OF_DAY);
        int minute = c.get(Calendar.MINUTE);

        TimePickerDialog timePickerDialog = new TimePickerDialog(requireContext(),
                (view, hourOfDay, minute1) -> {
                    selectedTime = String.format(Locale.getDefault(), "%02d:%02d:00", hourOfDay, minute1);
                    String amPm = hourOfDay < 12 ? "AM" : "PM";
                    int displayHour = hourOfDay > 12 ? hourOfDay - 12 : (hourOfDay == 0 ? 12 : hourOfDay);
                    String displayTime = String.format(Locale.getDefault(), "%02d:%02d %s", displayHour, minute1, amPm);
                    tvSelectTime.setText(displayTime);
                    tvSelectTime.setTextColor(ContextCompat.getColor(requireContext(), R.color.white));
                    tvSelectTime.setBackgroundResource(R.drawable.bg_button_primary);
                    validateContinueButton();
                }, hour, minute, false);
        timePickerDialog.show();
    }

    private String getMonthName(int month) {
        String[] monthNames = {"Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"};
        return monthNames[month];
    }

    private void handleBooking() {
        if (selectedPetId == null || selectedType == null || selectedCategory == null || selectedDate == null || selectedTime == null) {
            showInlineMessage("Please complete appointment details.", true);
            return;
        }

        clearInlineMessage();
        btnContinue.setEnabled(false);
        btnContinue.setText("Booking...");

        String dbType = "";
        String mappedCategory = "";
        String mappedService = null;
        String mappedTitle = "";
        int durationMinutes = 30;

        if ("Check-up".equals(selectedType)) {
            dbType = "checkup";
            if ("Routine".equals(selectedCategory)) {
                mappedCategory = "routine";
                mappedTitle = "Routine Check-up";
                durationMinutes = 30;
            } else if ("Vaccination".equals(selectedCategory)) {
                mappedCategory = "vaccination";
                mappedTitle = "Vaccination";
                durationMinutes = 30;
            } else if ("Follow-up".equals(selectedCategory)) {
                mappedCategory = "routine";
                mappedTitle = "Follow-up Check-up";
                durationMinutes = 30;
            }
        } else if ("Grooming".equals(selectedType)) {
            dbType = "grooming";
            mappedCategory = "grooming";
            mappedService = selectedCategory;
            mappedTitle = "Grooming Session";
            if ("Bath & Brush".equals(selectedCategory)) durationMinutes = 45;
            else if ("Full Grooming".equals(selectedCategory)) durationMinutes = 60;
            else durationMinutes = 30; // Bath, Nail Trimming
        } else if ("Surgery".equals(selectedType)) {
            dbType = "surgery";
            mappedCategory = "surgery";
            mappedTitle = selectedCategory;
            if ("Surgery".equals(selectedCategory) || "Dental Surgery".equals(selectedCategory)) durationMinutes = 120;
            else if ("Minor Surgery".equals(selectedCategory)) durationMinutes = 90;
        }

        String scheduledAt = selectedDate + "T" + selectedTime;
        String status = "scheduled";

        Log.d(TAG, "Attempting Booking with mapped values:");
        Log.d(TAG, "pet_id: " + selectedPetId);
        Log.d(TAG, "type (dbType): " + dbType);
        Log.d(TAG, "category: " + mappedCategory);
        Log.d(TAG, "service: " + mappedService);
        Log.d(TAG, "title: " + mappedTitle);
        Log.d(TAG, "scheduled_at: " + scheduledAt);
        Log.d(TAG, "duration_minutes: " + durationMinutes);
        Log.d(TAG, "status: " + status);

        AppointmentCreateRequest request = new AppointmentCreateRequest(
                selectedPetId,
                dbType,
                mappedCategory,
                mappedTitle,
                mappedService,
                scheduledAt,
                durationMinutes,
                status,
                "Booked from mobile app"
        );

        String token = sessionManager.getAccessToken();
        clientRepository.createAppointment(token, request, new Callback<List<AppointmentDto>>() {
            @Override
            public void onResponse(@NonNull Call<List<AppointmentDto>> call, @NonNull Response<List<AppointmentDto>> response) {
                Log.d(TAG, "Booking response code: " + response.code());
                if (response.isSuccessful()) {
                    Log.d(TAG, "Booking successful.");
                    Intent intent = new Intent(getActivity(), AppointmentConfirmationActivity.class);
                    startActivity(intent);
                } else {
                    btnContinue.setEnabled(true);
                    btnContinue.setText("Continue →");
                    String errorMsg = "Failed to book appointment. Code: " + response.code();
                    try {
                        if (response.errorBody() != null) {
                            String errorBody = response.errorBody().string();
                            Log.e(TAG, "Booking failed error body: " + errorBody);
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    showInlineMessage(errorMsg, true);
                }
            }

            @Override
            public void onFailure(@NonNull Call<List<AppointmentDto>> call, @NonNull Throwable t) {
                btnContinue.setEnabled(true);
                btnContinue.setText("Continue →");
                Log.e(TAG, "Network error during booking", t);
                showInlineMessage("Network error. Please try again.", true);
            }
        });
    }

    private void showInlineMessage(String message, boolean isError) {
        if (tvBookMessage != null) {
            tvBookMessage.setText(message);
            tvBookMessage.setTextColor(ContextCompat.getColor(requireContext(), isError ? R.color.danger_text : R.color.primary_teal));
            tvBookMessage.setVisibility(View.VISIBLE);
        }
    }

    private void clearInlineMessage() {
        if (tvBookMessage != null) {
            tvBookMessage.setText("");
            tvBookMessage.setVisibility(View.GONE);
        }
    }
}
