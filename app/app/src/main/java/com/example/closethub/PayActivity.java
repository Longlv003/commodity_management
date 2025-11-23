package com.example.closethub;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.closethub.models.ApiResponse;
import com.example.closethub.models.Bill;
import com.example.closethub.models.CartLookUpProduct;
import com.example.closethub.models.OrderRequest;
import com.example.closethub.models.User;
import com.example.closethub.models.WalletResponse;
import com.example.closethub.networks.ApiService;
import com.example.closethub.networks.RetrofitClient;
import com.google.gson.Gson;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class PayActivity extends AppCompatActivity {
    private ImageView imgBack;
    private TextView txtNameAccount, txtPhoneAccount, txtAddressAccount,
            txtSubtotal, txtShipping, txtTotal, txtWalletBalance;
    private Button btnChangeAddress, btnContinue;
    private RadioGroup radioGroupShipping, radioGroupPayment;
    private RadioButton radioFastDelivery, radioExpressDelivery, radioPayOff, radioPayOnline;
    
    private double subtotal = 0;
    private double fastDelivery = 25000;
    private double expressDelivery = 40000;
    private double currentShipping = 0;
    
    private String selectedShippingMethod = "fast"; // "fast" hoặc "express"
    private String selectedPaymentMethod = "cod"; // "cod" (cash on delivery) hoặc "online"
    
    private ArrayList<CartLookUpProduct> cartItems = new ArrayList<>();
    private void initUI() {
        imgBack = findViewById(R.id.imgBack);
        txtNameAccount = findViewById(R.id.txtNameAccount);
        txtPhoneAccount = findViewById(R.id.txtPhoneAccount);
        txtAddressAccount = findViewById(R.id.txtAddressAccount);
        txtSubtotal = findViewById(R.id.txtSubtotal);
        txtShipping = findViewById(R.id.txtShipping);
        txtTotal = findViewById(R.id.txtTotal);
        txtWalletBalance = findViewById(R.id.txtWalletBalance);
        btnChangeAddress = findViewById(R.id.btnChangeAddress);
        btnContinue = findViewById(R.id.btnContinue);
        radioGroupShipping = findViewById(R.id.radioGroupShipping);
        radioGroupPayment = findViewById(R.id.radioGroupPayment);
        radioFastDelivery = findViewById(R.id.radioFastDelivery);
        radioExpressDelivery = findViewById(R.id.radioExpressDelivery);
        radioPayOff = findViewById(R.id.radioPayOff);
        radioPayOnline = findViewById(R.id.radioPayOnline);
    }
    private ApiService apiService;
    private User user;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_pay);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
        initUI();
        apiService = RetrofitClient.getApiService();

        SharedPreferences prefs = getSharedPreferences("LoginPrefs", Context.MODE_PRIVATE);
        String userJson = prefs.getString("user_data", null);

        if (userJson == null) {
            Toast.makeText(PayActivity.this, "account found", Toast.LENGTH_SHORT).show();
            return;
        }

        Gson gson = new Gson();
        user = gson.fromJson(userJson, User.class);

        setupAddress();
        loadCartData();
        setupEvent();
    }

    private void setupAddress() {
        if (user == null) {
            txtNameAccount.setText("Chưa nhập địa chỉ giao hàng");
            txtPhoneAccount.setText("");
            txtAddressAccount.setText("");
            return;
        }
        
        String name = user.getName();
        String phone = user.getPhone();
        String address = user.getAddress();
        
        if (name == null || name.trim().isEmpty()) {
            txtNameAccount.setText("Chưa nhập tên người nhận");
        } else {
            txtNameAccount.setText(name);
        }
        
        if (phone == null || phone.trim().isEmpty()) {
            txtPhoneAccount.setText("");
        } else {
            txtPhoneAccount.setText("(" + phone + ")");
        }
        
        if (address == null || address.trim().isEmpty()) {
            txtAddressAccount.setText("Chưa nhập địa chỉ giao hàng");
        } else {
            txtAddressAccount.setText(address);
        }
    }

    private void loadCartData() {
        if (user == null || user.get_id() == null) {
            Toast.makeText(this, "Vui lòng đăng nhập", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        apiService.getCartList(user.get_id()).enqueue(new Callback<ApiResponse<List<CartLookUpProduct>>>() {
            @Override
            public void onResponse(Call<ApiResponse<List<CartLookUpProduct>>> call, Response<ApiResponse<List<CartLookUpProduct>>> response) {
                if (response.isSuccessful() && response.body() != null && response.body().getData() != null) {
                    cartItems.clear();
                    cartItems.addAll(response.body().getData());
                    
                    if (cartItems.isEmpty()) {
                        Toast.makeText(PayActivity.this, "Giỏ hàng trống", Toast.LENGTH_SHORT).show();
                        finish();
                        return;
                    }
                    
                    calculateSubtotal();
                    updateTotal();
                } else {
                    Toast.makeText(PayActivity.this, "Không thể tải giỏ hàng", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<List<CartLookUpProduct>>> call, Throwable throwable) {
                Toast.makeText(PayActivity.this, "Lỗi kết nối: " + throwable.getMessage(), Toast.LENGTH_SHORT).show();
                Log.e("PayActivity", "Load cart failed", throwable);
            }
        });
    }

    private void calculateSubtotal() {
        subtotal = 0;
        for (CartLookUpProduct item : cartItems) {
            if (item.getId_variant() != null) {
                subtotal += item.getQuantity() * item.getId_variant().getPrice();
            }
        }
        updateSubtotalDisplay();
    }

    private void updateSubtotalDisplay() {
        NumberFormat formatter = NumberFormat.getInstance(new Locale("vi", "VN"));
        txtSubtotal.setText(formatter.format(subtotal) + " ₫");
    }

    private void updateTotal() {
        double total = subtotal + currentShipping;
        NumberFormat formatter = NumberFormat.getInstance(new Locale("vi", "VN"));
        txtTotal.setText(formatter.format(total) + " ₫");
        
        // Cập nhật lại số dư ví nếu đang chọn thanh toán online
        if (selectedPaymentMethod.equals("online") && txtWalletBalance.getVisibility() == android.view.View.VISIBLE) {
            checkWalletBalance();
        }
    }

    private void setupEvent() {
        imgBack.setOnClickListener(v -> finish());
        
        btnChangeAddress.setOnClickListener(v -> {
            Intent intent = new Intent(PayActivity.this, AccountProfileActivity.class);
            startActivityForResult(intent, 100);
        });

        // Setup listeners cho phương thức vận chuyển
        // Do RadioButton nằm trong LinearLayout, cần force chỉ 1 được chọn
        setupShippingListeners();
        
        // Setup listeners cho phương thức thanh toán
        // Do RadioButton nằm trong LinearLayout, cần force chỉ 1 được chọn
        setupPaymentListeners();
        
        // Mặc định chọn giao hàng nhanh và thanh toán khi nhận hàng
        setDefaultSelections();

        // Xử lý khi nhấn nút đặt hàng
        btnContinue.setOnClickListener(v -> {
            if (validateOrder()) {
                placeOrder();
            }
        });
    }

    private void setupShippingListeners() {
        radioFastDelivery.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                // Nếu chọn giao hàng nhanh, force bỏ chọn giao hàng hoả tốc
                radioExpressDelivery.setOnCheckedChangeListener(null); // Tắt listener tạm thời
                radioExpressDelivery.setChecked(false);
                setupShippingListeners(); // Setup lại listeners
                
                currentShipping = fastDelivery;
                selectedShippingMethod = "fast";
                updateShippingDisplay();
                updateTotal();
                Log.d("Shipping", "Selected: Fast Delivery (25.000đ)");
            }
        });
        
        radioExpressDelivery.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                // Nếu chọn giao hàng hoả tốc, force bỏ chọn giao hàng nhanh
                radioFastDelivery.setOnCheckedChangeListener(null); // Tắt listener tạm thời
                radioFastDelivery.setChecked(false);
                setupShippingListeners(); // Setup lại listeners
                
                currentShipping = expressDelivery;
                selectedShippingMethod = "express";
                updateShippingDisplay();
                updateTotal();
                Log.d("Shipping", "Selected: Express Delivery (40.000đ)");
            }
        });
    }

    private void setupPaymentListeners() {
        radioPayOff.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                // Nếu chọn COD, force bỏ chọn online
                radioPayOnline.setOnCheckedChangeListener(null); // Tắt listener tạm thời
                radioPayOnline.setChecked(false);
                setupPaymentListeners(); // Setup lại listeners
                
                selectedPaymentMethod = "cod"; // Cash on Delivery - Thanh toán khi nhận hàng
                Log.d("Payment", "Selected: COD (Cash on Delivery)");
                
                // Ẩn TextView số dư ví
                txtWalletBalance.setVisibility(android.view.View.GONE);
                
                // Enable nút thanh toán khi chọn COD
                btnContinue.setEnabled(true);
                btnContinue.setText("Đặt hàng");
                btnContinue.setAlpha(1f);
            }
        });
        
        radioPayOnline.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                // Nếu chọn online, force bỏ chọn COD
                radioPayOff.setOnCheckedChangeListener(null); // Tắt listener tạm thời
                radioPayOff.setChecked(false);
                setupPaymentListeners(); // Setup lại listeners
                
                selectedPaymentMethod = "online"; // Thanh toán online
                Log.d("Payment", "Selected: Online Payment");
                
                // Kiểm tra số dư ví khi chọn thanh toán online
                checkWalletBalance();
            }
        });
    }

    private void setDefaultSelections() {
        // Tạm thời tắt listener để tránh trigger khi set default
        radioFastDelivery.setOnCheckedChangeListener(null);
        radioExpressDelivery.setOnCheckedChangeListener(null);
        radioPayOff.setOnCheckedChangeListener(null);
        radioPayOnline.setOnCheckedChangeListener(null);
        
        // Clear tất cả
        radioFastDelivery.setChecked(false);
        radioExpressDelivery.setChecked(false);
        radioPayOff.setChecked(false);
        radioPayOnline.setChecked(false);
        
        // Set default
        radioFastDelivery.setChecked(true);
        radioPayOff.setChecked(true);
        
        // Set lại listeners
        setupShippingListeners();
        setupPaymentListeners();
        
        // Set giá trị mặc định
        currentShipping = fastDelivery;
        selectedShippingMethod = "fast";
        selectedPaymentMethod = "cod";
        updateShippingDisplay();
        updateTotal();
    }

    private void updateShippingDisplay() {
        NumberFormat formatter = NumberFormat.getInstance(new Locale("vi", "VN"));
        if (currentShipping > 0) {
            txtShipping.setText(formatter.format(currentShipping) + " ₫");
        } else {
            txtShipping.setText("0 ₫");
        }
    }

    private boolean validateOrder() {
        // Kiểm tra địa chỉ
        if (user.getAddress() == null || user.getAddress().trim().isEmpty()) {
            Toast.makeText(this, "Vui lòng nhập địa chỉ giao hàng", Toast.LENGTH_SHORT).show();
            btnChangeAddress.performClick();
            return false;
        }

        // Kiểm tra tên
        if (user.getName() == null || user.getName().trim().isEmpty()) {
            Toast.makeText(this, "Vui lòng nhập tên người nhận", Toast.LENGTH_SHORT).show();
            btnChangeAddress.performClick();
            return false;
        }

        // Kiểm tra số điện thoại
        if (user.getPhone() == null || user.getPhone().trim().isEmpty()) {
            Toast.makeText(this, "Vui lòng nhập số điện thoại", Toast.LENGTH_SHORT).show();
            btnChangeAddress.performClick();
            return false;
        }

        // Kiểm tra phương thức vận chuyển
        // Do RadioButton nằm trong LinearLayout, cần check trực tiếp từ RadioButton
        if (!radioFastDelivery.isChecked() && !radioExpressDelivery.isChecked()) {
            Toast.makeText(this, "Vui lòng chọn phương thức vận chuyển", Toast.LENGTH_SHORT).show();
            return false;
        }
        
        // Đảm bảo có 1 phương thức vận chuyển được chọn
        if (selectedShippingMethod == null || selectedShippingMethod.isEmpty()) {
            Toast.makeText(this, "Vui lòng chọn phương thức vận chuyển", Toast.LENGTH_SHORT).show();
            return false;
        }

        // Kiểm tra phương thức thanh toán
        // Do RadioButton nằm trong LinearLayout, cần check trực tiếp từ RadioButton
        if (!radioPayOff.isChecked() && !radioPayOnline.isChecked()) {
            Toast.makeText(this, "Vui lòng chọn phương thức thanh toán", Toast.LENGTH_SHORT).show();
            return false;
        }
        
        // Đảm bảo có 1 phương thức thanh toán được chọn
        if (selectedPaymentMethod == null || selectedPaymentMethod.isEmpty()) {
            Toast.makeText(this, "Vui lòng chọn phương thức thanh toán", Toast.LENGTH_SHORT).show();
            return false;
        }

        // Kiểm tra giỏ hàng
        if (cartItems.isEmpty()) {
            Toast.makeText(this, "Giỏ hàng trống", Toast.LENGTH_SHORT).show();
            finish();
            return false;
        }

        return true;
    }

    private void placeOrder() {
        if (user == null || user.get_id() == null || user.getToken() == null) {
            Toast.makeText(this, "Vui lòng đăng nhập", Toast.LENGTH_SHORT).show();
            return;
        }

        // Kiểm tra nếu chọn thanh toán online nhưng button bị disable (số dư không đủ)
        if (selectedPaymentMethod.equals("online") && !btnContinue.isEnabled()) {
            Toast.makeText(this, "Số dư ví không đủ. Vui lòng nạp thêm tiền hoặc chọn thanh toán khi nhận hàng.", Toast.LENGTH_LONG).show();
            return;
        }

        // Tạo địa chỉ đầy đủ kèm thông tin phương thức vận chuyển và thanh toán
        String shippingInfo = "";
        if (selectedShippingMethod.equals("fast")) {
            shippingInfo = "Giao hàng nhanh (25.000đ)";
        } else if (selectedShippingMethod.equals("express")) {
            shippingInfo = "Giao hàng hoả tốc (40.000đ)";
        }

        String paymentInfo = "";
        if (selectedPaymentMethod.equals("cod")) {
            paymentInfo = "Thanh toán khi nhận hàng";
        } else if (selectedPaymentMethod.equals("online")) {
            paymentInfo = "Thanh toán online";
        }

        // Tạo địa chỉ đầy đủ với thông tin bổ sung
        String fullAddress = String.format("%s - %s - %s | %s | %s",
                user.getName(),
                user.getPhone(),
                user.getAddress(),
                shippingInfo,
                paymentInfo);
        
        OrderRequest orderRequest = new OrderRequest(user.get_id(), fullAddress);
        
        String token = "Bearer " + user.getToken();
        
        // Disable button để tránh click nhiều lần
        btnContinue.setEnabled(false);
        btnContinue.setText("Đang xử lý...");
        
        // Log thông tin đơn hàng để debug
        Log.d("PlaceOrder", "Shipping: " + selectedShippingMethod + ", Payment: " + selectedPaymentMethod);
        Log.d("PlaceOrder", "Total: " + (subtotal + currentShipping));

        apiService.placeOrder(token, orderRequest).enqueue(new Callback<ApiResponse<Bill>>() {
            @Override
            public void onResponse(Call<ApiResponse<Bill>> call, Response<ApiResponse<Bill>> response) {
                btnContinue.setEnabled(true);
                btnContinue.setText("Đặt hàng");

                if (response.isSuccessful() && response.body() != null) {
                    ApiResponse<Bill> apiResponse = response.body();
                    
                    if (apiResponse.getData() != null) {
                        Toast.makeText(PayActivity.this, "Đặt hàng thành công!", Toast.LENGTH_SHORT).show();
                        
                        // Chuyển về MainActivity và mở BillFragment
                        Intent intent = new Intent(PayActivity.this, MainActivity.class);
                        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                        startActivity(intent);
                        finish();
                    } else {
                        String errorMsg = apiResponse.getMsg() != null ? apiResponse.getMsg() : "Đặt hàng thất bại";
                        Toast.makeText(PayActivity.this, errorMsg, Toast.LENGTH_LONG).show();
                    }
                } else {
                    try {
                        String errorBody = response.errorBody() != null ? response.errorBody().string() : "Lỗi không xác định";
                        Log.e("PlaceOrder", "Error response: " + errorBody);
                        Toast.makeText(PayActivity.this, "Đặt hàng thất bại: " + response.message(), Toast.LENGTH_LONG).show();
                    } catch (Exception e) {
                        Toast.makeText(PayActivity.this, "Đặt hàng thất bại", Toast.LENGTH_SHORT).show();
                    }
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<Bill>> call, Throwable throwable) {
                btnContinue.setEnabled(true);
                btnContinue.setText("Đặt hàng");
                
                Toast.makeText(PayActivity.this, "Lỗi kết nối: " + throwable.getMessage(), Toast.LENGTH_LONG).show();
                Log.e("PlaceOrder", "Failed", throwable);
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        
        // Khi quay lại từ AccountProfileActivity, reload user data và update address
        if (requestCode == 100 && resultCode == RESULT_OK) {
            SharedPreferences prefs = getSharedPreferences("LoginPrefs", Context.MODE_PRIVATE);
            String userJson = prefs.getString("user_data", null);
            
            if (userJson != null) {
                Gson gson = new Gson();
                user = gson.fromJson(userJson, User.class);
                setupAddress();
            }
        }
    }

    /**
     * Kiểm tra số dư ví khi chọn thanh toán online
     */
    private void checkWalletBalance() {
        if (user == null || user.getToken() == null) {
            Toast.makeText(this, "Vui lòng đăng nhập", Toast.LENGTH_SHORT).show();
            // Không tự động chuyển về COD, chỉ ẩn TextView và disable button
            txtWalletBalance.setVisibility(android.view.View.GONE);
            btnContinue.setEnabled(false);
            btnContinue.setText("Vui lòng đăng nhập");
            return;
        }

        // Kiểm tra user có ví chưa
        if (!user.isHas_wallet()) {
            Toast.makeText(this, "Bạn chưa có ví. Vui lòng tạo ví trước khi thanh toán online.", Toast.LENGTH_LONG).show();
            // Không tự động chuyển về COD, chỉ ẩn TextView và disable button
            txtWalletBalance.setVisibility(android.view.View.GONE);
            btnContinue.setEnabled(false);
            btnContinue.setText("Chưa có ví");
            return;
        }

        String token = "Bearer " + user.getToken();
        double totalAmount = subtotal + currentShipping;

        // Hiển thị TextView số dư (đang tải)
        txtWalletBalance.setVisibility(android.view.View.VISIBLE);
        txtWalletBalance.setText("Đang kiểm tra số dư...");
        txtWalletBalance.setTextColor(getResources().getColor(android.R.color.darker_gray));
        
        // Tạm thời disable button khi đang kiểm tra
        btnContinue.setEnabled(false);
        btnContinue.setText("Đang kiểm tra...");

        // Lấy thông tin ví
        apiService.getWalletInfo(token).enqueue(new Callback<ApiResponse<WalletResponse>>() {
            @Override
            public void onResponse(Call<ApiResponse<WalletResponse>> call, Response<ApiResponse<WalletResponse>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    WalletResponse walletInfo = response.body().getData();
                    if (walletInfo != null) {
                        double balance = walletInfo.getBalance();
                        NumberFormat formatter = NumberFormat.getInstance(new Locale("vi", "VN"));
                        String balanceStr = formatter.format(balance);
                        String totalStr = formatter.format(totalAmount);
                        
                        if (balance < totalAmount) {
                            // Số dư không đủ - hiển thị màu đỏ và DISABLE button
                            txtWalletBalance.setText("Số dư ví: " + balanceStr + " ₫ (Không đủ. Cần: " + totalStr + " ₫)");
                            txtWalletBalance.setTextColor(getResources().getColor(android.R.color.holo_red_dark));
                            
                            // DISABLE nút thanh toán, KHÔNG tự động chuyển về COD
                            btnContinue.setEnabled(false);
                            btnContinue.setText("Số dư không đủ");
                            btnContinue.setAlpha(0.5f);
                            
                            Toast.makeText(PayActivity.this, 
                                "Số dư ví không đủ!\nSố dư: " + balanceStr + " ₫\nTổng tiền: " + totalStr + " ₫\nVui lòng nạp thêm tiền hoặc chọn thanh toán khi nhận hàng", 
                                Toast.LENGTH_LONG).show();
                        } else {
                            // Số dư đủ - hiển thị màu xanh và ENABLE button
                            double remaining = balance - totalAmount;
                            String remainingStr = formatter.format(remaining);
                            txtWalletBalance.setText("Số dư ví: " + balanceStr + " ₫ (Sau thanh toán còn: " + remainingStr + " ₫)");
                            txtWalletBalance.setTextColor(getResources().getColor(android.R.color.holo_green_dark));
                            
                            // ENABLE nút thanh toán
                            btnContinue.setEnabled(true);
                            btnContinue.setText("Đặt hàng");
                            btnContinue.setAlpha(1f);
                        }
                    } else {
                        txtWalletBalance.setVisibility(android.view.View.GONE);
                        Toast.makeText(PayActivity.this, "Không thể kiểm tra số dư ví", Toast.LENGTH_SHORT).show();
                        // Không tự động chuyển về COD, chỉ disable button
                        btnContinue.setEnabled(false);
                        btnContinue.setText("Lỗi kiểm tra ví");
                    }
                } else {
                    txtWalletBalance.setVisibility(android.view.View.GONE);
                    Toast.makeText(PayActivity.this, "Không thể kiểm tra số dư ví", Toast.LENGTH_SHORT).show();
                    // Không tự động chuyển về COD, chỉ disable button
                    btnContinue.setEnabled(false);
                    btnContinue.setText("Lỗi kiểm tra ví");
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<WalletResponse>> call, Throwable t) {
                Log.e("CheckWalletBalance", "Error: ", t);
                txtWalletBalance.setVisibility(android.view.View.GONE);
                Toast.makeText(PayActivity.this, "Lỗi kết nối khi kiểm tra ví", Toast.LENGTH_SHORT).show();
                // Không tự động chuyển về COD, chỉ disable button
                btnContinue.setEnabled(false);
                btnContinue.setText("Lỗi kết nối");
            }
        });
    }

}