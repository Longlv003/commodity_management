package com.example.closethub;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.closethub.adapter.CategoryAdapter;
import com.example.closethub.adapter.ProductAdapter;
import com.example.closethub.models.ApiResponse;
import com.example.closethub.models.Category;
import com.example.closethub.models.Product;
import com.example.closethub.networks.ApiService;
import com.example.closethub.networks.RetrofitClient;
import com.google.android.flexbox.FlexDirection;
import com.google.android.flexbox.FlexWrap;
import com.google.android.flexbox.FlexboxLayoutManager;
import com.google.android.flexbox.JustifyContent;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ViewAllProductActivity extends AppCompatActivity {
    private ImageView imgBack;
    private RecyclerView rcvCategory, rcvProduct;
    private void initUI () {
        imgBack = findViewById(R.id.imgBack);
        rcvCategory = findViewById(R.id.rcvCategory);
        rcvProduct = findViewById(R.id.rcvProduct);
    }
    ApiService apiService;
    private ArrayList<Category> categoryArrayList;
    private CategoryAdapter categoryAdapter;
    private ArrayList<Product> productArrayList;
    private ProductAdapter productAdapter;
    String categoryId = "";
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_view_all_product);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
        initUI();

        apiService = RetrofitClient.getApiService();

        imgBack.setOnClickListener(v -> {
            finish();
        });

        categoryArrayList = new ArrayList<>();
        FlexboxLayoutManager flexboxLayoutManager = new FlexboxLayoutManager(ViewAllProductActivity.this);
        flexboxLayoutManager.setFlexDirection(FlexDirection.ROW);
        flexboxLayoutManager.setFlexWrap(FlexWrap.WRAP);
        flexboxLayoutManager.setJustifyContent(JustifyContent.FLEX_START);

        rcvCategory.setLayoutManager(flexboxLayoutManager);

        categoryAdapter = new CategoryAdapter(ViewAllProductActivity.this, categoryArrayList);
        rcvCategory.setAdapter(categoryAdapter);

        categoryAdapter.setOnCategoryClickItemListener(category -> {
            productArrayList.clear();
            categoryId = category.get_id();
            GetListProductByCat(categoryId);
        });

        GetListCategory();

        productArrayList = new ArrayList<>();
        rcvProduct.setLayoutManager(
                new GridLayoutManager(ViewAllProductActivity.this, 2)
        );
        productAdapter = new ProductAdapter(ViewAllProductActivity.this, productArrayList);
        rcvProduct.setAdapter(productAdapter);

        if (categoryId.equals("")) {
            getListProduct();
        } else {
            GetListProductByCat(categoryId);
        }

    }

    private void getListProduct() {
        SharedPreferences sharedPref = getSharedPreferences("LoginPrefs", MODE_PRIVATE);
        String idUser = sharedPref.getString("id_user", null);
        
        apiService.getListProduct(idUser).enqueue(new Callback<ApiResponse<List<Product>>>() {
            @Override
            public void onResponse(Call<ApiResponse<List<Product>>> call, Response<ApiResponse<List<Product>>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    productArrayList.clear();
                    productArrayList.addAll(response.body().getData());
                    productAdapter.notifyDataSetChanged();
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<List<Product>>> call, Throwable throwable) {
                Log.e("Error", "ProductList Error", throwable);
            }
        });
    }

    private void GetListProductByCat(String categoryId) {
        SharedPreferences sharedPref = getSharedPreferences("LoginPrefs", MODE_PRIVATE);
        String idUser = sharedPref.getString("id_user", null);
        
        apiService.getListProductByCat(categoryId, idUser).enqueue(new Callback<ApiResponse<List<Product>>>() {
            @Override
            public void onResponse(Call<ApiResponse<List<Product>>> call, Response<ApiResponse<List<Product>>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    productArrayList.clear();
                    productArrayList.addAll(response.body().getData());
                    productAdapter.notifyDataSetChanged();
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<List<Product>>> call, Throwable throwable) {
                Log.e("Error", "ProductList Error", throwable);
            }
        });
    }

    private void GetListCategory() {
        apiService.getListCategory().enqueue(new Callback<ApiResponse<List<Category>>>() {
            @Override
            public void onResponse(Call<ApiResponse<List<Category>>> call, Response<ApiResponse<List<Category>>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    categoryArrayList.clear();
                    categoryArrayList.addAll(response.body().getData());
                    categoryAdapter.notifyDataSetChanged();
                } else {
                    Toast.makeText(ViewAllProductActivity.this, "Failed", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<List<Category>>> call, Throwable throwable) {
                Log.e("Error", "Failed", throwable);
            }
        });
    }
}