package com.example.closethub;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.CheckBox;
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
import com.google.gson.Gson;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class LoginActivity extends AppCompatActivity {
    private TextView txtRegister, txtForgot;
    private TextInputEditText edtEmail, edtPass;
    private CheckBox chkRemember;
    private Button btnLogin;
    private ImageView imgGoogle, imgFacebook, imgApple;
    private void initUI() {
        txtRegister = findViewById(R.id.txtRegister);
        txtForgot = findViewById(R.id.txtForgot);
//        imgGoogle = findViewById(R.id.imgGoogle);
//        imgFacebook = findViewById(R.id.imgFacebook);
//        imgApple = findViewById(R.id.imgApple);
        edtEmail = findViewById(R.id.edtEmail);
        edtPass = findViewById(R.id.edtPass);
        btnLogin = findViewById(R.id.btnLogin);
        chkRemember = findViewById(R.id.chkRemember);
    }
    private ApiService apiService;
    SharedPreferences sharedPreferences;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_login);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
        initUI();

        apiService = RetrofitClient.getApiService();

        sharedPreferences = getSharedPreferences("LoginPrefs", MODE_PRIVATE);
        edtEmail.setText(sharedPreferences.getString("email", ""));
        edtPass.setText(sharedPreferences.getString("password", ""));
        chkRemember.setChecked(sharedPreferences.getBoolean("remember", false));

        txtRegister.setOnClickListener(v -> {
            startActivity(new Intent(LoginActivity.this, RegisterActivity.class));
        });

        btnLogin.setOnClickListener(v -> {
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

            LoginAccount(user);
        });
    }

    private void LoginAccount(User user) {
        apiService.getLogin(user).enqueue(new Callback<ApiResponse<User>>() {
            @Override
            public void onResponse(Call<ApiResponse<User>> call, Response<ApiResponse<User>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    User loggedInUser = response.body().getData();

                    if (loggedInUser == null) {
                        Toast.makeText(LoginActivity.this, "Login thất bại: user null", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    SharedPreferences.Editor editor = sharedPreferences.edit();
                    Gson gson = new Gson();

                    if (chkRemember.isChecked()) {
                        editor.putString("email", user.getEmail());
                        editor.putString("password", user.getPass());
                        editor.putBoolean("remember", true);
                    } else {
                        sharedPreferences.edit().clear().apply();
                    }

                    // 🔹 Lưu toàn bộ object User (chuyển object -> JSON)
                    String userJson = gson.toJson(loggedInUser);
                    editor.putString("user_data", userJson);
                    editor.apply();

                    Toast.makeText(LoginActivity.this, "Login successfully", Toast.LENGTH_SHORT).show();
                    startActivity(new Intent(LoginActivity.this, MainActivity.class));
                    finish();
                } else {
                    Toast.makeText(LoginActivity.this, "Login failed", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<User>> call, Throwable throwable) {
                Toast.makeText(LoginActivity.this, "Login failed", Toast.LENGTH_SHORT).show();
                Log.e("Error", "Login Failed", throwable);
            }
        });
    }
}