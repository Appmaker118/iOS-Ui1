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

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class GlassClockView extends View {

    private Paint fillPaint;
    private Paint innerHighlightPaint;
    private Paint rimStrokePaint;
    private String timeText = "9:41";
    private final Rect textBounds = new Rect();
    private final SimpleDateFormat timeFormat = new SimpleDateFormat("h:mm", Locale.getDefault());

    // Condensed tall iOS 26/27 proportions (narrower width, taller height)
    private static final float SCALE_X = 0.84f;
    private static final float SCALE_Y = 1.24f;

    public GlassClockView(Context context) {
        super(context);
        init();
    }

    public GlassClockView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public GlassClockView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        Typeface clockTypeface = Typeface.create("sans-serif-medium", Typeface.BOLD);

        fillPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        fillPaint.setTypeface(clockTypeface);
        fillPaint.setTextAlign(Paint.Align.CENTER);
        fillPaint.setLetterSpacing(-0.045f);

        innerHighlightPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        innerHighlightPaint.setTypeface(clockTypeface);
        innerHighlightPaint.setTextAlign(Paint.Align.CENTER);
        innerHighlightPaint.setLetterSpacing(-0.045f);
        innerHighlightPaint.setColor(Color.argb(55, 255, 255, 255));

        rimStrokePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        rimStrokePaint.setTypeface(clockTypeface);
        rimStrokePaint.setTextAlign(Paint.Align.CENTER);
        rimStrokePaint.setLetterSpacing(-0.045f);
        rimStrokePaint.setStyle(Paint.Style.STROKE);
        rimStrokePaint.setStrokeWidth(2.4f);

        updateTime();
    }

    public void updateTime() {
        timeText = timeFormat.format(new Date());
        invalidate();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int width = MeasureSpec.getSize(widthMeasureSpec);
        float baseTextSize = width * 0.275f;
        if (baseTextSize <= 0) {
            baseTextSize = 200f;
        }
        fillPaint.setTextSize(baseTextSize);
        innerHighlightPaint.setTextSize(baseTextSize);
        rimStrokePaint.setTextSize(baseTextSize);

        fillPaint.getTextBounds("9:41", 0, 4, textBounds);
        int desiredHeight = (int) (textBounds.height() * SCALE_Y + dpToPx(14));
        setMeasuredDimension(width, desiredHeight);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        if (h > 0) {
            float unscaledH = h / SCALE_Y;
            // True translucent Liquid Glass gradient so wallpaper glows through
            Shader glassFill = new LinearGradient(
                    0, 0,
                    0, unscaledH,
                    new int[]{
                            Color.argb(175, 255, 255, 255),
                            Color.argb(95, 255, 255, 255),
                            Color.argb(155, 255, 255, 255)
                    },
                    new float[]{0f, 0.5f, 1f},
                    Shader.TileMode.CLAMP
            );
            fillPaint.setShader(glassFill);
            fillPaint.setShadowLayer(20f, 0f, 8f, Color.argb(40, 0, 0, 0));

            // Specular edge gradient (bright top & bottom rim)
            Shader glassRim = new LinearGradient(
                    0, 0,
                    0, unscaledH,
                    new int[]{
                            Color.argb(210, 255, 255, 255),
                            Color.argb(90, 255, 255, 255),
                            Color.argb(190, 255, 255, 255)
                    },
                    new float[]{0f, 0.5f, 1f},
                    Shader.TileMode.CLAMP
            );
            rimStrokePaint.setShader(glassRim);
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        float centerX = getWidth() / 2f;
        float unscaledHeight = getHeight() / SCALE_Y;

        canvas.save();
        // Scale around horizontal center so clock is slender & tall
        canvas.scale(SCALE_X, SCALE_Y, centerX, 0f);

        fillPaint.getTextBounds(timeText, 0, timeText.length(), textBounds);
        float baselineY = (unscaledHeight / 2f) + (textBounds.height() / 2f) - dpToPx(2);

        canvas.drawText(timeText, centerX, baselineY, fillPaint);
        canvas.drawText(timeText, centerX, baselineY - 1.5f, innerHighlightPaint);
        canvas.drawText(timeText, centerX, baselineY, rimStrokePaint);

        canvas.restore();
    }

    private float dpToPx(float dp) {
        return dp * getResources().getDisplayMetrics().density;
    }
}
