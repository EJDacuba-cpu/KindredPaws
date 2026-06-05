package com.firstapp.kidredpawpaws.models.supabase;

import com.google.gson.annotations.SerializedName;

public class AppointmentCreateRequest {
    @SerializedName("pet_id")
    private String petId;
    private String type;
    private String category;
    private String title;
    private String service;
    @SerializedName("scheduled_at")
    private String scheduledAt;
    @SerializedName("duration_minutes")
    private Integer durationMinutes;
    private String status;
    private String note;

    public AppointmentCreateRequest(String petId, String type, String category, String title, String service, String scheduledAt, Integer durationMinutes, String status, String note) {
        this.petId = petId;
        this.type = type;
        this.category = category;
        this.title = title;
        this.service = service;
        this.scheduledAt = scheduledAt;
        this.durationMinutes = durationMinutes;
        this.status = status;
        this.note = note;
    }

    public String getPetId() { return petId; }
    public void setPetId(String petId) { this.petId = petId; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getService() { return service; }
    public void setService(String service) { this.service = service; }

    public String getScheduledAt() { return scheduledAt; }
    public void setScheduledAt(String scheduledAt) { this.scheduledAt = scheduledAt; }

    public Integer getDurationMinutes() { return durationMinutes; }
    public void setDurationMinutes(Integer durationMinutes) { this.durationMinutes = durationMinutes; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }
}
