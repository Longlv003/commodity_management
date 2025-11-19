package com.example.closethub.networks;

import com.example.closethub.models.ApiResponse;
import com.example.closethub.models.Banner;
import com.example.closethub.models.Bill;
import com.example.closethub.models.Cart;
import com.example.closethub.models.CartLookUpProduct;
import com.example.closethub.models.CartRequest;
import com.example.closethub.models.Category;
import com.example.closethub.models.LoginResponse;
import com.example.closethub.models.OrderRequest;
import com.example.closethub.models.Product;
import com.example.closethub.models.User;
import com.example.closethub.models.Transaction;
import com.example.closethub.models.WalletLoginRequest;
import com.example.closethub.models.WalletRequest;
import com.example.closethub.models.WalletResponse;
import com.example.closethub.models.WalletTransactionRequest;
import com.example.closethub.models.FavoriteRequest;
import com.example.closethub.models.FavoriteResponse;
import com.example.closethub.models.FavoriteCheckResponse;

import java.util.List;
import java.util.Objects;

import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.DELETE;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.Headers;
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

    @Multipart
    @PUT("/api/account/update/{id}")
    Call<ApiResponse<User>> UpdateProfileUserMultipart(
            @Header("Authorization") String token,
            @Path("id") String userId,
            @Part MultipartBody.Part image,
            @Part("email") RequestBody email,
            @Part("phone") RequestBody phone,
            @Part("name") RequestBody name,
            @Part("address") RequestBody address
    );

    @GET("/api/banner/sale/list")
    Call<ApiResponse<List<Banner>>> getBanner();

    @GET("/api/category/list")
    Call<ApiResponse<List<Category>>> getListCategory();

    @GET("/api/categories/top4")
    Call<ApiResponse<List<Category>>> getTopCategories();

    @GET("/api/product/list")
    Call<ApiResponse<List<Product>>> getListProduct(@Query("user_id") String userId);

    @GET("/api/product/list/top-selling")
    Call<ApiResponse<List<Product>>> GetTopSellingProducts(@Query("user_id") String userId);

    @GET("/api/product/list-by-cat")
    Call<ApiResponse<List<Product>>> getListProductByCat(@Query("catID") String catID, @Query("user_id") String userId);

    // Old favorite API (deprecated - using userFavorite API instead)
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

    // New User Favorite APIs
    @POST("/api/favorite/add")
    Call<ApiResponse<Object>> addFavorite(
            @Header("Authorization") String token,
            @Query("user_id") String userId,
            @Body FavoriteRequest request
    );

    @DELETE("/api/favorite/remove/{product_id}")
    Call<ApiResponse<Object>> removeFavorite(
            @Header("Authorization") String token,
            @Path("product_id") String productId,
            @Query("user_id") String userId
    );

    @POST("/api/favorite/toggle")
    Call<ApiResponse<FavoriteResponse>> toggleFavoriteNew(
            @Header("Authorization") String token,
            @Query("user_id") String userId,
            @Body FavoriteRequest request
    );

    @GET("/api/favorite/check/{product_id}")
    Call<ApiResponse<FavoriteCheckResponse>> checkFavorite(
            @Header("Authorization") String token,
            @Path("product_id") String productId,
            @Query("user_id") String userId
    );

    @GET("/api/favorite/list")
    Call<ApiResponse<List<Product>>> getUserFavorites(
            @Header("Authorization") String token,
            @Query("user_id") String userId
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

    @POST("/api/order")
    Call<ApiResponse<Bill>> placeOrder(
            @Header("Authorization") String token,
            @Body OrderRequest orderRequest
    );

    @GET("/api/order/history/{id_user}")
    Call<ApiResponse<List<Bill>>> getBills(@Path("id_user") String userId);

    @Multipart
    @PUT("/api/account/{id}/upload/avatar")
    Call<ApiResponse<User>> uploadAvatar(
            @Header("Authorization") String token,
            @Path("id") String userId,
            @Part MultipartBody.Part image
    );

    @GET("/api/product/{id}")
    Call<ApiResponse<Product>> getProductDetail(@Path("id") String id);

    // Wallet APIs
    @POST("/api/wallet/create")
    Call<ApiResponse<WalletResponse>> createWallet(
            @Header("Authorization") String token,
            @Body WalletRequest walletRequest
    );

    @POST("/api/wallet/login")
    @Headers("Content-Type: application/json")
    Call<ApiResponse<Object>> loginWallet(
            @Header("Authorization") String token,
            @Body WalletLoginRequest walletLoginRequest
    );

    @GET("/api/wallet/info")
    Call<ApiResponse<WalletResponse>> getWalletInfo(
            @Header("Authorization") String token
    );

    @POST("/api/wallet/deposit")
    Call<ApiResponse<WalletResponse>> depositWallet(
            @Header("Authorization") String token,
            @Body WalletTransactionRequest request
    );

    @POST("/api/wallet/withdraw")
    Call<ApiResponse<WalletResponse>> withdrawWallet(
            @Header("Authorization") String token,
            @Body WalletTransactionRequest request
    );

    @GET("/api/wallet/history")
    Call<ApiResponse<List<Transaction>>> getWalletHistory(
            @Header("Authorization") String token
    );

}
