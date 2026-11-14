package com.replica.iosui.lockscreen;

import android.app.PendingIntent;
import android.graphics.drawable.Drawable;

public class LockNotificationItem {
    public final String key;
    public final String packageName;
    public final String title;
    public final String text;
    public final long postTime;
    public final Drawable appIcon;
    public final PendingIntent contentIntent;

    public LockNotificationItem(String key, String packageName, String title, String text,
                                long postTime, Drawable appIcon, PendingIntent contentIntent) {
        this.key = key;
        this.packageName = packageName;
        this.title = title;
        this.text = text;
        this.postTime = postTime;
        this.appIcon = appIcon;
        this.contentIntent = contentIntent;
    }
}
