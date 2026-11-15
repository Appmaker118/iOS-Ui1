package com.replica.iosui.lockscreen;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.KeyguardManager;
import android.app.PendingIntent;
import android.app.WallpaperManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.hardware.camera2.CameraManager;
import android.os.Build;
import android.os.Bundle;
import android.os.ParcelFileDescriptor;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.provider.MediaStore;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowInsets;
import android.view.WindowInsetsController;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.dynamicanimation.animation.DynamicAnimation;
import androidx.dynamicanimation.animation.SpringAnimation;
import androidx.dynamicanimation.animation.SpringForce;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.replica.iosui.R;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class LockScreenActivity extends AppCompatActivity {

    private ImageView ivWallpaper;
    private RelativeLayout slidingContent;
    private TextView tvDate;
    private GlassClockView glassClockView;
    private FrameLayout btnFlashlight;
    private ImageView ivFlashlightIcon;
    private FrameLayout btnCamera;
    private RecyclerView rvNotifications;
    private View swipeUpTouchZone;

    private LockNotificationAdapter notificationAdapter;
    private boolean isFlashlightOn = false;
    private String torchCameraId = null;
    private CameraManager cameraManager;

    private float touchStartY = 0f;
    private boolean isUnlocking = false;

    private final SimpleDateFormat dateFormat = new SimpleDateFormat("EEE MMM d", Locale.ENGLISH);

    private final BroadcastReceiver timeAndNotifReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent == null || intent.getAction() == null) return;

            String action = intent.getAction();
            if (Intent.ACTION_TIME_TICK.equals(action)
                    || Intent.ACTION_TIME_CHANGED.equals(action)
                    || Intent.ACTION_DATE_CHANGED.equals(action)) {
                updateClockAndDate();
            } else if (IosNotificationListenerService.ACTION_NOTIFICATIONS_UPDATED.equals(action)) {
                refreshNotifications();
            }
        }
    };

    private final CameraManager.TorchCallback torchCallback = new CameraManager.TorchCallback() {
        @Override
        public void onTorchModeChanged(String cameraId, boolean enabled) {
            if (cameraId.equals(torchCameraId)) {
                isFlashlightOn = enabled;
                updateFlashlightUi();
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        configureLockScreenWindow();
        setContentView(R.layout.activity_lock_screen);

        bindViews();
        loadSystemLockWallpaper();
        setupFlashlightAndCamera();
        setupNotificationsRecycler();
        setupSwipeUpToUnlock();
        updateClockAndDate();
    }

    private void configureLockScreenWindow() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true);
            setTurnScreenOn(true);
        }

        getWindow().addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
                        | WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
                        | WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
                        | WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
        );

        // Allow wallpaper & status bar overlay to extend behind camera cutout (Galaxy A21s punch-hole)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            WindowManager.LayoutParams lp = getWindow().getAttributes();
            lp.layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES;
            getWindow().setAttributes(lp);
        }

        hideSystemBars();
    }

    private void hideSystemBars() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            getWindow().setDecorFitsSystemWindows(false);
            WindowInsetsController controller = getWindow().getInsetsController();
            if (controller != null) {
                controller.hide(WindowInsets.Type.statusBars() | WindowInsets.Type.navigationBars());
                controller.setSystemBarsBehavior(WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);
            }
        } else {
            getWindow().getDecorView().setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                            | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
            );
        }
    }

    private void bindViews() {
        ivWallpaper = findViewById(R.id.iv_lock_wallpaper);
        slidingContent = findViewById(R.id.lock_sliding_content);
        tvDate = findViewById(R.id.tv_lock_date);
        glassClockView = findViewById(R.id.glass_clock_view);
        btnFlashlight = findViewById(R.id.btn_flashlight);
        ivFlashlightIcon = findViewById(R.id.iv_flashlight_icon);
        btnCamera = findViewById(R.id.btn_camera);
        rvNotifications = findViewById(R.id.rv_lock_notifications);
        swipeUpTouchZone = findViewById(R.id.swipe_up_touch_zone);
        com.replica.iosui.core.FontHelper.applySemibold(tvDate);
    }

    /**
     * Syncs directly with the Lock Screen wallpaper set in Samsung System Settings.
     * Falls back to Home Screen wallpaper if Samsung is using a unified wallpaper.
     */
    private void loadSystemLockWallpaper() {
        WallpaperManager wallpaperManager = WallpaperManager.getInstance(this);
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                ParcelFileDescriptor pfd = null;
                if (ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
                        || ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_IMAGES) == PackageManager.PERMISSION_GRANTED) {
                    pfd = wallpaperManager.getWallpaperFile(WallpaperManager.FLAG_LOCK);
                    if (pfd == null) {
                        pfd = wallpaperManager.getWallpaperFile(WallpaperManager.FLAG_SYSTEM);
                    }
                }
                if (pfd != null) {
                    Bitmap bitmap = BitmapFactory.decodeFileDescriptor(pfd.getFileDescriptor());
                    pfd.close();
                    if (bitmap != null) {
                        ivWallpaper.setImageBitmap(bitmap);
                        return;
                    }
                }
            }

            Drawable drawable = wallpaperManager.getDrawable();
            if (drawable != null) {
                ivWallpaper.setImageDrawable(drawable);
            }
        } catch (Exception e) {
            ivWallpaper.setBackgroundColor(Color.parseColor("#1C1C1E"));
        }
    }

    private void setupFlashlightAndCamera() {
        cameraManager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
        if (cameraManager != null) {
            try {
                for (String id : cameraManager.getCameraIdList()) {
                    Boolean hasFlash = cameraManager.getCameraCharacteristics(id)
                            .get(android.hardware.camera2.CameraCharacteristics.FLASH_INFO_AVAILABLE);
                    if (Boolean.TRUE.equals(hasFlash)) {
                        torchCameraId = id;
                        break;
                    }
                }
                cameraManager.registerTorchCallback(torchCallback, null);
            } catch (Exception ignored) {
            }
        }

        btnFlashlight.setOnClickListener(v -> {
            animateButtonBounce(btnFlashlight);
            triggerIosHaptic();
            toggleFlashlight();
        });

        btnCamera.setOnClickListener(v -> {
            animateButtonBounce(btnCamera);
            triggerIosHaptic();
            openCameraFromLockScreen();
        });
    }

    private void toggleFlashlight() {
        if (cameraManager == null || torchCameraId == null) return;
        try {
            isFlashlightOn = !isFlashlightOn;
            cameraManager.setTorchMode(torchCameraId, isFlashlightOn);
            updateFlashlightUi();
        } catch (Exception ignored) {
        }
    }

    private void updateFlashlightUi() {
        if (isFlashlightOn) {
            btnFlashlight.setBackgroundResource(R.drawable.bg_ios_glass_circle_active);
            ivFlashlightIcon.setColorFilter(Color.parseColor("#1C1C1E"));
        } else {
            btnFlashlight.setBackgroundResource(R.drawable.bg_ios_glass_circle);
            ivFlashlightIcon.setColorFilter(Color.WHITE);
        }
    }

    private void openCameraFromLockScreen() {
        Intent camIntent = new Intent(MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA_SECURE);
        camIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
try {
startActivity(camIntent);
finish();
} catch (Exception e) {
Intent fallback = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
fallback.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
try {
startActivity(fallback);
finish();
} catch (Exception ignored) {
}
}
}
private void setupNotificationsRecycler() {
LinearLayoutManager layoutManager = new LinearLayoutManager(this);
// Stacks notifications cleanly from the bottom up, just like iOS
layoutManager.setStackFromEnd(true);
rvNotifications.setLayoutManager(layoutManager);
notificationAdapter = new LockNotificationAdapter(item -> {
triggerIosHaptic();
unlockAndOpenNotification(item);
});
rvNotifications.setAdapter(notificationAdapter);
refreshNotifications();
}
private void refreshNotifications() {
List current = IosNotificationListenerService.getCurrentNotifications(this);
notificationAdapter.updateItems(current);
if (!current.isEmpty()) {
rvNotifications.scrollToPosition(current.size() - 1);
}
}
/**
* Handles interactive swipe-up on the bottom white Home Bar.
* Smoothly transitions to Samsung's system PIN / Fingerprint screen without interfering with device security.
*/
@SuppressLint("ClickableViewAccessibility")
private void setupSwipeUpToUnlock() {
View.OnTouchListener swipeListener = (v, event) -> {
if (isUnlocking) return true;
switch (event.getActionMasked()) {
case MotionEvent.ACTION_DOWN:
touchStartY = event.getRawY();
return true;
case MotionEvent.ACTION_MOVE:
float deltaY = event.getRawY() - touchStartY;
if (deltaY < 0) {
slidingContent.setTranslationY(deltaY);
float progress = Math.min(1f, Math.abs(deltaY) / 500f);
slidingContent.setAlpha(1f - (progress * 0.45f));
}
return true;
case MotionEvent.ACTION_UP:
case MotionEvent.ACTION_CANCEL:
float totalUpwardSwipe = touchStartY - event.getRawY();
if (totalUpwardSwipe > dpToPx(90)) {
completeSwipeUnlock(null);
} else {
springBackLockScreen();
}
return true;
}
return false;
};
swipeUpTouchZone.setOnTouchListener(swipeListener);
}
private void completeSwipeUnlock(PendingIntent pendingIntent) {
isUnlocking = true;
triggerIosHaptic();
float targetY = -getResources().getDisplayMetrics().heightPixels;
SpringAnimation slideUp = new SpringAnimation(slidingContent, DynamicAnimation.TRANSLATION_Y, targetY);
slideUp.getSpring().setStiffness(SpringForce.STIFFNESS_MEDIUM);
slideUp.getSpring().setDampingRatio(SpringForce.DAMPING_RATIO_NO_BOUNCY);
slideUp.addEndListener((animation, canceled, value, velocity) -> handoffToSystemKeyguard(pendingIntent));
slideUp.start();
}
private void springBackLockScreen() {
SpringAnimation springY = new SpringAnimation(slidingContent, DynamicAnimation.TRANSLATION_Y, 0f);
springY.getSpring().setStiffness(SpringForce.STIFFNESS_MEDIUM);
springY.getSpring().setDampingRatio(SpringForce.DAMPING_RATIO_MEDIUM_BOUNCY);
springY.start();
slidingContent.animate().alpha(1f).setDuration(180).start();
}
private void unlockAndOpenNotification(LockNotificationItem item) {
completeSwipeUnlock(item.contentIntent);
}
private void handoffToSystemKeyguard(PendingIntent pendingIntent) {
KeyguardManager km = (KeyguardManager) getSystemService(Context.KEYGUARD_SERVICE);
if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && km != null && km.isKeyguardLocked()) {
km.requestDismissKeyguard(this, new KeyguardManager.KeyguardDismissCallback() {
@Override
public void onDismissSucceeded() {
firePendingIntentIfPresent(pendingIntent);
finishAndRemoveTask();
}
@Override
public void onDismissCancelled() {
// User cancelled Samsung PIN entry -> reset iOS Lock Screen smoothly
isUnlocking = false;
slidingContent.setTranslationY(0f);
slidingContent.setAlpha(1f);
}
@Override
public void onDismissError() {
finishAndRemoveTask();
}
});
} else {
firePendingIntentIfPresent(pendingIntent);
finishAndRemoveTask();
}
}
private void firePendingIntentIfPresent(PendingIntent pendingIntent) {
if (pendingIntent != null) {
try {
pendingIntent.send();
} catch (Exception ignored) {
}
}
}
private void animateButtonBounce(View view) {
view.setScaleX(0.84f);
view.setScaleY(0.84f);
SpringAnimation scaleX = new SpringAnimation(view, DynamicAnimation.SCALE_X, 1f);
scaleX.getSpring().setStiffness(SpringForce.STIFFNESS_MEDIUM);
scaleX.getSpring().setDampingRatio(SpringForce.DAMPING_RATIO_MEDIUM_BOUNCY);
SpringAnimation scaleY = new SpringAnimation(view, DynamicAnimation.SCALE_Y, 1f);
scaleY.getSpring().setStiffness(SpringForce.STIFFNESS_MEDIUM);
scaleY.getSpring().setDampingRatio(SpringForce.DAMPING_RATIO_MEDIUM_BOUNCY);
scaleX.start();
scaleY.start();
}
private void triggerIosHaptic() {
Vibrator vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
if (vibrator != null && vibrator.hasVibrator()) {
if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
vibrator.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_CLICK));
} else {
vibrator.vibrate(18);
}
}
}
private void updateClockAndDate() {
tvDate.setText(dateFormat.format(new Date()));
glassClockView.updateTime();
}
@Override
protected void onResume() {
super.onResume();
hideSystemBars();
loadSystemLockWallpaper();
updateClockAndDate();
refreshNotifications();
IntentFilter filter = new IntentFilter();
filter.addAction(Intent.ACTION_TIME_TICK);
filter.addAction(Intent.ACTION_TIME_CHANGED);
filter.addAction(Intent.ACTION_DATE_CHANGED);
filter.addAction(IosNotificationListenerService.ACTION_NOTIFICATIONS_UPDATED);
if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
registerReceiver(timeAndNotifReceiver, filter, Context.RECEIVER_NOT_EXPORTED);
} else {
registerReceiver(timeAndNotifReceiver, filter);
}
}
@Override
protected void onPause() {
super.onPause();
try {
unregisterReceiver(timeAndNotifReceiver);
} catch (Exception ignored) {
}
}
@Override
protected void onDestroy() {
super.onDestroy();
if (cameraManager != null) {
try {
cameraManager.unregisterTorchCallback(torchCallback);
} catch (Exception ignored) {
}
}
}
@Override
public void onBackPressed() {
// Block back button on Lock Screen just like real iOS
}
private float dpToPx(float dp) {
return dp * getResources().getDisplayMetrics().density;
}
}
