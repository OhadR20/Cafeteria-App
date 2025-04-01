package com.example.cafeteriajava;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import androidx.core.app.NotificationCompat;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;
import androidx.work.Worker;
import androidx.work.WorkerParameters;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.concurrent.TimeUnit;

public class OrderCheckWorker extends Worker {
    public OrderCheckWorker(Context context, WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @Override
    public Result doWork() {
        Log.d("OrderCheckWorker", "Worker running");

        // Get the current user's unique ID from Firebase Authentication
        String currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        // Get a reference to the "Orders" in Firebase Realtime Database
        DatabaseReference ordersRef = FirebaseDatabase.getInstance().getReference("Orders");

        ordersRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                Log.d("OrderCheckWorker", "Checking orders");
                for (DataSnapshot orderSnapshot : dataSnapshot.getChildren()) {
                    String orderUserId = orderSnapshot.child("UserId").getValue(String.class); // Get the user ID associated with this order

                    // Only process orders that belong to the current user
                    if (currentUserId.equals(orderUserId)) { // Only process orders for the current user
                        Integer completed = orderSnapshot.child("completed").getValue(Integer.class);  // Retrieve the 'completed' status for the order
                        String orderId = orderSnapshot.getKey(); // Get the order's unique ID

                        // If the order is marked as completed (completed == 1) and a notification hasn't been shown yet for this order
                        if (completed != null && completed == 1 && !isNotificationAlreadyShown(orderId)) {
                            sendNotification(orderId); // Send a notification to the user
                            markNotificationAsShown(orderId); // Mark as shown to avoid duplicate notifications
                        }
                    }
                }
            }

            @Override
            public void onCancelled(DatabaseError error) {
                Log.e("OrderCheckWorker", "Error: " + error.getMessage());
            }
        });

        scheduleNextWork(); // Schedule the next work execution so that orders are checked periodically

        return Result.success();
    }

    // Method to send a notification for an order that has been completed
    private void sendNotification(String orderId) {
        // Get the NotificationManager service from the application context
        NotificationManager notificationManager = (NotificationManager) getApplicationContext().getSystemService(Context.NOTIFICATION_SERVICE);
        // Define a unique channel ID for order completion notifications
        String channelId = "order_complete_channel";

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            // Create a new channel with the specified ID, name, and importance level
            NotificationChannel channel = new NotificationChannel(channelId, "Order Complete", NotificationManager.IMPORTANCE_HIGH);
            // Register the channel with the NotificationManager
            notificationManager.createNotificationChannel(channel);
        }

        // Build the notification using NotificationCompat.Builder
        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(getApplicationContext(), channelId)
                .setSmallIcon(R.drawable.baseline_fastfood_24)
                .setContentTitle("Order Complete")
                .setContentText("Your order #" + orderId + " is now complete!")
                .setAutoCancel(true);

        // Issue the notification with a fixed notification ID
        notificationManager.notify(1, notificationBuilder.build());
    }


    // Method to check if a notification for a given order ID has already been shown
    private boolean isNotificationAlreadyShown(String orderId) {
        SharedPreferences preferences = getApplicationContext().getSharedPreferences("OrderPrefs", Context.MODE_PRIVATE);
        return preferences.getBoolean(orderId, false); // Returns true if the orderId is already marked as shown
    }

    // Method to mark that a notification has been shown for a given order ID
    private void markNotificationAsShown(String orderId) {
        SharedPreferences preferences = getApplicationContext().getSharedPreferences("OrderPrefs", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = preferences.edit();
        editor.putBoolean(orderId, true); // Mark the order as shown
        editor.apply();
    }

    // Method to schedule the next execution of this worker using WorkManager
    private void scheduleNextWork() {
        // Create a one-time work request for OrderCheckWorker with a delay of 5 seconds
        OneTimeWorkRequest nextWorkRequest = new OneTimeWorkRequest.Builder(OrderCheckWorker.class)
                .setInitialDelay(5, TimeUnit.SECONDS)  // Set the delay time between tasks
                .build();

        // Enqueue the work request so that it executes after the delay
        WorkManager.getInstance(getApplicationContext()).enqueue(nextWorkRequest);
    }

}

