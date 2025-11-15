package com.example.closethub;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.bumptech.glide.Glide;
import com.example.closethub.models.ApiResponse;
import com.example.closethub.models.User;
import com.example.closethub.networks.ApiService;
import com.example.closethub.networks.RetrofitClient;
import com.google.android.material.textfield.TextInputEditText;
import com.google.gson.Gson;

import java.io.InputStream;
import java.util.Calendar;
import java.util.Locale;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class AccountProfileActivity extends AppCompatActivity {
    private ImageView imgAvatar, imgBack;
    private ImageButton btnEditAvatar;
    private TextInputEditText edtEmail, edtPhoneNumber;
    private Button btnComplete, btnEdit;
    private void initUI() {
        imgBack = findViewById(R.id.imgBack);
        imgAvatar = findViewById(R.id.imgAvatar);
        btnEditAvatar = findViewById(R.id.btnEditAvatar);
        edtEmail = findViewById(R.id.edtEmail);
        edtPhoneNumber = findViewById(R.id.edtPhoneNumber);
        btnComplete = findViewById(R.id.btnComplete);
        btnEdit = findViewById(R.id.btnEdit);
    }
    private ApiService apiService = RetrofitClient.getApiService();
    private Uri selectedImageUri = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_account_profile);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
        initUI();
        UnEnable(false);
        btnComplete.setVisibility(View.GONE);

        SharedPreferences prefs = getSharedPreferences("LoginPrefs", MODE_PRIVATE);
        String userJson = prefs.getString("user_data", null);

        if (userJson == null) {
            Toast.makeText(this, "account found", Toast.LENGTH_SHORT).show();
            return;
        }

        Gson gson = new Gson();
        User user = gson.fromJson(userJson, User.class);

        String imageUrl = "http://10.0.2.2:3000/images/avatars/" + user.getImage();
        Glide.with(AccountProfileActivity.this)
                .load(imageUrl)
                .placeholder(R.drawable.ic_placeholder) // ảnh tạm khi đang load
                .error(R.drawable.ic_error)             // ảnh lỗi nếu load thất bại
                .into(imgAvatar);
        edtEmail.setText(user.getEmail());
        edtPhoneNumber.setText(user.getPhone());

        imgBack.setOnClickListener(v -> {
            finish();
        });

        btnEdit.setOnClickListener(v -> {
            UnEnable(true);
            btnComplete.setVisibility(View.VISIBLE);
            btnEdit.setVisibility(View.GONE);
        });

        ActivityResultLauncher<Intent> imagePickerLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        selectedImageUri = result.getData().getData();
                        imgAvatar.setImageURI(selectedImageUri);

                        if (userJson != null) {
                            uploadAvatar(user.getToken(), user.get_id(), selectedImageUri);
                        }
                    }
                }
        );

        btnEditAvatar.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_PICK);
            intent.setType("image/*");
            imagePickerLauncher.launch(intent);
        });


//        ActivityResultLauncher<Intent> imagePickerLauncher = registerForActivityResult(
//                new ActivityResultContracts.StartActivityForResult(),
//                result -> {
//                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
//                        selectedImageUri = result.getData().getData();
//                        imgAvatar.setImageURI(selectedImageUri);
//                    }
//                }
//        );
//
//        btnEditAvatar.setOnClickListener(v -> {
//            Intent intent = new Intent(Intent.ACTION_PICK);
//            intent.setType("image/*");
//            imagePickerLauncher.launch(intent);
//        });

        btnComplete.setOnClickListener(v -> {
            String email = edtEmail.getText().toString().trim();
            String phoneNumber = edtPhoneNumber.getText().toString().trim();

            UnEnable(false);
            btnEdit.setVisibility(View.VISIBLE);
            btnComplete.setVisibility(View.GONE);

            updateUserInfo(user.getToken(), user.get_id(), email, phoneNumber, selectedImageUri);
        });
    }

    private void uploadAvatar(String token, String userId, Uri imageUri) {
        try {
            if (imageUri == null) {
                Toast.makeText(this, "Chưa chọn ảnh!", Toast.LENGTH_SHORT).show();
                return;
            }

            InputStream inputStream = getContentResolver().openInputStream(imageUri);
            byte[] bytes = new byte[inputStream.available()];
            inputStream.read(bytes);
            inputStream.close();

            RequestBody requestFile = RequestBody.create(MediaType.parse("image/*"), bytes);
            MultipartBody.Part imagePart = MultipartBody.Part.createFormData("image", "avatar.jpg", requestFile);

            apiService.uploadAvatar("Bearer " + token, userId, imagePart)
                    .enqueue(new Callback<ApiResponse<User>>() {
                        @Override
                        public void onResponse(Call<ApiResponse<User>> call, Response<ApiResponse<User>> response) {
                            if (response.isSuccessful() && response.body() != null) {
                                Toast.makeText(AccountProfileActivity.this, "Cập nhật avatar thành công!", Toast.LENGTH_SHORT).show();
                                User updatedUser = response.body().getData();

                                // Lưu user mới
                                SharedPreferences.Editor editor = getSharedPreferences("LoginPrefs", MODE_PRIVATE).edit();
                                editor.putString("user_data", new Gson().toJson(updatedUser));
                                editor.apply();

                                Glide.with(AccountProfileActivity.this)
                                        .load("http://10.0.2.2:3000/images/avatars/" + updatedUser.getImage())
                                        .into(imgAvatar);
                            } else {
                                Toast.makeText(AccountProfileActivity.this, "Upload thất bại!", Toast.LENGTH_SHORT).show();
                            }
                        }

                        @Override
                        public void onFailure(Call<ApiResponse<User>> call, Throwable t) {
                            Toast.makeText(AccountProfileActivity.this, "Lỗi kết nối server!", Toast.LENGTH_SHORT).show();
                            Log.e("UploadAvatar", "Error:", t);
                        }
                    });

        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Không thể xử lý ảnh!", Toast.LENGTH_SHORT).show();
        }
    }


    private void updateUserInfo(String token, String userId, String email, String phone, Uri imageUri) {
        try {
            MultipartBody.Part imagePart = null;

            if (imageUri != null) {
                InputStream inputStream = getContentResolver().openInputStream(imageUri);
                byte[] bytes = new byte[inputStream.available()];
                inputStream.read(bytes);
                inputStream.close();

                RequestBody requestFile = RequestBody.create(MediaType.parse("image/*"), bytes);
                imagePart = MultipartBody.Part.createFormData("image", "avatar.jpg", requestFile);
            }

            RequestBody emailBody = RequestBody.create(MediaType.parse("text/plain"), email);
            RequestBody phoneBody = RequestBody.create(MediaType.parse("text/plain"), phone);

            apiService.UpdateProfileUserMultipart(token, userId, imagePart, emailBody, phoneBody)
                    .enqueue(new Callback<ApiResponse<User>>() {
                        @Override
                        public void onResponse(Call<ApiResponse<User>> call, Response<ApiResponse<User>> response) {
                            if (response.isSuccessful() && response.body() != null) {
                                Toast.makeText(AccountProfileActivity.this, "Cập nhật thành công!", Toast.LENGTH_SHORT).show();
                                User updatedUser = response.body().getData();

                                // Lưu lại user mới vào SharedPreferences
                                SharedPreferences.Editor editor = getSharedPreferences("LoginPrefs", MODE_PRIVATE).edit();
                                editor.putString("user_data", new Gson().toJson(updatedUser));
                                editor.apply();
                            } else {
                                Toast.makeText(AccountProfileActivity.this, "Cập nhật thất bại!", Toast.LENGTH_SHORT).show();
                                Log.e("Update", "Response: " + response.message());
                            }
                        }

                        @Override
                        public void onFailure(Call<ApiResponse<User>> call, Throwable t) {
                            Log.e("Update", "Lỗi khi cập nhật", t);
                            Toast.makeText(AccountProfileActivity.this, "Lỗi kết nối server", Toast.LENGTH_SHORT).show();
                        }
                    });

        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Không thể xử lý ảnh!", Toast.LENGTH_SHORT).show();
        }
    }

    private void UnEnable(boolean enable) {
        edtEmail.setFocusable(enable);
        edtEmail.setFocusableInTouchMode(enable);
        edtEmail.setClickable(enable);

        edtPhoneNumber.setFocusable(enable);
        edtPhoneNumber.setFocusableInTouchMode(enable);
        edtPhoneNumber.setClickable(enable);
    }
}
