package com.firstapp.kidredpawpaws;

import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.cardview.widget.CardView;
import androidx.fragment.app.Fragment;

import com.firstapp.kidredpawpaws.models.supabase.AppointmentDto;
import com.firstapp.kidredpawpaws.models.supabase.OwnerDto;
import com.firstapp.kidredpawpaws.models.supabase.PetDto;
import com.firstapp.kidredpawpaws.repositories.ClientRepository;
import com.firstapp.kidredpawpaws.utils.DateTimeUtils;
import com.firstapp.kidredpawpaws.utils.SessionManager;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class HomeFragment extends Fragment {

    private static final String TAG = "HomeFragment";
    private TextView tvGreeting, tvGreetingSubtitle;
    private TextView tvNoVisit, tvNoPets;
    private CardView cardNextVisit;
    private TextView tvPetNameVisit, tvServiceVisit, tvTimeVisit, tvLocationVisit;
    private LinearLayout llPetsContainer;

    private ClientRepository clientRepository;
    private SessionManager sessionManager;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home, container, false);

        clientRepository = new ClientRepository();
        sessionManager = new SessionManager(requireContext());

        tvGreeting = view.findViewById(R.id.tv_greeting);
        tvGreetingSubtitle = view.findViewById(R.id.tv_greeting_subtitle);
        tvNoVisit = view.findViewById(R.id.tv_no_visit);
        tvNoPets = view.findViewById(R.id.tv_no_pets);
        cardNextVisit = view.findViewById(R.id.card_next_visit);
        tvPetNameVisit = view.findViewById(R.id.tv_pet_name_visit);
        tvServiceVisit = view.findViewById(R.id.tv_service_visit);
        tvTimeVisit = view.findViewById(R.id.tv_time_visit);
        tvLocationVisit = view.findViewById(R.id.tv_location_visit);
        llPetsContainer = view.findViewById(R.id.ll_pets_container);

        view.findViewById(R.id.card_book_checkup).setOnClickListener(v -> navigateToTab(R.id.nav_book));
        view.findViewById(R.id.card_book_grooming).setOnClickListener(v -> navigateToTab(R.id.nav_book));
        view.findViewById(R.id.tv_view_all_pets).setOnClickListener(v -> navigateToTab(R.id.nav_pets));

        view.findViewById(R.id.iv_notifications).setOnClickListener(v ->
                Log.d(TAG, "Notifications coming soon"));

        loadHomeData();

        return view;
    }

    private void navigateToTab(int tabId) {
        if (getActivity() instanceof MainActivity) {
            ((MainActivity) getActivity()).navigateToTab(tabId);
        }
    }

    private void loadHomeData() {
        String email = sessionManager.getEmail();
        String ownerId = sessionManager.getOwnerId();
        String ownerName = sessionManager.getOwnerName();
        String accessToken = sessionManager.getAccessToken();

        if (email == null || email.isEmpty() || accessToken == null) {
            tvGreeting.setText("Hello, Guest!");
            tvGreetingSubtitle.setText("Please login again.");
            showNoPets();
            showNoVisit();
            return;
        }

        if (ownerName != null) {
            updateGreeting(ownerName);
        }

        if (ownerId == null) {
            fetchOwnerByEmail(accessToken, email);
        } else {
            fetchPets(accessToken, ownerId);
        }
    }

    private void fetchOwnerByEmail(String accessToken, String email) {
        clientRepository.getOwnerByEmail(accessToken, email, new Callback<List<OwnerDto>>() {
            @Override
            public void onResponse(@NonNull Call<List<OwnerDto>> call, @NonNull Response<List<OwnerDto>> response) {
                if (response.isSuccessful() && response.body() != null && !response.body().isEmpty()) {
                    OwnerDto owner = response.body().get(0);
                    sessionManager.saveOwnerId(owner.getId());
                    sessionManager.saveOwnerName(owner.getFullName());
                    updateGreeting(owner.getFullName());
                    fetchPets(accessToken, owner.getId());
                } else {
                    handleOwnerNotFound(response, email);
                }
            }

            @Override
            public void onFailure(@NonNull Call<List<OwnerDto>> call, @NonNull Throwable t) {
                updateGreeting("Guest");
            }
        });
    }

    private void handleOwnerNotFound(Response<List<OwnerDto>> response, String email) {
        tvGreeting.setText("Hello, Guest!");
        tvGreetingSubtitle.setText("No customer profile found.");
        showNoPets();
        showNoVisit();
    }

    private void fetchPets(String accessToken, String ownerId) {
        clientRepository.getPetsByOwnerId(accessToken, ownerId, new Callback<List<PetDto>>() {
            @Override
            public void onResponse(@NonNull Call<List<PetDto>> call, @NonNull Response<List<PetDto>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    List<PetDto> pets = response.body();
                    updatePetsList(pets);
                    if (!pets.isEmpty()) {
                        fetchAllAppointments(accessToken, pets);
                    } else {
                        showNoVisit();
                    }
                } else {
                    showNoPets();
                    showNoVisit();
                }
            }

            @Override
            public void onFailure(@NonNull Call<List<PetDto>> call, @NonNull Throwable t) {
                showNoPets();
            }
        });
    }

    private void fetchAllAppointments(String accessToken, List<PetDto> pets) {
        List<String> petIds = new ArrayList<>();
        for (PetDto p : pets) petIds.add(p.getId());
        String petIdsCsv = TextUtils.join(",", petIds);

        clientRepository.getAppointmentsByPetIds(accessToken, petIdsCsv, new Callback<List<AppointmentDto>>() {
            @Override
            public void onResponse(@NonNull Call<List<AppointmentDto>> call, @NonNull Response<List<AppointmentDto>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    findAndDisplayNearestUpcoming(response.body(), pets);
                } else {
                    showNoVisit();
                }
            }

            @Override
            public void onFailure(@NonNull Call<List<AppointmentDto>> call, @NonNull Throwable t) {
                showNoVisit();
            }
        });
    }

    private void findAndDisplayNearestUpcoming(List<AppointmentDto> appointments, List<PetDto> pets) {
        Date now = new Date();
        List<AppointmentDto> upcomingCandidates = new ArrayList<>();

        for (AppointmentDto appt : appointments) {
            Date apptDate = DateTimeUtils.parseIsoDateTime(appt.getScheduledAt());
            String status = appt.getStatus() != null ? appt.getStatus().toLowerCase() : "";
            if (apptDate != null && apptDate.after(now) && (status.equals("scheduled") || status.equals("pending"))) {
                upcomingCandidates.add(appt);
            }
        }

        if (upcomingCandidates.isEmpty()) {
            showNoVisit();
            return;
        }

        // Sort by scheduled_at ascending
        Collections.sort(upcomingCandidates, new Comparator<AppointmentDto>() {
            @Override
            public int compare(AppointmentDto a1, AppointmentDto a2) {
                Date d1 = DateTimeUtils.parseIsoDateTime(a1.getScheduledAt());
                Date d2 = DateTimeUtils.parseIsoDateTime(a2.getScheduledAt());
                if (d1 == null || d2 == null) return 0;
                return d1.compareTo(d2);
            }
        });

        AppointmentDto nearest = upcomingCandidates.get(0);
        PetDto pet = null;
        for (PetDto p : pets) {
            if (p.getId().equals(nearest.getPetId())) {
                pet = p;
                break;
            }
        }

        String petName = pet != null ? pet.getName() : "Unknown Pet";
        updateNextVisitUI(nearest, petName);
    }

    private void updateNextVisitUI(AppointmentDto appointment, String petName) {
        tvNoVisit.setVisibility(View.GONE);
        cardNextVisit.setVisibility(View.VISIBLE);

        String title = appointment.getTitle();
        if (title == null || title.isEmpty()) {
            title = appointment.getCategory() != null ? appointment.getCategory() : 
                   (appointment.getService() != null ? appointment.getService() : "Appointment");
        }
        
        tvPetNameVisit.setText(petName);
        tvServiceVisit.setText(title);
        
        String formattedDateTime = DateTimeUtils.formatAppointmentDateTime(appointment.getScheduledAt());
        tvTimeVisit.setText(formattedDateTime);
        
        tvLocationVisit.setText(appointment.getRoom() != null ? appointment.getRoom() : 
                               (appointment.getVet() != null ? appointment.getVet() : "Main Clinic"));

        Log.d(TAG, "Displaying upcoming appt: " + appointment.getId() + " for " + petName + " at " + formattedDateTime);
    }

    private void updateGreeting(String name) {
        if (name != null) {
            String firstName = name.split(" ")[0];
            tvGreeting.setText("Hello, " + firstName + "!");
        } else {
            tvGreeting.setText("Hello, Guest!");
        }
    }

    private void updatePetsList(List<PetDto> pets) {
        llPetsContainer.removeAllViews();
        if (pets == null || pets.isEmpty()) {
            showNoPets();
            return;
        }

        tvNoPets.setVisibility(View.GONE);
        int limit = Math.min(pets.size(), 2);
        for (int i = 0; i < limit; i++) {
            addPetCard(pets.get(i));
        }
    }

    private void addPetCard(PetDto pet) {
        View petCardView = getLayoutInflater().inflate(R.layout.item_pet_card_home, llPetsContainer, false);
        TextView tvName = petCardView.findViewById(R.id.tv_pet_name);
        TextView tvAge = petCardView.findViewById(R.id.tv_pet_age);
        TextView tvStatus = petCardView.findViewById(R.id.tv_pet_status);
        View avatar = petCardView.findViewById(R.id.iv_pet_avatar);

        tvName.setText(pet.getName());
        if (pet.getAgeYears() != null) {
            tvAge.setText(pet.getAgeYears() + " years old");
        } else {
            tvAge.setText("Age unknown");
        }
        
        if (pet.getStatus() != null && !pet.getStatus().isEmpty()) {
            tvStatus.setText(pet.getStatus());
            tvStatus.setVisibility(View.VISIBLE);
        } else {
            tvStatus.setVisibility(View.GONE);
        }

        if ("Cat".equalsIgnoreCase(pet.getSpecies())) {
            avatar.setBackgroundResource(R.drawable.bg_chip_teal);
        } else {
            avatar.setBackgroundResource(R.drawable.bg_chip_orange);
        }

        llPetsContainer.addView(petCardView);
    }

    private void showNoVisit() {
        cardNextVisit.setVisibility(View.GONE);
        tvNoVisit.setVisibility(View.VISIBLE);
        tvNoVisit.setText("No upcoming appointment\nBook a visit for your pet today.");
    }

    private void showNoPets() {
        llPetsContainer.removeAllViews();
        tvNoPets.setVisibility(View.VISIBLE);
        tvNoPets.setText("No pets yet.");
    }
}
