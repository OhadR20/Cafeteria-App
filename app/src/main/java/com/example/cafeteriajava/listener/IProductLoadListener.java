package com.example.cafeteriajava.listener;

import com.example.cafeteriajava.model.ProductModel;

import java.util.List;

public interface IProductLoadListener {
    void onProductLoadSuccess(List<ProductModel> productModelList);
    void onProductLoadFailed(String message);

}
