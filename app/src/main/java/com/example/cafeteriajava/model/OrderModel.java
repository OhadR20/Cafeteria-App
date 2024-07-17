package com.example.cafeteriajava.model;

import java.util.List;

public class OrderModel {
    private String orderId;
    private double totalPrice;
    private List<CartModel> cartItems;

    public OrderModel(String orderId, double totalPrice, List<CartModel> cartItems) {
        this.orderId = orderId;
        this.totalPrice = totalPrice;
        this.cartItems = cartItems;
    }

    public String getOrderId() {
        return orderId;
    }

    public void setOrderId(String orderId) {
        this.orderId = orderId;
    }

    public double getTotalPrice() {
        return totalPrice;
    }

    public void setTotalPrice(double totalPrice) {
        this.totalPrice = totalPrice;
    }

    public List<CartModel> getCartItems() {
        return cartItems;
    }

    public void setCartItems(List<CartModel> cartItems) {
        this.cartItems = cartItems;
    }
}
