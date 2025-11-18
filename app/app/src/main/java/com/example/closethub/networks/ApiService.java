package com.example.closethub.networks;

import com.example.closethub.models.ApiResponse;
import com.example.closethub.models.Banner;
import com.example.closethub.models.Bill;
import com.example.closethub.models.Cart;
import com.example.closethub.models.CartLookUpProduct;
import com.example.closethub.models.CartRequest;
import com.example.closethub.models.Category;
import com.example.closethub.models.LoginResponse;
import com.example.closethub.models.Product;
import com.example.closethub.models.User;

import java.util.List;
import java.util.Objects;

import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.DELETE;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.Multipart;
import retrofit2.http.POST;
import retrofit2.http.PUT;
import retrofit2.http.Part;
import retrofit2.http.Path;
import retrofit2.http.Query;

public interface ApiService {
    @POST("/api/account/register")
    Call<ApiResponse<User>> getRegister(@Body User user);

    @POST("/api/account/login")
    Call<ApiResponse<LoginResponse>> getLogin(@Body User user);

    @GET("/api/banner/sale/list")
    Call<ApiResponse<List<Banner>>> getBanner();

    @GET("/api/category/list")
    Call<ApiResponse<List<Category>>> getListCategory();

    @GET("/api/categories/top4")
    Call<ApiResponse<List<Category>>> getTopCategories();

    @GET("/api/product/list")
    Call<ApiResponse<List<Product>>> getListProduct();

    @GET("/api/product/list/top-selling")
    Call<ApiResponse<List<Product>>> GetTopSellingProducts();

    @GET("/api/product/list-by-cat")
    Call<ApiResponse<List<Product>>> getListProductByCat(@Query("catID") String catID);

    @PUT("/api/product/{id}/edit/favorite/{favorite}")
    Call<ApiResponse<Product>> toggleFavorite(
            @Header("Authorization") String token,
            @Path("id") String id,
            @Path("favorite") boolean favorite
    );

    @GET("/api/product/list/favorite")
    Call<ApiResponse<List<Product>>> getFavoriteProducts(
            @Header("Authorization") String token
    );

    @POST("/api/cart/add")
    Call<ApiResponse<Cart>> addToCart(
            @Header("Authorization") String token,
            @Body CartRequest cartRequest
    );

    @GET("/api/cart/list/{id_user}")
    Call<ApiResponse<List<CartLookUpProduct>>> getCartList(
            @Path("id_user") String userId
    );

    @PUT("/api/cart/update/{id}")
    Call<ApiResponse<CartLookUpProduct>> updateCartQuantity(
            @Header("Authorization") String token,
            @Path("id") String id,
            @Query("quantity") int quantity
    );

    @DELETE("/api/cart/delete/{id}")
    Call<ApiResponse<Cart>> deleteCartItem(
            @Header("Authorization") String token,
            @Path("id") String id
    );

//    @POST("/api/order")
//    Call<ApiResponse<Objects>> PayCart(
//            @Header("Authorization") String token,
//            @Body OrderRequest orderRequest
//    );

    @GET("/api/order/history/{id_user}")
    Call<ApiResponse<List<Bill>>> getBills(@Path("id_user") String userId);

    @Multipart
    @PUT("/api/account/update/{id}")
    Call<ApiResponse<User>> UpdateProfileUserMultipart(
            @Header("Authorization") String token,
            @Path("id") String userId,
            @Part MultipartBody.Part image,
            @Part("email") RequestBody email,
            @Part("phone") RequestBody phone
    );

    @Multipart
    @PUT("/api/account/{id}/upload/avatar")
    Call<ApiResponse<User>> uploadAvatar(
            @Header("Authorization") String token,
            @Path("id") String userId,
            @Part MultipartBody.Part image
    );

    @GET("/api/product/{id}")
    Call<ApiResponse<Product>> getProductDetail(@Path("id") String id);

}
