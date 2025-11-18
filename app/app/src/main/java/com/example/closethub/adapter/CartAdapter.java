package com.example.closethub.adapter;

import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.closethub.R;
import com.example.closethub.models.ApiResponse;
import com.example.closethub.models.Cart;
import com.example.closethub.models.CartLookUpProduct;
import com.example.closethub.models.User;
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

public class CartAdapter extends RecyclerView.Adapter<CartAdapter.CartViewHolder> {
    private Context context;
    private ArrayList<CartLookUpProduct> productArrayList;
    private ApiService apiService = RetrofitClient.getApiService();

    public CartAdapter(Context context, ArrayList<CartLookUpProduct> productArrayList) {
        this.context = context;
        this.productArrayList = productArrayList;
    }

    @NonNull
    @Override
    public CartViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(context);
        View view = inflater.inflate(R.layout.activity_item_product_cart, parent, false);
        return new CartViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CartViewHolder holder, int position) {
        SharedPreferences prefs = context.getSharedPreferences("LoginPrefs", Context.MODE_PRIVATE);
        String userJson = prefs.getString("user_data", null);

        if (userJson == null) {
            Toast.makeText(context, "account found", Toast.LENGTH_SHORT).show();
            return;
        }

        Gson gson = new Gson();
        User user = gson.fromJson(userJson, User.class);

        CartLookUpProduct product = productArrayList.get(position);

        String imageUrl = "";
        if (product.getId_product().getImage() != null && !product.getId_product().getImage().isEmpty()) {
            imageUrl = "http://10.0.2.2:3000/images/products/" + product.getId_product().getImage().get(0);
        }

        Glide.with(context)
                .load(imageUrl)
                .placeholder(R.drawable.ic_placeholder) // Ảnh tạm khi đang load
                .error(R.drawable.ic_error)             // Ảnh lỗi nếu load thất bại
                .into(holder.imgProduct);

        holder.txtName.setText(product.getId_product().getName());

        NumberFormat formatter = NumberFormat.getInstance(new Locale("vi", "VN"));
        String formattedPrice = formatter.format(product.getId_variant().getPrice()) + " ₫";
        holder.txtPrice.setText(formattedPrice);

        holder.txtSize.setText(product.getId_variant().getSize());

        String hexColor = mapColor(product.getId_variant().getColor());

        GradientDrawable bg = (GradientDrawable) holder.viewColor.getBackground();
        bg.setColor(Color.parseColor(hexColor));

        holder.edtQuantity.setText(String.valueOf(product.getQuantity()));

        holder.btnAddCart.setOnClickListener(v -> {
            int newQty = product.getQuantity() + 1;
            apiService.updateCartQuantity(user.getToken(), product.get_id(), newQty).enqueue(new Callback<ApiResponse<CartLookUpProduct>>() {
                @Override
                public void onResponse(Call<ApiResponse<CartLookUpProduct>> call, Response<ApiResponse<CartLookUpProduct>> response) {
                    if (response.isSuccessful() && response.body() != null) {
                        product.setQuantity(newQty);
                        holder.edtQuantity.setText(String.valueOf(newQty));
                        notifyItemChanged(position);
                        if (listener != null) listener.OnQuantityChanged();
                    }
                }
                @Override
                public void onFailure(Call<ApiResponse<CartLookUpProduct>> call, Throwable throwable) {
                    Log.e("Error", "Updating Quantity Error", throwable);
                }
            });
        });

        holder.btnRemoveCart.setOnClickListener(v -> {
            int newQty = product.getQuantity() - 1;
            if (newQty == 0) {
                int currentPosition = holder.getAdapterPosition();
                AlertDialog.Builder builder = new AlertDialog.Builder(context);
                builder.setTitle("Confirm Delete");
                builder.setMessage("Are you sure?");
                builder.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if(currentPosition != RecyclerView.NO_POSITION){
                            DeleteCart(user.getToken(), product.get_id(), currentPosition);
                        }
                    }
                }).setNegativeButton("No", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                        holder.edtQuantity.setText(String.valueOf(product.getQuantity()));
                    }
                });
                AlertDialog alertDialog = builder.create();
                alertDialog.setCancelable(false);
                alertDialog.show();
                return;
            }

            apiService.updateCartQuantity(user.getToken(), product.get_id(), newQty).enqueue(new Callback<ApiResponse<CartLookUpProduct>>() {
                @Override
                public void onResponse(Call<ApiResponse<CartLookUpProduct>> call, Response<ApiResponse<CartLookUpProduct>> response) {
                    if (response.isSuccessful() && response.body() != null) {
                        product.setQuantity(newQty);
                        holder.edtQuantity.setText(String.valueOf(newQty));
                        notifyItemChanged(position);
                        if (listener != null) listener.OnQuantityChanged();
                    }
                }
                @Override
                public void onFailure(Call<ApiResponse<CartLookUpProduct>> call, Throwable throwable) {
                    Log.e("Error", "Updating Quantity Error", throwable);
                }
            });
        });

        holder.imgDelete.setOnClickListener(v -> {
            int currentPosition = holder.getAdapterPosition();
            AlertDialog.Builder builder = new AlertDialog.Builder(context);
            builder.setTitle("Confirm Delete");
            builder.setMessage("Are you sure?");
            builder.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    if(currentPosition != RecyclerView.NO_POSITION){
                        DeleteCart(user.getToken(), product.get_id(), currentPosition);
                    }
                }
            }).setNegativeButton("No", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    dialog.dismiss();
                }
            });
            AlertDialog alertDialog = builder.create();
            alertDialog.setCancelable(false);
            alertDialog.show();
        });
    }

    private void DeleteCart(String token, String id, int position) {
        apiService.deleteCartItem(token, id).enqueue(new Callback<ApiResponse<Cart>>() {
            @Override
            public void onResponse(Call<ApiResponse<Cart>> call, Response<ApiResponse<Cart>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    productArrayList.remove(position);
                    notifyItemRemoved(position);
                    if (listener != null) listener.OnQuantityChanged();
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<Cart>> call, Throwable throwable) {

            }
        });
    }

    @Override
    public int getItemCount() {
        return productArrayList.size();
    }

    public interface OnQuantityChangeListener {
        void OnQuantityChanged();
    }

    private OnQuantityChangeListener listener;

    public void setOnQuantityChangeListener(OnQuantityChangeListener listener) {
        this.listener = listener;
    }

    private String mapColor(String colorName) {
        switch (colorName.toLowerCase()) {
            case "đỏ": return "#FF0000";
            case "đen": return "#000000";
            case "xanh": return "#007BFF";
            case "vàng": return "#FFD600";
            case "hồng": return "#FF69B4";
            case "trắng": return "#FFFFFF";
        }
        return "#CCCCCC"; // fallback
    }

    public class CartViewHolder extends RecyclerView.ViewHolder {
        ImageView imgFavorite, imgProduct, btnAddCart, btnRemoveCart, imgDelete;
        TextView txtName, txtPrice, txtSize;
        EditText edtQuantity;
        View viewColor;
        public CartViewHolder(@NonNull View itemView) {
            super(itemView);

            imgFavorite = itemView.findViewById(R.id.imgFavorite);
            imgProduct = itemView.findViewById(R.id.imgProduct);
            imgDelete = itemView.findViewById(R.id.imgDelete);
            txtName = itemView.findViewById(R.id.txtName);
            txtPrice = itemView.findViewById(R.id.txtPrice);
            txtSize = itemView.findViewById(R.id.txtSize);
            edtQuantity = itemView.findViewById(R.id.edtQuantity);
            btnAddCart = itemView.findViewById(R.id.btnAddCart);
            btnRemoveCart = itemView.findViewById(R.id.btnRemoveCart);

            viewColor = itemView.findViewById(R.id.viewColor);
        }
    }
}
