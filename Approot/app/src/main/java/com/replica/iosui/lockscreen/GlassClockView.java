package com.replica.iosui.lockscreen;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Shader;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.Nullable;
import androidx.core.content.res.ResourcesCompat;

import com.replica.iosui.R;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class GlassClockView extends View {

    private Paint fillPaint;
    private Paint strokePaint;
    private String timeText = "9:41";
    private final Rect textBounds = new Rect();
    private final SimpleDateFormat timeFormat = new SimpleDateFormat("h:mm", Locale.getDefault());

    // Vertical stretch factor to turn SF Pro Bold into the tall condensed iOS Lock Screen clock
    private static final float VERTICAL_STRETCH = 1.38f;

    public GlassClockView(Context context) {
        super(context);
        init(context);
    }

    public GlassClockView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public GlassClockView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    private void init(Context context) {
        Typeface sfBold = ResourcesCompat.getFont(context, R.font.sf_pro_bold);

        fillPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        fillPaint.setTypeface(sfBold);
        fillPaint.setTextAlign(Paint.Align.CENTER);
        fillPaint.setLetterSpacing(-0.04f);

        strokePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        strokePaint.setTypeface(sfBold);
        strokePaint.setTextAlign(Paint.Align.CENTER);
        strokePaint.setLetterSpacing(-0.04f);
        strokePaint.setStyle(Paint.Style.STROKE);
        strokePaint.setStrokeWidth(2.2f);
        strokePaint.setColor(Color.argb(115, 255, 255, 255));

        updateTime();
    }

    public void updateTime() {
        timeText = timeFormat.format(new Date());
        invalidate();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int width = MeasureSpec.getSize(widthMeasureSpec);
        // Responsive text size based on screen width (matches iOS proportions on A21s & flagships)
        float baseTextSize = width * 0.255f;
        if (baseTextSize <= 0) {
            baseTextSize = 180f;
        }
        fillPaint.setTextSize(baseTextSize);
        strokePaint.setTextSize(baseTextSize);

        fillPaint.getTextBounds("9:41", 0, 4, textBounds);
        int desiredHeight = (int) (textBounds.height() * VERTICAL_STRETCH + dpToPx(16));
        setMeasuredDimension(width, desiredHeight);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        if (h > 0) {
            // Translucent Liquid Glass vertical gradient so wallpaper bleeds through
            Shader glassShader = new LinearGradient(
                    0, 0,
                    0, h / VERTICAL_STRETCH,
                    new int[]{
                            Color.argb(230, 255, 255, 255),
                            Color.argb(165, 255, 255, 255),
                            Color.argb(215, 255, 255, 255)
                    },
                    new float[]{0f, 0.55f, 1f},
                    Shader.TileMode.CLAMP
            );
            fillPaint.setShader(glassShader);
            fillPaint.setShadowLayer(18f, 0f, 6f, Color.argb(45, 0, 0, 0));
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        float centerX = getWidth() / 2f;

        canvas.save();
        canvas.scale(1.0f, VERTICAL_STRETCH);

        fillPaint.getTextBounds(timeText, 0, timeText.length(), textBounds);
        float unscaledHeight = getHeight() / VERTICAL_STRETCH;
        float baselineY = (unscaledHeight / 2f) + (textBounds.height() / 2f) - dpToPx(2);

        // Draw subtle liquid glass fill + specular edge rim
        canvas.drawText(timeText, centerX, baselineY, fillPaint);
        canvas.drawText(timeText, centerX, baselineY, strokePaint);

        canvas.restore();
    }

    private float dpToPx(float dp) {
        return dp * getResources().getDisplayMetrics().density;
    }
}
