package com.example.closethub;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;

import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.example.closethub.adapter.ProductAdapter;
import com.example.closethub.models.ApiResponse;
import com.example.closethub.models.Product;
import com.example.closethub.networks.ApiService;
import com.example.closethub.networks.RetrofitClient;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link FavoriteFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class FavoriteFragment extends Fragment {

    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";

    // TODO: Rename and change types of parameters
    private String mParam1;
    private String mParam2;

    public FavoriteFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param param1 Parameter 1.
     * @param param2 Parameter 2.
     * @return A new instance of fragment FavoriteFragment.
     */
    // TODO: Rename and change types and number of parameters
    public static FavoriteFragment newInstance(String param1, String param2) {
        FavoriteFragment fragment = new FavoriteFragment();
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
    private RecyclerView rcvCategory, rcvProduct;
    private ApiService apiService = RetrofitClient.getApiService();
    private ArrayList<Product> productArrayList;
    private ProductAdapter productAdapter;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_favorite, container, false);
        initUI(view);

        productArrayList = new ArrayList<>();
        rcvProduct.setLayoutManager(
                new GridLayoutManager(getContext(), 2)
        );
        productAdapter = new ProductAdapter(getContext(), productArrayList);
        
        // Set callback để reload danh sách khi favorite được toggle
        productAdapter.setOnFavoriteToggleListener(isFavorite -> {
            if (!isFavorite) {
                // Nếu bỏ favorite, reload danh sách để xóa sản phẩm khỏi danh sách
                SharedPreferences sharedPref = getContext().getSharedPreferences("LoginPrefs", Context.MODE_PRIVATE);
                String token_user = sharedPref.getString("token", null);
                GetListFavorite(token_user);
            }
        });
        
        rcvProduct.setAdapter(productAdapter);

        SharedPreferences sharedPref = getContext().getSharedPreferences("LoginPrefs", Context.MODE_PRIVATE);
        String idUser = sharedPref.getString("id_user", null);
        String token_user = sharedPref.getString("token", null);

        GetListFavorite(token_user);

        return view;
    }

    private void GetListFavorite(String tokenUser) {
        SharedPreferences sharedPref = getContext().getSharedPreferences("LoginPrefs", Context.MODE_PRIVATE);
        String idUser = sharedPref.getString("id_user", null);

        if (idUser == null) {
            Log.e("Error", "User ID not found");
            return;
        }

        // Sử dụng API mới: getUserFavorites
        apiService.getUserFavorites("Bearer " + tokenUser, idUser).enqueue(new Callback<ApiResponse<List<Product>>>() {
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

    private void initUI (View view) {
        //imgBack = view.findViewById(R.id.imgBack);
        rcvProduct = view.findViewById(R.id.rcvProduct);
    }
}