package com.example.closethub.networks;

import com.example.closethub.models.ApiResponse;
import com.example.closethub.models.Banner;
import com.example.closethub.models.Category;
import com.example.closethub.models.Product;
import com.example.closethub.models.User;

import java.util.List;
import java.util.Objects;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.DELETE;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.PUT;
import retrofit2.http.Path;
import retrofit2.http.Query;

public interface ApiService {
    @POST("/api/account/register")
    Call<ApiResponse<User>> getRegister(@Body User user);

    @POST("/api/account/login")
    Call<ApiResponse<User>> getLogin(@Body User user);

    @GET("/api/banner/sale/list")
    Call<ApiResponse<List<Banner>>> getBanner();

    @GET("/api/category/list")
    Call<ApiResponse<List<Category>>> getListCategory();

    @GET("/api/product/list")
    Call<ApiResponse<List<Product>>> getListProduct();

    @GET("/api/product/list-by-cat")
    Call<ApiResponse<List<Product>>> getListProductByCat(@Query("catID") String catID);
}
