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

        // Create a SimpleAdapter to map data from orderDetailsList to layout views
        // "productName" and "productQuantity" are keys in the map
        adapter = new SimpleAdapter(this, orderDetailsList, R.layout.order_details_list_item,
                new String[]{"productName", "productQuantity"}, // Keys in the map to retrieve data
                new int[]{R.id.productName, R.id.productQuantity});

        orderDetailsListView.setAdapter(adapter);

        // Retrieve the orderId passed via intent from the previous activity
        String orderId = getIntent().getStringExtra("orderId");
        if (orderId != null) {
            loadOrderDetails(orderId);
        }
    }

    // Method to load order details from Firebase for an orderId
    private void loadOrderDetails(String orderId) {
        // Create a reference to the specific order in Firebase under "Orders"
        DatabaseReference orderRef = FirebaseDatabase.getInstance().getReference("Orders").child(orderId);
        orderRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                orderDetailsList.clear(); // Clear the current list to refresh the data
                Log.d("OrderDetailsActivity", "Loading details for Order ID: " + orderId);

                for (DataSnapshot itemSnapshot : snapshot.getChildren()) {
                    // Skip metadata fields
                    if ("UserId".equals(itemSnapshot.getKey()) ||
                            "completed".equals(itemSnapshot.getKey()) ||
                            "timestamp".equals(itemSnapshot.getKey())) {
                        continue;
                    }

                    try {
                        // Parse the cart item
                        CartModel item = itemSnapshot.getValue(CartModel.class);
                        if (item != null) {
                            Map<String, String> orderDetail = new HashMap<>(); // Create a map to store the product details
                            orderDetail.put("productName", item.getName()); // Map the product name to the key "productName"
                            orderDetail.put("productQuantity", "כמות: " + item.getQuantity()); // Map the product quantity to the key "productQuantity"
                            orderDetailsList.add(orderDetail); // Add this map to the order details list
                        } else {
                            Log.e("OrderDetailsActivity", "CartModel is null for key: " + itemSnapshot.getKey());
                        }
                    } catch (Exception e) {
                        Log.e("OrderDetailsActivity", "Error parsing itemSnapshot: ", e);
                    }
                }

                // Update the adapter on the main thread
                runOnUiThread(() -> adapter.notifyDataSetChanged());
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                // Log and display an error message if loading order details fails
                Log.e("OrderDetailsActivity", "Failed to load order details: " + error.getMessage());
                Toast.makeText(OrderDetailsActivity.this, "Failed to load order details", Toast.LENGTH_SHORT).show();
            }
        });
    }

}
