package com.firstapp.kidredpawpaws;

import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.appcompat.widget.SwitchCompat;
import androidx.fragment.app.Fragment;

import com.firstapp.kidredpawpaws.models.supabase.AppointmentDto;
import com.firstapp.kidredpawpaws.models.supabase.OwnerDto;
import com.firstapp.kidredpawpaws.models.supabase.OwnerUpdateRequest;
import com.firstapp.kidredpawpaws.models.supabase.PetDto;
import com.firstapp.kidredpawpaws.repositories.ClientRepository;
import com.firstapp.kidredpawpaws.utils.ModernDialogHelper;
import com.firstapp.kidredpawpaws.utils.SessionManager;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ProfileFragment extends Fragment {

    private static final String TAG = "ProfileFragment";
    private ClientRepository clientRepository;
    private SessionManager sessionManager;

    private TextView tvUserName, tvUserEmail, tvUserPhone, tvUserLocation, tvAvatarInitials;
    private TextView tvPetsCount, tvAppointmentsCount;
    private View ivEditProfile;
    private SwitchCompat switchDarkMode;
    private View rootView;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_profile, container, false);
        rootView = view.findViewById(R.id.nsv_profile_root);

        clientRepository = new ClientRepository();
        sessionManager = new SessionManager(requireContext());

        // Bind Views
        tvUserName = view.findViewById(R.id.tv_user_name);
        tvUserEmail = view.findViewById(R.id.tv_user_email);
        tvUserPhone = view.findViewById(R.id.tv_user_phone);
        tvUserLocation = view.findViewById(R.id.tv_user_location);
        tvAvatarInitials = view.findViewById(R.id.tv_avatar_initials);
        tvPetsCount = view.findViewById(R.id.tv_pets_count);
        tvAppointmentsCount = view.findViewById(R.id.tv_appointments_count);
        ivEditProfile = view.findViewById(R.id.iv_edit_profile);
        switchDarkMode = view.findViewById(R.id.switch_dark_mode);

        // Menu Items
        view.findViewById(R.id.cv_menu_pets).setOnClickListener(v -> navigateToPets(false));
        view.findViewById(R.id.cv_menu_history).setOnClickListener(v -> navigateToPets(true));
        
        // Modern Dialogs for Menu Items
        view.findViewById(R.id.cv_menu_notifications).setOnClickListener(v -> 
                ModernDialogHelper.showInfoDialog(requireContext(), "Notifications", "Notifications will be available soon. Keep an eye out for updates!"));
        
        view.findViewById(R.id.cv_menu_support).setOnClickListener(v -> 
                ModernDialogHelper.showInfoDialog(requireContext(), "Help & Support", "For support, please contact Kindred Paws Pet Care Center at support@kindredpaws.com."));
        
        view.findViewById(R.id.cv_menu_privacy).setOnClickListener(v -> 
                ModernDialogHelper.showInfoDialog(requireContext(), "Privacy Policy", "Your information is used only for appointment and pet care management within our clinic."));
        
        view.findViewById(R.id.cv_menu_about).setOnClickListener(v -> 
                ModernDialogHelper.showInfoDialog(requireContext(), "About Kindred Paws", "Kindred Paws Pet Care Center - Dedicated to providing the highest quality medical and grooming care for your furry companions."));

        ivEditProfile.setOnClickListener(v -> showEditProfileDialog());

        // Dark Mode Toggle
        setupDarkModeToggle();

        // Logout
        Button btnLogout = view.findViewById(R.id.btn_logout);
        btnLogout.setOnClickListener(v -> {
            sessionManager.clearSession();
            Intent intent = new Intent(getActivity(), LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            getActivity().finish();
        });

        loadProfileData();

        return view;
    }

    private void setupDarkModeToggle() {
        SharedPreferences prefs = requireActivity().getSharedPreferences("app_preferences", Context.MODE_PRIVATE);
        boolean isDarkMode = prefs.getBoolean("dark_mode_enabled", false);
        switchDarkMode.setChecked(isDarkMode);

        switchDarkMode.setOnCheckedChangeListener((buttonView, isChecked) -> {
            Log.d(TAG, "Dark mode enabled: " + isChecked);
            prefs.edit().putBoolean("dark_mode_enabled", isChecked).apply();

            // Smooth transition fade
            rootView.animate()
                    .alpha(0.85f)
                    .setDuration(150)
                    .withEndAction(() -> {
                        if (isChecked) {
                            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
                        } else {
                            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
                        }
                    })
                    .start();
        });
    }

    private void loadProfileData() {
        String ownerId = sessionManager.getOwnerId();
        String token = sessionManager.getAccessToken();

        if (ownerId == null || ownerId.isEmpty()) {
            return;
        }

        Log.d(TAG, "Loading profile for owner_id: " + ownerId);

        clientRepository.getOwnerById(token, ownerId, new Callback<List<OwnerDto>>() {
            @Override
            public void onResponse(@NonNull Call<List<OwnerDto>> call, @NonNull Response<List<OwnerDto>> response) {
                if (response.isSuccessful() && response.body() != null && !response.body().isEmpty()) {
                    displayOwnerData(response.body().get(0));
                }
            }

            @Override
            public void onFailure(@NonNull Call<List<OwnerDto>> call, @NonNull Throwable t) {
                Log.e(TAG, "Error loading profile", t);
            }
        });

        loadPetsAndAppointmentsCount(token, ownerId);
    }

    private void displayOwnerData(OwnerDto owner) {
        if (!isAdded() || getContext() == null) return;
        tvUserName.setText(owner.getFullName());
        tvUserEmail.setText(owner.getEmail());
        tvUserPhone.setText(owner.getPhone() != null ? owner.getPhone() : "No phone number");
        tvUserLocation.setText(owner.getLocation() != null ? owner.getLocation() : "No location set");

        String initials = "";
        if (owner.getFullName() != null && !owner.getFullName().isEmpty()) {
            String[] parts = owner.getFullName().trim().split("\\s+");
            if (parts.length > 0 && !parts[0].isEmpty()) {
                initials += parts[0].substring(0, 1).toUpperCase();
                if (parts.length > 1 && !parts[1].isEmpty()) {
                    initials += parts[1].substring(0, 1).toUpperCase();
                }
            }
        }
        tvAvatarInitials.setText(initials.isEmpty() ? "?" : initials);
    }

    private void loadPetsAndAppointmentsCount(String token, String ownerId) {
        clientRepository.getPetsByOwnerId(token, ownerId, new Callback<List<PetDto>>() {
            @Override
            public void onResponse(@NonNull Call<List<PetDto>> call, @NonNull Response<List<PetDto>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    List<PetDto> pets = response.body();
                    tvPetsCount.setText(String.valueOf(pets.size()));
                    if (!pets.isEmpty()) fetchAppointmentsCount(token, pets);
                    else tvAppointmentsCount.setText("0");
                }
            }
            @Override
            public void onFailure(@NonNull Call<List<PetDto>> call, @NonNull Throwable t) {}
        });
    }

    private void fetchAppointmentsCount(String token, List<PetDto> pets) {
        List<String> ids = new ArrayList<>();
        for (PetDto p : pets) ids.add(p.getId());
        clientRepository.getAppointmentsByPetIds(token, TextUtils.join(",", ids), new Callback<List<AppointmentDto>>() {
            @Override
            public void onResponse(@NonNull Call<List<AppointmentDto>> call, @NonNull Response<List<AppointmentDto>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    tvAppointmentsCount.setText(String.valueOf(response.body().size()));
                }
            }
            @Override
            public void onFailure(@NonNull Call<List<AppointmentDto>> call, @NonNull Throwable t) {}
        });
    }

    private void navigateToPets(boolean showAppointments) {
        if (getActivity() instanceof MainActivity) {
            if (showAppointments) {
                PetsFragment.requestedHistoryFilter = "appointments";
            } else {
                PetsFragment.requestedHistoryFilter = null;
            }
            ((MainActivity) getActivity()).navigateToTab(R.id.nav_pets);
        }
    }

    private void showEditProfileDialog() {
        Dialog dialog = new Dialog(requireContext());
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        View v = getLayoutInflater().inflate(R.layout.dialog_edit_profile, null);
        dialog.setContentView(v);

        dialog.show();

        // Apply modern mobile-friendly sizing
        Window window = dialog.getWindow();
        if (window != null) {
            DisplayMetrics metrics = new DisplayMetrics();
            requireActivity().getWindowManager().getDefaultDisplay().getMetrics(metrics);
            int width = (int) (metrics.widthPixels * 0.90);
            window.setLayout(width, ViewGroup.LayoutParams.WRAP_CONTENT);
            window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }

        final EditText etName = v.findViewById(R.id.et_edit_name);
        final EditText etPhone = v.findViewById(R.id.et_edit_phone);
        final EditText etLocation = v.findViewById(R.id.et_edit_location);
        Button btnCancel = v.findViewById(R.id.btn_cancel_edit);
        Button btnSave = v.findViewById(R.id.btn_save_edit);

        etName.setText(tvUserName.getText());
        etPhone.setText(tvUserPhone.getText().toString().equals("No phone number") ? "" : tvUserPhone.getText());
        etLocation.setText(tvUserLocation.getText().toString().equals("No location set") ? "" : tvUserLocation.getText());

        btnCancel.setOnClickListener(view -> dialog.dismiss());
        btnSave.setOnClickListener(view -> {
            String name = etName.getText().toString().trim();
            String phone = etPhone.getText().toString().trim();
            String location = etLocation.getText().toString().trim();
            if (!TextUtils.isEmpty(name)) {
                updateProfile(name, phone, location);
                dialog.dismiss();
            }
        });
    }

    private void updateProfile(String name, String phone, String location) {
        String ownerId = sessionManager.getOwnerId();
        String token = sessionManager.getAccessToken();
        clientRepository.updateOwner(token, ownerId, new OwnerUpdateRequest(name, phone, location), new Callback<Void>() {
            @Override
            public void onResponse(@NonNull Call<Void> call, @NonNull Response<Void> response) {
                if (response.isSuccessful()) loadProfileData();
                else ModernDialogHelper.showInfoDialog(requireContext(), "Update Failed", "We could not update your profile details. Please try again.");
            }
            @Override
            public void onFailure(@NonNull Call<Void> call, @NonNull Throwable t) {
                ModernDialogHelper.showInfoDialog(requireContext(), "Network Error", "Unable to connect to the server.");
            }
        });
    }
}
