package com.example.closethub.models;

public class WalletResponse {
    private String wallet_number;
    private double balance;
    private double new_balance; // Cho deposit/withdraw response
    private double amount; // Cho deposit/withdraw response
    private double total_deposits;
    private double total_withdrawals;
    private String create_date;
    private User id_user;

    public WalletResponse() {
    }

    public String getWallet_number() {
        return wallet_number;
    }

    public void setWallet_number(String wallet_number) {
        this.wallet_number = wallet_number;
    }

    public double getBalance() {
        return balance;
    }

    public void setBalance(double balance) {
        this.balance = balance;
    }

    public double getNew_balance() {
        return new_balance;
    }

    public void setNew_balance(double new_balance) {
        this.new_balance = new_balance;
    }

    public double getAmount() {
        return amount;
    }

    public void setAmount(double amount) {
        this.amount = amount;
    }

    public double getTotal_deposits() {
        return total_deposits;
    }

    public void setTotal_deposits(double total_deposits) {
        this.total_deposits = total_deposits;
    }

    public double getTotal_withdrawals() {
        return total_withdrawals;
    }

    public void setTotal_withdrawals(double total_withdrawals) {
        this.total_withdrawals = total_withdrawals;
    }

    public String getCreate_date() {
        return create_date;
    }

    public void setCreate_date(String create_date) {
        this.create_date = create_date;
    }

    public User getId_user() {
        return id_user;
    }

    public void setId_user(User id_user) {
        this.id_user = id_user;
    }
}

