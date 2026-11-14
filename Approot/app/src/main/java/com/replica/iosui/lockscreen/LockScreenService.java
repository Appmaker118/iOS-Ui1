package com.replica.iosui.lockscreen;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.IBinder;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

public class LockScreenService extends Service {

    private static final String CHANNEL_ID = "ios27_lockscreen_channel";
    private static final int NOTIF_ID = 1001;

    private final BroadcastReceiver screenReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent == null || intent.getAction() == null) return;

            String action = intent.getAction();
            if (Intent.ACTION_SCREEN_OFF.equals(action) || Intent.ACTION_SCREEN_ON.equals(action)) {
                launchLockScreen(context);
            }
        }
    };

    @Override
    public void onCreate() {
        super.onCreate();
        startForegroundServiceNotification();

        IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_SCREEN_OFF);
        filter.addAction(Intent.ACTION_SCREEN_ON);
        registerReceiver(screenReceiver, filter);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        try {
            unregisterReceiver(screenReceiver);
        } catch (Exception ignored) {
        }
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private void launchLockScreen(Context context) {
        Intent lockIntent = new Intent(context, LockScreenActivity.class);
        lockIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                | Intent.FLAG_ACTIVITY_SINGLE_TOP
                | Intent.FLAG_ACTIVITY_CLEAR_TOP
                | Intent.FLAG_ACTIVITY_NO_ANIMATION);
        try {
            context.startActivity(lockIntent);
        } catch (Exception ignored) {
        }
    }

    private void startForegroundServiceNotification() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "iOS 27 Lock Screen Engine",
                    NotificationManager.IMPORTANCE_MIN
            );
            channel.setShowBadge(false);
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }

        Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("iOS 27 Lock Screen Active")
                .setContentText("Running smoothly in background")
                .setSmallIcon(android.R.drawable.ic_lock_idle_lock)
                .setPriority(NotificationCompat.PRIORITY_MIN)
                .setOngoing(true)
                .build();

        startForeground(NOTIF_ID, notification);
    }
}
