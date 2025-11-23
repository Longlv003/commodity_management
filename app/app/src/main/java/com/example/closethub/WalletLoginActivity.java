package com.example.closethub;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.closethub.models.ApiResponse;
import com.example.closethub.models.User;
import com.example.closethub.models.WalletLoginRequest;
import com.example.closethub.models.WalletRequest;
import com.example.closethub.models.WalletResponse;
import com.example.closethub.networks.ApiService;
import com.example.closethub.networks.RetrofitClient;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.gson.Gson;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class WalletLoginActivity extends AppCompatActivity {
    private ImageView imgBack;
    private TextInputLayout textInputLayoutPin;
    private TextInputEditText edtPinHash;
    private Button btnLogin;
    private TextView txtForgotPin;
    private TextView txtCreateWallet;
    private TextView txtPinHint;
    private ApiService apiService;
    private User currentUser;
    private SharedPreferences sharedPreferences;

    private void initUI() {
        imgBack = findViewById(R.id.imgBack);
        textInputLayoutPin = findViewById(R.id.textInputLayoutPin);
        edtPinHash = findViewById(R.id.edtPinHash);
        btnLogin = findViewById(R.id.btnLogin);
        txtForgotPin = findViewById(R.id.txtForgotPin);
        txtCreateWallet = findViewById(R.id.txtCreateWallet);
        txtPinHint = findViewById(R.id.txtPinHint);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_wallet_login);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
        initUI();

        apiService = RetrofitClient.getApiService();
        sharedPreferences = getSharedPreferences("LoginPrefs", MODE_PRIVATE);

        // Lấy thông tin user từ SharedPreferences
        String userJson = sharedPreferences.getString("user_data", null);
        if (userJson != null) {
            Gson gson = new Gson();
            currentUser = gson.fromJson(userJson, User.class);
        }

        imgBack.setOnClickListener(v -> {
            finish();
        });

        txtForgotPin.setOnClickListener(v -> {
            Toast.makeText(this, "Tính năng đang phát triển", Toast.LENGTH_SHORT).show();
        });

        txtCreateWallet.setOnClickListener(v -> {
            // Mở dialog tạo ví
            showCreateWalletDialog();
        });

        // Kiểm tra has_wallet và cập nhật UI (sau khi set các listener khác)
        checkWalletStatus();
    }

    private void checkWalletStatus() {
        if (currentUser == null) {
            // Chưa đăng nhập - ẩn input và disable button
            textInputLayoutPin.setVisibility(android.view.View.GONE);
            txtPinHint.setVisibility(android.view.View.GONE);
            txtForgotPin.setVisibility(android.view.View.GONE);
            
            btnLogin.setText("Vui lòng đăng nhập");
            btnLogin.setEnabled(false);
            btnLogin.setAlpha(0.5f);
            return;
        }
        
        if (!currentUser.isHas_wallet()) {
            // Chưa có ví - ẩn input PIN và hint
            textInputLayoutPin.setVisibility(android.view.View.GONE);
            txtPinHint.setVisibility(android.view.View.GONE);
            txtForgotPin.setVisibility(android.view.View.GONE);
            
            btnLogin.setText("Tạo ví mới");
            btnLogin.setEnabled(true);
            btnLogin.setAlpha(1.0f);
            btnLogin.setOnClickListener(v -> {
                // Mở dialog tạo ví khi click vào button
                showCreateWalletDialog();
            });
        } else {
            // Đã có ví - hiển thị input PIN và hint
            textInputLayoutPin.setVisibility(android.view.View.VISIBLE);
            txtPinHint.setVisibility(android.view.View.VISIBLE);
            txtForgotPin.setVisibility(android.view.View.VISIBLE);
            
            btnLogin.setText("Đăng nhập");
            btnLogin.setEnabled(true);
            btnLogin.setAlpha(1.0f);
            edtPinHash.setEnabled(true);
            textInputLayoutPin.setHint("Mã PIN");
            
            // Set lại listener cho button login
            btnLogin.setOnClickListener(v -> {
                String pin = edtPinHash.getText().toString().trim();
                if (pin.isEmpty() || pin.length() != 6 || !pin.matches("\\d+")) {
                    Toast.makeText(this, "Mã PIN phải là 6 chữ số", Toast.LENGTH_SHORT).show();
                    return;
                }
                checkWalletAndLogin();
            });
        }
    }

    private void checkWalletAndLogin() {
        if (currentUser == null) {
            Toast.makeText(this, "Vui lòng đăng nhập để sử dụng ví", Toast.LENGTH_SHORT).show();
            return;
        }

        // Kiểm tra has_wallet
        if (!currentUser.isHas_wallet()) {
            // Chưa có ví - Toast thông báo và disable button
            Toast.makeText(this, "Bạn chưa có ví. Vui lòng tạo ví mới.", Toast.LENGTH_SHORT).show();
            btnLogin.setText("Chưa có ví");
            btnLogin.setEnabled(false);
            btnLogin.setAlpha(0.5f); // Làm mờ button
            edtPinHash.setEnabled(false); // Disable input
            return;
        } else {
            // Đã có ví - kiểm tra PIN và đăng nhập
            performWalletLogin();
        }
    }

    private void showCreateWalletDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_create_wallet, null);
        builder.setView(dialogView);

        TextInputEditText edtCreatePin = dialogView.findViewById(R.id.edtCreatePin);
        Button btnCancel = dialogView.findViewById(R.id.btnCancel);
        Button btnCreate = dialogView.findViewById(R.id.btnCreate);

        AlertDialog dialog = builder.create();

        btnCancel.setOnClickListener(v -> {
            dialog.dismiss();
        });

        btnCreate.setOnClickListener(v -> {
            String pin = edtCreatePin.getText().toString().trim();
            if (pin.isEmpty() || pin.length() != 6 || !pin.matches("\\d+")) {
                Toast.makeText(this, "Mã PIN phải là 6 chữ số", Toast.LENGTH_SHORT).show();
                return;
            }
            createWallet(pin, dialog);
        });

        dialog.show();
    }

    private void createWallet(String pin, AlertDialog dialog) {
        String token = sharedPreferences.getString("token", "");
        if (token.isEmpty()) {
            Toast.makeText(this, "Vui lòng đăng nhập lại", Toast.LENGTH_SHORT).show();
            return;
        }

        WalletRequest walletRequest = new WalletRequest(pin);
        apiService.createWallet("Bearer " + token, walletRequest).enqueue(new Callback<ApiResponse<WalletResponse>>() {
            @Override
            public void onResponse(Call<ApiResponse<WalletResponse>> call, Response<ApiResponse<WalletResponse>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    WalletResponse walletResponse = response.body().getData();
                    if (walletResponse != null) {
                        // Cập nhật has_wallet trong user
                        currentUser.setHas_wallet(true);
                        
                        // Lưu lại user đã cập nhật
                        Gson gson = new Gson();
                        String updatedUserJson = gson.toJson(currentUser);
                        sharedPreferences.edit().putString("user_data", updatedUserJson).apply();

                        dialog.dismiss();
                        Toast.makeText(WalletLoginActivity.this, "Tạo ví thành công!", Toast.LENGTH_SHORT).show();
                        
                        // Cập nhật UI sau khi tạo ví thành công
                        checkWalletStatus();
                        
                        // Sau khi tạo ví thành công, có thể cho phép đăng nhập luôn
                        edtPinHash.setText(pin);
                        performWalletLogin();
                    }
                } else {
                    String errorMsg = "Tạo ví thất bại";
                    if (response.body() != null && response.body().getMsg() != null) {
                        errorMsg = response.body().getMsg();
                    }
                    Toast.makeText(WalletLoginActivity.this, errorMsg, Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<WalletResponse>> call, Throwable t) {
                Toast.makeText(WalletLoginActivity.this, "Lỗi: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void performWalletLogin() {
        String pin = edtPinHash.getText().toString().trim();
        
        if (pin.isEmpty() || pin.length() != 6 || !pin.matches("\\d+")) {
            Toast.makeText(this, "Mã PIN phải là 6 chữ số", Toast.LENGTH_SHORT).show();
            return;
        }

        // Lấy token user để xác thực
        String token = sharedPreferences.getString("token", "");
        if (token.isEmpty()) {
            Toast.makeText(this, "Vui lòng đăng nhập lại", Toast.LENGTH_SHORT).show();
            return;
        }

        // Login wallet với token user và PIN
        loginWithWallet(pin, token);
    }

    private void loginWithWallet(String pin, String userToken) {
        WalletLoginRequest loginRequest = new WalletLoginRequest(pin);
        apiService.loginWallet("Bearer " + userToken, loginRequest).enqueue(new Callback<ApiResponse<Object>>() {
            @Override
            public void onResponse(Call<ApiResponse<Object>> call, Response<ApiResponse<Object>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    Toast.makeText(WalletLoginActivity.this, "Đăng nhập ví thành công!", Toast.LENGTH_SHORT).show();
                    // Chuyển sang màn hình ví điện tử
                    Intent intent = new Intent(WalletLoginActivity.this, WalletActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(intent);
                    finish();
                } else {
                    String errorMsg = "Đăng nhập ví thất bại";
                    if (response.body() != null && response.body().getMsg() != null) {
                        errorMsg = response.body().getMsg();
                    }
                    Toast.makeText(WalletLoginActivity.this, errorMsg, Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<Object>> call, Throwable t) {
                Toast.makeText(WalletLoginActivity.this, "Lỗi: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }
}