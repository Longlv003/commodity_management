package com.example.closethub;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
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

import com.example.closethub.models.User;
import com.example.closethub.networks.ApiService;
import com.example.closethub.networks.RetrofitClient;
import com.google.gson.Gson;

import java.text.NumberFormat;
import java.util.Locale;

public class PayActivity extends AppCompatActivity {
    private ImageView imgBack;
    private TextView txtNameAccount, txtPhoneAccount, txtAddressAccount,
            txtSubtotal, txtShipping, txtTotal;
    private Button btnChangeAddress, btnContinue;
    private RadioGroup radioGroupShipping, radioGroupPayment;
    private RadioButton radioFastDelivery, radioExpressDelivery, radioPayOff, radioPayOnline;
    private int subtotal = 0;
    private int fastDelivery = 25000;
    private int expressDelivery = 40000;
    private int currentShipping = 0;
    private void initUI() {
        imgBack = findViewById(R.id.imgBack);
        txtNameAccount = findViewById(R.id.txtNameAccount);
        txtPhoneAccount = findViewById(R.id.txtPhoneAccount);
        txtAddressAccount = findViewById(R.id.txtAddressAccount);
        txtSubtotal = findViewById(R.id.txtSubtotal);
        txtShipping = findViewById(R.id.txtShipping);
        txtTotal = findViewById(R.id.txtTotal);
        btnChangeAddress = findViewById(R.id.btnChangeAddress);
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

        setupEvent();

    }

    private void setupEvent() {
        imgBack.setOnClickListener(v -> {finish();});
    }


}