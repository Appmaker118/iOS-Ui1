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
import android.graphics.PixelFormat;
import android.graphics.drawable.Drawable;
import android.hardware.camera2.CameraManager;
import android.os.Build;
import android.os.ParcelFileDescriptor;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.provider.MediaStore;
import android.provider.Settings;
import android.view.ContextThemeWrapper;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.core.app.ActivityCompat;
import androidx.dynamicanimation.animation.DynamicAnimation;
import androidx.dynamicanimation.animation.SpringAnimation;
import androidx.dynamicanimation.animation.SpringForce;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.replica.iosui.R;
import com.replica.iosui.core.FontHelper;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class LockScreenOverlayManager {

    private final Context context;
    private final WindowManager windowManager;
    private View rootView;
    private boolean isShowing = false;
    private boolean isUnlocking = false;

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

    private final SimpleDateFormat dateFormat = new SimpleDateFormat("EEE MMM d", Locale.ENGLISH);

    private final BroadcastReceiver updateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context ctx, Intent intent) {
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

    public LockScreenOverlayManager(Context context) {
        this.context = new ContextThemeWrapper(context, R.style.Theme_Ios27Ui_LockScreen);
        this.windowManager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        initCamera();
    }

    private void initCamera() {
        cameraManager = (CameraManager) context.getSystemService(Context.CAMERA_SERVICE);
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
            } catch (Exception ignored) {
            }
        }
    }

    public void show(boolean useAccessibilityOverlay) {
        if (isShowing) {
            updateClockAndDate();
            refreshNotifications();
            return;
        }

        if (!useAccessibilityOverlay && Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(context)) {
            return;
        }

        try {
            rootView = LayoutInflater.from(context).inflate(R.layout.activity_lock_screen, null);
            bindViews();
            loadSystemLockWallpaper();
            setupFlashlightAndCamera();
            setupNotificationsRecycler();
            setupSwipeUpToUnlock();
            updateClockAndDate();

            int windowType;
            if (useAccessibilityOverlay && Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1) {
                windowType = WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY;
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                windowType = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY;
            } else {
                windowType = WindowManager.LayoutParams.TYPE_SYSTEM_ERROR;
            }

            int flags = WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
                    | WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
                    | WindowManager.LayoutParams.FLAG_FULLSCREEN
                    | WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
                    | WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD;

            WindowManager.LayoutParams params = new WindowManager.LayoutParams(
                    WindowManager.LayoutParams.MATCH_PARENT,
                    WindowManager.LayoutParams.MATCH_PARENT,
                    windowType,
                    flags,
                    PixelFormat.TRANSLUCENT
            );
            params.gravity = Gravity.TOP | Gravity.START;

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                params.layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES;
            }

            windowManager.addView(rootView, params);
            isShowing = true;
            isUnlocking = false;

            IntentFilter filter = new IntentFilter();
            filter.addAction(Intent.ACTION_TIME_TICK);
            filter.addAction(Intent.ACTION_TIME_CHANGED);
            filter.addAction(Intent.ACTION_DATE_CHANGED);
            filter.addAction(IosNotificationListenerService.ACTION_NOTIFICATIONS_UPDATED);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                context.registerReceiver(updateReceiver, filter, Context.RECEIVER_NOT_EXPORTED);
            } else {
                context.registerReceiver(updateReceiver, filter);
            }
        } catch (Exception e) {
            isShowing = false;
        }
    }

    public void dismiss() {
        if (!isShowing || rootView == null) return;
        try {
            context.unregisterReceiver(updateReceiver);
        } catch (Exception ignored) {
        }
        try {
            windowManager.removeViewImmediate(rootView);
        } catch (Exception ignored) {
        }
        rootView = null;
        isShowing = false;
        isUnlocking = false;
    }

    private void bindViews() {
        ivWallpaper = rootView.findViewById(R.id.iv_lock_wallpaper);
        slidingContent = rootView.findViewById(R.id.lock_sliding_content);
        tvDate = rootView.findViewById(R.id.tv_lock_date);
        glassClockView = rootView.findViewById(R.id.glass_clock_view);
        btnFlashlight = rootView.findViewById(R.id.btn_flashlight);
        ivFlashlightIcon = rootView.findViewById(R.id.iv_flashlight_icon);
        btnCamera = rootView.findViewById(R.id.btn_camera);
        rvNotifications = rootView.findViewById(R.id.rv_lock_notifications);
        swipeUpTouchZone = rootView.findViewById(R.id.swipe_up_touch_zone);

        FontHelper.applySemibold(tvDate);
    }

    private void loadSystemLockWallpaper() {
        WallpaperManager wallpaperManager = WallpaperManager.getInstance(context);
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                ParcelFileDescriptor pfd = null;
                if (ActivityCompat.checkSelfPermission(context, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
                        || ActivityCompat.checkSelfPermission(context, Manifest.permission.READ_MEDIA_IMAGES) == PackageManager.PERMISSION_GRANTED) {
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
        btnFlashlight.setOnClickListener(v -> {
            animateButtonBounce(btnFlashlight);
            triggerIosHaptic();
            if (cameraManager == null || torchCameraId == null) return;
            try {
isFlashlightOn = !isFlashlightOn;
cameraManager.setTorchMode(torchCameraId, isFlashlightOn);
if (isFlashlightOn) {
btnFlashlight.setBackgroundResource(R.drawable.bg_ios_glass_circle_active);
ivFlashlightIcon.setColorFilter(Color.parseColor("#1C1C1E"));
} else {
btnFlashlight.setBackgroundResource(R.drawable.bg_ios_glass_circle);
ivFlashlightIcon.setColorFilter(Color.WHITE);
}
} catch (Exception ignored) {
}
});
btnCamera.setOnClickListener(v -> {
animateButtonBounce(btnCamera);
triggerIosHaptic();
dismiss();
Intent camIntent = new Intent(MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA_SECURE);
camIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
try {
context.startActivity(camIntent);
} catch (Exception ignored) {
}
});
}
private void setupNotificationsRecycler() {
LinearLayoutManager layoutManager = new LinearLayoutManager(context);
layoutManager.setStackFromEnd(true);
rvNotifications.setLayoutManager(layoutManager);
notificationAdapter = new LockNotificationAdapter(item -> {
triggerIosHaptic();
completeSwipeUnlock(item.contentIntent);
});
rvNotifications.setAdapter(notificationAdapter);
refreshNotifications();
}
private void refreshNotifications() {
if (notificationAdapter == null) return;
List current = IosNotificationListenerService.getCurrentNotifications(context);
notificationAdapter.updateItems(current);
if (!current.isEmpty()) {
rvNotifications.scrollToPosition(current.size() - 1);
}
}
@SuppressLint("ClickableViewAccessibility")
private void setupSwipeUpToUnlock() {
swipeUpTouchZone.setOnTouchListener((v, event) -> {
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
if (totalUpwardSwipe > dpToPx(85)) {
completeSwipeUnlock(null);
} else {
SpringAnimation springY = new SpringAnimation(slidingContent, DynamicAnimation.TRANSLATION_Y, 0f);
springY.getSpring().setStiffness(SpringForce.STIFFNESS_MEDIUM);
springY.getSpring().setDampingRatio(SpringForce.DAMPING_RATIO_MEDIUM_BOUNCY);
springY.start();
slidingContent.animate().alpha(1f).setDuration(180).start();
}
return true;
}
return false;
});
}
private void completeSwipeUnlock(PendingIntent pendingIntent) {
isUnlocking = true;
triggerIosHaptic();
float targetY = -context.getResources().getDisplayMetrics().heightPixels;
SpringAnimation slideUp = new SpringAnimation(slidingContent, DynamicAnimation.TRANSLATION_Y, targetY);
slideUp.getSpring().setStiffness(SpringForce.STIFFNESS_MEDIUM);
slideUp.getSpring().setDampingRatio(SpringForce.DAMPING_RATIO_NO_BOUNCY);
slideUp.addEndListener((animation, canceled, value, velocity) -> {
dismiss();
if (pendingIntent != null) {
try {
pendingIntent.send();
} catch (Exception ignored) {
}
}
});
slideUp.start();
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
Vibrator vibrator = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
if (vibrator != null && vibrator.hasVibrator()) {
if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
vibrator.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_CLICK));
} else {
vibrator.vibrate(18);
}
}
}
private void updateClockAndDate() {
if (tvDate != null) tvDate.setText(dateFormat.format(new Date()));
if (glassClockView != null) glassClockView.updateTime();
}
private float dpToPx(float dp) {
return dp * context.getResources().getDisplayMetrics().density;
}
}
