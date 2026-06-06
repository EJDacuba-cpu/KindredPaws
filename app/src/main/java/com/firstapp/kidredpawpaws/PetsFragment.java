package com.firstapp.kidredpawpaws;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.firstapp.kidredpawpaws.models.supabase.AppointmentDto;
import com.firstapp.kidredpawpaws.models.supabase.MedicalRecordDto;
import com.firstapp.kidredpawpaws.models.supabase.PetDto;
import com.firstapp.kidredpawpaws.repositories.ClientRepository;
import com.firstapp.kidredpawpaws.utils.ModernDialogHelper;
import com.firstapp.kidredpawpaws.utils.SessionManager;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class PetsFragment extends Fragment {

    private static final String TAG = "PetsFragment";
    public static String requestedHistoryFilter = null; // "appointments" or "medical"

    private ClientRepository clientRepository;
    private SessionManager sessionManager;

    private TextView tvPetName, tvPetSpeciesAge, tvPetInitials;
    private TextView tvPetStatus;
    private TextView tvTabProfile, tvTabHistory;
    private TextView tvFilterMedical, tvFilterAppointments;
    private TextView tvHistoryTitle, tvHistoryFilterLabel;
    private LinearLayout llProfileContent, llHistoryContent, llHistoryContainer;
    private LinearLayout llNoPetsState;
    private View cvPetHeader;
    private TextView tvNoHistory;

    private List<PetDto> petList;
    private PetDto selectedPet;
    private String selectedPetId = null;
    private boolean isHistoryTab = true; 
    private boolean isMedicalFilter = true; // Secondary filter

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_pets, container, false);

        clientRepository = new ClientRepository();
        sessionManager = new SessionManager(requireContext());

        // Bind Views
        tvPetName = view.findViewById(R.id.tv_pet_name);
        tvPetSpeciesAge = view.findViewById(R.id.tv_pet_species_age);
        tvPetInitials = view.findViewById(R.id.tv_pet_initials);
        tvPetStatus = view.findViewById(R.id.tv_pet_status);
        tvTabProfile = view.findViewById(R.id.tv_tab_profile);
        tvTabHistory = view.findViewById(R.id.tv_tab_history);
        
        tvFilterMedical = view.findViewById(R.id.tv_filter_medical);
        tvFilterAppointments = view.findViewById(R.id.tv_filter_appointments);
        tvHistoryTitle = view.findViewById(R.id.tv_history_title);
        tvHistoryFilterLabel = view.findViewById(R.id.tv_history_filter);

        llProfileContent = view.findViewById(R.id.ll_profile_content);
        llHistoryContent = view.findViewById(R.id.ll_history_content);
        llHistoryContainer = view.findViewById(R.id.ll_history_container);
        llNoPetsState = view.findViewById(R.id.ll_no_pets_state);
        cvPetHeader = view.findViewById(R.id.cv_pet_header);
        tvNoHistory = view.findViewById(R.id.tv_no_history);

        ImageView ivSwitchPet = view.findViewById(R.id.iv_switch_pet);
        if (ivSwitchPet != null) {
            ivSwitchPet.setOnClickListener(v -> showPetSelector());
        }

        if (tvTabProfile != null) {
            tvTabProfile.setOnClickListener(v -> switchTab(false));
        }
        if (tvTabHistory != null) {
            tvTabHistory.setOnClickListener(v -> switchTab(true));
        }

        if (tvFilterMedical != null) {
            tvFilterMedical.setOnClickListener(v -> switchHistoryFilter(true));
        }
        if (tvFilterAppointments != null) {
            tvFilterAppointments.setOnClickListener(v -> switchHistoryFilter(false));
        }

        // Handle navigation from Profile
        if ("appointments".equals(requestedHistoryFilter)) {
            isHistoryTab = true;
            isMedicalFilter = false;
            requestedHistoryFilter = null; // Clear after use
        }

        loadPets();

        return view;
    }

    private void loadPets() {
        String ownerId = sessionManager.getOwnerId();
        String token = sessionManager.getAccessToken();

        Log.d(TAG, "Loading pets for owner_id: " + ownerId);

        if (ownerId == null || ownerId.isEmpty()) {
            showEmptyState("Please login again.");
            return;
        }

        clientRepository.getPetsByOwnerId(token, ownerId, new Callback<List<PetDto>>() {
            @Override
            public void onResponse(@NonNull Call<List<PetDto>> call, @NonNull Response<List<PetDto>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    petList = response.body();
                    Log.d(TAG, "Pets count: " + petList.size());
                    if (petList.isEmpty()) {
                        showEmptyState("No pets found.");
                    } else {
                        hideEmptyState();
                        // Maintain selected pet if switching filters or reloading
                        boolean found = false;
                        if (selectedPetId != null) {
                            for (PetDto p : petList) {
                                if (p.getId().equals(selectedPetId)) {
                                    displayPet(p);
                                    found = true;
                                    break;
                                }
                            }
                        }
                        if (!found) {
                            displayPet(petList.get(0));
                        }
                    }
                } else {
                    showEmptyState("Error loading pets.");
                }
            }

            @Override
            public void onFailure(@NonNull Call<List<PetDto>> call, @NonNull Throwable t) {
                showEmptyState("Network error.");
            }
        });
    }

    private void displayPet(PetDto pet) {
        selectedPet = pet;
        selectedPetId = pet.getId();
        Log.d(TAG, "Displaying Pet ID: " + selectedPetId);
        Log.d(TAG, "Displaying Pet Name: " + pet.getName());

        if (tvPetName != null) tvPetName.setText(pet.getName());
        
        String species = pet.getSpecies() != null ? pet.getSpecies() : "Unknown";
        String breed = pet.getBreed();
        String age = pet.getAgeYears() != null ? pet.getAgeYears() + " Years" : "Age Unknown";
        
        StringBuilder subHeader = new StringBuilder(species);
        if (breed != null && !breed.isEmpty()) {
            subHeader.append(" • ").append(breed);
        }
        subHeader.append(" • ").append(age);
        
        if (tvPetSpeciesAge != null) tvPetSpeciesAge.setText(subHeader.toString());
        
        // Avatar Initials
        String initials = "";
        if (pet.getName() != null && !pet.getName().isEmpty()) {
            String[] parts = pet.getName().trim().split("\\s+");
            if (parts.length > 0 && !parts[0].isEmpty()) {
                initials += parts[0].substring(0, 1).toUpperCase();
                if (parts.length > 1 && !parts[1].isEmpty()) {
                    initials += parts[1].substring(0, 1).toUpperCase();
                }
            }
        }
        if (tvPetInitials != null) tvPetInitials.setText(initials.isEmpty() ? "?" : initials);
        if (tvPetStatus != null) tvPetStatus.setText(pet.getStatus() != null ? pet.getStatus() : "Healthy");

        updateProfileInfo(pet);
        
        // Reload history data for the new pet
        refreshHistory();
        
        // Refresh UI state
        switchTab(isHistoryTab);
        updateHistoryFilterUI();
    }

    private void updateProfileInfo(PetDto pet) {
        if (llProfileContent == null) return;
        TextView tvName = llProfileContent.findViewById(R.id.tv_info_name);
        TextView tvSpecies = llProfileContent.findViewById(R.id.tv_info_species);
        TextView tvBreed = llProfileContent.findViewById(R.id.tv_info_breed);
        TextView tvAge = llProfileContent.findViewById(R.id.tv_info_age);
        TextView tvStatus = llProfileContent.findViewById(R.id.tv_info_status);

        if (tvName != null) tvName.setText("Name: " + pet.getName());
        if (tvSpecies != null) tvSpecies.setText("Species: " + (pet.getSpecies() != null ? pet.getSpecies() : "--"));
        if (tvBreed != null) tvBreed.setText("Breed: " + (pet.getBreed() != null ? pet.getBreed() : "--"));
        if (tvAge != null) tvAge.setText("Age: " + (pet.getAgeYears() != null ? pet.getAgeYears() + " Years" : "--"));
        if (tvStatus != null) tvStatus.setText("Status: " + (pet.getStatus() != null ? pet.getStatus() : "--"));
    }

    private void refreshHistory() {
        if (selectedPet == null) return;
        if (isMedicalFilter) {
            loadMedicalHistory(selectedPet.getId());
        } else {
            loadAppointmentHistory(selectedPet.getId());
        }
    }

    private void loadMedicalHistory(String petId) {
        Log.d(TAG, "medical records reload pet id: " + petId);
        String token = sessionManager.getAccessToken();
        if (llHistoryContainer == null) return;
        llHistoryContainer.removeAllViews();
        if (tvNoHistory != null) tvNoHistory.setVisibility(View.GONE);

        clientRepository.getMedicalRecordsByPetId(token, petId, new Callback<List<MedicalRecordDto>>() {
            @Override
            public void onResponse(@NonNull Call<List<MedicalRecordDto>> call, @NonNull Response<List<MedicalRecordDto>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    List<MedicalRecordDto> records = response.body();
                    Log.d(TAG, "medical records count: " + records.size());
                    if (records.isEmpty()) {
                        if (tvNoHistory != null) {
                            tvNoHistory.setText("No medical history yet.");
                            tvNoHistory.setVisibility(View.VISIBLE);
                        }
                    } else {
                        for (MedicalRecordDto record : records) addMedicalRecordCard(record);
                    }
                } else {
                    if (tvNoHistory != null) {
                        tvNoHistory.setText("Error loading history.");
                        tvNoHistory.setVisibility(View.VISIBLE);
                    }
                }
            }
            @Override
            public void onFailure(@NonNull Call<List<MedicalRecordDto>> call, @NonNull Throwable t) {
                if (tvNoHistory != null) {
                    tvNoHistory.setText("Network error.");
                    tvNoHistory.setVisibility(View.VISIBLE);
                }
            }
        });
    }

    private void loadAppointmentHistory(String petId) {
        Log.d(TAG, "appointment history reload pet id: " + petId);
        String token = sessionManager.getAccessToken();
        if (llHistoryContainer == null) return;
        llHistoryContainer.removeAllViews();
        if (tvNoHistory != null) tvNoHistory.setVisibility(View.GONE);

        clientRepository.getAppointmentsByPetId(token, petId, new Callback<List<AppointmentDto>>() {
            @Override
            public void onResponse(@NonNull Call<List<AppointmentDto>> call, @NonNull Response<List<AppointmentDto>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    List<AppointmentDto> appointments = response.body();
                    Log.d(TAG, "appointments count: " + appointments.size());
                    if (appointments.isEmpty()) {
                        if (tvNoHistory != null) {
                            tvNoHistory.setText("No appointment history yet.");
                            tvNoHistory.setVisibility(View.VISIBLE);
                        }
                    } else {
                        for (AppointmentDto appt : appointments) addAppointmentCard(appt);
                    }
                } else {
                    if (tvNoHistory != null) {
                        tvNoHistory.setText("Error loading appointments.");
                        tvNoHistory.setVisibility(View.VISIBLE);
                    }
                }
            }
            @Override
            public void onFailure(@NonNull Call<List<AppointmentDto>> call, @NonNull Throwable t) {
                if (tvNoHistory != null) {
                    tvNoHistory.setText("Network error.");
                    tvNoHistory.setVisibility(View.VISIBLE);
                }
            }
        });
    }

    private void addMedicalRecordCard(MedicalRecordDto record) {
        View card = getLayoutInflater().inflate(R.layout.item_medical_history, llHistoryContainer, false);
        TextView tvDate = card.findViewById(R.id.tv_history_date);
        TextView tvTitle = card.findViewById(R.id.tv_history_title);
        TextView tvPatient = card.findViewById(R.id.tv_history_patient);
        TextView tvNote = card.findViewById(R.id.tv_history_note);
        View root = card.findViewById(R.id.cv_history_item);

        tvDate.setText(formatDate(record.getRecordDate()));
        tvTitle.setText(record.getTitle());
        tvPatient.setText("Patient: " + (selectedPet != null ? selectedPet.getName() : "Unknown"));
        tvNote.setText(record.getNote() != null && !record.getNote().isEmpty() ? record.getNote() : "No notes available.");

        View.OnClickListener listener = v -> {
            Log.d(TAG, "clicked medical record id: " + record.getId());
            showMedicalRecordDetails(record);
        };
        if (root != null) root.setOnClickListener(listener);

        llHistoryContainer.addView(card);
    }

    private void addAppointmentCard(AppointmentDto appt) {
        View card = getLayoutInflater().inflate(R.layout.item_medical_history, llHistoryContainer, false);
        TextView tvDate = card.findViewById(R.id.tv_history_date);
        TextView tvTitle = card.findViewById(R.id.tv_history_title);
        TextView tvPatient = card.findViewById(R.id.tv_history_patient);
        TextView tvNote = card.findViewById(R.id.tv_history_note);
        View root = card.findViewById(R.id.cv_history_item);

        String dateStr = formatDate(appt.getScheduledAt());
        String timeStr = formatTime(appt.getScheduledAt());
        tvDate.setText(dateStr + " • " + timeStr);
        
        String title = appt.getTitle() != null ? appt.getTitle() : (appt.getCategory() != null ? appt.getCategory() : "Appointment");
        tvTitle.setText(title);
        
        String status = appt.getStatus() != null ? appt.getStatus().substring(0, 1).toUpperCase() + appt.getStatus().substring(1) : "Scheduled";
        tvPatient.setText(appt.getType() + " • " + status);
        
        String note = appt.getService() != null ? "Service: " + appt.getService() : (appt.getNote() != null ? appt.getNote() : "No details available.");
        tvNote.setText(note);

        View.OnClickListener listener = v -> {
            Log.d(TAG, "clicked appointment id: " + appt.getId());
            showAppointmentDetails(appt);
        };
        if (root != null) root.setOnClickListener(listener);

        llHistoryContainer.addView(card);
    }

    private void showMedicalRecordDetails(MedicalRecordDto record) {
        List<ModernDialogHelper.DetailItem> details = new ArrayList<>();
        details.add(new ModernDialogHelper.DetailItem("Title", record.getTitle() != null ? record.getTitle() : "Not specified"));
        details.add(new ModernDialogHelper.DetailItem("Date", formatDate(record.getRecordDate())));
        details.add(new ModernDialogHelper.DetailItem("Patient", selectedPet != null ? selectedPet.getName() : "Unknown"));
        details.add(new ModernDialogHelper.DetailItem("Type", record.getType() != null ? record.getType() : "Not specified"));
        details.add(new ModernDialogHelper.DetailItem("Attending", record.getAttending() != null ? record.getAttending() : "Not specified"));
        details.add(new ModernDialogHelper.DetailItem("Notes", record.getNote() != null && !record.getNote().isEmpty() ? record.getNote() : "No notes available"));

        ModernDialogHelper.showDetailsDialog(requireContext(), "Medical Record Details", details);
    }

    private void showAppointmentDetails(AppointmentDto appt) {
        List<ModernDialogHelper.DetailItem> details = new ArrayList<>();
        details.add(new ModernDialogHelper.DetailItem("Pet", selectedPet != null ? selectedPet.getName() : "Unknown"));
        details.add(new ModernDialogHelper.DetailItem("Type", appt.getType() != null ? appt.getType() : "Not specified"));
        details.add(new ModernDialogHelper.DetailItem("Category", appt.getService() != null ? appt.getService() : (appt.getCategory() != null ? appt.getCategory() : "Not specified")));
        details.add(new ModernDialogHelper.DetailItem("Date", formatDate(appt.getScheduledAt())));
        details.add(new ModernDialogHelper.DetailItem("Time", formatTime(appt.getScheduledAt())));
        details.add(new ModernDialogHelper.DetailItem("Status", appt.getStatus() != null ? appt.getStatus().toUpperCase() : "SCHEDULED"));
        details.add(new ModernDialogHelper.DetailItem("Note", appt.getNote() != null && !appt.getNote().isEmpty() ? appt.getNote() : "No details available"));

        ModernDialogHelper.showDetailsDialog(requireContext(), "Appointment Details", details);
    }

    private String formatDate(String iso) {
        if (iso == null || iso.isEmpty()) return "Unknown Date";
        try {
            SimpleDateFormat in = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault());
            Date d = in.parse(iso.split("\\.")[0]);
            return new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(d);
        } catch (Exception e) {
            try {
                return new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(iso));
            } catch (Exception e2) { return iso; }
        }
    }

    private String formatTime(String iso) {
        if (iso == null || !iso.contains("T")) return "";
        try {
            SimpleDateFormat in = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault());
            Date d = in.parse(iso.split("\\.")[0]);
            return new SimpleDateFormat("hh:mm a", Locale.getDefault()).format(d);
        } catch (Exception e) { return ""; }
    }

    private void switchTab(boolean history) {
        isHistoryTab = history;
        if (tvTabHistory == null || tvTabProfile == null || llHistoryContent == null || llProfileContent == null) return;
        int white = ContextCompat.getColor(requireContext(), R.color.white);
        int gray = ContextCompat.getColor(requireContext(), R.color.text_secondary);

        if (history) {
            tvTabHistory.setBackgroundResource(R.drawable.bg_button_primary);
            tvTabHistory.setTextColor(white);
            tvTabProfile.setBackground(null);
            tvTabProfile.setTextColor(gray);
            llHistoryContent.setVisibility(View.VISIBLE);
            llProfileContent.setVisibility(View.GONE);
            updateHistoryFilterUI();
            refreshHistory();
        } else {
            tvTabProfile.setBackgroundResource(R.drawable.bg_button_primary);
            tvTabProfile.setTextColor(white);
            tvTabHistory.setBackground(null);
            tvTabHistory.setTextColor(gray);
            llProfileContent.setVisibility(View.VISIBLE);
            llHistoryContent.setVisibility(View.GONE);
        }
    }

    private void switchHistoryFilter(boolean medical) {
        isMedicalFilter = medical;
        updateHistoryFilterUI();
        refreshHistory();
    }

    private void updateHistoryFilterUI() {
        if (tvFilterMedical == null || tvFilterAppointments == null) return;
        int white = ContextCompat.getColor(requireContext(), R.color.white);
        int teal = ContextCompat.getColor(requireContext(), R.color.primary_teal);

        if (isMedicalFilter) {
            tvFilterMedical.setBackgroundResource(R.drawable.bg_button_primary);
            tvFilterMedical.setTextColor(white);
            tvFilterAppointments.setBackgroundResource(R.drawable.bg_chip_teal);
            tvFilterAppointments.setTextColor(teal);
            if (tvHistoryTitle != null) tvHistoryTitle.setText("Medical History");
            if (tvHistoryFilterLabel != null) tvHistoryFilterLabel.setText("Recent Records");
        } else {
            tvFilterAppointments.setBackgroundResource(R.drawable.bg_button_primary);
            tvFilterAppointments.setTextColor(white);
            tvFilterMedical.setBackgroundResource(R.drawable.bg_chip_teal);
            tvFilterMedical.setTextColor(teal);
            if (tvHistoryTitle != null) tvHistoryTitle.setText("Appointment History");
            if (tvHistoryFilterLabel != null) tvHistoryFilterLabel.setText("All Appointments");
        }
    }

    private void showPetSelector() {
        if (petList == null || petList.isEmpty()) return;
        List<String> names = new ArrayList<>();
        int selectedIndex = -1;
        for (int i = 0; i < petList.size(); i++) {
            names.add(petList.get(i).getName());
            if (selectedPetId != null && petList.get(i).getId().equals(selectedPetId)) {
                selectedIndex = i;
            }
        }
        ModernDialogHelper.showListDialog(requireContext(), "Select Pet", names, selectedIndex, position -> {
            PetDto pet = petList.get(position);
            Log.d(TAG, "clicked pet id: " + pet.getId());
            Log.d(TAG, "clicked pet name: " + pet.getName());
            displayPet(pet);
            Log.d(TAG, "selected pet id after update: " + selectedPetId);
        });
    }

    private void showEmptyState(String message) {
        if (llNoPetsState == null || cvPetHeader == null) return;
        llNoPetsState.setVisibility(View.VISIBLE);
        TextView tvMsg = llNoPetsState.findViewById(R.id.tv_no_pets_msg);
        if (tvMsg != null) tvMsg.setText(message);
        cvPetHeader.setVisibility(View.GONE);
        View chips = getView() != null ? getView().findViewById(R.id.ll_info_chips) : null;
        if (chips != null) chips.setVisibility(View.GONE);
        llProfileContent.setVisibility(View.GONE);
        llHistoryContent.setVisibility(View.GONE);
    }

    private void hideEmptyState() {
        if (llNoPetsState == null || cvPetHeader == null) return;
        llNoPetsState.setVisibility(View.GONE);
        cvPetHeader.setVisibility(View.VISIBLE);
        View chips = getView() != null ? getView().findViewById(R.id.ll_info_chips) : null;
        if (chips != null) chips.setVisibility(View.VISIBLE);
        switchTab(isHistoryTab);
    }
}
