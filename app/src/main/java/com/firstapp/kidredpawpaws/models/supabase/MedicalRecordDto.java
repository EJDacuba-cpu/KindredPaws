package com.firstapp.kidredpawpaws.models.supabase;

import com.google.gson.annotations.SerializedName;

public class MedicalRecordDto {
    private String id;
    @SerializedName("pet_id")
    private String petId;
    private String title;
    @SerializedName("record_date")
    private String recordDate;
    private String type;
    private String attending;
    private String note;
    @SerializedName("created_at")
    private String createdAt;

    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getPetId() { return petId; }
    public void setPetId(String petId) { this.petId = petId; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getRecordDate() { return recordDate; }
    public void setRecordDate(String recordDate) { this.recordDate = recordDate; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getAttending() { return attending; }
    public void setAttending(String attending) { this.attending = attending; }

    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }

    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }
}
