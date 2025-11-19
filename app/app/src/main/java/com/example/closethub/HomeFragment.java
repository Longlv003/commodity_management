package com.example.closethub;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;

import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.ViewPager2;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.example.closethub.adapter.BannerAdapter;
import com.example.closethub.adapter.CategoryAdapter;
import com.example.closethub.adapter.ProductAdapter;
import com.example.closethub.models.ApiResponse;
import com.example.closethub.models.Banner;
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

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link HomeFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class HomeFragment extends Fragment {

    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";

    // TODO: Rename and change types of parameters
    private String mParam1;
    private String mParam2;

    public HomeFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param param1 Parameter 1.
     * @param param2 Parameter 2.
     * @return A new instance of fragment HomeFragment.
     */
    // TODO: Rename and change types and number of parameters
    public static HomeFragment newInstance(String param1, String param2) {
        HomeFragment fragment = new HomeFragment();
        Bundle args = new Bundle();
        args.putString(ARG_PARAM1, param1);
        args.putString(ARG_PARAM2, param2);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mParam1 = getArguments().getString(ARG_PARAM1);
            mParam2 = getArguments().getString(ARG_PARAM2);
        }
    }

    ApiService apiService;
    private ViewPager2 viewPagerBanner;
    private BannerAdapter bannerAdapter;
    private ArrayList<Banner> bannerArrayList = new ArrayList<>();
    private int currentPage = 0;
    private Handler handler;
    private Runnable runnable;
    private LinearLayout layoutIndicator;

    private RecyclerView rcvCategory, rcvProduct;
    private ArrayList<Category> categoryArrayList;
    private CategoryAdapter categoryAdapter;
    private ArrayList<Product> productArrayList;
    private ProductAdapter productAdapter;
    private TextView txtViewAll, txtViewAllCategory, txtCategory;
    String categoryId = "";

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home, container, false);
        apiService = RetrofitClient.getApiService();
        initViews(view);

        setupBannerAdapter();
        loadBannersFromAPI();

        categoryArrayList = new ArrayList<>();
        FlexboxLayoutManager flexboxLayoutManager = new FlexboxLayoutManager(getContext());
        flexboxLayoutManager.setFlexDirection(FlexDirection.ROW);
        flexboxLayoutManager.setFlexWrap(FlexWrap.WRAP);
        flexboxLayoutManager.setJustifyContent(JustifyContent.FLEX_START);

        rcvCategory.setLayoutManager(flexboxLayoutManager);

        categoryAdapter = new CategoryAdapter(getContext(), categoryArrayList);
        rcvCategory.setAdapter(categoryAdapter);

        categoryAdapter.setOnCategoryClickItemListener(category -> {
            productArrayList.clear();
            txtCategory.setText(category.getName());

            categoryId = category.get_id();
            GetListProductByCat(categoryId);
        });

        getTopCategories();

        productArrayList = new ArrayList<>();
//        rcvProduct.setLayoutManager(
//                new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false)
//        );
        rcvProduct.setLayoutManager(
                new GridLayoutManager(getContext(), 2)
        );
        productAdapter = new ProductAdapter(getContext(), productArrayList);
        rcvProduct.setAdapter(productAdapter);

        if (categoryId.equals("")) {
            GetTopSellingProducts();
        } else {
            GetListProductByCat(categoryId);
        }

        txtViewAll.setOnClickListener(v -> {
            startActivity(new Intent(getContext(), ViewAllProductActivity.class));
        });

        txtViewAllCategory.setOnClickListener(v -> {
            startActivity(new Intent(getContext(), ViewAllProductActivity.class));
        });

        return view;
    }

    private void GetTopSellingProducts() {
        SharedPreferences sharedPref = getContext().getSharedPreferences("LoginPrefs", Context.MODE_PRIVATE);
        String idUser = sharedPref.getString("id_user", null);
        
        apiService.GetTopSellingProducts(idUser).enqueue(new Callback<ApiResponse<List<Product>>>() {
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
                Toast.makeText(getContext(), "Error: " + throwable.getMessage(), Toast.LENGTH_SHORT).show();
                Log.e("Error", "ProductList Error", throwable);
            }
        });
    }

    private void GetListProductByCat(String categoryId) {
        SharedPreferences sharedPref = getContext().getSharedPreferences("LoginPrefs", Context.MODE_PRIVATE);
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

    private void getTopCategories() {
        apiService.getTopCategories().enqueue(new Callback<ApiResponse<List<Category>>>() {
            @Override
            public void onResponse(Call<ApiResponse<List<Category>>> call, Response<ApiResponse<List<Category>>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    categoryArrayList.clear();
                    categoryArrayList.addAll(response.body().getData());
                    categoryAdapter.notifyDataSetChanged();
                } else {
                    Toast.makeText(getContext(), "Failed", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<List<Category>>> call, Throwable throwable) {
                Log.e("Error", "Failed", throwable);
            }
        });
    }

    private void loadBannersFromAPI() {
        apiService.getBanner().enqueue(new Callback<ApiResponse<List<Banner>>>() {
            @Override
            public void onResponse(Call<ApiResponse<List<Banner>>> call, Response<ApiResponse<List<Banner>>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    bannerArrayList.clear();
                    bannerArrayList.addAll(response.body().getData());
                    //bannerAdapter.notifyDataSetChanged();

                    // Cập nhật adapter
                    bannerAdapter.updateData(bannerArrayList);

                    // Cập nhật indicator
                    setupIndicator();

                    // Bắt đầu auto scroll nếu có banner
                    startAutoScroll();

                    startAutoScroll();
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<List<Banner>>> call, Throwable throwable) {
                Toast.makeText(getContext(), "Failed", Toast.LENGTH_SHORT).show();
                Log.e("Error", "Failed", throwable);
            }
        });
    }

    private void setupIndicator() {
        if (layoutIndicator == null) return;
        
        Context context = getContext();
        if (context == null) return;

        layoutIndicator.removeAllViews();

        for (int i = 0; i < bannerArrayList.size(); i++) {
            ImageView imageView = new ImageView(context);
            imageView.setImageResource(
                    i == 0 ? R.drawable.dot_active : R.drawable.dot_inactive
            );

            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
            );
            params.setMargins(0, 0, 0, 0);

            imageView.setLayoutParams(params);
            layoutIndicator.addView(imageView);
        }
    }

    private void startAutoScroll() {
        handler = new Handler(Looper.getMainLooper());
        runnable = new Runnable() {
            @Override
            public void run() {
                if (bannerArrayList != null && !bannerArrayList.isEmpty()) {
                    currentPage = (currentPage + 1) % bannerArrayList.size();
                    viewPagerBanner.setCurrentItem(currentPage, true);
                }
                handler.postDelayed(this, 5000);
            }
        };
        handler.postDelayed(runnable, 5000);
    }

    private void setupBannerAdapter() {
        bannerAdapter = new BannerAdapter(getContext(), bannerArrayList);
        viewPagerBanner.setAdapter(bannerAdapter);

        viewPagerBanner.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                super.onPageSelected(position);
                currentPage = position;
                updateIndicator();
            }
        });
    }

    private void updateIndicator() {
        if (layoutIndicator == null) return;

        for (int i = 0; i < layoutIndicator.getChildCount(); i++) {
            ImageView imageView = (ImageView) layoutIndicator.getChildAt(i);
            imageView.setImageResource(
                    i == currentPage ? R.drawable.dot_active : R.drawable.dot_inactive
            );
        }
    }

    private void initViews(View view) {
        viewPagerBanner = view.findViewById(R.id.viewPagerBanner);
        layoutIndicator = view.findViewById(R.id.layoutIndicator);
        rcvCategory = view.findViewById(R.id.rcvCategory);
        rcvProduct = view.findViewById(R.id.rcvProduct);
        txtCategory = view.findViewById(R.id.txtCategory);
        txtViewAll = view.findViewById(R.id.txtViewAll);
        txtViewAllCategory = view.findViewById(R.id.txtViewAllCategory);
    }

}