package com.example.closethub.adapter;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.closethub.R;
import com.example.closethub.models.ApiResponse;
import com.example.closethub.models.Cart;
import com.example.closethub.models.CartRequest;
import com.example.closethub.models.Product;
import com.example.closethub.networks.ApiService;
import com.example.closethub.networks.RetrofitClient;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ProductAdapter extends RecyclerView.Adapter<ProductAdapter.ProductViewHolder> {
    private Context context;
    private ArrayList<Product> productArrayList;
    private ApiService apiService = RetrofitClient.getApiService();

    public ProductAdapter(Context context, ArrayList<Product> productArrayList) {
        this.context = context;
        this.productArrayList = productArrayList;
    }

    @NonNull
    @Override
    public ProductViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        View view = inflater.inflate(R.layout.activity_item_product, parent, false);
        return new ProductViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ProductViewHolder holder, int position) {
        Product product = productArrayList.get(position);

        // Lấy ảnh đầu tiên trong danh sách ảnh sản phẩm
        String imageUrl = "";
        if (product.getImage() != null && !product.getImage().isEmpty()) {
            imageUrl = "http://10.0.2.2:3000/images/products/" + product.getImage().get(0);
        }

        Glide.with(holder.itemView.getContext())
                .load(imageUrl)
                .placeholder(R.drawable.ic_placeholder) // Ảnh tạm khi đang load
                .error(R.drawable.ic_error)             // Ảnh lỗi nếu load thất bại
                .into(holder.imgProduct);

        holder.txtName.setText(product.getName());

        // Hiển thị giá min - max
        NumberFormat formatter = NumberFormat.getInstance(new Locale("vi", "VN"));
        String formattedMinPrice = formatter.format(product.getMin_price()) + " ₫";
        String formattedMaxPrice = formatter.format(product.getMax_price()) + " ₫";

        // Nếu min và max bằng nhau thì chỉ hiển thị 1 giá
        if (product.getMin_price() == product.getMax_price()) {
            holder.txtMinPrice.setText(formattedMinPrice);
            holder.txtMaxPrice.setText(""); // ẩn giá max
        } else {
            holder.txtMinPrice.setText(formattedMinPrice);
            holder.txtMaxPrice.setText(formattedMaxPrice);
        }

        if (product.getQuantity() <= 0) {
            holder.txtQty.setText("Hết hàng");
            holder.txtQty.setTextColor(context.getResources().getColor(R.color.red));

            // Ẩn nút Add to cart
            holder.txtAddCart.setVisibility(View.GONE);
        } else {
            holder.txtQty.setText("Quantity: " + String.valueOf(product.getQuantity()));
            holder.txtQty.setTextColor(context.getResources().getColor(R.color.black));

            // Hiện nút Add to cart
            holder.txtAddCart.setVisibility(View.VISIBLE);
            holder.txtAddCart.setOnClickListener(v -> {
                AddToCart(product);
            });
        }

        if (product.isIs_favorite()) {
            holder.imgFavorite.setImageResource(R.drawable.ic_favorite_red);
        } else {
            holder.imgFavorite.setImageResource(R.drawable.ic_favorite);
        }

        // Xử lý click favorite
        holder.imgFavorite.setOnClickListener(v -> {
            toggleFavorite(product, holder);
        });

        holder.txtAddCart.setOnClickListener(v -> {
            AddToCart(product);
        });
    }

    private void toggleFavorite(Product product, ProductViewHolder holder) {
        boolean newFavoriteStatus = !product.isIs_favorite();

        SharedPreferences sharedPref = context.getSharedPreferences("LoginPrefs", Context.MODE_PRIVATE);
        String idUser = sharedPref.getString("id_user", null);
        String token_user = sharedPref.getString("token", null);

        if(idUser == null) {
            Toast.makeText(context, "Vui lòng đăng nhập để thêm yêu thích", Toast.LENGTH_SHORT).show();
            return;
        }

        apiService.toggleFavorite(token_user, product.get_id(), newFavoriteStatus).enqueue(new Callback<ApiResponse<Product>>() {
            @Override
            public void onResponse(Call<ApiResponse<Product>> call, Response<ApiResponse<Product>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    ApiResponse<Product> apiResponse = response.body();

                    // Cập nhật trạng thái local
                    product.setIs_favorite(newFavoriteStatus);

                    // Cập nhật icon
                    if (newFavoriteStatus) {
                        holder.imgFavorite.setImageResource(R.drawable.ic_favorite_red);
                        Toast.makeText(context, "Đã thêm vào yêu thích", Toast.LENGTH_SHORT).show();
                    } else {
                        holder.imgFavorite.setImageResource(R.drawable.ic_favorite);
                        Toast.makeText(context, "Đã xóa khỏi yêu thích", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Toast.makeText(context, "Cập nhật thất bại", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<Product>> call, Throwable throwable) {
                Toast.makeText(context, "Lỗi kết nối: " + throwable.getMessage(), Toast.LENGTH_SHORT).show();
                Log.e("ToggleFavorite", "Error: ", throwable);
            }
        });
    }

    private void AddToCart(Product product) {
        SharedPreferences sharedPref = context.getSharedPreferences("LoginPrefs", Context.MODE_PRIVATE);
        String idUser = sharedPref.getString("id_user", null);
        String token_user = sharedPref.getString("token", null);

        if(idUser == null) {
            Toast.makeText(context, "Vui lòng đăng nhập để thêm vào giỏ hàng", Toast.LENGTH_SHORT).show();
            return;
        }

        String idProduct = product.get_id();
        int quantity = 1;
        CartRequest cartRequest = new CartRequest();
        cartRequest.setId_user(idUser);
        cartRequest.setId_product(idProduct);
        cartRequest.setQuantity(quantity);

        apiService.addToCart(token_user, cartRequest).enqueue(new Callback<ApiResponse<Cart>>() {
            @Override
            public void onResponse(Call<ApiResponse<Cart>> call, Response<ApiResponse<Cart>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    Toast.makeText(context, "Add thanh cong", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(context, "Add that bai", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<Cart>> call, Throwable throwable) {
                Toast.makeText(context, "that bai", Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public int getItemCount() {
        return productArrayList.size();
    }

    public class ProductViewHolder extends RecyclerView.ViewHolder {
        ImageView imgFavorite, imgProduct, btnAddCart;
        TextView txtName, txtMinPrice, txtMaxPrice, txtAddCart, txtQty;

        public ProductViewHolder(@NonNull View itemView) {
            super(itemView);

            imgFavorite = itemView.findViewById(R.id.imgFavorite);
            imgProduct = itemView.findViewById(R.id.imgProduct);
            txtName = itemView.findViewById(R.id.txtName);
            txtMinPrice = itemView.findViewById(R.id.txtMinPrice);
            txtMaxPrice = itemView.findViewById(R.id.txtMaxPrice);
            txtQty = itemView.findViewById(R.id.txtQty);
            txtAddCart = itemView.findViewById(R.id.txtAddCart);
            //btnAddCart = itemView.findViewById(R.id.btnAddCart);
        }
    }
}
