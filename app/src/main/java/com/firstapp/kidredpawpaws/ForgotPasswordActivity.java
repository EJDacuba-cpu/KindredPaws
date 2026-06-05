package com.firstapp.kidredpawpaws;

import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.util.Patterns;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.firstapp.kidredpawpaws.models.auth.ForgotPasswordRequest;
import com.firstapp.kidredpawpaws.network.ApiClient;
import com.firstapp.kidredpawpaws.network.ApiService;

import java.io.IOException;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ForgotPasswordActivity extends AppCompatActivity {

    private static final String TAG = "ForgotPasswordActivity";

    private EditText etEmail;
    private Button btnSendReset;
    private TextView tvForgotMessage;
    private TextView tvBackToLogin;
    private ApiService apiService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_forgot_password);

        apiService = ApiClient.getClient().create(ApiService.class);

        etEmail = findViewById(R.id.et_email);
        btnSendReset = findViewById(R.id.btn_send_reset);
        tvForgotMessage = findViewById(R.id.tv_forgot_message);
        tvBackToLogin = findViewById(R.id.tv_back_to_login);

        // Prefill email if passed via intent
        String emailExtra = getIntent().getStringExtra("email");
        if (emailExtra != null) {
            etEmail.setText(emailExtra);
        }

        btnSendReset.setOnClickListener(v -> handleForgotPassword());

        tvBackToLogin.setOnClickListener(v -> finish());
    }

    private void handleForgotPassword() {
        clearMessage();
        String email = etEmail.getText().toString().trim();

        if (TextUtils.isEmpty(email)) {
            showMessage("Please enter your email.", true);
            return;
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            showMessage("Please enter a valid email address.", true);
            return;
        }

        setLoading(true);
        ForgotPasswordRequest request = new ForgotPasswordRequest(email);

        Log.d(TAG, "Attempting password recovery for: " + email);

        apiService.forgotPassword(request).enqueue(new Callback<Void>() {
            @Override
            public void onResponse(@NonNull Call<Void> call, @NonNull Response<Void> response) {
                setLoading(false);
                if (response.isSuccessful()) {
                    Log.d(TAG, "Password reset link sent successfully.");
                    showMessage("Password reset link sent. Please check your email.", false);
                } else {
                    String errorBody = "";
                    try {
                        if (response.errorBody() != null) {
                            errorBody = response.errorBody().string();
                        }
                    } catch (IOException e) {
                        Log.e(TAG, "Error reading error body", e);
                    }

                    Log.e(TAG, "Forgot password failed. Code: " + response.code());
                    Log.e(TAG, "Full error body: " + errorBody);

                    String lowerError = errorBody.toLowerCase();
                    if (lowerError.contains("rate limit") || lowerError.contains("too many") || response.code() == 429) {
                        showMessage("Too many reset attempts. Please wait and try again later.", true);
                    } else if (lowerError.contains("invalid email")) {
                        showMessage("Please enter a valid email address.", true);
                    } else {
                        showMessage("Failed to send reset link. Code: " + response.code(), true);
                    }
                }
            }

            @Override
            public void onFailure(@NonNull Call<Void> call, @NonNull Throwable t) {
                setLoading(false);
                Log.e(TAG, "Network error during forgot password", t);
                showMessage("Network error. Please try again.", true);
            }
        });
    }

    private void showMessage(String message, boolean isError) {
        tvForgotMessage.setText(message);
        int colorRes = isError ? R.color.danger_text : R.color.primary_teal;
        tvForgotMessage.setTextColor(ContextCompat.getColor(this, colorRes));
        tvForgotMessage.setVisibility(View.VISIBLE);
    }

    private void clearMessage() {
        tvForgotMessage.setText("");
        tvForgotMessage.setVisibility(View.GONE);
    }

    private void setLoading(boolean isLoading) {
        if (isLoading) {
            btnSendReset.setEnabled(false);
            btnSendReset.setText("Sending...");
        } else {
            btnSendReset.setEnabled(true);
            btnSendReset.setText("Send Reset Link →");
        }
    }
}
