package com.firstapp.kidredpawpaws;

import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.firstapp.kidredpawpaws.models.auth.AuthResponse;
import com.firstapp.kidredpawpaws.models.auth.LoginRequest;
import com.firstapp.kidredpawpaws.models.supabase.OwnerDto;
import com.firstapp.kidredpawpaws.network.ApiClient;
import com.firstapp.kidredpawpaws.network.ApiService;
import com.firstapp.kidredpawpaws.repositories.ClientRepository;
import com.firstapp.kidredpawpaws.utils.SessionManager;

import java.io.IOException;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class LoginActivity extends AppCompatActivity {

    private static final String TAG = "LoginActivity";
    private EditText etEmail, etPassword;
    private Button btnLogin;
    private TextView tvLoginMessage, tvForgotPassword;
    private ImageView ivPasswordToggle;
    private boolean isPasswordVisible = false;
    private ApiService apiService;
    private ClientRepository clientRepository;
    private SessionManager sessionManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        apiService = ApiClient.getClient().create(ApiService.class);
        clientRepository = new ClientRepository();
        sessionManager = new SessionManager(this);

        etEmail = findViewById(R.id.et_email);
        etPassword = findViewById(R.id.et_password);
        btnLogin = findViewById(R.id.btn_login);
        tvLoginMessage = findViewById(R.id.tv_login_message);
        tvForgotPassword = findViewById(R.id.tv_forgot_password);
        ivPasswordToggle = findViewById(R.id.iv_password_toggle);

        ivPasswordToggle.setOnClickListener(v -> {
            if (isPasswordVisible) {
                etPassword.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
                ivPasswordToggle.setImageResource(android.R.drawable.ic_menu_view);
            } else {
                etPassword.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
                ivPasswordToggle.setImageResource(android.R.drawable.ic_menu_close_clear_cancel);
            }
            isPasswordVisible = !isPasswordVisible;
            etPassword.setSelection(etPassword.getText().length());
        });

        btnLogin.setOnClickListener(v -> handleLogin());

        tvForgotPassword.setOnClickListener(v -> {
            Intent intent = new Intent(LoginActivity.this, ForgotPasswordActivity.class);
            intent.putExtra("email", etEmail.getText().toString().trim());
            startActivity(intent);
        });

        findViewById(R.id.tv_signup_link).setOnClickListener(v -> {
            Intent intent = new Intent(LoginActivity.this, SignUpActivity.class);
            startActivity(intent);
        });
    }

    private void handleLogin() {
        clearLoginMessage();
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        if (TextUtils.isEmpty(email) || TextUtils.isEmpty(password)) {
            showLoginError("Please enter email and password.");
            return;
        }

        setLoginLoading(true);
        LoginRequest loginRequest = new LoginRequest(email, password);
        
        Log.d(TAG, "Attempting login for: " + email);
        
        apiService.login(loginRequest).enqueue(new Callback<AuthResponse>() {
            @Override
            public void onResponse(@NonNull Call<AuthResponse> call, @NonNull Response<AuthResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    AuthResponse authResponse = response.body();
                    
                    if (authResponse.getAccessToken() != null && !authResponse.getAccessToken().isEmpty() && authResponse.getUser() != null) {
                        Log.d(TAG, "Auth success. Email: " + authResponse.getUser().getEmail());
                        
                        sessionManager.saveSession(
                                authResponse.getAccessToken(),
                                authResponse.getUser().getId(),
                                authResponse.getUser().getEmail()
                        );

                        Log.d(TAG, "Access Token: " + authResponse.getAccessToken());
                        fetchOwnerProfile(authResponse.getAccessToken(), authResponse.getUser().getEmail());
                    } else {
                        setLoginLoading(false);
                        Log.e(TAG, "Auth success but data missing");
                        showLoginError("Login failed. Invalid response from server.");
                    }
                } else {
                    setLoginLoading(false);
                    String errorMessage = getFriendlyErrorMessage(response);
                    showLoginError(errorMessage);
                }
            }

            @Override
            public void onFailure(@NonNull Call<AuthResponse> call, @NonNull Throwable t) {
                setLoginLoading(false);
                Log.e(TAG, "Network error during auth", t);
                showLoginError("Network error. Please try again.");
            }
        });
    }

    private void fetchOwnerProfile(String accessToken, String email) {
        Log.d(TAG, "Fetching owner profile for email: " + email);
        clientRepository.getOwnerByEmail(accessToken, email, new Callback<List<OwnerDto>>() {
            @Override
            public void onResponse(@NonNull Call<List<OwnerDto>> call, @NonNull Response<List<OwnerDto>> response) {
                setLoginLoading(false);
                Log.d(TAG, "Owner lookup response code: " + response.code());
                
                if (response.isSuccessful() && response.body() != null && !response.body().isEmpty()) {
                    OwnerDto owner = response.body().get(0);
                    Log.d(TAG, "Owner found. Count: " + response.body().size());
                    Log.d(TAG, "Saving ownerId: " + owner.getId());
                    
                    sessionManager.saveOwnerId(owner.getId());
                    sessionManager.saveOwnerName(owner.getFullName());

                    showLoginSuccess("Login successful.");
                    navigateToMain();
                } else {
                    Log.w(TAG, "Owner not found in database for email: " + email);
                    if (response.errorBody() != null) {
                        try {
                            Log.e(TAG, "Owner lookup error body: " + response.errorBody().string());
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                    showLoginError("No customer profile found for this email.");
                }
            }

            @Override
            public void onFailure(@NonNull Call<List<OwnerDto>> call, @NonNull Throwable t) {
                setLoginLoading(false);
                Log.e(TAG, "Owner lookup API failure", t);
                showLoginError("Failed to fetch customer profile. Please try again.");
            }
        });
    }

    private void navigateToMain() {
        btnLogin.postDelayed(() -> {
            Intent intent = new Intent(LoginActivity.this, MainActivity.class);
            startActivity(intent);
            finish();
        }, 500);
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

        Log.e(TAG, "Auth failed. Code: " + response.code());
        Log.e(TAG, "Full error body: " + errorBody);

        String lowerError = errorBody.toLowerCase();

        if (lowerError.contains("email not confirmed")) {
            return "Please confirm your email before logging in.";
        }
        if (lowerError.contains("invalid login credentials")) {
            return "Invalid email or password.";
        }
        if (lowerError.contains("user not found")) {
            return "Account not found. Please sign up first.";
        }

        return "Login failed. Code: " + response.code();
    }

    private void showLoginError(String message) {
        tvLoginMessage.setText(message);
        tvLoginMessage.setTextColor(ContextCompat.getColor(this, R.color.danger_text));
        tvLoginMessage.setVisibility(View.VISIBLE);
    }

    private void showLoginSuccess(String message) {
        tvLoginMessage.setText(message);
        tvLoginMessage.setTextColor(ContextCompat.getColor(this, R.color.primary_teal));
        tvLoginMessage.setVisibility(View.VISIBLE);
    }

    private void clearLoginMessage() {
        tvLoginMessage.setText("");
        tvLoginMessage.setVisibility(View.GONE);
    }

    private void setLoginLoading(boolean isLoading) {
        if (isLoading) {
            btnLogin.setEnabled(false);
            btnLogin.setText("Logging in...");
        } else {
            btnLogin.setEnabled(true);
            btnLogin.setText("Log In →");
        }
    }
}
