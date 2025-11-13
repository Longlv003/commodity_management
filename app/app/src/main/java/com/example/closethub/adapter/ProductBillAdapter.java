package com.example.closethub.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.closethub.R;
import com.example.closethub.models.Product;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Locale;

public class ProductBillAdapter extends RecyclerView.Adapter<ProductBillAdapter.PBillViewHolder> {
    private Context context;
    private ArrayList<Product> productArrayList;

    public ProductBillAdapter(Context context, ArrayList<Product> productArrayList) {
        this.context = context;
        this.productArrayList = productArrayList;
    }

    @NonNull
    @Override
    public PBillViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        View view = inflater.inflate(R.layout.activity_item_product_bill, parent, false);
        return new PBillViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull PBillViewHolder holder, int position) {
        Product p = productArrayList.get(position);
        holder.txtName.setText(p.getName());

        NumberFormat formatter = NumberFormat.getInstance(new Locale("vi", "VN"));
        holder.txtPrice.setText(formatter.format(p.getMin_price()));
        holder.txtQuantity.setText(String.valueOf(p.getQuantity()));
        holder.txtAmount.setText(formatter.format(p.getMin_price() * p.getQuantity()));
    }

    @Override
    public int getItemCount() {
        return productArrayList.size();
    }

    public class PBillViewHolder extends RecyclerView.ViewHolder {
        TextView txtName, txtPrice, txtQuantity, txtAmount;
        public PBillViewHolder(@NonNull View itemView) {
            super(itemView);

            txtName = itemView.findViewById(R.id.txtName);
            txtPrice = itemView.findViewById(R.id.txtPrice);
            txtQuantity = itemView.findViewById(R.id.txtQuantity);
            txtAmount = itemView.findViewById(R.id.txtAmount);
        }
    }
}
