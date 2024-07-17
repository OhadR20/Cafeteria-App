package com.example.cafeteriajava.model;

public class ProductModel {
    private String key, name, img, price, available;

    public ProductModel() {
    }

    public String getKey() {
        return key;
    }
    public void setKey(String key) {
        this.key = key;
    }


    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getImg() {
        return img;
    }
    public void setImage(String image) {
        this.img = image;
    }

    public String getPrice() {
        return price;
    }
    public void setPrice(String price) {
        this.price = price;
    }

    public String getAvailable(){return available;}
    public void setAvailable(String available){this.available = available;}
}
