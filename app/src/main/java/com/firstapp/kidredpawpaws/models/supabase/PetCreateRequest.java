package com.firstapp.kidredpawpaws.models.supabase;

import com.google.gson.annotations.SerializedName;

public class PetCreateRequest {
    @SerializedName("owner_id")
    private String ownerId;
    private String name;
    private String species;
    private String breed;
    @SerializedName("age_years")
    private Integer ageYears;
    private String status;

    public PetCreateRequest(String ownerId, String name, String species, String breed, Integer ageYears, String status) {
        this.ownerId = ownerId;
        this.name = name;
        this.species = species;
        this.breed = breed;
        this.ageYears = ageYears;
        this.status = status;
    }

    // Getters and Setters
    public String getOwnerId() { return ownerId; }
    public void setOwnerId(String ownerId) { this.ownerId = ownerId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getSpecies() { return species; }
    public void setSpecies(String species) { this.species = species; }

    public String getBreed() { return breed; }
    public void setBreed(String breed) { this.breed = breed; }

    public Integer getAgeYears() { return ageYears; }
    public void setAgeYears(Integer ageYears) { this.ageYears = ageYears; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
