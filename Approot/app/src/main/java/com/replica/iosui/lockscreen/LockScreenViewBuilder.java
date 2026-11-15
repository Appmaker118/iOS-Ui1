package com.replica.iosui.lockscreen;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import com.replica.iosui.R;

public class LockScreenViewBuilder {

    public static class Views {
        public FrameLayout root;
        public ImageView ivWallpaper;
        public RelativeLayout slidingContent;
        public TextView tvDate;
        public GlassClockView glassClockView;
        public FrameLayout btnFlashlight;
        public ImageView ivFlashlightIcon;
        public FrameLayout btnCamera;
        public LinearLayout notificationsContainer;
        public FrameLayout swipeUpTouchZone;
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

        // 2. Subtle vignette
        View vignette = new View(context);
        vignette.setLayoutParams(new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
        ));
        vignette.setBackgroundColor(Color.parseColor("#18000000"));
        v.root.addView(vignette);

        // 3. Sliding Foreground Container
        v.slidingContent = new RelativeLayout(context);
        v.slidingContent.setLayoutParams(new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
        ));
        v.root.addView(v.slidingContent);

        // 4. Top Date + Tall Condensed Glass Clock
        LinearLayout clockHeader = new LinearLayout(context);
        clockHeader.setId(View.generateViewId());
        RelativeLayout.LayoutParams headerLp = new RelativeLayout.LayoutParams(
                RelativeLayout.LayoutParams.MATCH_PARENT,
                RelativeLayout.LayoutParams.WRAP_CONTENT
        );
        headerLp.addRule(RelativeLayout.ALIGN_PARENT_TOP);
        headerLp.topMargin = Math.round(64 * d);
        clockHeader.setLayoutParams(headerLp);
        clockHeader.setOrientation(LinearLayout.VERTICAL);
        clockHeader.setGravity(Gravity.CENTER_HORIZONTAL);

        v.tvDate = new TextView(context);
        v.tvDate.setText("Wed Apr 1");
        v.tvDate.setTextColor(Color.parseColor("#F2FFFFFF"));
        v.tvDate.setTextSize(20f);
        v.tvDate.setTypeface(Typeface.create("sans-serif-medium", Typeface.BOLD));
        v.tvDate.setShadowLayer(6f, 0f, 2f, Color.parseColor("#40000000"));
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

        // 5. Bottom Action Zone: Flashlight (Left) & Camera (Right)
        FrameLayout bottomActions = new FrameLayout(context);
        bottomActions.setId(View.generateViewId());
        RelativeLayout.LayoutParams actionsLp = new RelativeLayout.LayoutParams(
                RelativeLayout.LayoutParams.MATCH_PARENT,
                RelativeLayout.LayoutParams.WRAP_CONTENT
        );
        actionsLp.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
        actionsLp.bottomMargin = Math.round(44 * d);
        int sidePad = Math.round(34 * d);
        bottomActions.setPadding(sidePad, 0, sidePad, 0);
        bottomActions.setLayoutParams(actionsLp);

        int btnSize = Math.round(50 * d);
        int iconSize = Math.round(22 * d);

        // Flashlight Button
        v.btnFlashlight = new FrameLayout(context);
        FrameLayout.LayoutParams flashLp = new FrameLayout.LayoutParams(btnSize, btnSize);
        flashLp.gravity = Gravity.START | Gravity.CENTER_VERTICAL;
        v.btnFlashlight.setLayoutParams(flashLp);
        v.btnFlashlight.setBackgroundResource(R.drawable.bg_ios_glass_circle);
        v.btnFlashlight.setClickable(true);
        v.btnFlashlight.setFocusable(true);

        v.ivFlashlightIcon = new ImageView(context);
        FrameLayout.LayoutParams flashIconLp = new FrameLayout.LayoutParams(iconSize, iconSize);
        flashIconLp.gravity = Gravity.CENTER;
        v.ivFlashlightIcon.setLayoutParams(flashIconLp);
        v.ivFlashlightIcon.setImageResource(R.drawable.ic_ios_flashlight);
        v.btnFlashlight.addView(v.ivFlashlightIcon);
        bottomActions.addView(v.btnFlashlight);

        // Camera Button
        v.btnCamera = new FrameLayout(context);
        FrameLayout.LayoutParams camLp = new FrameLayout.LayoutParams(btnSize, btnSize);
        camLp.gravity = Gravity.END | Gravity.CENTER_VERTICAL;
        v.btnCamera.setLayoutParams(camLp);
        v.btnCamera.setBackgroundResource(R.drawable.bg_ios_glass_circle);
        v.btnCamera.setClickable(true);
        v.btnCamera.setFocusable(true);

        ImageView ivCamIcon = new ImageView(context);
        FrameLayout.LayoutParams camIconLp = new FrameLayout.LayoutParams(iconSize, iconSize);
        camIconLp.gravity = Gravity.CENTER;
        ivCamIcon.setLayoutParams(camIconLp);
        ivCamIcon.setImageResource(R.drawable.ic_ios_camera);
        v.btnCamera.addView(ivCamIcon);
        bottomActions.addView(v.btnCamera);

        v.slidingContent.addView(bottomActions);

        // 6. Bottom-Up Stacked Notifications Zone
        ScrollView notifScroll = new ScrollView(context);
        RelativeLayout.LayoutParams scrollLp = new RelativeLayout.LayoutParams(
                RelativeLayout.LayoutParams.MATCH_PARENT,
                RelativeLayout.LayoutParams.WRAP_CONTENT
        );
        scrollLp.addRule(RelativeLayout.ABOVE, bottomActions.getId());
        scrollLp.addRule(RelativeLayout.BELOW, clockHeader.getId());
        scrollLp.topMargin = Math.round(12 * d);
        scrollLp.bottomMargin = Math.round(16 * d);
        notifScroll.setLayoutParams(scrollLp);
        notifScroll.setVerticalScrollBarEnabled(false);

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

        // 7. Bottom Swipe-Up Touch Zone + White iOS Home Bar
        v.swipeUpTouchZone = new FrameLayout(context);
        RelativeLayout.LayoutParams swipeLp = new RelativeLayout.LayoutParams(
                RelativeLayout.LayoutParams.MATCH_PARENT,
                Math.round(56 * d)
        );
        swipeLp.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
        v.swipeUpTouchZone.setLayoutParams(swipeLp);

        View homeBar = new View(context);
        FrameLayout.LayoutParams barLp = new FrameLayout.LayoutParams(
                Math.round(134 * d),
                Math.round(5 * d)
        );
        barLp.gravity = Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL;
        barLp.bottomMargin = Math.round(10 * d);
        homeBar.setLayoutParams(barLp);
        homeBar.setBackgroundResource(R.drawable.bg_ios_home_bar);
        v.swipeUpTouchZone.addView(homeBar);

        v.slidingContent.addView(v.swipeUpTouchZone);

        return v;
    }
}
