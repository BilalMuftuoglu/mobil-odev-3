package com.example.myapplication;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import java.util.Map;

public class MyFirebaseMessagingService extends FirebaseMessagingService {


    @Override
    public void onNewToken(@NonNull String token) {
        System.out.println("************new token*************");
        super.onNewToken(token);
    }

    FirebaseAuth mAuth;
    String accountType;

    @Override
    public void onMessageReceived(@NonNull RemoteMessage message) {
        super.onMessageReceived(message);

        System.out.println("************message received*************");

        mAuth = FirebaseAuth.getInstance();
        if(mAuth.getCurrentUser().getEmail().endsWith("@std.yildiz.edu.tr")){
            accountType = "students";
        }else{
            accountType = "instructors";
        }

        String title = message.getNotification().getTitle();
        String body = message.getNotification().getBody();

        Map<String, String> data = message.getData();
        System.out.println(data);
        System.out.println(title);
        System.out.println(body);
        String courseId = data.get("courseId");

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, "MyNotifications")
                .setContentTitle(title)
                .setContentText(body)
                .setSmallIcon(R.drawable.ic_launcher_background)
                .setAutoCancel(true);


        Intent intent = new Intent(this, ClassroomActivity.class);
        intent.putExtra("courseId", courseId);
        intent.putExtra("accountType", accountType);

        PendingIntent pendingIntent = PendingIntent.getActivity(this, 10, intent, PendingIntent.FLAG_MUTABLE);

        builder.setContentIntent(pendingIntent);

        NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        int id = (int) System.currentTimeMillis();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel("MyNotifications", "MyNotifications", NotificationManager.IMPORTANCE_HIGH);
            manager.createNotificationChannel(channel);
        }
        manager.notify(id, builder.build());
    }
}
