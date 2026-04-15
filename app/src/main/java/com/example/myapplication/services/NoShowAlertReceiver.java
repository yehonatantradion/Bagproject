package com.example.myapplication.services;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import androidx.core.app.NotificationCompat;

import com.example.myapplication.R;
import com.example.myapplication.model.Attendance;
import com.example.myapplication.screens.CurrentShiftActivity;

/**
 * BroadcastReceiver שמופעל 30 דקות אחרי תחילת משמרת.
 * בודק אם העובד אישר הגעה — אם לא, שולח התראה למנהל.
 */
public class NoShowAlertReceiver extends BroadcastReceiver {

    public static final String CHANNEL_ID = "no_show_alert_channel";

    @Override
    public void onReceive(Context context, Intent intent) {
        String workerId   = intent.getStringExtra("workerId");
        String workerName = intent.getStringExtra("workerName");
        String shiftDate  = intent.getStringExtra("shiftDate");
        String shiftType  = intent.getStringExtra("shiftType");

        if (workerId == null || shiftDate == null) return;

        createChannel(context);

        // goAsync() מאפשר עבודה אסינכרונית בתוך BroadcastReceiver
        PendingResult pendingResult = goAsync();

        DatabaseService.getInstance().getWorkerAttendance(
                workerId, shiftDate,
                new DatabaseService.DatabaseCallback<Attendance>() {
                    @Override
                    public void onCompleted(Attendance attendance) {
                        try {
                            // אם העובד לא אישר הגעה — שלח התראה למנהל
                            if (attendance == null || !attendance.isConfirmed()) {
                                sendManagerAlert(context, workerId, workerName, shiftType, shiftDate);
                            }
                        } finally {
                            pendingResult.finish();
                        }
                    }

                    @Override
                    public void onFailed(Exception e) {
                        pendingResult.finish();
                    }
                });
    }

    private void sendManagerAlert(Context context, String workerId, String workerName,
                                  String shiftType, String shiftDate) {
        String name = (workerName != null && !workerName.isEmpty()) ? workerName : "עובד";
        String type = (shiftType  != null && !shiftType.isEmpty())  ? shiftType  : "";

        String title = "⚠️ עובד לא אישר הגעה";
        String text  = name + " לא אישר הגעה למשמרת " + type + " (" + shiftDate + ")";

        // לחיצה על ההתראה פותחת את מסך המשמרת הנוכחית
        Intent tapIntent = new Intent(context, CurrentShiftActivity.class);
        tapIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent pi = PendingIntent.getActivity(
                context, Math.abs(workerId.hashCode()),
                tapIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        NotificationCompat.Builder builder =
                new NotificationCompat.Builder(context, CHANNEL_ID)
                        .setSmallIcon(R.drawable.ic_launcher_foreground)
                        .setContentTitle(title)
                        .setContentText(text)
                        .setStyle(new NotificationCompat.BigTextStyle().bigText(text))
                        .setPriority(NotificationCompat.PRIORITY_HIGH)
                        .setAutoCancel(true)
                        .setContentIntent(pi);

        NotificationManager nm = (NotificationManager)
                context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (nm != null) {
            // ID ייחודי לכל עובד + תאריך
            int notifId = Math.abs((workerId + shiftDate).hashCode());
            nm.notify(notifId, builder.build());
        }
    }

    /** יוצר את ערוץ ההתראות (נדרש ב-Android O ומעלה) */
    public static void createChannel(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel ch = new NotificationChannel(
                    CHANNEL_ID,
                    "התראות אי-הגעה",
                    NotificationManager.IMPORTANCE_HIGH);
            ch.setDescription("התראה למנהל כשעובד לא מאשר הגעה למשמרת");
            NotificationManager nm = (NotificationManager)
                    context.getSystemService(Context.NOTIFICATION_SERVICE);
            if (nm != null) nm.createNotificationChannel(ch);
        }
    }
}
