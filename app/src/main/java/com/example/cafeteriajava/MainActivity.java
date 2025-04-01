package com.example.cafeteriajava;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;
import androidx.work.OneTimeWorkRequest;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;
import java.util.concurrent.TimeUnit;
import com.example.cafeteriajava.eventbus.MyUpdateCartEvent;
import com.example.cafeteriajava.model.CartModel;
import com.google.android.material.badge.BadgeDrawable;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public class MainActivity extends AppCompatActivity {

    private String currentuser = FirebaseAuth.getInstance().getCurrentUser().getUid(); // Get the unique ID of the current user from Firebase Authentication
    private BottomNavigationView bottomNavigationView; // Declare the BottomNavigationView for navigating between different fragments

    // Declare Bluetooth related variables
    private BluetoothAdapter bluetoothAdapter;
    private BluetoothSocket bluetoothSocket;
    private BluetoothDevice bluetoothDevice;

    private static final UUID MY_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");  // Define a universally unique identifier (UUID) for Bluetooth connection (commonly used for SPP)
    private static final int REQUEST_ENABLE_BT = 1;
    private static final int REQUEST_ACCESS_FINE_LOCATION = 2;
    private static final int REQUEST_BLUETOOTH_CONNECT = 3;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState); // Call parent class onCreate
        setContentView(R.layout.activity_main); // Set the layout for the main activity


        bottomNavigationView = findViewById(R.id.bottomNavigationView); // Bind the bottom navigation view from the layout
        bottomNavigationView.setOnNavigationItemSelectedListener(item -> { // Set up a listener for bottom navigation item selections
            Fragment selectedFragment = null;
            // Switch between fragments based on the selected menu item
            switch (item.getItemId()) {
                case R.id.home:
                    selectedFragment = new HomeFragment();
                    break;
                case R.id.profile:
                    selectedFragment = new ProfileFragment();
                    break;
                case R.id.cart:
                    selectedFragment = new CartFragment();
                    break;
            }
            if (selectedFragment != null) {
                loadFragment(selectedFragment); // Load the chosen fragment
            }
            return true;
        });

        // Load the HomeFragment by default
        if (savedInstanceState == null) {
            loadFragment(new HomeFragment());
        }

        // Register to EventBus
        EventBus.getDefault().register(this);

        // Load initial cart count
        countCartItems();



        OneTimeWorkRequest initialWorkRequest = new OneTimeWorkRequest.Builder(OrderCheckWorker.class).build(); // Create a one-time work request to check orders using WorkManager
        WorkManager.getInstance(this).enqueue(initialWorkRequest); // Enqueue the work request to execute the OrderCheckWorker task




    }

    @Override
    protected void onStart() {
        super.onStart();
        if (bluetoothAdapter == null || !bluetoothAdapter.isEnabled()) { // Check if the Bluetooth adapter is null or disabled
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE); // Create an intent to request enabling Bluetooth
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT); // Request Bluetooth enablement
        }
        if (checkSelfPermission(android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) { // Check if the ACCESS_FINE_LOCATION permission is granted (required for Bluetooth scanning)
            requestPermissions(new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION}, REQUEST_ACCESS_FINE_LOCATION); // Request ACCESS_FINE_LOCATION permission from the user
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.POST_NOTIFICATIONS}, 1);
        }

        Intent serviceIntent = new Intent(this, FirebaseOrderListenerService.class); // Create an intent to start a service that listens for order updates from Firebase

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // Create a notification channel for order update notifications
            NotificationChannel channel = new NotificationChannel("YOUR_CHANNEL_ID", "Order Updates", NotificationManager.IMPORTANCE_HIGH);
            channel.setDescription("Notifications for order completion");
            // Get the notification manager system service
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel); // Create the channel
            startForegroundService(serviceIntent);  // Use foreground service for background processes

        }
        else {
            startService(serviceIntent);
        }



    }



    private void loadFragment(Fragment fragment) { // Helper method to load a given fragment into the container layout
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction(); // Begin a fragment transaction
        transaction.replace(R.id.frame_layout, fragment); // Replace the current fragment in the container
        transaction.commit(); // Commit the transaction to display the fragment
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        EventBus.getDefault().unregister(this);
    }

    @Subscribe(threadMode = ThreadMode.MAIN, sticky = true) // Subscribe to the MyUpdateCartEvent from EventBus
    public void onUpdateCart(MyUpdateCartEvent event) {
        countCartItems(); // When a cart update event is received, recount the cart items and update the badge
    }

    private void countCartItems() { // Method to count the number of items in the user's cart from Firebase Realtime Database
        List<CartModel> cartModels = new ArrayList<>(); // Create a list to hold CartModel objects
        FirebaseDatabase.getInstance().getReference("Cart") // Access the "Cart" node in the Firebase database under the current user's ID
                .child(currentuser)
                .addListenerForSingleValueEvent(new ValueEventListener() { // Add a single event listener to fetch the current cart data once
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        int cartCount = 0; // Initialize cart count
                        for (DataSnapshot cartSnapshot : snapshot.getChildren()) {
                            CartModel cartModel = cartSnapshot.getValue(CartModel.class); // Convert the snapshot to a CartModel object
                            if (cartModel != null) {
                                cartCount += cartModel.getQuantity(); // Increase the cart count by the quantity of each cart item
                            }
                        }
                        showCartBadge(cartCount); // Update the cart badge with the calculated count
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        // If there is an error retrieving the data, show a Snackbar with the error message
                        Snackbar.make(findViewById(R.id.frame_layout), error.getMessage(), Snackbar.LENGTH_LONG).show();
                    }
                });
    }

    // Method to display the cart count badge on the BottomNavigationView
    void showCartBadge(int cartCount) {
        BadgeDrawable badge = bottomNavigationView.getOrCreateBadge(R.id.cart); // Get or create a badge for the cart menu item
        badge.setVisible(cartCount > 0); // Set the badge to be visible only if there is at least one item in the cart
        badge.setNumber(cartCount); // Set the badge number to the cart count

        // Adjust badge position
        badge.setVerticalOffset(10); // Adjust this value as needed to position the badge higher
        badge.setHorizontalOffset(10); // Adjust this value as needed to position the badge to the side
    }
}
