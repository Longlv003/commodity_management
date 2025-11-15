package com.example.closethub;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.textfield.TextInputEditText;

public class WalletLoginActivity extends AppCompatActivity {
    private ImageView imgBack;
    private TextInputEditText edtPinHash;
    private Button btnLogin;
    private TextView txtForgotPin;
    private void initUI() {
        imgBack = findViewById(R.id.imgBack);
        edtPinHash = findViewById(R.id.edtPinHash);
        btnLogin = findViewById(R.id.btnLogin);
        txtForgotPin = findViewById(R.id.txtForgotPin);
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

        imgBack.setOnClickListener(v -> {
            finish();
        });
    }
}