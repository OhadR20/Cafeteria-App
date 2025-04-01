package com.example.cafeteriajava;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.cafeteriajava.adapter.MyCartAdapter;
import com.example.cafeteriajava.eventbus.MyUpdateCartEvent;
import com.example.cafeteriajava.listener.ICartLoadListener;
import com.example.cafeteriajava.model.CartModel;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import butterknife.BindView;

public class CartFragment extends Fragment implements ICartLoadListener {

    @BindView(R.id.recycler_cart)
    RecyclerView recyclerCart;
    @BindView(R.id.mainLayout)
    RelativeLayout mainLayout;
    @BindView(R.id.txtTotal)
    TextView txtTotal;
    @BindView(R.id.CheckOut)
    Button checkOutButton;

    // Get the current user's unique ID from Firebase Authentication
    private String currentuser = FirebaseAuth.getInstance().getCurrentUser().getUid();
    private ICartLoadListener cartLoadListener;

    @Override
    public void onStart() {
        super.onStart();
        EventBus.getDefault().register(this);
    }

    @Override
    public void onStop() {
        if (EventBus.getDefault().hasSubscriberForEvent(MyUpdateCartEvent.class))
            EventBus.getDefault().removeStickyEvent(MyUpdateCartEvent.class);
        EventBus.getDefault().unregister(this);
        super.onStop();
    }

    @Subscribe(threadMode = ThreadMode.MAIN, sticky = true)
    public void onUpdateCart(MyUpdateCartEvent event) {
        loadCartFromFirebase(); // Reload the cart from Firebase
    }
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_cart, container, false);
        init(view);
        loadCartFromFirebase(); // Load cart data from Firebase
        return view;
    }

    // Method to load cart items from Firebase Realtime Database
    private void loadCartFromFirebase() {
        List<CartModel> cartModels = new ArrayList<>(); // Create a list to hold CartModel objects
        FirebaseDatabase.getInstance()
                // Reference the "Cart" under the current user's ID in Firebase Database
                .getReference("Cart")
                .child(currentuser)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if (snapshot.exists()) {
                            for (DataSnapshot cartSnapshot : snapshot.getChildren()) {
                                CartModel cartModel = cartSnapshot.getValue(CartModel.class); // Convert snapshot into a CartModel object
                                cartModel.setKey(cartSnapshot.getKey()); // Set the unique key for the cart item
                                cartModels.add(cartModel); // Add the cart item to the list
                            }
                            cartLoadListener.onCartLoadSuccess(cartModels);
                        } else {
                            cartLoadListener.onCartLoadFailed("Cart empty");
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        cartLoadListener.onCartLoadFailed(error.getMessage());
                    }
                });
    }

    // Initialize UI components and setup the RecyclerView and checkout button
    private void init(View view) {
        recyclerCart = view.findViewById(R.id.recycler_cart);
        mainLayout = view.findViewById(R.id.mainLayout);
        txtTotal = view.findViewById(R.id.txtTotal);
        checkOutButton = view.findViewById(R.id.CheckOut);

        cartLoadListener = this;
        LinearLayoutManager layoutManager = new LinearLayoutManager(getContext());
        recyclerCart.setLayoutManager(layoutManager);
        recyclerCart.addItemDecoration(new DividerItemDecoration(requireContext(), layoutManager.getOrientation()));

        checkOutButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                createOrder();
            }
        });
    }

    // method for when cart loading is successful
    @Override
    public void onCartLoadSuccess(List<CartModel> cartModelList) {
        double sum = 0;
        // Calculate the total price of all cart items
        for (CartModel cartModel : cartModelList) {
            sum += cartModel.getTotalPrice();
        }
        // Display the total price
        txtTotal.setText(new StringBuilder("₪").append(sum));
        // Create and set an adapter for the RecyclerView with the list of cart items
        MyCartAdapter adapter = new MyCartAdapter(getContext(), cartModelList);
        recyclerCart.setAdapter(adapter);
    }

    @Override
    public void onCartLoadFailed(String message) {
        Snackbar.make(mainLayout, message, Snackbar.LENGTH_LONG).show();
    }

    // Method to create an order from the current cart items
    private void createOrder() {
        FirebaseDatabase.getInstance() // Reference the user's cart in Firebase Database
                .getReference("Cart")
                .child(currentuser)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if (snapshot.exists()) { // Check if the cart has items
                            List<CartModel> cartModels = new ArrayList<>(); // Create a list to store cart items
                            for (DataSnapshot cartSnapshot : snapshot.getChildren()) { // Iterate over each cart item snapshot
                                CartModel cartModel = cartSnapshot.getValue(CartModel.class);
                                cartModel.setKey(cartSnapshot.getKey()); // Set the unique key for each cart item
                                cartModels.add(cartModel);
                            }

                            // Reference the "Orders" in Firebase Database
                            DatabaseReference ordersRef = FirebaseDatabase.getInstance().getReference("Orders");
                            DatabaseReference userOrdersRef = FirebaseDatabase.getInstance().getReference("users").child(currentuser).child("orders");

                            // Fetch existing orders to generate a unique order number
                            ordersRef.addListenerForSingleValueEvent(new ValueEventListener() {
                                @Override
                                public void onDataChange(@NonNull DataSnapshot orderSnapshot) {
                                    long orderNumber = generateUniqueOrderNumber(orderSnapshot); // Generate a unique order number
                                    DatabaseReference newOrderRef = ordersRef.child(String.valueOf(orderNumber)); // Create a new order node with the unique order number

                                    // Save each cart item starting with index 0
                                    for (int i = 0; i < cartModels.size(); i++) {
                                        newOrderRef.child(String.valueOf(i)).setValue(cartModels.get(i));
                                    }

                                    // Set additional fields: UserId, completed status, and timestamp
                                    Map<String, Object> orderDetails = new HashMap<>();
                                    orderDetails.put("UserId", currentuser);
                                    orderDetails.put("completed", 0);
                                    orderDetails.put("timestamp", com.google.firebase.database.ServerValue.TIMESTAMP);

                                    // Save order details to both "Orders" and "users/{userId}/orders/"
                                    newOrderRef.updateChildren(orderDetails).addOnSuccessListener(aVoid -> {
                                        // Save to user's profile
                                        userOrdersRef.child(String.valueOf(orderNumber)).updateChildren(orderDetails);

                                        // Clear the cart after placing the order
                                        FirebaseDatabase.getInstance()
                                                .getReference("Cart")
                                                .child(currentuser)
                                                .removeValue();

                                        // Show success message
                                        Snackbar.make(mainLayout, "Order placed successfully!", Snackbar.LENGTH_LONG).show();
                                    }).addOnFailureListener(e -> {
                                        Snackbar.make(mainLayout, e.getMessage(), Snackbar.LENGTH_LONG).show();
                                    });
                                }

                                @Override
                                public void onCancelled(@NonNull DatabaseError error) {
                                    Snackbar.make(mainLayout, error.getMessage(), Snackbar.LENGTH_LONG).show();
                                }
                            });

                        } else {
                            Snackbar.make(mainLayout, "Cart is empty", Snackbar.LENGTH_LONG).show();
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Snackbar.make(mainLayout, error.getMessage(), Snackbar.LENGTH_LONG).show();
                    }
                });
    }






    // method to generate a unique order number based on existing orders
    private long generateUniqueOrderNumber(DataSnapshot orderSnapshot) {
        Set<Long> existingOrderNumbers = new HashSet<>(); // Create a set to store all existing order numbers
        // Iterate over each order in the snapshot and add its key
        for (DataSnapshot snapshot : orderSnapshot.getChildren()) {
            existingOrderNumbers.add(Long.parseLong(snapshot.getKey()));
        }
        long orderNumber;
        Random random = new Random();
        // Generate a random 11-digit order number that is not already in use
        do {
            orderNumber = 10000000000L + (long) (random.nextDouble() * 89999999999L);
        } while (existingOrderNumbers.contains(orderNumber));
        return orderNumber;
    }



}
