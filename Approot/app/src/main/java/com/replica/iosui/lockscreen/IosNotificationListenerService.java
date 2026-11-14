package com.replica.iosui.lockscreen;

import android.app.Notification;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;
import android.text.TextUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class IosNotificationListenerService extends NotificationListenerService {

    public static final String ACTION_NOTIFICATIONS_UPDATED = "com.replica.iosui.NOTIFICATIONS_UPDATED";
    private static IosNotificationListenerService instance;

    @Override
    public void onListenerConnected() {
        super.onListenerConnected();
        instance = this;
        notifyLockScreen();
    }

    @Override
    public void onListenerDisconnected() {
        super.onListenerDisconnected();
        if (instance == this) {
            instance = null;
        }
    }

    @Override
    public void onNotificationPosted(StatusBarNotification sbn) {
        super.onNotificationPosted(sbn);
        notifyLockScreen();
    }

    @Override
    public void onNotificationRemoved(StatusBarNotification sbn) {
        super.onNotificationRemoved(sbn);
        notifyLockScreen();
    }

    private void notifyLockScreen() {
        Intent intent = new Intent(ACTION_NOTIFICATIONS_UPDATED);
        intent.setPackage(getPackageName());
        sendBroadcast(intent);
    }

    public static List<LockNotificationItem> getCurrentNotifications(Context context) {
        List<LockNotificationItem> result = new ArrayList<>();
        if (instance == null) {
            return result;
        }

        StatusBarNotification[] active;
        try {
            active = instance.getActiveNotifications();
        } catch (Exception e) {
            return result;
        }

        if (active == null) {
            return result;
        }

        PackageManager pm = context.getPackageManager();
        String ownPkg = context.getPackageName();

        for (StatusBarNotification sbn : active) {
            if (sbn == null || ownPkg.equals(sbn.getPackageName())) {
                continue;
            }

            Notification notif = sbn.getNotification();
            if (notif == null) {
                continue;
            }

            // Skip ongoing silent background services (like VPN or battery monitors)
            if ((notif.flags & Notification.FLAG_ONGOING_EVENT) != 0
                    && (notif.flags & Notification.FLAG_FOREGROUND_SERVICE) != 0) {
                continue;
            }

            Bundle extras = notif.extras;
            if (extras == null) {
                continue;
            }

            CharSequence titleCs = extras.getCharSequence(Notification.EXTRA_TITLE);
            CharSequence textCs = extras.getCharSequence(Notification.EXTRA_TEXT);
            if (TextUtils.isEmpty(textCs)) {
                textCs = extras.getCharSequence(Notification.EXTRA_BIG_TEXT);
            }

            String title = titleCs != null ? titleCs.toString().trim() : "";
            String text = textCs != null ? textCs.toString().trim() : "";

            if (TextUtils.isEmpty(title) && TextUtils.isEmpty(text)) {
                continue;
            }

            if (TextUtils.isEmpty(title)) {
                title = getAppName(pm, sbn.getPackageName());
            }

            Drawable appIcon = getAppIcon(pm, sbn.getPackageName());

            result.add(new LockNotificationItem(
                    sbn.getKey(),
                    sbn.getPackageName(),
                    title,
                    text,
                    sbn.getPostTime(),
                    appIcon,
                    notif.contentIntent
            ));
        }

        // Sort oldest -> newest so newest sits right at the bottom of the stack
        Collections.sort(result, (a, b) -> Long.compare(a.postTime, b.postTime));
        return result;
    }

    private static String getAppName(PackageManager pm, String packageName) {
        try {
            ApplicationInfo info = pm.getApplicationInfo(packageName, 0);
            return pm.getApplicationLabel(info).toString();
        } catch (Exception e) {
            return packageName;
        }
    }

    private static Drawable getAppIcon(PackageManager pm, String packageName) {
        try {
            return pm.getApplicationIcon(packageName);
        } catch (Exception e) {
            return null;
        }
    }
}
