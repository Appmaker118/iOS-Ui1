package com.replica.iosui.lockscreen;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.PendingIntent;
import android.app.WallpaperManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.hardware.camera2.CameraManager;
import android.os.Build;
import android.os.ParcelFileDescriptor;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.provider.MediaStore;
import android.provider.Settings;
import android.text.TextUtils;
import android.text.format.DateUtils;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.core.app.ActivityCompat;
import androidx.dynamicanimation.animation.DynamicAnimation;
import androidx.dynamicanimation.animation.SpringAnimation;
import androidx.dynamicanimation.animation.SpringForce;

import com.replica.iosui.core.IosAccessibilityService;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class LockScreenOverlayManager {

    private static LockScreenOverlayManager sharedInstance;

    private final Context context;
    private WindowManager activeWindowManager;
    private LockScreenViewBuilder.Views views;
    private boolean isShowing = false;
    private boolean isUnlocking = false;

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

    public static LockScreenOverlayManager getInstance(Context context) {
        if (sharedInstance == null) {
            sharedInstance = new LockScreenOverlayManager(context.getApplicationContext());
        }
        return sharedInstance;
    }

    public LockScreenOverlayManager(Context context) {
        this.context = context.getApplicationContext();
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

        IosAccessibilityService accService = IosAccessibilityService.getInstance();
        boolean canUseAccWindow = useAccessibilityOverlay
                && accService != null
                && Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1;

        boolean canDrawOverlay = Build.VERSION.SDK_INT < Build.VERSION_CODES.M || Settings.canDrawOverlays(context);

        if (!canUseAccWindow && !canDrawOverlay) {
            Toast.makeText(context, "Please grant 'Appear on top' permission first!", Toast.LENGTH_LONG).show();
            return;
        }

        try {
            Context uiContext = canUseAccWindow ? accService : context;
            activeWindowManager = (WindowManager) uiContext.getSystemService(Context.WINDOW_SERVICE);

            views = LockScreenViewBuilder.build(uiContext);
            loadSystemLockWallpaper();
            setupFlashlightAndCamera();
            refreshNotifications();
            setupSwipeUpToUnlock();
            updateClockAndDate();

            int windowType;
            if (canUseAccWindow) {
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
                    | WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED;

            WindowManager.LayoutParams params = new WindowManager.LayoutParams(
                    WindowManager.LayoutParams.MATCH_PARENT,
                    WindowManager.LayoutParams.MATCH_PARENT,
                    windowType,
                    flags,
                    PixelFormat.TRANSLUCENT
            );
            params.gravity = Gravity.TOP | Gravity.START;
            params.screenOrientation = android.content.pm.ActivityInfo.SCREEN_ORIENTATION_PORTRAIT;

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                params.layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES;
            }

            activeWindowManager.addView(views.root, params);
            isShowing = true;
            isUnlocking = false;

            IntentFilter filter = new IntentFilter();
            filter.addAction(Intent.ACTION_TIME_TICK);
            filter.addAction(Intent.ACTION_TIME_CHANGED);
            filter.addAction(Intent.ACTION_DATE_CHANGED);
            filter.addAction(IosNotificationListenerService.ACTION_NOTIFICATIONS_UPDATED);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                context.registerReceiver(updateReceiver, filter, Context.RECEIVER_EXPORTED);
            } else {
                context.registerReceiver(updateReceiver, filter);
            }
        } catch (Throwable t) {
            isShowing = false;
            Toast.makeText(context, "LockScreen Error: " + t.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    public void dismiss() {
        if (!isShowing || views == null || views.root == null) return;
        try {
            context.unregisterReceiver(updateReceiver);
        } catch (Exception ignored) {
        }
        try {
            if (activeWindowManager != null) {
                activeWindowManager.removeViewImmediate(views.root);
            }
        } catch (Exception ignored) {
        }
        views = null;
        activeWindowManager = null;
        isShowing = false;
        isUnlocking = false;
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
                        views.ivWallpaper.setImageBitmap(bitmap);
                        return;
                    }
                }
            }
            Drawable drawable = wallpaperManager.getDrawable();
            if (drawable != null) {
                views.ivWallpaper.setImageDrawable(drawable);
            }
        } catch (Exception e) {
            views.ivWallpaper.setBackgroundColor(Color.parseColor("#1C1C1E"));
        }
    }

    private void setupFlashlightAndCamera() {
        views.btnFlashlight.setOnClickListener(v -> {
            animateButtonBounce(views.btnFlashlight);
            triggerIosHaptic();
            if (cameraManager == null || torchCameraId == null) return;
            try {
isFlashlightOn = !isFlashlightOn;
cameraManager.setTorchMode(torchCameraId, isFlashlightOn);
float d = context.getResources().getDisplayMetrics().density;
if (isFlashlightOn) {
views.btnFlashlight.setBackground(LockScreenViewBuilder.createGlassCircleDrawable(true, d));
views.ivFlashlightIcon.setImageBitmap(
LockScreenViewBuilder.createFlashlightBitmap(Math.round(48 * d), Color.parseColor("#1C1C1E")));
} else {
views.btnFlashlight.setBackground(LockScreenViewBuilder.createGlassCircleDrawable(false, d));
views.ivFlashlightIcon.setImageBitmap(
LockScreenViewBuilder.createFlashlightBitmap(Math.round(48 * d), Color.WHITE));
}
} catch (Exception ignored) {
}
});
views.btnCamera.setOnClickListener(v -> {
animateButtonBounce(views.btnCamera);
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
private void refreshNotifications() {
if (views == null || views.notificationsContainer == null) return;
views.notificationsContainer.removeAllViews();
List<?> current = IosNotificationListenerService.getCurrentNotifications(context);
float d = context.getResources().getDisplayMetrics().density;
for (Object obj : current) {
LockNotificationItem item = (LockNotificationItem) obj;
LinearLayout card = new LinearLayout(context);
card.setOrientation(LinearLayout.HORIZONTAL);
card.setGravity(Gravity.CENTER_VERTICAL);
card.setBackground(LockScreenViewBuilder.createNotificationCardDrawable(d));
int padH = Math.round(14 * d);
int padV = Math.round(12 * d);
card.setPadding(padH, padV, padH, padV);
LinearLayout.LayoutParams cardLp = new LinearLayout.LayoutParams(
LinearLayout.LayoutParams.MATCH_PARENT,
LinearLayout.LayoutParams.WRAP_CONTENT
);
cardLp.topMargin = Math.round(8 * d);
card.setLayoutParams(cardLp);
ImageView ivIcon = new ImageView(context);
int iconSize = Math.round(38 * d);
ivIcon.setLayoutParams(new LinearLayout.LayoutParams(iconSize, iconSize));
ivIcon.setScaleType(ImageView.ScaleType.FIT_CENTER);
if (item.appIcon != null) {
ivIcon.setImageBitmap(getRoundedIcon(item.appIcon));
} else {
GradientDrawable fallbackBadge = new GradientDrawable();
fallbackBadge.setShape(GradientDrawable.RECTANGLE);
fallbackBadge.setCornerRadius(9f * d);
fallbackBadge.setColor(Color.parseColor("#3A3A3C"));
ivIcon.setBackground(fallbackBadge);
}
card.addView(ivIcon);
LinearLayout textCol = new LinearLayout(context);
textCol.setOrientation(LinearLayout.VERTICAL);
LinearLayout.LayoutParams colLp = new LinearLayout.LayoutParams(
0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f
);
colLp.leftMargin = Math.round(12 * d);
textCol.setLayoutParams(colLp);
LinearLayout topRow = new LinearLayout(context);
topRow.setOrientation(LinearLayout.HORIZONTAL);
topRow.setGravity(Gravity.CENTER_VERTICAL);
TextView tvTitle = new TextView(context);
tvTitle.setText(item.title);
tvTitle.setTextColor(Color.WHITE);
tvTitle.setTextSize(14f);
tvTitle.setTypeface(Typeface.create("sans-serif-medium", Typeface.BOLD));
tvTitle.setMaxLines(1);
tvTitle.setEllipsize(TextUtils.TruncateAt.END);
tvTitle.setLayoutParams(new LinearLayout.LayoutParams(
0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f
));
topRow.addView(tvTitle);
TextView tvTime = new TextView(context);
tvTime.setText(formatIosTime(item.postTime));
tvTime.setTextColor(Color.parseColor("#B3FFFFFF"));
tvTime.setTextSize(12f);
topRow.addView(tvTime);
textCol.addView(topRow);
TextView tvBody = new TextView(context);
tvBody.setText(item.text);
tvBody.setTextColor(Color.parseColor("#E6FFFFFF"));
tvBody.setTextSize(13f);
tvBody.setMaxLines(2);
tvBody.setEllipsize(TextUtils.TruncateAt.END);
textCol.addView(tvBody);
card.addView(textCol);
card.setOnClickListener(v -> {
triggerIosHaptic();
completeSwipeUnlock(item.contentIntent);
});
views.notificationsContainer.addView(card);
}
}
private String formatIosTime(long postTime) {
long diff = System.currentTimeMillis() - postTime;
if (diff < DateUtils.MINUTE_IN_MILLIS) return "now";
return DateUtils.getRelativeTimeSpanString(
postTime, System.currentTimeMillis(),
DateUtils.MINUTE_IN_MILLIS, DateUtils.FORMAT_ABBREV_RELATIVE
).toString();
}
private Bitmap getRoundedIcon(Drawable drawable) {
int size = 112;
Bitmap src = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888);
Canvas srcCanvas = new Canvas(src);
if (drawable instanceof BitmapDrawable && ((BitmapDrawable) drawable).getBitmap() != null) {
Bitmap bmp = ((BitmapDrawable) drawable).getBitmap();
srcCanvas.drawBitmap(bmp, null, new Rect(0, 0, size, size), new Paint(Paint.FILTER_BITMAP_FLAG));
} else {
Drawable copy = drawable.getConstantState() != null
? drawable.getConstantState().newDrawable().mutate()
: drawable.mutate();
copy.setBounds(0, 0, size, size);
copy.draw(srcCanvas);
}
Bitmap output = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888);
Canvas canvas = new Canvas(output);
final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
final Rect rect = new Rect(0, 0, size, size);
final RectF rectF = new RectF(rect);
float radius = size * 0.225f;
canvas.drawRoundRect(rectF, radius, radius, paint);
paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_IN));
canvas.drawBitmap(src, rect, rect, paint);
return output;
}
@SuppressLint("ClickableViewAccessibility")
private void setupSwipeUpToUnlock() {
views.swipeUpTouchZone.setOnTouchListener((v, event) -> {
if (isUnlocking) return true;
switch (event.getActionMasked()) {
case MotionEvent.ACTION_DOWN:
touchStartY = event.getRawY();
return true;
case MotionEvent.ACTION_MOVE:
float deltaY = event.getRawY() - touchStartY;
if (deltaY < 0) {
views.slidingContent.setTranslationY(deltaY);
float progress = Math.min(1f, Math.abs(deltaY) / 500f);
views.slidingContent.setAlpha(1f - (progress * 0.45f));
}
return true;
case MotionEvent.ACTION_UP:
case MotionEvent.ACTION_CANCEL:
float totalUpwardSwipe = touchStartY - event.getRawY();
if (totalUpwardSwipe > dpToPx(85)) {
completeSwipeUnlock(null);
} else {
SpringAnimation springY = new SpringAnimation(views.slidingContent, DynamicAnimation.TRANSLATION_Y, 0f);
springY.getSpring().setStiffness(SpringForce.STIFFNESS_MEDIUM);
springY.getSpring().setDampingRatio(SpringForce.DAMPING_RATIO_MEDIUM_BOUNCY);
springY.start();
views.slidingContent.animate().alpha(1f).setDuration(180).start();
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
SpringAnimation slideUp = new SpringAnimation(views.slidingContent, DynamicAnimation.TRANSLATION_Y, targetY);
slideUp.getSpring().setStiffness(SpringForce.STIFFNESS_MEDIUM);
slideUp.getSpring().setDampingRatio(SpringForce.DAMPING_RATIO_NO_BOUNCY);
slideUp.addEndListener((animation, canceled, value, velocity) -> {
KeyguardUnlockBridgeActivity.triggerSystemUnlock(context, pendingIntent);
dismiss();
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
if (views == null) return;
if (views.tvDate != null) views.tvDate.setText(dateFormat.format(new Date()));
if (views.glassClockView != null) views.glassClockView.updateTime();
}
private float dpToPx(float dp) {
return dp * context.getResources().getDisplayMetrics().density;
}
}
