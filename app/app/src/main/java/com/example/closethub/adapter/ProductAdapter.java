package com.example.closethub.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.closethub.R;
import com.example.closethub.models.Product;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Locale;

public class ProductAdapter extends RecyclerView.Adapter<ProductAdapter.ProductViewHolder> {
    private Context context;
    private ArrayList<Product> productArrayList;

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
        String imageUrl = "http://10.0.2.2:3000/images/products/" + product.getImage();
        Glide.with(holder.itemView.getContext())
                .load(imageUrl)
                .placeholder(R.drawable.ic_placeholder) // ảnh tạm khi đang load
                .error(R.drawable.ic_error)             // ảnh lỗi nếu load thất bại
                .into(holder.imgProduct);

        holder.txtName.setText(product.getName());
        //holder.txtPrice.setText(String.valueOf(product.getPrice()));
        NumberFormat formatter = NumberFormat.getInstance(new Locale("vi", "VN"));
        String formattedPrice = formatter.format(product.getPrice()) + " ₫";
        holder.txtPrice.setText("Price: " + formattedPrice);
        holder.txtQty.setText("Quantity: " + String.valueOf(product.getQty()));

        holder.txtAddCart.setOnClickListener(v -> {
            AddToCart(product);
        });
    }

    private void AddToCart(Product product) {

    }

    @Override
    public int getItemCount() {
        return productArrayList.size();
    }

    public class ProductViewHolder extends RecyclerView.ViewHolder {
        ImageView imgFavorite, imgProduct, btnAddCart;
        TextView txtName, txtPrice, txtAddCart, txtQty;

        public ProductViewHolder(@NonNull View itemView) {
            super(itemView);

            imgFavorite = itemView.findViewById(R.id.imgFavorite);
            imgProduct = itemView.findViewById(R.id.imgProduct);
            txtName = itemView.findViewById(R.id.txtName);
            txtPrice = itemView.findViewById(R.id.txtPrice);
            txtQty = itemView.findViewById(R.id.txtQty);
            txtAddCart = itemView.findViewById(R.id.txtAddCart);
            //btnAddCart = itemView.findViewById(R.id.btnAddCart);
        }
    }
}
