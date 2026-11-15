package com.replica.iosui;

import android.Manifest;
import android.content.ComponentName;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.replica.iosui.core.IosAccessibilityService;
import com.replica.iosui.lockscreen.IosNotificationListenerService;
import com.replica.iosui.lockscreen.LockScreenActivity;
import com.replica.iosui.lockscreen.LockScreenService;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private TextView tvStatus;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        ScrollView scrollView = new ScrollView(this);
        scrollView.setBackgroundColor(Color.BLACK);

        LinearLayout container = new LinearLayout(this);
        container.setOrientation(LinearLayout.VERTICAL);
        int pad = dp(24);
        container.setPadding(pad, dp(48), pad, pad);

        TextView title = new TextView(this);
        title.setText("iOS 27 Replica");
        title.setTextColor(Color.WHITE);
        title.setTextSize(30f);
        container.addView(title);

        tvStatus = new TextView(this);
        tvStatus.setTextColor(Color.parseColor("#30D158"));
        tvStatus.setTextSize(14f);
        LinearLayout.LayoutParams statusLp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        statusLp.topMargin = dp(8);
        statusLp.bottomMargin = dp(20);
        tvStatus.setLayoutParams(statusLp);
        container.addView(tvStatus);

        addButton(container, "1. Grant Overlay, Camera & Wallpaper Access", v -> requestCorePermissions());
        addButton(container, "2. Enable Accessibility Service (iOS 27 Engine)", v ->
                startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)));
        addButton(container, "3. Enable Lock Screen Notifications Sync", v ->
                startActivity(new Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)));
        addButton(container, "4. Set iOS 27 UI as Default Home App", v ->
                startActivity(new Intent(Settings.ACTION_HOME_SETTINGS)));
        addButton(container, "5. Start Lock Screen Background Service", v -> startLockService());
        addButton(container, "Preview iOS Lock Screen Now", v ->
                startActivity(new Intent(this, LockScreenActivity.class)));

        scrollView.addView(container);
        setContentView(scrollView);
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateStatusText();
    }

    private void updateStatusText() {
        boolean overlayOk = Build.VERSION.SDK_INT < Build.VERSION_CODES.M || Settings.canDrawOverlays(this);
        boolean accessOk = IosAccessibilityService.isRunning();
        boolean notifOk = isNotificationServiceEnabled();

        tvStatus.setText(
                "Overlay: " + (overlayOk ? "ON" : "OFF") +
                "  |  Accessibility: " + (accessOk ? "ON" : "OFF") +
                "  |  Notifications: " + (notifOk ? "ON" : "OFF")
        );
    }

    private boolean isNotificationServiceEnabled() {
        ComponentName cn = new ComponentName(this, IosNotificationListenerService.class);
        String flat = Settings.Secure.getString(getContentResolver(), "enabled_notification_listeners");
        return !TextUtils.isEmpty(flat) && flat.contains(cn.flattenToString());
    }

    private void addButton(LinearLayout parent, String label, View.OnClickListener listener) {
        Button btn = new Button(this);
        btn.setText(label);
        btn.setAllCaps(false);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp(52));
        lp.bottomMargin = dp(12);
        btn.setLayoutParams(lp);
        btn.setOnClickListener(listener);
        parent.addView(btn);
    }

    private void requestCorePermissions() {
        List<String> perms = new ArrayList<>();
        perms.add(Manifest.permission.CAMERA);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            perms.add(Manifest.permission.READ_MEDIA_IMAGES);
            perms.add(Manifest.permission.POST_NOTIFICATIONS);
        } else {
            perms.add(Manifest.permission.READ_EXTERNAL_STORAGE);
        }
        ActivityCompat.requestPermissions(this, perms.toArray(new String), 101);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)) {
            Intent overlayIntent = new Intent(
                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:" + getPackageName())
            );
            startActivity(overlayIntent);
        }
    }

    private void startLockService() {
        try {
            Intent serviceIntent = new Intent(this, LockScreenService.class);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(serviceIntent);
            } else {
                startService(serviceIntent);
            }
            Toast.makeText(this, "iOS Lock Screen Service Started!", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
