package com.firstapp.kidredpawpaws.models.supabase;

import com.google.gson.annotations.SerializedName;

public class OwnerUpdateRequest {
    @SerializedName("full_name")
    private String fullName;
    private String phone;
    private String location;

    public OwnerUpdateRequest(String fullName, String phone, String location) {
        this.fullName = fullName;
        this.phone = phone;
        this.location = location;
    }

    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }
}
