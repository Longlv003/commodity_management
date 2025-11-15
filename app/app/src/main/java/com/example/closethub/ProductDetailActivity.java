package com.example.closethub;

import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.closethub.adapter.ColorProductAdapter;
import com.example.closethub.adapter.SizeProductAdapter;
import com.example.closethub.models.ApiResponse;
import com.example.closethub.models.Product;
import com.example.closethub.models.Variant;
import com.example.closethub.networks.ApiService;
import com.example.closethub.networks.RetrofitClient;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ProductDetailActivity extends AppCompatActivity {
    private ImageView imgBack, imgFavorite, imgProduct, imgAddQuantity, imgRemoveQuantity;
    private RecyclerView rcvColor, rcvSize;
    private EditText edtQuantity;
    private TextView txtNameProduct, txtPrice, txtDescription;
    private Button btnAddToCart;
    private void initUI() {
        imgBack = findViewById(R.id.imgBack);
        imgFavorite = findViewById(R.id.imgFavorite);
        imgProduct = findViewById(R.id.imgProduct);
        imgAddQuantity = findViewById(R.id.imgAddQuantity);
        imgRemoveQuantity = findViewById(R.id.imgRemoveQuantity);
        rcvColor = findViewById(R.id.rcvColor);
        rcvSize = findViewById(R.id.rcvSize);
        edtQuantity = findViewById(R.id.edtQuantity);
        txtNameProduct = findViewById(R.id.txtNameProduct);
        txtPrice = findViewById(R.id.txtPrice);
        txtDescription = findViewById(R.id.txtDescription);
        btnAddToCart = findViewById(R.id.btnAddToCart);
    }
    private ApiService apiService;
    private SizeProductAdapter sizeProductAdapter;
    private ColorProductAdapter colorProductAdapter;
    private Product product;
    private String selectedSize = null;
    private String selectedColor = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_product_detail);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
        initUI();
        apiService = RetrofitClient.getApiService();

        String productId = getIntent().getStringExtra("product_id");
        Log.d("DETAIL", "productId = " + productId);

        imgBack.setOnClickListener(v -> {
            finish();
        });

        LoadDataDetail(productId);
    }

    private void LoadDataDetail(String productId) {
        apiService.getProductDetail(productId).enqueue(new Callback<ApiResponse<Product>>() {
            @Override
            public void onResponse(Call<ApiResponse<Product>> call, Response<ApiResponse<Product>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    product = response.body().getData();

                    // Lấy ảnh đầu tiên trong danh sách ảnh sản phẩm
                    String imageUrl = "";
                    if (product.getImage() != null && !product.getImage().isEmpty()) {
                        imageUrl = "http://10.0.2.2:3000/images/products/" + product.getImage().get(0);
                    }

                    Glide.with(ProductDetailActivity.this)
                            .load(imageUrl)
                            .placeholder(R.drawable.ic_placeholder) // Ảnh tạm khi đang load
                            .error(R.drawable.ic_error)             // Ảnh lỗi nếu load thất bại
                            .into(imgProduct);

                    if (product.isIs_favorite()) {
                        imgFavorite.setImageResource(R.drawable.ic_favorite_red);
                    } else {
                        imgFavorite.setImageResource(R.drawable.ic_favorite);
                    }

                    txtNameProduct.setText(product.getName());
                    txtDescription.setText(product.getDescription());

                    ArrayList<String> listSize = new ArrayList<>();
                    ArrayList<String> listColor = new ArrayList<>();

                    for (Variant v : product.getVariants()) {
                        if (!listSize.contains(v.getSize())) {
                            listSize.add(v.getSize());
                        }
                        if (!listColor.contains(v.getColor())) {
                            listColor.add(v.getColor());
                        }
                    }
                    Log.d("LIST_SIZE", "Count: "+listSize.size());
                    Log.d("LIST_COLOR", "Count: "+listColor.size());

                    sizeProductAdapter = new SizeProductAdapter(listSize, size -> {
                        selectedSize = size;
                        updatePrice();
                    });
                    rcvSize.setLayoutManager(new LinearLayoutManager(ProductDetailActivity.this, LinearLayoutManager.HORIZONTAL, false));
                    rcvSize.setAdapter(sizeProductAdapter);

                    colorProductAdapter = new ColorProductAdapter(listColor, color -> {
                        selectedColor = color;
                        updatePrice();
                    });
                    rcvColor.setLayoutManager(new LinearLayoutManager(ProductDetailActivity.this, LinearLayoutManager.HORIZONTAL, false));
                    rcvColor.setAdapter(colorProductAdapter);
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<Product>> call, Throwable throwable) {
                Log.e("API_ERROR", "Load detail failed", throwable);
            }
        });
    }

    private void updatePrice() {
        if (selectedSize == null || selectedColor == null) return;

        for (Variant v : product.getVariants()) {
            if (v.getSize().equals(selectedSize) &&
                    v.getColor().equals(selectedColor)) {

                txtPrice.setText(v.getPrice() + "đ");
                break;
            }
        }
    }

}