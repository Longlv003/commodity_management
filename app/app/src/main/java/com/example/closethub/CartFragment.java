package com.example.closethub;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;

import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.example.closethub.adapter.CartAdapter;
import com.example.closethub.models.ApiResponse;
import com.example.closethub.models.CartLookUpProduct;
import com.example.closethub.models.OrderRequest;
import com.example.closethub.networks.ApiService;
import com.example.closethub.networks.RetrofitClient;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

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
    private TextView txtAmount, txtQuantity, txtTotalAmount, txtShip, txtTitle;
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

        SharedPreferences sharedPref = getContext().getSharedPreferences("LoginPrefs", Context.MODE_PRIVATE);
        String idUser = sharedPref.getString("id_user", null);
        String token_user = sharedPref.getString("token", null);

        productArrayList = new ArrayList<>();
        //rcvCategory.setLayoutManager(new LinearLayoutManager(this));
        rcvProductList.setLayoutManager(
                new LinearLayoutManager(getContext())
        );

        cartAdapter = new CartAdapter(getContext(), productArrayList);
        cartAdapter.setOnQuantityChangeListener(() -> {
            updateQuantitySummary();
        });
        rcvProductList.setAdapter(cartAdapter);

        if(idUser == null) {
            Toast.makeText(getContext(), "Vui lòng đăng nhập để thêm vào giỏ hàng", Toast.LENGTH_SHORT).show();
        }
        GetListProduct(idUser);

        btnPay.setOnClickListener(v -> {
            if (productArrayList.isEmpty()) {
                Toast.makeText(getContext(), "Giỏ hàng trống!", Toast.LENGTH_SHORT).show();
                return;
            }

            EditText edtAddress = new EditText(getContext());
            edtAddress.setHint("Nhập địa chỉ giao hàng");

            AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
            builder.setTitle("Xác nhận thanh toán")
                    .setMessage("Vui lòng nhập địa chỉ giao hàng")
                    .setView(edtAddress)
                    .setPositiveButton("Thanh toán", (dialog, which) -> {
                        String address = edtAddress.getText().toString().trim();
                        if (address.isEmpty()) {
                            Toast.makeText(getContext(), "Bạn chưa nhập địa chỉ", Toast.LENGTH_SHORT).show();
                            return;
                        }

                        if(idUser == null) {
                            Toast.makeText(getContext(), "Vui lòng đăng nhập để thêm vào giỏ hàng", Toast.LENGTH_SHORT).show();
                            return;
                        }

                        OrderRequest orderRequest = new OrderRequest();
                        orderRequest.setId_user(idUser);
                        orderRequest.setAddress(address);
                        PayCart(token_user, orderRequest);
                        //PayCart(token_user, id_user, address);
                    })
                    .setNegativeButton("Hủy", (dialog, which) -> dialog.dismiss())
                    .setCancelable(false); // Không tắt khi bấm ra ngoài

            AlertDialog alertDialog = builder.create();
            alertDialog.show();
        });

        return view;
    }

    private void PayCart(String tokenUser, OrderRequest orderRequest) {
        apiService.PayCart(tokenUser, orderRequest).enqueue(new Callback<ApiResponse<Objects>>() {
            @Override
            public void onResponse(Call<ApiResponse<Objects>> call, Response<ApiResponse<Objects>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    Toast.makeText(getContext(), "Thanh toán thành công!", Toast.LENGTH_SHORT).show();
                    productArrayList.clear();
                    cartAdapter.notifyDataSetChanged();
                    updateQuantitySummary();
                } else {
                    Toast.makeText(getContext(), "Sản phẩm hết hàng!", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<Objects>> call, Throwable throwable) {
                Toast.makeText(getContext(), "Lỗi mạng: " + throwable.getMessage(), Toast.LENGTH_SHORT).show();
                Log.e("Error", "Pay", throwable);
            }
        });
    }

//    private void PayCart(String token, String id,String address) {
//        apiService.PayCart(token, id, address).enqueue(new Callback<ApiResponse<Objects>>() {
//            @Override
//            public void onResponse(Call<ApiResponse<Objects>> call, Response<ApiResponse<Objects>> response) {
//                if (response.isSuccessful() && response.body() != null) {
//                    Toast.makeText(getContext(), "Thanh toán thành công!", Toast.LENGTH_SHORT).show();
//                    productArrayList.clear();
//                    cartAdapter.notifyDataSetChanged();
//                    updateQuantitySummary();
//                } else {
//                    Toast.makeText(getContext(), "Thanh toán thất bại!", Toast.LENGTH_SHORT).show();
//                }
//            }
//
//            @Override
//            public void onFailure(Call<ApiResponse<Objects>> call, Throwable throwable) {
//                Toast.makeText(getContext(), "Lỗi mạng: " + throwable.getMessage(), Toast.LENGTH_SHORT).show();
//                Log.e("Error", "Pay", throwable);
//            }
//        });
//    }


    private void GetListProduct(String id) {
        apiService.getListProductMyCart(id).enqueue(new Callback<ApiResponse<List<CartLookUpProduct>>>() {
            @Override
            public void onResponse(Call<ApiResponse<List<CartLookUpProduct>>> call, Response<ApiResponse<List<CartLookUpProduct>>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    productArrayList.clear();
                    productArrayList.addAll(response.body().getData());
                    cartAdapter.notifyDataSetChanged();
                    updateQuantitySummary();
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
            totalAmount += item.getQuantity() * item.getId_product().getMin_price();
        }

        txtQuantity.setText(String.valueOf(totalQuantity));

        NumberFormat formatter = NumberFormat.getInstance(new Locale("vi", "VN"));
        txtAmount.setText(formatter.format(totalAmount) + " ₫");

        double shipFee = 15000; // ví dụ
        if (productArrayList.size() == 0) {
            txtTitle.setText("");
            shipFee = 0;
            btnPay.setEnabled(false);
            btnPay.setAlpha(0.5f);
        }

        txtShip.setText(formatter.format(shipFee) + " ₫");

        txtTotalAmount.setText(formatter.format(totalAmount + shipFee) + " ₫");
    }

    private void initViews(View view) {
        txtTitle = view.findViewById(R.id.txtTitle);
        rcvProductList = view.findViewById(R.id.rcvProductCart);
        txtAmount = view.findViewById(R.id.txtAmount);
        txtQuantity = view.findViewById(R.id.txtQuantity);
        txtTotalAmount = view.findViewById(R.id.txtTotalAmount);
        txtShip = view.findViewById(R.id.txtShip);
        btnPay = view.findViewById(R.id.btnPay);
    }
}