package com.example.cafeteriajava;

import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.cafeteriajava.adapter.OrdersAdapter;
import com.example.cafeteriajava.model.CartModel;
import com.example.cafeteriajava.model.OrderModel;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ViewOrdersActivity extends AppCompatActivity {

    private RecyclerView ordersRecyclerView;
    private OrdersAdapter ordersAdapter;
    private List<OrderModel> orderList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_view_orders);

        ordersRecyclerView = findViewById(R.id.ordersRecyclerView);
        ordersRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        orderList = new ArrayList<>();
        ordersAdapter = new OrdersAdapter(this, orderList);
        ordersRecyclerView.setAdapter(ordersAdapter);

        loadOrdersFromFirebase();
    }

    private void loadOrdersFromFirebase() {
        FirebaseDatabase.getInstance()
                .getReference("Orders")
                .orderByChild("timestamp") // Order by timestamp
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        orderList.clear();
                        for (DataSnapshot orderSnapshot : snapshot.getChildren()) {
                            String orderId = orderSnapshot.getKey();
                            double totalPrice = 0;
                            List<CartModel> cartItems = new ArrayList<>();
                            for (DataSnapshot itemSnapshot : orderSnapshot.getChildren()) {
                                // Avoid parsing non-cart-item nodes like "UserId", "timestamp", etc.
                                if (itemSnapshot.getValue() instanceof Map) {
                                    try {
                                        CartModel cartModel = itemSnapshot.getValue(CartModel.class);
                                        if (cartModel != null) {
                                            cartModel.setKey(itemSnapshot.getKey());
                                            totalPrice += cartModel.getTotalPrice();
                                            cartItems.add(cartModel);
                                        }
                                    } catch (Exception e) {
                                        Log.e("FirebaseError", "Error converting data", e);
                                    }
                                }
                            }
                            OrderModel orderModel = new OrderModel(orderId, totalPrice, cartItems);
                            orderList.add(orderModel);
                        }
                        ordersAdapter.notifyDataSetChanged();
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Toast.makeText(ViewOrdersActivity.this, "Failed to load orders", Toast.LENGTH_SHORT).show();
                        Log.e("ViewOrdersActivity", error.getMessage());
                    }
                });
    }


}
