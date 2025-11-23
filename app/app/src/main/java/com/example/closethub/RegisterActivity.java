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
    private TextInputEditText edtName, edtEmail, edtPhone, edtAddress, edtPass, edtConfirmPass;
    private Button btnCreate;
    private TextView txtLogin;
    private void initUI() {
        imgBack = findViewById(R.id.imgBack);
//        imgGoogle = findViewById(R.id.imgGoogle);
//        imgFacebook = findViewById(R.id.imgFacebook);
//        imgApple = findViewById(R.id.imgApple);
        edtName = findViewById(R.id.edtName);
        edtEmail = findViewById(R.id.edtEmail);
        edtPhone = findViewById(R.id.edtPhone);
        edtAddress = findViewById(R.id.edtAddress);
        edtPass = findViewById(R.id.edtPass);
        edtConfirmPass = findViewById(R.id.edtConfirmPass);
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
            String name = edtName.getText().toString().trim();
            String email = edtEmail.getText().toString().trim();
            String phone = edtPhone.getText().toString().trim();
            String address = edtAddress.getText().toString().trim();
            String pass = edtPass.getText().toString().trim();
            String confirmPass = edtConfirmPass.getText().toString().trim();

            // Validate các trường bắt buộc
            if (name.isEmpty()) {
                Toast.makeText(this, "Vui lòng nhập họ và tên", Toast.LENGTH_SHORT).show();
                edtName.requestFocus();
                return;
            }

            if (email.isEmpty()) {
                Toast.makeText(this, "Vui lòng nhập email", Toast.LENGTH_SHORT).show();
                edtEmail.requestFocus();
                return;
            }

            if (!email.endsWith("@gmail.com")) {
                Toast.makeText(this, "Email phải là @gmail.com", Toast.LENGTH_SHORT).show();
                edtEmail.requestFocus();
                return;
            }

            if (phone.isEmpty()) {
                Toast.makeText(this, "Vui lòng nhập số điện thoại", Toast.LENGTH_SHORT).show();
                edtPhone.requestFocus();
                return;
            }

            if (address.isEmpty()) {
                Toast.makeText(this, "Vui lòng nhập địa chỉ", Toast.LENGTH_SHORT).show();
                edtAddress.requestFocus();
                return;
            }

            if (pass.isEmpty()) {
                Toast.makeText(this, "Vui lòng nhập mật khẩu", Toast.LENGTH_SHORT).show();
                edtPass.requestFocus();
                return;
            }

            if (pass.length() < 6) {
                Toast.makeText(this, "Mật khẩu phải có ít nhất 6 ký tự", Toast.LENGTH_SHORT).show();
                edtPass.requestFocus();
                return;
            }

            if (confirmPass.isEmpty()) {
                Toast.makeText(this, "Vui lòng nhập lại mật khẩu", Toast.LENGTH_SHORT).show();
                edtConfirmPass.requestFocus();
                return;
            }

            // Kiểm tra mật khẩu trùng khớp
            if (!pass.equals(confirmPass)) {
                Toast.makeText(this, "Mật khẩu không trùng khớp", Toast.LENGTH_SHORT).show();
                edtConfirmPass.requestFocus();
                return;
            }

            User user = new User();
            user.setName(name);
            user.setEmail(email);
            user.setPhone(phone);
            user.setAddress(address);
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