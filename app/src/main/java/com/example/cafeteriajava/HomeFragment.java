package com.example.cafeteriajava;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.RelativeLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.cafeteriajava.adapter.MyProductAdapter;
import com.example.cafeteriajava.eventbus.MyUpdateCartEvent;
import com.example.cafeteriajava.listener.ICartLoadListener;
import com.example.cafeteriajava.listener.IProductLoadListener;
import com.example.cafeteriajava.model.CartModel;
import com.example.cafeteriajava.model.ProductModel;
import com.example.cafeteriajava.utils.SpaceItemDecoration;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.nex3z.notificationbadge.NotificationBadge;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;

public class HomeFragment extends Fragment implements IProductLoadListener, ICartLoadListener {

    @BindView(R.id.recycler_products)
    RecyclerView recyclerProducts;
    @BindView(R.id.mainLayout)
    RelativeLayout mainLayout;



    IProductLoadListener productLoadListener;
    ICartLoadListener cartLoadListener;
    private String currentuser = FirebaseAuth.getInstance().getCurrentUser().getUid(); // Get the current user's unique ID from Firebase Authentication

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
        countCartItem(); // When a cart update event is received, recount the cart items
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home, container, false);
        init(view);
        loadProductFromFirebase(); // Load the products from Firebase into the RecyclerView
        countCartItem(); // Count the number of items in the cart
        return view;
    }

    // Method to load products from Firebase Realtime Database
    private void loadProductFromFirebase() {
        List<ProductModel> productModels = new ArrayList<>(); // Create a list to hold the ProductModel objects
        FirebaseDatabase.getInstance() // Get a reference to the "products" in the Firebase database
                .getReference("products")
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if (snapshot.exists()) {
                            for (DataSnapshot productSnapshot : snapshot.getChildren()) {
                                ProductModel productModel = productSnapshot.getValue(ProductModel.class);
                                productModel.setKey(productSnapshot.getKey());
                                productModels.add(productModel); // Add the product to the list
                            }
                            productLoadListener.onProductLoadSuccess(productModels);
                        } else {
                            productLoadListener.onProductLoadFailed("Can't find Product");
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        productLoadListener.onProductLoadFailed(error.getMessage());
                    }
                });
    }

    // Method to initialize the fragment's UI elements and set up RecyclerView
    private void init(View view) {
        recyclerProducts = view.findViewById(R.id.recycler_products);
        mainLayout = view.findViewById(R.id.mainLayout);

        productLoadListener = this;
        cartLoadListener = this;

        GridLayoutManager gridLayoutManager = new GridLayoutManager(getContext(), 2); // Create a GridLayoutManager with 2 columns for the RecyclerView
        recyclerProducts.setLayoutManager(gridLayoutManager);
        recyclerProducts.addItemDecoration(new SpaceItemDecoration()); // Add item decoration to the RecyclerView to set spacing between grid items

    }

    @Override
    public void onProductLoadSuccess(List<ProductModel> productModelList) {
        // Create an adapter for the RecyclerView using the loaded product list and the cart load listener
        MyProductAdapter adapter = new MyProductAdapter(getContext(), productModelList, cartLoadListener);
        recyclerProducts.setAdapter(adapter);
    }

    @Override
    public void onProductLoadFailed(String message) {
        Snackbar.make(mainLayout, message, Snackbar.LENGTH_LONG).show(); // Display an error message using a Snackbar
    }

    @Override
    public void onCartLoadSuccess(List<CartModel> cartModelList) {
        int cartSum = 0;
        // Iterate through each cart model and accumulate the total quantity
        for (CartModel cartModel : cartModelList)
            cartSum += cartModel.getQuantity();
        ((MainActivity) getActivity()).showCartBadge(cartSum);
    }

    @Override
    public void onCartLoadFailed(String message) {
        Snackbar.make(mainLayout, message, Snackbar.LENGTH_LONG).show(); // Display an error message using a Snackbar
    }

    // Method to count the cart items from Firebase Realtime Database
    private void countCartItem() {
        List<CartModel> cartModels = new ArrayList<>(); // Create a list to hold the CartModel objects
        FirebaseDatabase.getInstance().getReference("Cart") // Access the "Cart" in the Firebase database under the current user's ID
                .child(currentuser)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        for (DataSnapshot cartSnapshot : snapshot.getChildren()) {
                            CartModel cartModel = cartSnapshot.getValue(CartModel.class); // Convert the snapshot into a CartModel object
                            cartModel.setKey(cartSnapshot.getKey());
                            cartModels.add(cartModel); // Add the cart item to the list
                        }
                        cartLoadListener.onCartLoadSuccess(cartModels);
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        cartLoadListener.onCartLoadFailed(error.getMessage());
                    }
                });
    }
}
