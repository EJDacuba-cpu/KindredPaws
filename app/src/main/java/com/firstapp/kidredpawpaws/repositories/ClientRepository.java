package com.firstapp.kidredpawpaws.repositories;

import com.firstapp.kidredpawpaws.models.supabase.AppointmentCreateRequest;
import com.firstapp.kidredpawpaws.models.supabase.AppointmentDto;
import com.firstapp.kidredpawpaws.models.supabase.MedicalRecordDto;
import com.firstapp.kidredpawpaws.models.supabase.OwnerDto;
import com.firstapp.kidredpawpaws.models.supabase.OwnerUpdateRequest;
import com.firstapp.kidredpawpaws.models.supabase.PetCreateRequest;
import com.firstapp.kidredpawpaws.models.supabase.PetDto;
import com.firstapp.kidredpawpaws.models.supabase.StaffDto;
import com.firstapp.kidredpawpaws.network.ApiClient;
import com.firstapp.kidredpawpaws.network.ApiService;

import java.util.ArrayList;
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

    public void getOwnerById(String accessToken, String ownerId, Callback<List<OwnerDto>> callback) {
        String authHeader = "Bearer " + accessToken;
        apiService.getOwnerById(authHeader, "eq." + ownerId, "*").enqueue(callback);
    }

    public void updateOwner(String accessToken, String ownerId, OwnerUpdateRequest request, Callback<Void> callback) {
        String authHeader = "Bearer " + accessToken;
        apiService.updateOwner(authHeader, "eq." + ownerId, request).enqueue(callback);
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

    public void getStaff(String accessToken, Callback<List<StaffDto>> callback) {
        String authHeader = "Bearer " + accessToken;
        apiService.getStaff(authHeader, "*").enqueue(callback);
    }

    public void getNextAppointmentByPetId(String accessToken, String petId, Callback<List<AppointmentDto>> callback) {
        String authHeader = "Bearer " + accessToken;
        apiService.getAppointments(authHeader, "eq." + petId, "*", "scheduled_at.asc", 1).enqueue(callback);
    }

    public void getAppointmentsByPetId(String accessToken, String petId, Callback<List<AppointmentDto>> callback) {
        String authHeader = "Bearer " + accessToken;
        apiService.getAppointments(authHeader, "eq." + petId, "*", "scheduled_at.desc", 1000).enqueue(callback);
    }

    public void getAppointmentsForConflictCheck(String accessToken, String startOfDay, String endOfDay, Callback<List<AppointmentDto>> callback) {
        String authHeader = "Bearer " + accessToken;
        List<String> range = new ArrayList<>();
        range.add("gte." + startOfDay);
        range.add("lte." + endOfDay);
        apiService.getAppointmentsByRange(authHeader, range, "in.(scheduled,pending)", "*", "scheduled_at.asc").enqueue(callback);
    }

    public void createAppointment(String accessToken, AppointmentCreateRequest request, Callback<List<AppointmentDto>> callback) {
        String authHeader = "Bearer " + accessToken;
        String preferHeader = "return=representation";
        apiService.createAppointment(authHeader, preferHeader, request).enqueue(callback);
    }

    public void getMedicalRecordsByPetId(String accessToken, String petId, Callback<List<MedicalRecordDto>> callback) {
        String authHeader = "Bearer " + accessToken;
        apiService.getMedicalRecords(authHeader, "eq." + petId, "*", "record_date.desc").enqueue(callback);
    }

    public void getAppointmentsByPetIds(String accessToken, String petIdsCsv, Callback<List<AppointmentDto>> callback) {
        String authHeader = "Bearer " + accessToken;
        apiService.getAppointments(authHeader, "in.(" + petIdsCsv + ")", "*", "scheduled_at.desc", 1000).enqueue(callback);
    }
}
