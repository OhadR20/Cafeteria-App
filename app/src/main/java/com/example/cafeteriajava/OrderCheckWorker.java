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

        String currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        DatabaseReference ordersRef = FirebaseDatabase.getInstance().getReference("Orders");

        ordersRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                Log.d("OrderCheckWorker", "Checking orders");
                for (DataSnapshot orderSnapshot : dataSnapshot.getChildren()) {
                    String orderUserId = orderSnapshot.child("UserId").getValue(String.class);

                    if (currentUserId.equals(orderUserId)) { // Only process orders for the current user
                        Integer completed = orderSnapshot.child("completed").getValue(Integer.class);
                        String orderId = orderSnapshot.getKey();

                        if (completed != null && completed == 1 && !isNotificationAlreadyShown(orderId)) {
                            sendNotification(orderId);
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

        scheduleNextWork();

        return Result.success();
    }

    private void sendNotification(String orderId) {
        NotificationManager notificationManager = (NotificationManager) getApplicationContext().getSystemService(Context.NOTIFICATION_SERVICE);
        String channelId = "order_complete_channel";

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(channelId, "Order Complete", NotificationManager.IMPORTANCE_HIGH);
            notificationManager.createNotificationChannel(channel);
        }

        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(getApplicationContext(), channelId)
                .setSmallIcon(R.drawable.baseline_fastfood_24)
                .setContentTitle("Order Complete")
                .setContentText("Your order #" + orderId + " is now complete!")
                .setAutoCancel(true);

        notificationManager.notify(1, notificationBuilder.build());
    }


    private boolean isNotificationAlreadyShown(String orderId) {
        SharedPreferences preferences = getApplicationContext().getSharedPreferences("OrderPrefs", Context.MODE_PRIVATE);
        return preferences.getBoolean(orderId, false); // Returns true if the orderId is already marked as shown
    }

    private void markNotificationAsShown(String orderId) {
        SharedPreferences preferences = getApplicationContext().getSharedPreferences("OrderPrefs", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = preferences.edit();
        editor.putBoolean(orderId, true); // Mark the order as shown
        editor.apply();
    }

    private void scheduleNextWork() {
        OneTimeWorkRequest nextWorkRequest = new OneTimeWorkRequest.Builder(OrderCheckWorker.class)
                .setInitialDelay(5, TimeUnit.SECONDS)  // Set the delay time between tasks
                .build();

        WorkManager.getInstance(getApplicationContext()).enqueue(nextWorkRequest);
    }

}

