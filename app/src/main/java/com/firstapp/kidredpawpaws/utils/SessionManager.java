package com.firstapp.kidredpawpaws.utils;

import android.content.Context;
import android.content.SharedPreferences;

public class SessionManager {
    private static final String PREF_NAME = "KindredPawsSession";
    private static final String KEY_ACCESS_TOKEN = "access_token";
    private static final String KEY_USER_ID = "user_id";
    private static final String KEY_EMAIL = "email";
    private static final String KEY_OWNER_ID = "owner_id";
    private static final String KEY_FULL_NAME = "full_name";

    private final SharedPreferences sharedPreferences;
    private final SharedPreferences.Editor editor;

    public SessionManager(Context context) {
        sharedPreferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        editor = sharedPreferences.edit();
    }

    public void saveSession(String accessToken, String userId, String email) {
        editor.putString(KEY_ACCESS_TOKEN, accessToken);
        editor.putString(KEY_USER_ID, userId);
        editor.putString(KEY_EMAIL, email);
        editor.apply();
    }

    public String getAccessToken() {
        return sharedPreferences.getString(KEY_ACCESS_TOKEN, null);
    }

    public String getUserId() {
        return sharedPreferences.getString(KEY_USER_ID, null);
    }

    public String getEmail() {
        return sharedPreferences.getString(KEY_EMAIL, null);
    }

    public void saveOwnerId(String ownerId) {
        editor.putString(KEY_OWNER_ID, ownerId);
        editor.apply();
    }

    public String getOwnerId() {
        return sharedPreferences.getString(KEY_OWNER_ID, null);
    }

    public void saveOwnerName(String ownerName) {
        editor.putString(KEY_FULL_NAME, ownerName);
        editor.apply();
    }

    public String getOwnerName() {
        return sharedPreferences.getString(KEY_FULL_NAME, null);
    }

    public void saveFullName(String fullName) {
        editor.putString(KEY_FULL_NAME, fullName);
        editor.apply();
    }

    public String getFullName() {
        return sharedPreferences.getString(KEY_FULL_NAME, null);
    }

    public boolean isLoggedIn() {
        return getAccessToken() != null;
    }

    public void clearSession() {
        editor.clear();
        editor.apply();
    }
}
