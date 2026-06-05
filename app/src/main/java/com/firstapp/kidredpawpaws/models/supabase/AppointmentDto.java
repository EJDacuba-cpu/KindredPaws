package com.firstapp.kidredpawpaws.models.supabase;

import com.google.gson.annotations.SerializedName;

public class AppointmentDto {
    private String id;
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
    @SerializedName("staff_id")
    private String staffId;
    private String note;
    private String vet;
    private String room;
    @SerializedName("created_at")
    private String createdAt;

    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

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

    public String getStaffId() { return staffId; }
    public void setStaffId(String staffId) { this.staffId = staffId; }

    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }

    public String getVet() { return vet; }
    public void setVet(String vet) { this.vet = vet; }

    public String getRoom() { return room; }
    public void setRoom(String room) { this.room = room; }

    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }
}
