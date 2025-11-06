package com.example.closethub;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.GravityCompat;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.Fragment;

import com.google.android.material.navigation.NavigationView;

public class MainActivity extends AppCompatActivity {
    private ImageView imgHome, imgBill, imgCart, imgHeart, imgMenu;
    private DrawerLayout drawerLayout;
    private NavigationView navigationView;
    private void initUI() {
        imgHome = findViewById(R.id.imgHome);
        imgBill = findViewById(R.id.imgBill);
        imgCart = findViewById(R.id.imgCart);
        imgHeart = findViewById(R.id.imgHeart);
        imgMenu = findViewById(R.id.imgMenu);
        drawerLayout = findViewById(R.id.drawerLayout);
        navigationView = findViewById(R.id.navigationView);
    }
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
        initUI();

        imgHome.setOnClickListener(v -> selectBottomBar(0));
        imgCart.setOnClickListener(v -> selectBottomBar(1));
        imgBill.setOnClickListener(v -> selectBottomBar(2));
        imgHeart.setOnClickListener(v -> selectBottomBar(3));

        // Mặc định chọn Home
        selectBottomBar(0);

        imgMenu.setOnClickListener(v -> {
            drawerLayout.openDrawer(GravityCompat.START);
        });

        // Xử lý khi người dùng bấm vào item trong menu
        navigationView.setNavigationItemSelectedListener(new NavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                int id = item.getItemId();

                if (id == R.id.nav_home) {
                    selectBottomBar(0);
                } else if (id == R.id.nav_profile) {
                    startActivity(new Intent(MainActivity.this, AccountProfileActivity.class));
                } else if (id == R.id.nav_settings) {
                    //selectBottomBar(0);
                } else if (id == R.id.nav_logout) {
                    startActivity(new Intent(MainActivity.this, LoginActivity.class));
                }

                // Đóng menu sau khi chọn
                drawerLayout.closeDrawer(GravityCompat.START);
                return true;
            }
        });
    }
    private void selectBottomBar(int index) {
        // Reset icon
        imgHome.setBackgroundResource(0);
        imgCart.setBackgroundResource(0);
        imgBill.setBackgroundResource(0);
        imgHeart.setBackgroundResource(0);

        Fragment fragment = null;

        switch (index) {
            case 0:
                imgHome.setBackgroundResource(R.drawable.bg_selected_icon);
                fragment = new HomeFragment();
                break;
            case 1:
                imgCart.setBackgroundResource(R.drawable.bg_selected_icon);
                fragment = new CartFragment();
                break;
            case 2:
                imgBill.setBackgroundResource(R.drawable.bg_selected_icon);
                fragment = new BillFragment();
                break;
            case 3:
                imgHeart.setBackgroundResource(R.drawable.bg_selected_icon);
                fragment = new FavoriteFragment();
                break;
        }

        // Chuyển fragment
        if (fragment != null) {
            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.frameLayout, fragment)
                    .commit();
        }
    }
}