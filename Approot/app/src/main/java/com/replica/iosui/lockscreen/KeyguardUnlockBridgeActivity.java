package com.replica.iosui.lockscreen;

import android.app.Activity;
import android.app.KeyguardManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;

public class KeyguardUnlockBridgeActivity extends Activity {

    private static final String EXTRA_PENDING_INTENT = "extra_pending_intent";
    private PendingIntent pendingNotificationIntent;

    public static void triggerSystemUnlock(Context context, PendingIntent notifIntent) {
        Intent intent = new Intent(context, KeyguardUnlockBridgeActivity.class);
        intent.putExtra(EXTRA_PENDING_INTENT, notifIntent);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                | Intent.FLAG_ACTIVITY_NO_ANIMATION
                | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        try {
            context.startActivity(intent);
        } catch (Exception e) {
            if (notifIntent != null) {
                try {
                    notifIntent.send();
                } catch (Exception ignored) {
                }
            }
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        overridePendingTransition(0, 0);
        super.onCreate(savedInstanceState);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            pendingNotificationIntent = getIntent().getParcelableExtra(EXTRA_PENDING_INTENT, PendingIntent.class);
        } else {
            pendingNotificationIntent = getIntent().getParcelableExtra(EXTRA_PENDING_INTENT);
        }

        KeyguardManager km = (KeyguardManager) getSystemService(Context.KEYGUARD_SERVICE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && km != null && km.isKeyguardLocked()) {
            km.requestDismissKeyguard(this, new KeyguardManager.KeyguardDismissCallback() {
                @Override
                public void onDismissSucceeded() {
                    firePendingAndFinish();
                }

                @Override
                public void onDismissCancelled() {
                    pendingNotificationIntent = null;
                    finish();
                    overridePendingTransition(0, 0);
                }

                @Override
                public void onDismissError() {
                    firePendingAndFinish();
                }
            });
        } else {
            firePendingAndFinish();
        }
    }

    private void firePendingAndFinish() {
        if (pendingNotificationIntent != null) {
            try {
                pendingNotificationIntent.send();
            } catch (Exception ignored) {
            }
            pendingNotificationIntent = null;
        }
        finish();
        overridePendingTransition(0, 0);
    }
}
