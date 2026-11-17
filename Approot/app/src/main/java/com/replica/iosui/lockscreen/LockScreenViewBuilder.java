package com.replica.iosui.lockscreen;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

public class LockScreenViewBuilder {

    public static class Views {
        public FrameLayout root;
        public ImageView ivWallpaper;
        public FrameLayout slidingContent;
        public TextView tvDate;
        public GlassClockView glassClockView;
        public FrameLayout btnFlashlight;
        public ImageView ivFlashlightIcon;
        public FrameLayout btnCamera;
        public LinearLayout notificationsContainer;
        public FrameLayout swipeUpTouchZone;
    }

    public static GradientDrawable createGlassCircleDrawable(boolean active, float d) {
        GradientDrawable gd = new GradientDrawable();
        gd.setShape(GradientDrawable.OVAL);
        if (active) {
            gd.setColor(Color.parseColor("#EBFFFFFF"));
            gd.setStroke(Math.round(1.2f * d), Color.WHITE);
        } else {
            gd.setColor(Color.parseColor("#381C1C1E"));
            gd.setStroke(Math.round(1.0f * d), Color.parseColor("#59FFFFFF"));
        }
        return gd;
    }

    public static GradientDrawable createNotificationCardDrawable(float d) {
        GradientDrawable gd = new GradientDrawable();
        gd.setShape(GradientDrawable.RECTANGLE);
        gd.setCornerRadius(22f * d);
        gd.setColor(Color.parseColor("#521E1E22"));
        gd.setStroke(Math.round(0.9f * d), Color.parseColor("#4DFFFFFF"));
        return gd;
    }

    public static Views build(Context context) {
        Views v = new Views();
        float d = context.getResources().getDisplayMetrics().density;

        v.root = new FrameLayout(context);
        v.root.setLayoutParams(new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
        ));
        v.root.setBackgroundColor(Color.BLACK);

        // 1. Full-screen Lock Screen Wallpaper
        v.ivWallpaper = new ImageView(context);
        v.ivWallpaper.setLayoutParams(new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
        ));
        v.ivWallpaper.setScaleType(ImageView.ScaleType.CENTER_CROP);
        v.root.addView(v.ivWallpaper);

        // 2. Subtle top/bottom vignette for contrast
        View vignette = new View(context);
        vignette.setLayoutParams(new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
        ));
        GradientDrawable vignetteGrad = new GradientDrawable(
                GradientDrawable.Orientation.TOP_BOTTOM,
                new int[]{
                        Color.parseColor("#24000000"),
                        Color.parseColor("#06000000"),
                        Color.parseColor("#30000000")
                }
        );
        vignette.setBackground(vignetteGrad);
        v.root.addView(vignette);

        // 3. Sliding Foreground Container
        v.slidingContent = new FrameLayout(context);
        v.slidingContent.setLayoutParams(new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
        ));
        v.root.addView(v.slidingContent);

        // 4. Clean Thin Pure White Date + Long Sheer Liquid Glass Clock at Top
        LinearLayout clockHeader = new LinearLayout(context);
        FrameLayout.LayoutParams headerLp = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.WRAP_CONTENT
        );
        headerLp.gravity = Gravity.TOP | Gravity.CENTER_HORIZONTAL;
        headerLp.topMargin = Math.round(62 * d);
        clockHeader.setLayoutParams(headerLp);
        clockHeader.setOrientation(LinearLayout.VERTICAL);
        clockHeader.setGravity(Gravity.CENTER_HORIZONTAL);

        v.tvDate = new TextView(context);
        LinearLayout.LayoutParams dateLp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        dateLp.gravity = Gravity.CENTER_HORIZONTAL;
        v.tvDate.setLayoutParams(dateLp);
        v.tvDate.setGravity(Gravity.CENTER);
        v.tvDate.setText("Wed Apr 1");
        v.tvDate.setTextColor(Color.WHITE);
        v.tvDate.setTextSize(19.5f);
        v.tvDate.setLetterSpacing(0.015f);
        v.tvDate.setTypeface(Typeface.create("sans-serif-light", Typeface.NORMAL));
        v.tvDate.setShadowLayer(6f, 0f, 1.5f, Color.parseColor("#45000000"));
        clockHeader.addView(v.tvDate);

        v.glassClockView = new GlassClockView(context);
        LinearLayout.LayoutParams clockLp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        clockLp.topMargin = Math.round(-4 * d);
        v.glassClockView.setLayoutParams(clockLp);
        clockHeader.addView(v.glassClockView);

        v.slidingContent.addView(clockHeader);

        // 5. Bottom-Up Stacked Notifications Zone
        ScrollView notifScroll = new ScrollView(context);
        FrameLayout.LayoutParams scrollLp = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                Math.round(310 * d)
        );
        scrollLp.gravity = Gravity.BOTTOM;
        scrollLp.bottomMargin = Math.round(108 * d);
        notifScroll.setLayoutParams(scrollLp);
        notifScroll.setVerticalScrollBarEnabled(false);
        notifScroll.setFillViewport(true);

        v.notificationsContainer = new LinearLayout(context);
        v.notificationsContainer.setLayoutParams(new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.WRAP_CONTENT
        ));
        v.notificationsContainer.setOrientation(LinearLayout.VERTICAL);
        v.notificationsContainer.setGravity(Gravity.BOTTOM);
        int notifPad = Math.round(16 * d);
        v.notificationsContainer.setPadding(notifPad, 0, notifPad, 0);
        notifScroll.addView(v.notificationsContainer);

        v.slidingContent.addView(notifScroll);

        // 6. Bottom Action Buttons: Flashlight (Left) & Camera (Right)
        FrameLayout bottomActions = new FrameLayout(context);
        FrameLayout.LayoutParams actionsLp = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                Math.round(54 * d)
        );
        actionsLp.gravity = Gravity.BOTTOM;
        actionsLp.bottomMargin = Math.round(42 * d);
        int sidePad = Math.round(34 * d);
        bottomActions.setPadding(sidePad, 0, sidePad, 0);
        bottomActions.setLayoutParams(actionsLp);

        int btnSize = Math.round(50 * d);
        int iconSize = Math.round(24 * d);

        // Flashlight Button
        v.btnFlashlight = new FrameLayout(context);
        FrameLayout.LayoutParams flashLp = new FrameLayout.LayoutParams(btnSize, btnSize);
        flashLp.gravity = Gravity.START | Gravity.CENTER_VERTICAL;
        v.btnFlashlight.setLayoutParams(flashLp);
        v.btnFlashlight.setBackground(createGlassCircleDrawable(false, d));
        v.btnFlashlight.setClickable(true);
        v.btnFlashlight.setFocusable(true);

        v.ivFlashlightIcon = new ImageView(context);
        FrameLayout.LayoutParams flashIconLp = new FrameLayout.LayoutParams(iconSize, iconSize);
        flashIconLp.gravity = Gravity.CENTER;
        v.ivFlashlightIcon.setLayoutParams(flashIconLp);
        v.ivFlashlightIcon.setImageBitmap(createFlashlightBitmap(Math.round(48 * d), Color.WHITE));
        v.btnFlashlight.addView(v.ivFlashlightIcon);
        bottomActions.addView(v.btnFlashlight);

        // Camera Button
        v.btnCamera = new FrameLayout(context);
        FrameLayout.LayoutParams camLp = new FrameLayout.LayoutParams(btnSize, btnSize);
        camLp.gravity = Gravity.END | Gravity.CENTER_VERTICAL;
        v.btnCamera.setLayoutParams(camLp);
        v.btnCamera.setBackground(createGlassCircleDrawable(false, d));
        v.btnCamera.setClickable(true);
        v.btnCamera.setFocusable(true);

        ImageView ivCamIcon = new ImageView(context);
        FrameLayout.LayoutParams camIconLp = new FrameLayout.LayoutParams(iconSize, iconSize);
        camIconLp.gravity = Gravity.CENTER;
        ivCamIcon.setLayoutParams(camIconLp);
        ivCamIcon.setImageBitmap(createCameraBitmap(Math.round(48 * d), Color.WHITE));
        v.btnCamera.addView(ivCamIcon);
        bottomActions.addView(v.btnCamera);

        v.slidingContent.addView(bottomActions);

        // 7. Bottom Swipe-Up Touch Zone + Crisp White iOS Home Bar
        v.swipeUpTouchZone = new FrameLayout(context);
        FrameLayout.LayoutParams swipeLp = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                Math.round(46 * d)
        );
        swipeLp.gravity = Gravity.BOTTOM;
        v.swipeUpTouchZone.setLayoutParams(swipeLp);

        View homeBar = new View(context);
        FrameLayout.LayoutParams barLp = new FrameLayout.LayoutParams(
                Math.round(134 * d),
                Math.round(5 * d)
        );
        barLp.gravity = Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL;
        barLp.bottomMargin = Math.round(9 * d);
        homeBar.setLayoutParams(barLp);

        GradientDrawable homeBarBg = new GradientDrawable();
        homeBarBg.setShape(GradientDrawable.RECTANGLE);
homeBarBg.setCornerRadius(100f * d);
homeBarBg.setColor(Color.parseColor("#EBFFFFFF"));
homeBar.setBackground(homeBarBg);
v.swipeUpTouchZone.addView(homeBar);
v.slidingContent.addView(v.swipeUpTouchZone);
return v;
}
public static Bitmap createFlashlightBitmap(int size, int color) {
Bitmap bmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888);
Canvas c = new Canvas(bmp);
Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
p.setColor(color);
p.setStyle(Paint.Style.FILL);
float s = size / 24f;
c.drawRoundRect(new RectF(8f * s, 2.5f * s, 16f * s, 5.2f * s), 1.2f * s, 1.2f * s, p);
Path body = new Path();
body.moveTo(8f * s, 6.3f * s);
body.lineTo(16f * s, 6.3f * s);
body.lineTo(14.1f * s, 10.1f * s);
body.lineTo(14.1f * s, 20.3f * s);
body.quadTo(14.1f * s, 21.6f * s, 12.7f * s, 21.6f * s);
body.lineTo(11.3f * s, 21.6f * s);
body.quadTo(9.9f * s, 21.6f * s, 9.9f * s, 20.3f * s);
body.lineTo(9.9f * s, 10.1f * s);
body.close();
c.drawPath(body, p);
Paint dotPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
dotPaint.setColor(color == Color.WHITE ? Color.parseColor("#66000000") : Color.WHITE);
c.drawCircle(12f * s, 12.5f * s, 1.15f * s, dotPaint);
return bmp;
}
public static Bitmap createCameraBitmap(int size, int color) {
Bitmap bmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888);
Canvas c = new Canvas(bmp);
Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
p.setColor(color);
p.setStyle(Paint.Style.FILL);
float s = size / 24f;
Path bump = new Path();
bump.moveTo(7.8f * s, 6.6f * s);
bump.lineTo(9.2f * s, 4.4f * s);
bump.lineTo(14.8f * s, 4.4f * s);
bump.lineTo(16.2f * s, 6.6f * s);
bump.close();
c.drawPath(bump, p);
c.drawRoundRect(new RectF(2f * s, 6.5f * s, 22f * s, 19.8f * s), 3.2f * s, 3.2f * s, p);
Paint ring = new Paint(Paint.ANTI_ALIAS_FLAG);
ring.setColor(Color.parseColor("#66000000"));
c.drawCircle(12f * s, 13.2f * s, 4.1f * s, ring);
c.drawCircle(12f * s, 13.2f * s, 2.6f * s, p);
return bmp;
}
}
