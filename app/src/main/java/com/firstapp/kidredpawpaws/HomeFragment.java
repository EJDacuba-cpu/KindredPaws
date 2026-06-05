package com.firstapp.kidredpawpaws;

import android.os.Bundle;
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
import com.firstapp.kidredpawpaws.utils.SessionManager;

import java.io.IOException;
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

        Log.d(TAG, "Loading Home data. Logged-in email: " + email);
        Log.d(TAG, "Current saved ownerId: " + ownerId + ", ownerName: " + ownerName);

        if (email == null || email.isEmpty() || accessToken == null) {
            Log.e(TAG, "No email or access token found in SessionManager.");
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
            Log.d(TAG, "OwnerId is missing. Fetching owner by email...");
            fetchOwnerByEmail(accessToken, email);
        } else {
            Log.d(TAG, "OwnerId exists. Fetching pets...");
            fetchPets(accessToken, ownerId);
        }
    }

    private void fetchOwnerByEmail(String accessToken, String email) {
        clientRepository.getOwnerByEmail(accessToken, email, new Callback<List<OwnerDto>>() {
            @Override
            public void onResponse(@NonNull Call<List<OwnerDto>> call, @NonNull Response<List<OwnerDto>> response) {
                if (response.isSuccessful() && response.body() != null && !response.body().isEmpty()) {
                    OwnerDto owner = response.body().get(0);
                    Log.d(TAG, "Owner lookup success result: Found owner " + owner.getId());
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
                Log.e(TAG, "Owner lookup result: API failure", t);
                updateGreeting("Guest");
            }
        });
    }

    private void handleOwnerNotFound(Response<List<OwnerDto>> response, String email) {
        Log.w(TAG, "Owner lookup result: Not found for email " + email);
        if (response.errorBody() != null) {
            try {
                Log.e(TAG, "Error body: " + response.errorBody().string());
            } catch (IOException e) {
                Log.e(TAG, "Error reading error body", e);
            }
        }
        tvGreeting.setText("Hello, Guest!");
        tvGreetingSubtitle.setText("No customer profile found.");
        showNoPets();
        showNoVisit();
    }

    private void fetchPets(String accessToken, String ownerId) {
        Log.d(TAG, "Fetching pets for ownerId: " + ownerId);
        clientRepository.getPetsByOwnerId(accessToken, ownerId, new Callback<List<PetDto>>() {
            @Override
            public void onResponse(@NonNull Call<List<PetDto>> call, @NonNull Response<List<PetDto>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    List<PetDto> pets = response.body();
                    Log.d(TAG, "Pets response: SUCCESS. Found " + pets.size() + " pets.");
                    updatePetsList(pets);
                    if (!pets.isEmpty()) {
                        fetchNextAppointment(accessToken, pets.get(0).getId(), pets.get(0).getName());
                    } else {
                        showNoVisit();
                    }
                } else {
                    Log.e(TAG, "Pets response: FAILURE. Status: " + response.code());
                    showNoPets();
                    showNoVisit();
                }
            }

            @Override
            public void onFailure(@NonNull Call<List<PetDto>> call, @NonNull Throwable t) {
                Log.e(TAG, "Pets response: API FAILURE", t);
                showNoPets();
            }
        });
    }

    private void fetchNextAppointment(String accessToken, String petId, String petName) {
        Log.d(TAG, "Fetching next appointment for petId: " + petId);
        clientRepository.getNextAppointmentByPetId(accessToken, petId, new Callback<List<AppointmentDto>>() {
            @Override
            public void onResponse(@NonNull Call<List<AppointmentDto>> call, @NonNull Response<List<AppointmentDto>> response) {
                if (response.isSuccessful() && response.body() != null && !response.body().isEmpty()) {
                    AppointmentDto appointment = response.body().get(0);
                    Log.d(TAG, "Appointment result: Found upcoming for pet " + petId);
                    updateNextVisit(appointment, petName);
                } else {
                    Log.d(TAG, "Appointment result: No upcoming appointments found for pet " + petId);
                    showNoVisit();
                }
            }

            @Override
            public void onFailure(@NonNull Call<List<AppointmentDto>> call, @NonNull Throwable t) {
                Log.e(TAG, "Appointment result: API failure", t);
                showNoVisit();
            }
        });
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
        // Show first two pets
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

    private void updateNextVisit(AppointmentDto appointment, String petName) {
        tvNoVisit.setVisibility(View.GONE);
        cardNextVisit.setVisibility(View.VISIBLE);

        String displayTitle = appointment.getTitle();
        if (displayTitle == null || displayTitle.isEmpty()) {
            displayTitle = appointment.getService() != null ? appointment.getService() : appointment.getType();
        }
        
        tvPetNameVisit.setText(petName);
        tvServiceVisit.setText(displayTitle);
        tvTimeVisit.setText(appointment.getScheduledAt());
        tvLocationVisit.setText(appointment.getRoom() != null ? appointment.getRoom() : 
                               (appointment.getVet() != null ? appointment.getVet() : "Clinic"));
    }

    private void showNoVisit() {
        cardNextVisit.setVisibility(View.GONE);
        tvNoVisit.setVisibility(View.VISIBLE);
    }

    private void showNoPets() {
        llPetsContainer.removeAllViews();
        tvNoPets.setVisibility(View.VISIBLE);
        tvNoPets.setText("No pets yet.");
    }
}
