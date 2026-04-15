package com.example.myapplication.services;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import androidx.core.app.NotificationCompat;

import com.example.myapplication.R;
import com.example.myapplication.screens.EmployeeDashboardActivity;

public class ShiftReminderReceiver extends BroadcastReceiver {

    public static final String CHANNEL_ID = "shift_reminder_channel";

    @Override
    public void onReceive(Context context, Intent intent) {
        String workerName = intent.getStringExtra("workerName");
        String shiftDate  = intent.getStringExtra("shiftDate");
        String startTime  = intent.getStringExtra("startTime");

        if (workerName == null) workerName = "עובד";
        if (startTime  == null) startTime  = "";

        String title = "תזכורת: משמרת בקרוב";
        String text  = "שלום " + workerName + ", המשמרת שלך מתחילה בשעה " +
                       startTime + " (" + shiftDate + ")";

        // פתיחת EmployeeDashboardActivity בלחיצה על ההתראה
        Intent tapIntent = new Intent(context, EmployeeDashboardActivity.class);
        tapIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                context, 0, tapIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setContentTitle(title)
                .setContentText(text)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(text))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent);

        NotificationManager nm = (NotificationManager)
                context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (nm != null) {
            // ID ייחודי לכל התראה
            int notificationId = (workerName + shiftDate).hashCode();
            nm.notify(notificationId, builder.build());
        }
    }
}
