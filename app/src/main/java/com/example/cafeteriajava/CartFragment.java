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
        loadCartFromFirebase();
    }
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_cart, container, false);
        init(view);
        loadCartFromFirebase();
        return view;
    }

    private void loadCartFromFirebase() {
        List<CartModel> cartModels = new ArrayList<>();
        FirebaseDatabase.getInstance()
                .getReference("Cart")
                .child(currentuser)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if (snapshot.exists()) {
                            for (DataSnapshot cartSnapshot : snapshot.getChildren()) {
                                CartModel cartModel = cartSnapshot.getValue(CartModel.class);
                                cartModel.setKey(cartSnapshot.getKey());
                                cartModels.add(cartModel);
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

    @Override
    public void onCartLoadSuccess(List<CartModel> cartModelList) {
        double sum = 0;
        for (CartModel cartModel : cartModelList) {
            sum += cartModel.getTotalPrice();
        }
        txtTotal.setText(new StringBuilder("₪").append(sum));
        MyCartAdapter adapter = new MyCartAdapter(getContext(), cartModelList);
        recyclerCart.setAdapter(adapter);
    }

    @Override
    public void onCartLoadFailed(String message) {
        Snackbar.make(mainLayout, message, Snackbar.LENGTH_LONG).show();
    }


    private void createOrder() {
        FirebaseDatabase.getInstance()
                .getReference("Cart")
                .child(currentuser)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if (snapshot.exists()) {
                            List<CartModel> cartModels = new ArrayList<>();
                            for (DataSnapshot cartSnapshot : snapshot.getChildren()) {
                                CartModel cartModel = cartSnapshot.getValue(CartModel.class);
                                cartModel.setKey(cartSnapshot.getKey());
                                cartModels.add(cartModel);
                            }

                            DatabaseReference ordersRef = FirebaseDatabase.getInstance().getReference("Orders");

                            ordersRef.addListenerForSingleValueEvent(new ValueEventListener() {
                                @Override
                                public void onDataChange(@NonNull DataSnapshot orderSnapshot) {
                                    long orderNumber = generateUniqueOrderNumber(orderSnapshot);
                                    DatabaseReference newOrderRef = ordersRef.child(String.valueOf(orderNumber));

                                    // Save each cart item starting with index 0 (like before)
                                    for (int i = 0; i < cartModels.size(); i++) {
                                        newOrderRef.child(String.valueOf(i))  // Use i to start numbering from 0
                                                .setValue(cartModels.get(i));
                                    }

                                    // Set the 'completed' field directly under the order id
                                    newOrderRef.child("UserId").setValue(currentuser);
                                    newOrderRef.child("completed").setValue(0)
                                            .addOnSuccessListener(aVoid -> {
                                                // Clear the cart after placing the order
                                                FirebaseDatabase.getInstance()
                                                        .getReference("Cart")
                                                        .child(currentuser)
                                                        .removeValue();

                                                // Show success message
                                                Snackbar.make(mainLayout, "Order placed successfully!", Snackbar.LENGTH_LONG).show();
                                            })
                                            .addOnFailureListener(e -> {
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




    private long generateUniqueOrderNumber(DataSnapshot orderSnapshot) {
        Set<Long> existingOrderNumbers = new HashSet<>();
        for (DataSnapshot snapshot : orderSnapshot.getChildren()) {
            existingOrderNumbers.add(Long.parseLong(snapshot.getKey()));
        }
        long orderNumber;
        Random random = new Random();
        do {
            orderNumber = 10000000000L + (long) (random.nextDouble() * 89999999999L);
        } while (existingOrderNumbers.contains(orderNumber));
        return orderNumber;
    }



}
