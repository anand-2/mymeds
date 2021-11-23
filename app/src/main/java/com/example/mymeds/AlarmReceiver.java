package com.example.mymeds;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.graphics.BitmapFactory;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

public class AlarmReceiver extends BroadcastReceiver
{
    @Override
    public void onReceive(Context context, Intent intent)
    {
        String medName = intent.getStringExtra("med_name");
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context,MainActivity.defaultNotifChannel)
                .setSmallIcon(R.mipmap.ic_launcher_foreground)
                .setLargeIcon(BitmapFactory.decodeResource(context.getResources(), R.mipmap.ic_launcher_foreground))
                .setContentTitle("Medicine Reminder - " + medName.toUpperCase())
                .setContentText("Please stay alive! 😒")
                .setPriority(NotificationCompat.PRIORITY_MAX);

        NotificationManagerCompat managerCompat = NotificationManagerCompat.from(context);
        managerCompat.notify((int) System.currentTimeMillis(), builder.build());
    }
}
