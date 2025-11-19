package com.example.closethub.models;

public class FavoriteResponse {
    private boolean is_favorite;
    private String product_id;

    public FavoriteResponse() {
    }

    public boolean isIs_favorite() {
        return is_favorite;
    }

    public void setIs_favorite(boolean is_favorite) {
        this.is_favorite = is_favorite;
    }

    public String getProduct_id() {
        return product_id;
    }

    public void setProduct_id(String product_id) {
        this.product_id = product_id;
    }
}

