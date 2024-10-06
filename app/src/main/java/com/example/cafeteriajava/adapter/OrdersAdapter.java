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
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.List;

public class OrdersAdapter extends RecyclerView.Adapter<OrdersAdapter.OrderViewHolder> {

    private Context context;
    private List<OrderModel> orderList;

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
        OrderModel order = orderList.get(position);
        holder.tvOrderId.setText(order.getOrderId());
        holder.tvTotalPrice.setText(String.valueOf(order.getTotalPrice()));


        holder.itemView.setOnClickListener(v -> {
            Intent intent = new Intent(context, OrderDetailsActivity.class);
            intent.putExtra("orderId", order.getOrderId());
            context.startActivity(intent);
        });

        // Set up the "Complete" button to update the completed field in Firebase
        holder.btnMarkComplete.setOnClickListener(v -> {
            markOrderAsComplete(order.getOrderId());
        });

        // Set up the "Delete" button to delete the order
        holder.btnDeleteOrder.setOnClickListener(v -> {
            deleteOrder(order.getOrderId(), position);
        });

    }

    @Override
    public int getItemCount() {
        return orderList.size();
    }

    // Method to update the "completed" field in Firebase
    private void markOrderAsComplete(String orderId) {
        DatabaseReference orderRef = FirebaseDatabase.getInstance().getReference("Orders").child(orderId);
        orderRef.child("completed").setValue(1).addOnSuccessListener(aVoid -> {
            Toast.makeText(context, "Order marked as completed!", Toast.LENGTH_SHORT).show();
        }).addOnFailureListener(e -> {
            Toast.makeText(context, "Failed to update order", Toast.LENGTH_SHORT).show();
        });
    }

    // Method to delete the order from Firebase
    private void deleteOrder(String orderId, int position) {
        DatabaseReference orderRef = FirebaseDatabase.getInstance().getReference("Orders").child(orderId);
        orderRef.removeValue().addOnSuccessListener(aVoid -> {
            Toast.makeText(context, "Order deleted successfully", Toast.LENGTH_SHORT).show();
            // Remove the order from the local list and notify the adapter
            orderList.remove(position);
            notifyItemRemoved(position);
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
            btnDeleteOrder = itemView.findViewById(R.id.btnDeleteOrder); // Initialize delete button

        }
    }
}
