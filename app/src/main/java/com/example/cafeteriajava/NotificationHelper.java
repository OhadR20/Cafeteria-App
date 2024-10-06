package com.example.cafeteriajava;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import androidx.core.app.NotificationCompat;

public class NotificationHelper {

    public static void showOrderCompletedNotification(Context context, String orderId) {
        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        String channelId = "order_complete_channel";

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(channelId, "Order Complete", NotificationManager.IMPORTANCE_HIGH);
            notificationManager.createNotificationChannel(channel);
        }

        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(context, channelId)
                .setSmallIcon(R.drawable.baseline_fastfood_24)
                .setContentTitle("Order Complete")
                .setContentText("Your order #" + orderId + " is now complete!")
                .setAutoCancel(true);

        // Make sure notification ID is unique for every notification (use orderId as notification ID)
        notificationManager.notify(Integer.parseInt(orderId), notificationBuilder.build());
    }

}

