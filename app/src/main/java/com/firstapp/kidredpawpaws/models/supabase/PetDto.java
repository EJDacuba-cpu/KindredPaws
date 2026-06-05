package com.firstapp.kidredpawpaws.models.supabase;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class PetDto {
    private String id;
    private String name;
    private String species;
    private String breed;
    @SerializedName("owner_id")
    private String ownerId;
    @SerializedName("photo_url")
    private String photoUrl;
    @SerializedName("age_years")
    private Integer ageYears;
    private String status;
    private List<String> tags;
    @SerializedName("next_visit")
    private String nextVisit;
    @SerializedName("created_at")
    private String createdAt;

    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getSpecies() { return species; }
    public void setSpecies(String species) { this.species = species; }

    public String getBreed() { return breed; }
    public void setBreed(String breed) { this.breed = breed; }

    public String getOwnerId() { return ownerId; }
    public void setOwnerId(String ownerId) { this.ownerId = ownerId; }

    public String getPhotoUrl() { return photoUrl; }
    public void setPhotoUrl(String photoUrl) { this.photoUrl = photoUrl; }

    public Integer getAgeYears() { return ageYears; }
    public void setAgeYears(Integer ageYears) { this.ageYears = ageYears; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public List<String> getTags() { return tags; }
    public void setTags(List<String> tags) { this.tags = tags; }

    public String getNextVisit() { return nextVisit; }
    public void setNextVisit(String nextVisit) { this.nextVisit = nextVisit; }

    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }
}
