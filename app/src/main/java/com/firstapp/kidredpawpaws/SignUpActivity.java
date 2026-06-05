package com.firstapp.kidredpawpaws;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.firstapp.kidredpawpaws.models.auth.AuthResponse;
import com.firstapp.kidredpawpaws.models.auth.SignUpRequest;
import com.firstapp.kidredpawpaws.models.supabase.OwnerCreateRequest;
import com.firstapp.kidredpawpaws.models.supabase.OwnerDto;
import com.firstapp.kidredpawpaws.network.ApiClient;
import com.firstapp.kidredpawpaws.network.ApiService;

import java.io.IOException;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class SignUpActivity extends AppCompatActivity {

    private static final String TAG = "SignUpActivity";

    private EditText etName, etEmail, etPhone, etPassword, etConfirmPassword;
    private Button btnSignUp;
    private TextView tvSignUpMessage;
    private TextView tvLoginLink;

    private ApiService apiService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sign_up);

        apiService = ApiClient.getClient().create(ApiService.class);

        etName = findViewById(R.id.et_name);
        etEmail = findViewById(R.id.et_email);
        etPhone = findViewById(R.id.et_phone);
        etPassword = findViewById(R.id.et_password);
        etConfirmPassword = findViewById(R.id.et_confirm_password);
        btnSignUp = findViewById(R.id.btn_signup);
        tvSignUpMessage = findViewById(R.id.tv_signup_message);
        tvLoginLink = findViewById(R.id.tv_login_link);

        btnSignUp.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                handleSignUp();
            }
        });

        tvLoginLink.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(SignUpActivity.this, LoginActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                startActivity(intent);
                finish();
            }
        });
    }

    private void handleSignUp() {
        clearSignUpMessage();
        String name = etName.getText().toString().trim();
        String email = etEmail.getText().toString().trim();
        String phone = etPhone.getText().toString().trim();
        String password = etPassword.getText().toString().trim();
        String confirmPassword = etConfirmPassword.getText().toString().trim();

        if (TextUtils.isEmpty(name)
                || TextUtils.isEmpty(email)
                || TextUtils.isEmpty(phone)
                || TextUtils.isEmpty(password)
                || TextUtils.isEmpty(confirmPassword)) {

            showSignUpError("Please complete all fields.");
            return;
        }

        if (!password.equals(confirmPassword)) {
            showSignUpError("Passwords do not match.");
            return;
        }

        if (password.length() < 6) {
            showSignUpError("Password must be at least 6 characters.");
            return;
        }

        setSignUpLoading(true);

        SignUpRequest signUpRequest = new SignUpRequest(email, password);

        Log.d(TAG, "Signing up email: " + email);

        apiService.signUp(signUpRequest).enqueue(new Callback<AuthResponse>() {
            @Override
            public void onResponse(@NonNull Call<AuthResponse> call, @NonNull Response<AuthResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    AuthResponse authResponse = response.body();
                    Log.d(TAG, "Sign up successful. Code: " + response.code());
                    
                    if (authResponse.getAccessToken() != null && !authResponse.getAccessToken().isEmpty()) {
                        // Access token exists, insert into owners table
                        createOwnerProfile(name, email, phone, authResponse.getAccessToken());
                    } else {
                        // Access token missing (wait for email confirmation)
                        setSignUpLoading(false);
                        showSignUpSuccess("Account created. Please confirm your email, then login.");
                        navigateToLoginWithDelay();
                    }
                } else {
                    setSignUpLoading(false);
                    String errorMessage = getFriendlyErrorMessage(response);
                    showSignUpError(errorMessage);
                }
            }

            @Override
            public void onFailure(@NonNull Call<AuthResponse> call, @NonNull Throwable throwable) {
                setSignUpLoading(false);
                Log.e(TAG, "Network error during sign up", throwable);
                showSignUpError("Network error. Please try again.");
            }
        });
    }

    private void createOwnerProfile(String name, String email, String phone, String accessToken) {
        OwnerCreateRequest ownerRequest = new OwnerCreateRequest(name, email, phone);
        String authHeader = "Bearer " + accessToken;
        String preferHeader = "return=representation";

        apiService.createOwner(authHeader, preferHeader, ownerRequest).enqueue(new Callback<List<OwnerDto>>() {
            @Override
            public void onResponse(@NonNull Call<List<OwnerDto>> call, @NonNull Response<List<OwnerDto>> response) {
                setSignUpLoading(false);
                if (response.isSuccessful()) {
                    Log.d(TAG, "Owner profile created successfully.");
                    showSignUpSuccess("Account created successfully. Please login.");
                    navigateToLoginWithDelay();
                } else {
                    try {
                        if (response.errorBody() != null) {
                            Log.e(TAG, "Failed to create owner profile. Error: " + response.errorBody().string());
                        }
                    } catch (IOException e) {
                        Log.e(TAG, "Error reading error body", e);
                    }
                    Log.e(TAG, "Failed to create owner profile. Code: " + response.code());
                    showSignUpError("Account created, but customer profile setup failed.");
                }
            }

            @Override
            public void onFailure(@NonNull Call<List<OwnerDto>> call, @NonNull Throwable t) {
                setSignUpLoading(false);
                Log.e(TAG, "Network error creating owner profile", t);
                showSignUpError("Account created, but customer profile setup failed.");
            }
        });
    }

    private void navigateToLoginWithDelay() {
        btnSignUp.postDelayed(new Runnable() {
            @Override
            public void run() {
                Intent intent = new Intent(SignUpActivity.this, LoginActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                startActivity(intent);
                finish();
            }
        }, 1500);
    }

    private String getFriendlyErrorMessage(Response<AuthResponse> response) {
        String errorBody = "";
        try {
            if (response.errorBody() != null) {
                errorBody = response.errorBody().string();
            }
        } catch (IOException e) {
            Log.e(TAG, "Error reading error body", e);
        }

        Log.e(TAG, "Sign up failed. Code: " + response.code());
        Log.e(TAG, "Full error body: " + errorBody);

        String lowerError = errorBody.toLowerCase();

        if (lowerError.contains("user already registered") || lowerError.contains("already exists")) {
            return "Email is already registered. Please login instead.";
        }
        if (lowerError.contains("password should be at least")) {
            return "Password must be at least 6 characters.";
        }
        if (lowerError.contains("signup requires a valid password")) {
            return "Please enter a valid password.";
        }
        if (lowerError.contains("invalid email")) {
            return "Please enter a valid email address.";
        }
        if (lowerError.contains("rate limit") || lowerError.contains("too many") || response.code() == 429) {
            return "Too many sign up attempts. Please wait and try again later.";
        }

        return "Sign up failed. Please try again. Code: " + response.code();
    }

    private void showSignUpError(String message) {
        tvSignUpMessage.setText(message);
        tvSignUpMessage.setTextColor(ContextCompat.getColor(this, R.color.danger_text));
        tvSignUpMessage.setVisibility(View.VISIBLE);
    }

    private void showSignUpSuccess(String message) {
        tvSignUpMessage.setText(message);
        tvSignUpMessage.setTextColor(ContextCompat.getColor(this, R.color.primary_teal));
        tvSignUpMessage.setVisibility(View.VISIBLE);
    }

    private void clearSignUpMessage() {
        tvSignUpMessage.setText("");
        tvSignUpMessage.setVisibility(View.GONE);
    }

    private void setSignUpLoading(boolean isLoading) {
        if (isLoading) {
            btnSignUp.setEnabled(false);
            btnSignUp.setText("Creating account...");
        } else {
            btnSignUp.setEnabled(true);
            btnSignUp.setText("Sign Up →");
        }
    }
}
