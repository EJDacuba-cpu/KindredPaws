package com.firstapp.kidredpawpaws;

import android.app.Dialog;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.text.TextUtils;
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
import androidx.cardview.widget.CardView;
import androidx.fragment.app.Fragment;

import com.firstapp.kidredpawpaws.models.supabase.AppointmentDto;
import com.firstapp.kidredpawpaws.models.supabase.PetCreateRequest;
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

public class BookFragment extends Fragment {

    private static final String TAG = "BookFragment";
    private ClientRepository clientRepository;
    private SessionManager sessionManager;

    private TextView tvUpcomingLabel;
    private CardView cvUpcoming;
    private TextView tvPetName, tvService, tvDateTime;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_book, container, false);

        clientRepository = new ClientRepository();
        sessionManager = new SessionManager(requireContext());

        tvUpcomingLabel = view.findViewById(R.id.tv_upcoming_label);
        cvUpcoming = view.findViewById(R.id.cv_upcoming_appointment);
        tvPetName = view.findViewById(R.id.tv_upcoming_pet);
        tvService = view.findViewById(R.id.tv_upcoming_service);
        tvDateTime = view.findViewById(R.id.tv_upcoming_datetime);

        View cardVet = view.findViewById(R.id.card_book_vet);
        View cardGrooming = view.findViewById(R.id.card_book_grooming);

        if (cardVet != null) {
            cardVet.setOnClickListener(v -> startBookingFlow("vet"));
        }
        if (cardGrooming != null) {
            cardGrooming.setOnClickListener(v -> startBookingFlow("grooming"));
        }

        // Updated Add Pet button location to header
        View tvAddPetHeader = view.findViewById(R.id.tv_add_pet_header);
        if (tvAddPetHeader != null) {
            tvAddPetHeader.setOnClickListener(v -> showAddPetDialog());
        }

        loadUpcomingAppointment();

        return view;
    }

    private void startBookingFlow(String type) {
        Intent intent = new Intent(getActivity(), AppointmentBookingActivity.class);
        intent.putExtra("booking_type", type);
        startActivity(intent);
    }

    private void loadUpcomingAppointment() {
        String ownerId = sessionManager.getOwnerId();
        String token = sessionManager.getAccessToken();

        if (ownerId == null || token == null) return;

        clientRepository.getPetsByOwnerId(token, ownerId, new Callback<List<PetDto>>() {
            @Override
            public void onResponse(@NonNull Call<List<PetDto>> call, @NonNull Response<List<PetDto>> response) {
                if (response.isSuccessful() && response.body() != null && !response.body().isEmpty()) {
                    List<PetDto> pets = response.body();
                    List<String> petIds = new ArrayList<>();
                    for (PetDto p : pets) petIds.add(p.getId());
                    fetchAppointments(token, petIds, pets);
                }
            }
            @Override
            public void onFailure(@NonNull Call<List<PetDto>> call, @NonNull Throwable t) {
                Log.e(TAG, "Error fetching pets", t);
            }
        });
    }

    private void fetchAppointments(String token, List<String> petIds, List<PetDto> pets) {
        String idsCsv = String.join(",", petIds);
        clientRepository.getAppointmentsByPetIds(token, idsCsv, new Callback<List<AppointmentDto>>() {
            @Override
            public void onResponse(@NonNull Call<List<AppointmentDto>> call, @NonNull Response<List<AppointmentDto>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    displayFirstUpcoming(response.body(), pets);
                }
            }
            @Override
            public void onFailure(@NonNull Call<List<AppointmentDto>> call, @NonNull Throwable t) {
                Log.e(TAG, "Error fetching appointments", t);
            }
        });
    }

    private void displayFirstUpcoming(List<AppointmentDto> appointments, List<PetDto> pets) {
        if (appointments == null || appointments.isEmpty() || !isAdded()) return;

        AppointmentDto upcoming = null;
        Date now = new Date();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault());

        for (int i = appointments.size() - 1; i >= 0; i--) {
            AppointmentDto appt = appointments.get(i);
            try {
                String s = appt.getScheduledAt().split("\\.")[0].replace("Z", "");
                Date d = sdf.parse(s);
                if (d != null && d.after(now) && "scheduled".equals(appt.getStatus())) {
                    upcoming = appt;
                    break;
                }
            } catch (Exception ignored) {}
        }

        if (upcoming != null) {
            tvUpcomingLabel.setVisibility(View.VISIBLE);
            cvUpcoming.setVisibility(View.VISIBLE);
            
            String name = "Unknown Pet";
            for (PetDto p : pets) {
                if (p.getId().equals(upcoming.getPetId())) {
                    name = p.getName();
                    break;
                }
            }
            
            tvPetName.setText(name);
            tvService.setText(upcoming.getTitle() != null ? upcoming.getTitle() : upcoming.getCategory());
            tvDateTime.setText(formatDate(upcoming.getScheduledAt()) + " • " + formatTime(upcoming.getScheduledAt()));
        } else {
            tvUpcomingLabel.setVisibility(View.GONE);
            cvUpcoming.setVisibility(View.GONE);
        }
    }

    private void showAddPetDialog() {
        Dialog dialog = new Dialog(requireContext());
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        View v = getLayoutInflater().inflate(R.layout.dialog_add_pet, null);
        dialog.setContentView(v);

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            dialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        }

        EditText etName = v.findViewById(R.id.et_pet_name);
        EditText etSpecies = v.findViewById(R.id.et_pet_species);
        EditText etBreed = v.findViewById(R.id.et_pet_breed);
        EditText etAge = v.findViewById(R.id.et_pet_age);
        TextView tvErr = v.findViewById(R.id.tv_add_pet_error);
        Button btnCancel = v.findViewById(R.id.btn_cancel_pet);
        Button btnSave = v.findViewById(R.id.btn_save_pet);

        btnCancel.setOnClickListener(view -> dialog.dismiss());
        btnSave.setOnClickListener(view -> {
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
                        loadUpcomingAppointment();
                    } else {
                        tvErr.setText("Failed to add pet. Please try again.");
                        tvErr.setVisibility(View.VISIBLE);
                    }
                }
                @Override
                public void onFailure(@NonNull Call<List<PetDto>> call, @NonNull Throwable t) {
                    tvErr.setText("Network error. Please try again.");
                    tvErr.setVisibility(View.VISIBLE);
                }
            });
        });
        dialog.show();
    }

    private String formatDate(String iso) {
        try {
            SimpleDateFormat in = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault());
            Date d = in.parse(iso.split("\\.")[0]);
            return new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(d);
        } catch (Exception e) { return iso; }
    }

    private String formatTime(String iso) {
        try {
            SimpleDateFormat in = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault());
            Date d = in.parse(iso.split("\\.")[0]);
            return new SimpleDateFormat("hh:mm a", Locale.getDefault()).format(d);
        } catch (Exception e) { return ""; }
    }
}
