package com.example.cafeteriajava.adapter;

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
import com.example.cafeteriajava.listener.ICartLoadListener;
import com.example.cafeteriajava.listener.IRecyclerViewClickListener;
import com.example.cafeteriajava.model.CartModel;
import com.example.cafeteriajava.model.ProductModel;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.auth.FirebaseAuth;

import org.greenrobot.eventbus.EventBus;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.Unbinder;

public class MyProductAdapter extends RecyclerView.Adapter<MyProductAdapter.MyProductViewHolder> {

    private Context context;
    // List of ProductModel objects
    private List<ProductModel> productModelList;
    private ICartLoadListener iCartLoadListener;

    // Retrieve the current user's unique ID via FirebaseAuth
    private String currentuser = FirebaseAuth.getInstance().getCurrentUser().getUid();
    public MyProductAdapter(Context context, List<ProductModel> productModelList, ICartLoadListener iCartLoadListener) {
        this.context = context;
        this.productModelList = productModelList;
        this.iCartLoadListener = iCartLoadListener;
    }

    @NonNull
    @Override
    public MyProductViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new MyProductViewHolder(LayoutInflater.from(context).inflate(R.layout.layout_product_item, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull MyProductViewHolder holder, int position) {
        Glide.with(context) // Load the product image using Glide
                .load(productModelList.get(position).getImg())
                .into(holder.imageView);
        // Set the product price
        holder.txtPrice.setText(new StringBuilder("₪").append(productModelList.get(position).getPrice()));
        // Set the product name
        holder.txtName.setText(new StringBuilder().append(productModelList.get(position).getName()));

        // Check availability and set icon
        if (productModelList.get(position).getAvailable().equals("1")) {
            holder.availabilityIcon.setImageResource(R.drawable.baseline_check_24); // Replace with your checkmark drawable
        } else {
            holder.availabilityIcon.setImageResource(R.drawable.baseline_clear_24); // Replace with your cross drawable
        }


        holder.setListener((view, adapterPosition) -> {
            // When the product is clicked, add it to the cart
            addToCart(productModelList.get(position));
        });

    }

    // Method to add a product to the user's cart in Firebase
    private void addToCart(ProductModel productModel) {
        // Reference the specific product in the "products" by its key
        DatabaseReference productRef = FirebaseDatabase
                .getInstance()
                .getReference("products")
                .child(productModel.getKey());

        // Retrieve the product data from Firebase
        productRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    // Retrieve the product availability status from Firebase
                    String available = snapshot.child("available").getValue(String.class);
                    if ("1".equals(available)) {
                        DatabaseReference userCart = FirebaseDatabase // Reference the current user's cart in Firebase
                                .getInstance()
                                .getReference("Cart")
                                .child(currentuser); // In other project, you will add user id here

                        // Check if the product already exists in the user's cart
                        userCart.child(productModel.getKey())
                                .addListenerForSingleValueEvent(new ValueEventListener() {
                                    @Override
                                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                                        if (snapshot.exists()) // If user already has item in cart
                                        {
                                            // Just update quantity and total price
                                            CartModel cartModel = snapshot.getValue(CartModel.class);
                                            cartModel.setQuantity(cartModel.getQuantity() + 1);
                                            Map<String, Object> updateData = new HashMap<>();
                                            updateData.put("quantity", cartModel.getQuantity());
                                            updateData.put("totalPrice", cartModel.getQuantity() * Float.parseFloat(cartModel.getPrice()));

                                            // Update the cart item in Firebase with the new data
                                            userCart.child(productModel.getKey())
                                                    .updateChildren(updateData)
                                                    .addOnSuccessListener(unused -> {
                                                        iCartLoadListener.onCartLoadFailed("Add To Cart Success");
                                                    })
                                                    .addOnFailureListener(e -> iCartLoadListener.onCartLoadFailed(e.getMessage()));

                                        } else // If item isn't in cart, add new
                                        {
                                            CartModel cartModel = new CartModel();
                                            cartModel.setName(productModel.getName());
                                            cartModel.setImg(productModel.getImg());
                                            cartModel.setKey(productModel.getKey());
                                            cartModel.setPrice(productModel.getPrice());
                                            cartModel.setQuantity(1);
                                            cartModel.setTotalPrice(Float.parseFloat(productModel.getPrice()));

                                            // Add the new cart item to Firebase under the current user's cart
                                            userCart.child(productModel.getKey())
                                                    .setValue(cartModel)
                                                    .addOnSuccessListener(unused -> {
                                                        iCartLoadListener.onCartLoadFailed("Add To Cart Success");
                                                    })
                                                    .addOnFailureListener(e -> iCartLoadListener.onCartLoadFailed(e.getMessage()));
                                        }
                                        EventBus.getDefault().postSticky(new MyUpdateCartEvent());
                                    }

                                    @Override
                                    public void onCancelled(@NonNull DatabaseError error) {
                                        iCartLoadListener.onCartLoadFailed(error.getMessage());
                                    }
                                });
                    } else {
                        // Product is not available
                        iCartLoadListener.onCartLoadFailed("Product is not available");
                    }
                } else {
                    // Product does not exist in the database
                    iCartLoadListener.onCartLoadFailed("Product does not exist");
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                iCartLoadListener.onCartLoadFailed(error.getMessage());
            }
        });
    }


    @Override
    public int getItemCount() { // Return the total number of products to be displayed in the list
        return productModelList.size();
    }

    public class MyProductViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
        @BindView(R.id.imageView)
        ImageView imageView;
        @BindView(R.id.txtName)
        TextView txtName;
        @BindView(R.id.txtPrice)
        TextView txtPrice;
        @BindView(R.id.availabilityIcon)
        ImageView availabilityIcon;

        IRecyclerViewClickListener listener;

        public void setListener(IRecyclerViewClickListener listener) {
            this.listener = listener;
        }

        private Unbinder unbinder;
        public MyProductViewHolder(@NonNull View itemView) {
            super(itemView);
            unbinder = ButterKnife.bind(this, itemView);
            itemView.setOnClickListener(this);
        }

        @Override
        public void onClick(View v) {
            listener.onRecyclerClick(v,getAdapterPosition());
        }
    }
}
