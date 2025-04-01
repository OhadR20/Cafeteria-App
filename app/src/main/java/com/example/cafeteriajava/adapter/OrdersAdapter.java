package com.example.cafeteriajava.adapter;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.cafeteriajava.OrderDetailsActivity;
import com.example.cafeteriajava.R;
import com.example.cafeteriajava.model.OrderModel;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.List;

public class OrdersAdapter extends RecyclerView.Adapter<OrdersAdapter.OrderViewHolder> {

    private Context context;
    private List<OrderModel> orderList; // List of OrderModel objects representing orders fetched from Firebase

    public OrdersAdapter(Context context, List<OrderModel> orderList) {
        this.context = context;
        this.orderList = orderList;
    }

    @NonNull
    @Override
    public OrderViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.order_item, parent, false);
        return new OrderViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull OrderViewHolder holder, int position) {
        // Get the order at the current position
        OrderModel order = orderList.get(position);

        // Fetch the user first name and display it
        fetchUserFirstName(order.getOrderId(), holder.tvOrderId);

        holder.tvTotalPrice.setText(String.valueOf(order.getTotalPrice()));

        // Fetch the "completed" field directly from Firebase
        DatabaseReference orderRef = FirebaseDatabase.getInstance().getReference("Orders").child(order.getOrderId()).child("completed");
        orderRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    int completed = snapshot.getValue(Integer.class);
                    // If completed equals 1, mark order with a green background; else red
                    if (completed == 1) {
                        holder.itemView.setBackgroundColor(context.getResources().getColor(android.R.color.holo_green_light)); // Green
                    } else {
                        holder.itemView.setBackgroundColor(context.getResources().getColor(android.R.color.holo_red_light)); // Red
                    }
                } else {
                    holder.itemView.setBackgroundColor(context.getResources().getColor(android.R.color.holo_red_light)); // Default to red if "completed" is missing
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                holder.itemView.setBackgroundColor(context.getResources().getColor(android.R.color.holo_red_light)); // Handle errors gracefully
            }
        });

        holder.itemView.setOnClickListener(v -> {
            Intent intent = new Intent(context, OrderDetailsActivity.class);
            // Pass the orderId to the details activity
            intent.putExtra("orderId", order.getOrderId());
            context.startActivity(intent);
        });

        holder.btnMarkComplete.setOnClickListener(v -> markOrderAsComplete(order.getOrderId()));

        holder.btnDeleteOrder.setOnClickListener(v -> deleteOrder(order.getOrderId(), position));
    }


    @Override
    public int getItemCount() {
        // Return the total number of orders to be displayed
        return orderList.size();
    }

    // Method to fetch the user's first name using the order's UserId and display it in tvOrderId
    private void fetchUserFirstName(String orderId, TextView tvOrderId) {
        // Reference to the UserId field in the order data
        DatabaseReference orderRef = FirebaseDatabase.getInstance().getReference("Orders").child(orderId).child("UserId");

        orderRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    String userId = snapshot.getValue(String.class);

                    if (userId != null) {
                        // Fetch the user's first name using the UserId
                        DatabaseReference userRef = FirebaseDatabase.getInstance().getReference("users").child(userId).child("firstname");
                        userRef.addListenerForSingleValueEvent(new ValueEventListener() {
                            @Override
                            public void onDataChange(@NonNull DataSnapshot userSnapshot) {
                                if (userSnapshot.exists()) {
                                    String firstName = userSnapshot.getValue(String.class);
                                    tvOrderId.setText(firstName); // Set the first name to tvOrderId
                                } else {
                                    tvOrderId.setText("Unknown User"); // Handle case where firstname is not found
                                }
                            }

                            @Override
                            public void onCancelled(@NonNull DatabaseError error) {
                                tvOrderId.setText("Error"); // Handle errors
                            }
                        });
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                tvOrderId.setText("Error"); // Handle errors
            }
        });
    }

    // Method to mark an order as complete in Firebase
    private void markOrderAsComplete(String orderId) {
        // Reference the global order using the orderId
        DatabaseReference orderRef = FirebaseDatabase.getInstance().getReference("Orders").child(orderId);
        // Reference to the users to update the order in the user's orders list
        DatabaseReference userOrdersRef = FirebaseDatabase.getInstance().getReference("users");

        // Set the "completed" field to 1 (completed) in the global order node
        orderRef.child("completed").setValue(1).addOnSuccessListener(aVoid -> {
            // Fetch the UserId for this order to update the user's order data
            orderRef.child("UserId").addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    if (snapshot.exists()) {
                        String userId = snapshot.getValue(String.class);
                        // Update the "completed" status in the user's orders list
                        userOrdersRef.child(userId)
                                .child("orders")
                                .child(orderId)
                                .child("completed").setValue(1)
                                .addOnSuccessListener(aVoid1 -> Toast.makeText(context, "Order marked as completed!", Toast.LENGTH_SHORT).show())
                                .addOnFailureListener(e -> Toast.makeText(context, "Failed to update user order", Toast.LENGTH_SHORT).show());
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    Toast.makeText(context, "Failed to retrieve UserId", Toast.LENGTH_SHORT).show();
                }
            });
        }).addOnFailureListener(e -> Toast.makeText(context, "Failed to update global order", Toast.LENGTH_SHORT).show());
    }

    // Method to delete an order from Firebase and update the local list
    private void deleteOrder(String orderId, int position) {
        // Reference the global order node to be deleted
        DatabaseReference orderRef = FirebaseDatabase.getInstance().getReference("Orders").child(orderId);

        orderRef.removeValue().addOnSuccessListener(aVoid -> {
            // If the deletion is successful, remove the order from the local list and update the RecyclerView
            if (position >= 0 && position < orderList.size()) {
                orderList.remove(position);
                notifyItemRemoved(position);
                notifyItemRangeChanged(position, orderList.size()); // Optional, ensures the list stays updated
            }

            // Show a Toast message based on whether orders remain in the list
            if (orderList.isEmpty()) {
                Toast.makeText(context, "No more orders to display", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(context, "Order deleted successfully", Toast.LENGTH_SHORT).show();
            }
        }).addOnFailureListener(e -> {
            Toast.makeText(context, "Failed to delete order", Toast.LENGTH_SHORT).show();
        });
    }


    public static class OrderViewHolder extends RecyclerView.ViewHolder {

        TextView tvOrderId, tvTotalPrice;
        Button btnMarkComplete, btnDeleteOrder;

        public OrderViewHolder(@NonNull View itemView) {
            super(itemView);
            tvOrderId = itemView.findViewById(R.id.tvOrderId);
            tvTotalPrice = itemView.findViewById(R.id.tvTotalPrice);
            btnMarkComplete = itemView.findViewById(R.id.btnMarkComplete);
            btnDeleteOrder = itemView.findViewById(R.id.btnDeleteOrder);
        }
    }
}
