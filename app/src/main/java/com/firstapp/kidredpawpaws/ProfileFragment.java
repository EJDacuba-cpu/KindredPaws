package com.firstapp.kidredpawpaws;

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
import androidx.fragment.app.Fragment;

import com.firstapp.kidredpawpaws.models.supabase.AppointmentDto;
import com.firstapp.kidredpawpaws.models.supabase.OwnerDto;
import com.firstapp.kidredpawpaws.models.supabase.OwnerUpdateRequest;
import com.firstapp.kidredpawpaws.models.supabase.PetDto;
import com.firstapp.kidredpawpaws.repositories.ClientRepository;
import com.firstapp.kidredpawpaws.utils.SessionManager;
import com.google.android.material.bottomnavigation.BottomNavigationView;

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

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_profile, container, false);

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

        // Menu Items
        view.findViewById(R.id.cv_menu_pets).setOnClickListener(v -> navigateToPets(false));
        
        // Navigate to Pets screen with History/Appointments selected
        view.findViewById(R.id.cv_menu_history).setOnClickListener(v -> navigateToPets(true));
        
        view.findViewById(R.id.cv_menu_notifications).setOnClickListener(v -> showSoonDialog("Notifications will be available soon."));
        view.findViewById(R.id.cv_menu_support).setOnClickListener(v -> showInfoDialog("Help & Support", "For support, please contact Kindred Paws Pet Care Center."));
        view.findViewById(R.id.cv_menu_privacy).setOnClickListener(v -> showInfoDialog("Privacy Policy", "Your information is used only for appointment and pet care management."));
        view.findViewById(R.id.cv_menu_about).setOnClickListener(v -> showInfoDialog("About", "Kindred Paws Pet Care Center - Providing quality care for your beloved pets."));

        ivEditProfile.setOnClickListener(v -> showEditProfileDialog());

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

    private void loadProfileData() {
        String ownerId = sessionManager.getOwnerId();
        String token = sessionManager.getAccessToken();

        if (ownerId == null || ownerId.isEmpty()) {
            showSoonDialog("Please login again.");
            return;
        }

        Log.d(TAG, "Loading profile for owner_id: " + ownerId);

        clientRepository.getOwnerById(token, ownerId, new Callback<List<OwnerDto>>() {
            @Override
            public void onResponse(@NonNull Call<List<OwnerDto>> call, @NonNull Response<List<OwnerDto>> response) {
                if (response.isSuccessful() && response.body() != null && !response.body().isEmpty()) {
                    OwnerDto owner = response.body().get(0);
                    displayOwnerData(owner);
                } else {
                    Log.e(TAG, "Failed to load profile. Code: " + response.code());
                    showSoonDialog("Failed to load profile.");
                }
            }

            @Override
            public void onFailure(@NonNull Call<List<OwnerDto>> call, @NonNull Throwable t) {
                Log.e(TAG, "Network error loading profile", t);
            }
        });

        loadPetsAndAppointmentsCount(token, ownerId);
    }

    private void displayOwnerData(OwnerDto owner) {
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
                    int count = pets.size();
                    tvPetsCount.setText(String.valueOf(count));
                    if (count > 0) fetchAppointmentsCount(token, pets);
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

    private void showSoonDialog(String message) {
        new AlertDialog.Builder(requireContext())
                .setMessage(message)
                .setPositiveButton("OK", null)
                .show();
    }

    private void showInfoDialog(String title, String message) {
        new AlertDialog.Builder(requireContext())
                .setTitle(title)
                .setMessage(message)
                .setPositiveButton("Close", null)
                .show();
    }

    private void showEditProfileDialog() {
        LinearLayout layout = new LinearLayout(requireContext());
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(50, 40, 50, 10);

        final EditText etName = new EditText(requireContext());
        etName.setHint("Full Name");
        etName.setText(tvUserName.getText());
        layout.addView(etName);

        final EditText etPhone = new EditText(requireContext());
        etPhone.setHint("Phone");
        etPhone.setText(tvUserPhone.getText().toString().equals("No phone number") ? "" : tvUserPhone.getText());
        layout.addView(etPhone);

        final EditText etLocation = new EditText(requireContext());
        etLocation.setHint("Location");
        etLocation.setText(tvUserLocation.getText().toString().equals("No location set") ? "" : tvUserLocation.getText());
        layout.addView(etLocation);

        new AlertDialog.Builder(requireContext())
                .setTitle("Edit Profile")
                .setView(layout)
                .setPositiveButton("Save", (dialog, which) -> {
                    String name = etName.getText().toString().trim();
                    String phone = etPhone.getText().toString().trim();
                    String location = etLocation.getText().toString().trim();
                    if (!TextUtils.isEmpty(name)) updateProfile(name, phone, location);
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void updateProfile(String name, String phone, String location) {
        String ownerId = sessionManager.getOwnerId();
        String token = sessionManager.getAccessToken();
        clientRepository.updateOwner(token, ownerId, new OwnerUpdateRequest(name, phone, location), new Callback<Void>() {
            @Override
            public void onResponse(@NonNull Call<Void> call, @NonNull Response<Void> response) {
                if (response.isSuccessful()) loadProfileData();
                else showSoonDialog("Failed to update profile.");
            }
            @Override
            public void onFailure(@NonNull Call<Void> call, @NonNull Throwable t) {
                showSoonDialog("Network error.");
            }
        });
    }
}
