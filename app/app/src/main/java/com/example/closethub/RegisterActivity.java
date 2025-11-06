package com.example.closethub;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.closethub.models.ApiResponse;
import com.example.closethub.models.User;
import com.example.closethub.networks.ApiService;
import com.example.closethub.networks.RetrofitClient;
import com.google.android.material.textfield.TextInputEditText;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class RegisterActivity extends AppCompatActivity {
    private ImageView imgBack, imgGoogle, imgFacebook, imgApple;
    private TextInputEditText edtEmail, edtPass;
    private Button btnCreate;
    private TextView txtLogin;
    private void initUI() {
        imgBack = findViewById(R.id.imgBack);
//        imgGoogle = findViewById(R.id.imgGoogle);
//        imgFacebook = findViewById(R.id.imgFacebook);
//        imgApple = findViewById(R.id.imgApple);
        edtEmail = findViewById(R.id.edtEmail);
        edtPass = findViewById(R.id.edtPass);
        btnCreate = findViewById(R.id.btnCreate);
        txtLogin = findViewById(R.id.txtLogin);
    }
    private ApiService apiService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_register);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
        initUI();

        imgBack.setOnClickListener(v -> {BackLogin();});
        txtLogin.setOnClickListener(v -> {BackLogin();});

        apiService = RetrofitClient.getApiService();

        btnCreate.setOnClickListener(v -> {
            String email = edtEmail.getText().toString().trim();
            String pass = edtPass.getText().toString().trim();

            if (!email.endsWith("@gmail.com")) {
                Toast.makeText(this, "Email Error", Toast.LENGTH_SHORT).show();
                return;
            }

            if (email.length() == 0 || pass.length() == 0) {
                Toast.makeText(this, "Nhap email va pass", Toast.LENGTH_SHORT).show();
                return;
            }

            User user = new User();
            user.setEmail(email);
            user.setPass(pass);

            RegisterAccount(user);
        });


    }

    private void RegisterAccount(User user) {
        apiService.getRegister(user).enqueue(new Callback<ApiResponse<User>>() {
            @Override
            public void onResponse(Call<ApiResponse<User>> call, Response<ApiResponse<User>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    Toast.makeText(RegisterActivity.this, "Register successfully", Toast.LENGTH_SHORT).show();
                    startActivity(new Intent(RegisterActivity.this, LoginActivity.class));
                    finish();
                } else {
                    Toast.makeText(RegisterActivity.this, "Register failed", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<User>> call, Throwable throwable) {
                Toast.makeText(RegisterActivity.this, "Register failed", Toast.LENGTH_SHORT).show();
                Log.e("Error", "Register Failed", throwable);
            }
        });
    }

    private void BackLogin() {
        startActivity(new Intent(RegisterActivity.this, LoginActivity.class));
    }
}