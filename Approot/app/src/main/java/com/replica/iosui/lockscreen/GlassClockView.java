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

    private Paint sheerGlassFillPaint;
    private Paint diagonalShinePaint;
    private Paint glassEdgeRimPaint;
    private String timeText = "9:41";
    private final Rect textBounds = new Rect();
    private final SimpleDateFormat timeFormat = new SimpleDateFormat("h:mm", Locale.getDefault());

    // Long, slender, calm iOS Liquid Glass proportions
    private static final float SCALE_X = 0.72f;
    private static final float SCALE_Y = 1.52f;

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
        Typeface sleekTypeface = Typeface.create("sans-serif-light", Typeface.BOLD);

        sheerGlassFillPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        sheerGlassFillPaint.setTypeface(sleekTypeface);
        sheerGlassFillPaint.setTextAlign(Paint.Align.CENTER);
        sheerGlassFillPaint.setLetterSpacing(-0.045f);

        diagonalShinePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        diagonalShinePaint.setTypeface(sleekTypeface);
        diagonalShinePaint.setTextAlign(Paint.Align.CENTER);
        diagonalShinePaint.setLetterSpacing(-0.045f);

        glassEdgeRimPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        glassEdgeRimPaint.setTypeface(sleekTypeface);
        glassEdgeRimPaint.setTextAlign(Paint.Align.CENTER);
        glassEdgeRimPaint.setLetterSpacing(-0.045f);
        glassEdgeRimPaint.setStyle(Paint.Style.STROKE);
        glassEdgeRimPaint.setStrokeWidth(1.65f);

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
        sheerGlassFillPaint.setTextSize(baseTextSize);
        diagonalShinePaint.setTextSize(baseTextSize);
        glassEdgeRimPaint.setTextSize(baseTextSize);

        sheerGlassFillPaint.getTextBounds("9:41", 0, 4, textBounds);
        int desiredHeight = (int) (textBounds.height() * SCALE_Y + dpToPx(16));
        setMeasuredDimension(width, desiredHeight);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        if (w > 0 && h > 0) {
            float unscaledH = h / SCALE_Y;

            // Ultra-sheer glass tint so the wallpaper blends directly through the numbers
            Shader sheerBody = new LinearGradient(
                    0, 0,
                    0, unscaledH,
                    new int[]{
                            Color.argb(68, 255, 255, 255),
                            Color.argb(26, 255, 255, 255),
                            Color.argb(58, 255, 255, 255)
                    },
                    new float[]{0f, 0.52f, 1f},
                    Shader.TileMode.CLAMP
            );
            sheerGlassFillPaint.setShader(sheerBody);
            sheerGlassFillPaint.setShadowLayer(24f, 0f, 10f, Color.argb(28, 0, 0, 0));

            // Soft diagonal glass light reflection (gives realistic polished glass shine)
            Shader diagonalShine = new LinearGradient(
                    w * 0.22f, 0,
                    w * 0.78f, unscaledH,
                    new int[]{
                            Color.argb(95, 255, 255, 255),
                            Color.argb(12, 255, 255, 255),
                            Color.argb(72, 255, 255, 255)
                    },
                    new float[]{0f, 0.48f, 1f},
                    Shader.TileMode.CLAMP
            );
            diagonalShinePaint.setShader(diagonalShine);

            // Delicate refractive glass rim that keeps digits crisp without looking bulky
            Shader glassRim = new LinearGradient(
                    0, 0,
                    0, unscaledH,
                    new int[]{
                            Color.argb(175, 255, 255, 255),
                            Color.argb(65, 255, 255, 255),
                            Color.argb(150, 255, 255, 255)
                    },
                    new float[]{0f, 0.5f, 1f},
                    Shader.TileMode.CLAMP
            );
            glassEdgeRimPaint.setShader(glassRim);
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        float centerX = getWidth() / 2f;
        float unscaledHeight = getHeight() / SCALE_Y;

        canvas.save();
        canvas.scale(SCALE_X, SCALE_Y, centerX, 0f);

        sheerGlassFillPaint.getTextBounds(timeText, 0, timeText.length(), textBounds);
        float baselineY = (unscaledHeight / 2f) + (textBounds.height() / 2f) - dpToPx(2);

        canvas.drawText(timeText, centerX, baselineY, sheerGlassFillPaint);
        canvas.drawText(timeText, centerX, baselineY, diagonalShinePaint);
        canvas.drawText(timeText, centerX, baselineY, glassEdgeRimPaint);

        canvas.restore();
    }

    private float dpToPx(float dp) {
        return dp * getResources().getDisplayMetrics().density;
    }
}
