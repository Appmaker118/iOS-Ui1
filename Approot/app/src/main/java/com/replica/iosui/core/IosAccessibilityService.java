package com.replica.iosui.core;

import android.accessibilityservice.AccessibilityService;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.view.accessibility.AccessibilityEvent;

import com.replica.iosui.lockscreen.LockScreenOverlayManager;

public class IosAccessibilityService extends AccessibilityService {

    private static IosAccessibilityService instance;
    private LockScreenOverlayManager lockScreenOverlayManager;

    private final BroadcastReceiver screenReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent == null || intent.getAction() == null) return;
            String action = intent.getAction();
            if (Intent.ACTION_SCREEN_OFF.equals(action) || Intent.ACTION_SCREEN_ON.equals(action)) {
                if (lockScreenOverlayManager != null) {
                    lockScreenOverlayManager.show(true);
                }
            }
        }
    };

    @Override
    protected void onServiceConnected() {
        super.onServiceConnected();
        instance = this;
        lockScreenOverlayManager = new LockScreenOverlayManager(this);

        IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_SCREEN_OFF);
        filter.addAction(Intent.ACTION_SCREEN_ON);
        registerReceiver(screenReceiver, filter);
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
    }

    @Override
    public void onInterrupt() {
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        instance = null;
        if (lockScreenOverlayManager != null) {
            lockScreenOverlayManager.dismiss();
        }
        try {
            unregisterReceiver(screenReceiver);
        } catch (Exception ignored) {
        }
    }

    public static boolean isRunning() {
        return instance != null;
    }
}
