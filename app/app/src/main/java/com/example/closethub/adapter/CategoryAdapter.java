package com.example.closethub.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.closethub.R;
import com.example.closethub.models.Category;

import java.util.ArrayList;

public class CategoryAdapter extends RecyclerView.Adapter<CategoryAdapter.CategoryViewHolder> {
    private Context context;
    private ArrayList<Category> categoryArrayList;
    private OnCategoryClickListener listener;

    // Interface để callback về Fragment
    public interface OnCategoryClickListener {
        void onCategoryClickItem(Category category);
    }

    public void setOnCategoryClickItemListener(OnCategoryClickListener listener) {
        this.listener = listener;
    }

    public CategoryAdapter(Context context, ArrayList<Category> categoryArrayList) {
        this.context = context;
        this.categoryArrayList = categoryArrayList;
    }

    @NonNull
    @Override
    public CategoryViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(context);
        View view = inflater.inflate(R.layout.activity_item_category, parent, false);
        return new CategoryViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CategoryViewHolder holder, int position) {
        Category category = categoryArrayList.get(position);
        holder.txtName.setText(category.getName());

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onCategoryClickItem(category);
            }
        });
    }

    @Override
    public int getItemCount() {
        return categoryArrayList.size();
    }

    public class CategoryViewHolder extends RecyclerView.ViewHolder {
        TextView txtName;
        public CategoryViewHolder(@NonNull View itemView) {
            super(itemView);
            txtName = itemView.findViewById(R.id.txtName);
        }
    }
}
