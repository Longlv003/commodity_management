package com.example.closethub.models;

public class User {
    private String _id;
    private String email;
    private String pass;
    private String role;
    private Boolean is_active;
    //private Boolean is_delete;
    private String phone;
    private String image;

    public String get_id() {
        return _id;
    }

    public void set_id(String _id) {
        this._id = _id;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPass() {
        return pass;
    }

    public void setPass(String pass) {
        this.pass = pass;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public Boolean getIs_active() {
        return is_active;
    }

    public void setIs_active(Boolean is_active) {
        this.is_active = is_active;
    }

//    public Boolean getIs_delete() {
//        return is_delete;
//    }
//
//    public void setIs_delete(Boolean is_delete) {
//        this.is_delete = is_delete;
//    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getImage() {
        return image;
    }

    public void setImage(String image) {
        this.image = image;
    }
}
