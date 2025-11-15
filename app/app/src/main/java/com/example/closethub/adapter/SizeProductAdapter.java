package com.example.closethub.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.closethub.R;

import java.util.ArrayList;

public class SizeProductAdapter extends RecyclerView.Adapter<SizeProductAdapter.ViewHolder> {
    private ArrayList<String> sizeArrayList;
    private int selectedIndex = -1;
    private OnSizeClickListener listener;

    public interface OnSizeClickListener {
        void onSizeClick(String size);
    }

    public SizeProductAdapter(ArrayList<String> sizeArrayList, OnSizeClickListener listener) {
        this.sizeArrayList = sizeArrayList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        View view = inflater.inflate(R.layout.activity_item_size_product, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        String size = sizeArrayList.get(position);

        holder.txtSize.setText(size);

        if (position == selectedIndex) {
            holder.txtSize.setBackgroundResource(R.drawable.bg_selected);
            holder.txtSize.setTextColor(0xFFFFFFFF); // trắng
        } else {
            holder.txtSize.setBackgroundResource(R.drawable.bg_unselected);
            holder.txtSize.setTextColor(0xFF111111); // đen
        }

        holder.itemView.setOnClickListener(v -> {
            selectedIndex = position;
            notifyDataSetChanged();
            listener.onSizeClick(size);
        });
    }

    @Override
    public int getItemCount() {
        return sizeArrayList.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        TextView txtSize;
        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            txtSize = itemView.findViewById(R.id.txtSize);
        }
    }
}
