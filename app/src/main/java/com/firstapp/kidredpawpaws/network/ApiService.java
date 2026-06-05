package com.firstapp.kidredpawpaws.network;

import com.firstapp.kidredpawpaws.models.auth.AuthResponse;
import com.firstapp.kidredpawpaws.models.auth.LoginRequest;
import com.firstapp.kidredpawpaws.models.auth.SignUpRequest;
import com.firstapp.kidredpawpaws.models.supabase.AppointmentCreateRequest;
import com.firstapp.kidredpawpaws.models.supabase.AppointmentDto;
import com.firstapp.kidredpawpaws.models.supabase.MedicalRecordDto;
import com.firstapp.kidredpawpaws.models.supabase.OwnerCreateRequest;
import com.firstapp.kidredpawpaws.models.supabase.OwnerDto;
import com.firstapp.kidredpawpaws.models.supabase.PetCreateRequest;
import com.firstapp.kidredpawpaws.models.supabase.PetDto;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.POST;
import retrofit2.http.Query;

public interface ApiService {

    @POST("auth/v1/signup")
    Call<AuthResponse> signUp(@Body SignUpRequest request);

    @POST("auth/v1/token?grant_type=password")
    Call<AuthResponse> login(@Body LoginRequest request);

    @POST("rest/v1/owners")
    Call<List<OwnerDto>> createOwner(
            @Header("Authorization") String authorization,
            @Header("Prefer") String prefer,
            @Body OwnerCreateRequest request
    );

    @GET("rest/v1/owners")
    Call<List<OwnerDto>> getOwners(
            @Header("Authorization") String authorization,
            @Query("email") String email,
            @Query("select") String select
    );

    @GET("rest/v1/pets")
    Call<List<PetDto>> getPets(
            @Header("Authorization") String authorization,
            @Query("owner_id") String ownerId,
            @Query("select") String select
    );

    @POST("rest/v1/pets")
    Call<List<PetDto>> createPet(
            @Header("Authorization") String authorization,
            @Header("Prefer") String prefer,
            @Body PetCreateRequest request
    );

    @GET("rest/v1/appointments")
    Call<List<AppointmentDto>> getAppointments(
            @Header("Authorization") String authorization,
            @Query("pet_id") String petId,
            @Query("select") String select,
            @Query("order") String order,
            @Query("limit") Integer limit
    );

    @POST("rest/v1/appointments")
    Call<List<AppointmentDto>> createAppointment(
            @Header("Authorization") String authorization,
            @Header("Prefer") String prefer,
            @Body AppointmentCreateRequest request
    );

    @GET("rest/v1/medical_records")
    Call<List<MedicalRecordDto>> getMedicalRecords(
            @Header("Authorization") String authorization,
            @Query("pet_id") String petId,
            @Query("select") String select
    );
}
