package com.example.closethub.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.closethub.R;
import com.example.closethub.models.Banner;

import java.util.ArrayList;

public class BannerAdapter extends RecyclerView.Adapter<BannerAdapter.BannerViewHolder> {
    private Context context;
    private ArrayList<Banner> bannerArrayList;

    public BannerAdapter(Context context, ArrayList<Banner> bannerArrayList) {
        this.context = context;
        this.bannerArrayList = bannerArrayList;
    }

    @NonNull
    @Override
    public BannerViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        View view = inflater.inflate(R.layout.activity_layout_banner, parent, false);
        return new BannerViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull BannerViewHolder holder, int position) {
        Banner banner = bannerArrayList.get(position);
        String imageUrl = "http://10.0.2.2:3000/images/banner/" + banner.getImage();
        Glide.with(holder.itemView.getContext())
                .load(imageUrl)
                .placeholder(R.drawable.ic_placeholder) // ảnh tạm khi đang load
                .error(R.drawable.ic_error)             // ảnh lỗi nếu load thất bại
                .into(holder.imgBanner);
    }

    @Override
    public int getItemCount() {
        return bannerArrayList.size();
    }

    public void updateData(ArrayList<Banner> newBannerList) {
        this.bannerArrayList = newBannerList;
        notifyDataSetChanged();
    }

    public class BannerViewHolder extends RecyclerView.ViewHolder {
        ImageView imgBanner;
        public BannerViewHolder(@NonNull View itemView) {
            super(itemView);
            imgBanner = itemView.findViewById(R.id.imgBanner);
        }
    }
}
