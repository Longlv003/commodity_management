package com.example.closethub;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;

import android.widget.Button;
import com.bumptech.glide.Glide;
import com.example.closethub.models.ApiResponse;
import com.example.closethub.models.ChangePinRequest;
import com.example.closethub.models.User;
import com.example.closethub.models.WalletResponse;
import com.example.closethub.networks.ApiService;
import com.example.closethub.networks.RetrofitClient;
import com.google.android.material.textfield.TextInputEditText;

import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class WalletAccountFragment extends Fragment {
    private TextView txtWalletNumber, txtCreateDate, txtTotalDeposits,
            txtTotalWithdrawals, txtChangePin, txtLogOut, txtNameAccount, txtEmail;
    private ImageView imgAvatar;
    private ApiService apiService;
    private SharedPreferences sharedPreferences;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_wallet_account, container, false);
        
        initUI(view);
        
        apiService = RetrofitClient.getApiService();
        sharedPreferences = getContext().getSharedPreferences("LoginPrefs", Context.MODE_PRIVATE);

        txtChangePin.setOnClickListener(v -> showChangePinDialog());
        txtLogOut.setOnClickListener(v -> showLogoutConfirmDialog());

        loadWalletInfo();
        
        return view;
    }

    private void initUI(View view) {
        txtWalletNumber = view.findViewById(R.id.txtWalletNumber);
        txtCreateDate = view.findViewById(R.id.txtCreateDate);
        txtTotalDeposits = view.findViewById(R.id.txtTotalDeposits);
        txtTotalWithdrawals = view.findViewById(R.id.txtTotalWithdrawals);
        txtChangePin = view.findViewById(R.id.txtChangePin);
        txtLogOut = view.findViewById(R.id.txtLogOut);
        imgAvatar = view.findViewById(R.id.imgAvatar);
        txtEmail = view.findViewById(R.id.txtEmailAccount);
        txtNameAccount = view.findViewById(R.id.txtNameAccount);
    }

    private void loadWalletInfo() {
        String token = sharedPreferences.getString("token", "");
        if (token.isEmpty()) {
            Toast.makeText(getContext(), "Vui lòng đăng nhập lại", Toast.LENGTH_SHORT).show();
            return;
        }

        apiService.getWalletInfo("Bearer " + token).enqueue(new Callback<ApiResponse<WalletResponse>>() {
            @Override
            public void onResponse(Call<ApiResponse<WalletResponse>> call, Response<ApiResponse<WalletResponse>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    WalletResponse walletInfo = response.body().getData();
                    if (walletInfo != null) {
                        txtWalletNumber.setText(walletInfo.getWallet_number() != null ? walletInfo.getWallet_number() : "---");
                        
                        // Format date
                        if (walletInfo.getCreate_date() != null) {
                            try {
                                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault());
                                Date date = sdf.parse(walletInfo.getCreate_date());
                                if (date != null) {
                                    SimpleDateFormat displayFormat = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
                                    txtCreateDate.setText(displayFormat.format(date));
                                } else {
                                    txtCreateDate.setText(walletInfo.getCreate_date());
                                }
                            } catch (Exception e) {
                                txtCreateDate.setText(walletInfo.getCreate_date());
                            }
                        }

                        // Format total deposits và withdrawals
                        NumberFormat formatter = NumberFormat.getInstance(new Locale("vi", "VN"));
                        txtTotalDeposits.setText(formatter.format(walletInfo.getTotal_deposits()) + " ₫");
                        txtTotalWithdrawals.setText(formatter.format(walletInfo.getTotal_withdrawals()) + " ₫");

                        // Hiển thị thông tin user (name, email, image)
                        User user = walletInfo.getId_user();
                        if (user != null) {
                            // Hiển thị name
                            if (user.getName() != null && !user.getName().isEmpty()) {
                                txtNameAccount.setText(user.getName());
                            } else {
                                txtNameAccount.setText("Chưa cập nhật");
                            }

                            // Hiển thị email
                            if (user.getEmail() != null && !user.getEmail().isEmpty()) {
                                txtEmail.setText(user.getEmail());
                            } else {
                                txtEmail.setText("Chưa cập nhật");
                            }

                            // Load image
                            if (user.getImage() != null && !user.getImage().isEmpty()) {
                                String imageUrl = "http://10.0.2.2:3000/images/avatars/" + user.getImage();
                                Glide.with(WalletAccountFragment.this)
                                        .load(imageUrl)
                                        .placeholder(R.drawable.ic_placeholder)
                                        .error(R.drawable.ic_error)
                                        .into(imgAvatar);
                            } else {
                                // Nếu không có image, dùng placeholder
                                imgAvatar.setImageResource(R.drawable.ic_placeholder);
                            }
                        }
                    }
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<WalletResponse>> call, Throwable t) {
                Toast.makeText(getContext(), "Lỗi: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showChangePinDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        View dialogView = LayoutInflater.from(getContext()).inflate(R.layout.dialog_change_pin, null);
        builder.setView(dialogView);

        TextInputEditText edtOldPin = dialogView.findViewById(R.id.edtOldPin);
        TextInputEditText edtNewPin = dialogView.findViewById(R.id.edtNewPin);
        TextInputEditText edtConfirmPin = dialogView.findViewById(R.id.edtConfirmPin);
        Button btnCancel = dialogView.findViewById(R.id.btnCancel);
        Button btnConfirm = dialogView.findViewById(R.id.btnConfirm);

        AlertDialog dialog = builder.create();

        btnCancel.setOnClickListener(v -> dialog.dismiss());

        btnConfirm.setOnClickListener(v -> {
            String oldPin = edtOldPin.getText().toString().trim();
            String newPin = edtNewPin.getText().toString().trim();
            String confirmPin = edtConfirmPin.getText().toString().trim();

            if (oldPin.isEmpty() || oldPin.length() != 6 || !oldPin.matches("\\d+")) {
                Toast.makeText(getContext(), "Mã PIN cũ phải là 6 chữ số", Toast.LENGTH_SHORT).show();
                return;
            }

            if (newPin.isEmpty() || newPin.length() != 6 || !newPin.matches("\\d+")) {
                Toast.makeText(getContext(), "Mã PIN mới phải là 6 chữ số", Toast.LENGTH_SHORT).show();
                return;
            }

            if (!newPin.equals(confirmPin)) {
                Toast.makeText(getContext(), "Mã PIN mới không khớp", Toast.LENGTH_SHORT).show();
                return;
            }

            changePin(oldPin, newPin, dialog);
        });

        dialog.show();
    }

    private void changePin(String oldPin, String newPin, AlertDialog dialog) {
        String token = sharedPreferences.getString("token", "");
        if (token.isEmpty()) {
            Toast.makeText(getContext(), "Vui lòng đăng nhập lại", Toast.LENGTH_SHORT).show();
            return;
        }

        // Kiểm tra PIN cũ và PIN mới không được giống nhau
        if (oldPin.equals(newPin)) {
            Toast.makeText(getContext(), "PIN mới phải khác PIN cũ", Toast.LENGTH_SHORT).show();
            return;
        }

        ChangePinRequest request = new ChangePinRequest(oldPin, newPin);
        apiService.changePin("Bearer " + token, request).enqueue(new Callback<ApiResponse<Object>>() {
            @Override
            public void onResponse(Call<ApiResponse<Object>> call, Response<ApiResponse<Object>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    String message = response.body().getMsg();
                    if (message != null && !message.isEmpty()) {
                        Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(getContext(), "Đổi PIN thành công", Toast.LENGTH_SHORT).show();
                    }
                    dialog.dismiss();
                } else {
                    String errorMsg = "Đổi PIN thất bại";
                    if (response.body() != null && response.body().getMsg() != null) {
                        errorMsg = response.body().getMsg();
                    }
                    Toast.makeText(getContext(), errorMsg, Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<Object>> call, Throwable t) {
                Toast.makeText(getContext(), "Lỗi: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showLogoutConfirmDialog() {
        new AlertDialog.Builder(getContext())
                .setTitle("Đăng xuất ví")
                .setMessage("Bạn có chắc chắn muốn đăng xuất khỏi ví?")
                .setPositiveButton("Đăng xuất", (dialog, which) -> {
                    logoutWallet();
                })
                .setNegativeButton("Hủy", (dialog, which) -> {
                    dialog.dismiss();
                })
                .show();
    }

    private void logoutWallet() {
        // Đóng WalletActivity và quay về MainActivity
        if (getActivity() != null) {
            Toast.makeText(getContext(), "Đã đăng xuất khỏi ví", Toast.LENGTH_SHORT).show();
            
            // Quay về MainActivity
            Intent intent = new Intent(getActivity(), MainActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            getActivity().finish();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        loadWalletInfo();
    }
}

