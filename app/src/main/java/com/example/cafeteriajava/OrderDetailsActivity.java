package com.example.cafeteriajava;

import android.os.Bundle;
import android.util.Log;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.example.cafeteriajava.model.CartModel;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class OrderDetailsActivity extends AppCompatActivity {

    private ListView orderDetailsListView;
    private List<Map<String, String>> orderDetailsList = new ArrayList<>();
    private SimpleAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_order_details);

        orderDetailsListView = findViewById(R.id.orderDetailsListView);

        adapter = new SimpleAdapter(this, orderDetailsList, R.layout.order_details_list_item,
                new String[]{"productName", "productQuantity"},
                new int[]{R.id.productName, R.id.productQuantity});

        orderDetailsListView.setAdapter(adapter);

        String orderId = getIntent().getStringExtra("orderId");
        if (orderId != null) {
            loadOrderDetails(orderId);
        }
    }

    private void loadOrderDetails(String orderId) {
        DatabaseReference orderRef = FirebaseDatabase.getInstance().getReference("Orders").child(orderId);
        orderRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                orderDetailsList.clear();
                Log.d("OrderDetailsActivity", "Loading details for Order ID: " + orderId);

                for (DataSnapshot itemSnapshot : snapshot.getChildren()) {

                    // Skip the "completed" field or any non-Map data
                    if (itemSnapshot.getKey().equals("completed") || !(itemSnapshot.getValue() instanceof Map)) {
                        Log.d("OrderDetailsActivity", "Skipping non-CartModel data: " + itemSnapshot.getKey());
                        continue; // Skip this iteration
                    }

                    try {
                        // Try to convert the snapshot to CartModel
                        CartModel item = itemSnapshot.getValue(CartModel.class);
                        if (item != null) {
                            Map<String, String> orderDetail = new HashMap<>();
                            orderDetail.put("productName", item.getName());
                            orderDetail.put("productQuantity", "כמות: " + item.getQuantity());
                            orderDetailsList.add(orderDetail);
                        } else {
                            Log.e("OrderDetailsActivity", "CartModel is null for key: " + itemSnapshot.getKey());
                        }
                    } catch (Exception e) {
                        Log.e("OrderDetailsActivity", "Error parsing itemSnapshot: ", e);
                    }
                }

                // Update the adapter on the main thread
                runOnUiThread(() -> {
                    adapter.notifyDataSetChanged();
                });
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e("OrderDetailsActivity", "Failed to load order details: " + error.getMessage());
                Toast.makeText(OrderDetailsActivity.this, "Failed to load order details", Toast.LENGTH_SHORT).show();
            }
        });
    }
}
