package com.example.cafeteriajava;

import android.app.Notification;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.IBinder;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class FirebaseOrderListenerService extends Service {
    private DatabaseReference ordersRef; // Reference to the user's orders in the Firebase database
    private ValueEventListener orderCompleteListener;

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        // Build a notification to show that the service is actively listening for order updates
        Notification notification = new NotificationCompat.Builder(this, "order_update_channel")
                .setContentTitle("Order Update Listener")
                .setContentText("Listening for order status updates...")
                .setSmallIcon(R.drawable.baseline_update_24)
                .setPriority(NotificationCompat.PRIORITY_HIGH)  // Ensure high priority
                .setCategory(NotificationCompat.CATEGORY_SERVICE)  // Mark as ongoing service
                .build();

        // Start this service in the foreground
        startForeground(1, notification); // Start the service in foreground

        // Get the current user's unique ID from Firebase Authentication
        String currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        // Reference the current user's orders in the Firebase database
        ordersRef = FirebaseDatabase.getInstance().getReference("Orders").child(currentUserId);

        // Start listening to order completion status
        startOrderCompleteListener(currentUserId);

        return START_STICKY; // Ensures the service continues running
    }

    // Method to attach a Firebase listener for order completion events
    private void startOrderCompleteListener(String userId) {
        orderCompleteListener = ordersRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                for (DataSnapshot orderSnapshot : dataSnapshot.getChildren()) {
                    // Retrieve the user ID associated with the order
                    String orderUserId = orderSnapshot.child("UserId").getValue(String.class);

                    // Only process orders that belong to the current user
                    if (userId.equals(orderUserId)) { // Only process orders for the current user
                        // Get the "completed" status of the order
                        int completed = orderSnapshot.child("completed").getValue(Integer.class);
                        String orderId = orderSnapshot.getKey(); // Retrieve the order's unique key

                        // If the order is marked as complete and a notification hasn't been shown yet
                        if (completed == 1 && !isNotificationAlreadyShown(orderId)) {
                            // Trigger local notification for completed order
                            sendOrderCompleteNotification(orderId);
                            markNotificationAsShown(orderId); // Mark as shown to avoid duplicate notifications
                        }
                    }
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                // Handle possible errors
            }
        });
    }

    // Method to trigger a notification
    private void sendOrderCompleteNotification(String orderId) {
        NotificationHelper.showOrderCompletedNotification(this, orderId);
    }

    @Override
    public void onDestroy() {
        if (orderCompleteListener != null) {
            ordersRef.removeEventListener(orderCompleteListener); // Cleanup listener
        }
        super.onDestroy();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }


    // Check if a notification for the given order ID has already been shown
    private boolean isNotificationAlreadyShown(String orderId) {
        SharedPreferences preferences = getApplicationContext().getSharedPreferences("OrderPrefs", Context.MODE_PRIVATE);
        return preferences.getBoolean(orderId, false); // Returns true if the orderId is already marked as shown
    }

    // Mark that a notification has been shown for the given order ID
    private void markNotificationAsShown(String orderId) {
        SharedPreferences preferences = getApplicationContext().getSharedPreferences("OrderPrefs", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = preferences.edit();
        editor.putBoolean(orderId, true); // Mark the order as shown
        editor.apply();
    }

}
