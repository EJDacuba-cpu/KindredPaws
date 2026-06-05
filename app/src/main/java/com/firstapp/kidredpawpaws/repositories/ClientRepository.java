package com.firstapp.kidredpawpaws.repositories;

import com.firstapp.kidredpawpaws.models.supabase.AppointmentCreateRequest;
import com.firstapp.kidredpawpaws.models.supabase.AppointmentDto;
import com.firstapp.kidredpawpaws.models.supabase.MedicalRecordDto;
import com.firstapp.kidredpawpaws.models.supabase.OwnerDto;
import com.firstapp.kidredpawpaws.models.supabase.PetCreateRequest;
import com.firstapp.kidredpawpaws.models.supabase.PetDto;
import com.firstapp.kidredpawpaws.network.ApiClient;
import com.firstapp.kidredpawpaws.network.ApiService;

import java.util.List;

import retrofit2.Callback;

public class ClientRepository {
    private final ApiService apiService;

    public ClientRepository() {
        apiService = ApiClient.getClient().create(ApiService.class);
    }

    public void getOwnerByEmail(String accessToken, String email, Callback<List<OwnerDto>> callback) {
        String authHeader = "Bearer " + accessToken;
        apiService.getOwners(authHeader, "eq." + email, "*").enqueue(callback);
    }

    public void getPetsByOwnerId(String accessToken, String ownerId, Callback<List<PetDto>> callback) {
        String authHeader = "Bearer " + accessToken;
        apiService.getPets(authHeader, "eq." + ownerId, "*").enqueue(callback);
    }

    public void createPet(String accessToken, PetCreateRequest request, Callback<List<PetDto>> callback) {
        String authHeader = "Bearer " + accessToken;
        String preferHeader = "return=representation";
        apiService.createPet(authHeader, preferHeader, request).enqueue(callback);
    }

    public void getNextAppointmentByPetId(String accessToken, String petId, Callback<List<AppointmentDto>> callback) {
        String authHeader = "Bearer " + accessToken;
        apiService.getAppointments(authHeader, "eq." + petId, "*", "scheduled_at.asc", 1).enqueue(callback);
    }

    public void createAppointment(String accessToken, AppointmentCreateRequest request, Callback<List<AppointmentDto>> callback) {
        String authHeader = "Bearer " + accessToken;
        String preferHeader = "return=representation";
        apiService.createAppointment(authHeader, preferHeader, request).enqueue(callback);
    }

    public void getMedicalRecordsByPetId(String accessToken, String petId, Callback<List<MedicalRecordDto>> callback) {
        String authHeader = "Bearer " + accessToken;
        apiService.getMedicalRecords(authHeader, "eq." + petId, "*").enqueue(callback);
    }
}
