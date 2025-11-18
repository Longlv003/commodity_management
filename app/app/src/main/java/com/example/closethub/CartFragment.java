package com.example.closethub;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;

import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.example.closethub.adapter.CartAdapter;
import com.example.closethub.models.ApiResponse;
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

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link CartFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class CartFragment extends Fragment {

    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";

    // TODO: Rename and change types of parameters
    private String mParam1;
    private String mParam2;

    public CartFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param param1 Parameter 1.
     * @param param2 Parameter 2.
     * @return A new instance of fragment CartFragment.
     */
    // TODO: Rename and change types and number of parameters
    public static CartFragment newInstance(String param1, String param2) {
        CartFragment fragment = new CartFragment();
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

    private RecyclerView rcvProductList;
    private TextView txtAmount, txtQuantity, txtTotalAmount, txtTitle;

    private Button btnPay;
    private ApiService apiService;
    private ArrayList<CartLookUpProduct> productArrayList;
    private CartAdapter cartAdapter;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_cart, container, false);
        apiService = RetrofitClient.getApiService();
        initViews(view);

        SharedPreferences prefs = getContext().getSharedPreferences("LoginPrefs", Context.MODE_PRIVATE);
        String userJson = prefs.getString("user_data", null);

        if (userJson == null) {
            Toast.makeText(getContext(), "account found", Toast.LENGTH_SHORT).show();
        }

        Gson gson = new Gson();
        User user = gson.fromJson(userJson, User.class);

        productArrayList = new ArrayList<>();
        rcvProductList.setLayoutManager(
                new LinearLayoutManager(getContext())
        );

        cartAdapter = new CartAdapter(getContext(), productArrayList);
        cartAdapter.setOnQuantityChangeListener(() -> {
            updateQuantitySummary();
        });
        rcvProductList.setAdapter(cartAdapter);

        if(user.get_id() == null) {
            Toast.makeText(getContext(), "Vui lòng đăng nhập để thêm vào giỏ hàng", Toast.LENGTH_SHORT).show();
        }
        GetListProduct(user.get_id());

        btnPay.setOnClickListener(v -> {
            startActivity(new Intent(getContext(), PayActivity.class));
        });

        return view;
    }

    private void GetListProduct(String id) {
        apiService.getCartList(id).enqueue(new Callback<ApiResponse<List<CartLookUpProduct>>>() {
            @Override
            public void onResponse(Call<ApiResponse<List<CartLookUpProduct>>> call, Response<ApiResponse<List<CartLookUpProduct>>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    productArrayList.clear();
                    List<CartLookUpProduct> data = response.body().getData();
                    if (data == null || data.isEmpty()) {
                        Log.e("CART", "No data returned from API");
                        return;
                    }

                    productArrayList.addAll(data);
                    cartAdapter.notifyDataSetChanged();
                    updateQuantitySummary();
                } else {
                    Toast.makeText(getContext(), "...", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<List<CartLookUpProduct>>> call, Throwable throwable) {
                Toast.makeText(getContext(), "Error: " +throwable.getMessage() , Toast.LENGTH_SHORT).show();
                Log.e("Error", "ProductMyCart Failed", throwable);
            }
        });
    }

    private void updateQuantitySummary() {
        int totalQuantity = 0;
        double totalAmount = 0;

        for (CartLookUpProduct item : productArrayList) {
            totalQuantity += item.getQuantity();
            totalAmount += item.getQuantity() * item.getId_variant().getPrice();
        }

        txtQuantity.setText(String.valueOf(totalQuantity));

        NumberFormat formatter = NumberFormat.getInstance(new Locale("vi", "VN"));
        txtAmount.setText(formatter.format(totalAmount) + " ₫");

        txtTotalAmount.setText(formatter.format(totalAmount) + " ₫");
    }

    private void initViews(View view) {
        txtTitle = view.findViewById(R.id.txtTitle);
        rcvProductList = view.findViewById(R.id.rcvProductCart);
        txtAmount = view.findViewById(R.id.txtAmount);
        txtQuantity = view.findViewById(R.id.txtQuantity);
        txtTotalAmount = view.findViewById(R.id.txtTotalAmount);

        btnPay = view.findViewById(R.id.btnPay);
    }
}