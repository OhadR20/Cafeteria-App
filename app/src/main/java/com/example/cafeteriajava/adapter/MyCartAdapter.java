package com.example.cafeteriajava.adapter;

import android.app.AlertDialog;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.cafeteriajava.R;
import com.example.cafeteriajava.eventbus.MyUpdateCartEvent;
import com.example.cafeteriajava.model.CartModel;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.auth.FirebaseAuth;

import org.greenrobot.eventbus.EventBus;

import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.Unbinder;

public class MyCartAdapter extends RecyclerView.Adapter<MyCartAdapter.MyCartViewHolder> {

    private Context context;
    private List<CartModel> cartModelList; // List holding all cart items to be displayed
    private String currentuser = FirebaseAuth.getInstance().getCurrentUser().getUid(); // Retrieve the current user's unique ID using FirebaseAuth

    // Constructor for MyCartAdapter that takes the context and list of cart items
    public MyCartAdapter(Context context, List<CartModel> cartModelList) {
        this.context = context;
        this.cartModelList = cartModelList;
    }

    @NonNull
    @Override
    public MyCartViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new MyCartViewHolder(LayoutInflater.from(context)
                .inflate(R.layout.layout_cart_item,parent,false));
    }

    @Override
    public void onBindViewHolder(@NonNull MyCartViewHolder holder, int position) {
        Glide.with(context)
                .load(cartModelList.get(position).getImg())
                .into(holder.imageView);
        // Set the price text
        holder.txtPrice.setText(new StringBuilder("₪").append(cartModelList.get(position).getPrice()));
        // Set the product name text
        holder.txtName.setText(new StringBuilder().append(cartModelList.get(position).getName()));
        // Set the quantity text
        holder.txtQuantity.setText(new StringBuilder().append(cartModelList.get(position).getQuantity()));

        //Event
        holder.btnPlus.setOnClickListener(v -> {
            plusCartItem(holder,cartModelList.get(position));
        });
        holder.btnMinus.setOnClickListener(v -> {
            minusCartItem(holder,cartModelList.get(position));
        });

        holder.btnDelete.setOnClickListener(v -> {
            // Create an alert dialog to confirm deletion of the item
            AlertDialog dialog = new AlertDialog.Builder(context)
                    .setTitle("Delete item")
                    .setMessage("Do you really want to delete item?")
                    .setNegativeButton("CANCEL", (dialogInterface, i) -> dialogInterface.dismiss())
                    .setPositiveButton("OK", (dialogInterface, i) -> {

                        //Temp remove
                        notifyItemRemoved(position);

                        // Delete the cart item from Firebase and update the UI
                        deleteFromFirebase(cartModelList.get(position));
                        dialogInterface.dismiss();
                    }).create();
                    dialog.show();
        });

    }

    // Method to delete a cart item from Firebase database
    private void deleteFromFirebase(CartModel cartModel) {
        FirebaseDatabase.getInstance()
                .getReference("Cart") // Reference to "Cart" in Firebase
                .child(currentuser) // Reference to current user's cart
                .child(cartModel.getKey()) // Reference to the specific cart item by its key
                .removeValue() // Remove the value from the database
                .addOnSuccessListener(aVoid -> EventBus.getDefault().postSticky(new MyUpdateCartEvent()));
    }

    // Method to increase the quantity of a cart item
    private void plusCartItem(MyCartViewHolder holder, CartModel cartModel) {
        cartModel.setQuantity(cartModel.getQuantity() + 1); // Increase the quantity by one
        cartModel.setTotalPrice(cartModel.getQuantity()*Float.parseFloat(cartModel.getPrice())); // Update the total price based on the new quantity

        holder.txtQuantity.setText(new StringBuilder().append(cartModel.getQuantity())); // Update the UI text to show the new quantity
        updateFirebase(cartModel); // Update the changes in Firebase database
    }

    // Method to decrease the quantity of a cart item
    private void minusCartItem(MyCartViewHolder holder, CartModel cartModel) {
        if (cartModel.getQuantity() > 1)
        {
            cartModel.setQuantity(cartModel.getQuantity() - 1); // Decrease the quantity by one
            cartModel.setTotalPrice(cartModel.getQuantity()*Float.parseFloat(cartModel.getPrice())); // Update the total price based on the new quantity
            
            //Update quantity
            holder.txtQuantity.setText(new StringBuilder().append(cartModel.getQuantity()));
            updateFirebase(cartModel); // Update the changes in Firebase database
        }
    }

    // Method to update a cart item in Firebase with the new quantity and total price
    private void updateFirebase(CartModel cartModel) {
        FirebaseDatabase.getInstance()
                .getReference("Cart") // Reference to "Cart" in Firebase
                .child(currentuser) // Reference to current user's cart
                .child(cartModel.getKey()) // Reference to the specific cart item
                .setValue(cartModel) // Update the value in Firebase
                .addOnSuccessListener(aVoid -> EventBus.getDefault().postSticky(new MyUpdateCartEvent()));
    }

    // Return the number of items in the cart
    @Override
    public int getItemCount() {
        return cartModelList.size();
    }

    public class MyCartViewHolder extends RecyclerView.ViewHolder {
        @BindView(R.id.btnMinus)
        ImageView btnMinus;
        @BindView(R.id.btnPlus)
        ImageView btnPlus;
        @BindView(R.id.btnDelete)
        ImageView btnDelete;
        @BindView(R.id.imageView)
        ImageView imageView;
        @BindView(R.id.txtName)
        TextView txtName;
        @BindView(R.id.txtPrice)
        TextView txtPrice;
        @BindView(R.id.txtQuantity)
        TextView txtQuantity;

        Unbinder unbinder;
        public MyCartViewHolder(@NonNull View itemView) {
            super(itemView);
            unbinder = ButterKnife.bind(this,itemView);
        }
    }
}
