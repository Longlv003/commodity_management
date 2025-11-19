package com.example.closethub.models;

public class FavoriteRequest {
    private String product_id;

    public FavoriteRequest() {
    }

    public FavoriteRequest(String product_id) {
        this.product_id = product_id;
    }

    public String getProduct_id() {
        return product_id;
    }

    public void setProduct_id(String product_id) {
        this.product_id = product_id;
    }
}

