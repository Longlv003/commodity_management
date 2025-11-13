package com.example.closethub.adapter;

import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.closethub.R;
import com.example.closethub.models.ApiResponse;
import com.example.closethub.models.Cart;
import com.example.closethub.models.CartLookUpProduct;
import com.example.closethub.networks.ApiService;
import com.example.closethub.networks.RetrofitClient;

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
        SharedPreferences sharedPref = context.getSharedPreferences("LoginPrefs", Context.MODE_PRIVATE);
        String idUser = sharedPref.getString("id_user", null);
        String token_user = sharedPref.getString("token", null);

        CartLookUpProduct product = productArrayList.get(position);
        String imageUrl = "http://10.0.2.2:3000/images/products/" + product.getId_product().getImage();

        Glide.with(context)
                .load(imageUrl)
                .placeholder(R.drawable.ic_placeholder)
                .error(R.drawable.ic_error)
                .into(holder.imgProduct);

        holder.txtName.setText(product.getId_product().getName());

        NumberFormat formatter = NumberFormat.getInstance(new Locale("vi", "VN"));
        String formattedPrice = formatter.format(product.getId_product().getMin_price()) + " ₫";
        holder.txtPrice.setText(formattedPrice);

        holder.edtQuantity.setText(String.valueOf(product.getQuantity()));

        holder.btnAddCart.setOnClickListener(v -> {
            int newQty = product.getQuantity() + 1;
            apiService.UpdateQuantity(token_user, product.get_id(), newQty).enqueue(new Callback<ApiResponse<List<CartLookUpProduct>>>() {
                @Override
                public void onResponse(Call<ApiResponse<List<CartLookUpProduct>>> call, Response<ApiResponse<List<CartLookUpProduct>>> response) {
                    if (response.isSuccessful() && response.body() != null) {
                        product.setQuantity(newQty);
                        holder.edtQuantity.setText(String.valueOf(newQty));
                        if (listener != null) listener.OnQuantityChanged();
                    }
                }

                @Override
                public void onFailure(Call<ApiResponse<List<CartLookUpProduct>>> call, Throwable throwable) {
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
                            DeleteCart(token_user, product.get_id(), currentPosition);
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
            }
            apiService.UpdateQuantity(token_user, product.get_id(), newQty).enqueue(new Callback<ApiResponse<List<CartLookUpProduct>>>() {
                @Override
                public void onResponse(Call<ApiResponse<List<CartLookUpProduct>>> call, Response<ApiResponse<List<CartLookUpProduct>>> response) {
                    if (response.isSuccessful() && response.body() != null) {
                        product.setQuantity(newQty);
                        holder.edtQuantity.setText(String.valueOf(newQty));
                        if (listener != null) listener.OnQuantityChanged();
                    }
                }

                @Override
                public void onFailure(Call<ApiResponse<List<CartLookUpProduct>>> call, Throwable throwable) {
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
                        DeleteCart(token_user, product.get_id(), currentPosition);
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
        apiService.DeleteProduct(token, id).enqueue(new Callback<ApiResponse<Cart>>() {
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

    public class CartViewHolder extends RecyclerView.ViewHolder {
        ImageView imgFavorite, imgProduct, btnAddCart, btnRemoveCart, imgDelete;
        TextView txtName, txtPrice;
        EditText edtQuantity;
        public CartViewHolder(@NonNull View itemView) {
            super(itemView);

            imgFavorite = itemView.findViewById(R.id.imgFavorite);
            imgProduct = itemView.findViewById(R.id.imgProduct);
            imgDelete = itemView.findViewById(R.id.imgDelete);
            txtName = itemView.findViewById(R.id.txtName);
            txtPrice = itemView.findViewById(R.id.txtPrice);
            edtQuantity = itemView.findViewById(R.id.edtQuantity);
            btnAddCart = itemView.findViewById(R.id.btnAddCart);
            btnRemoveCart = itemView.findViewById(R.id.btnRemoveCart);
        }
    }
}
