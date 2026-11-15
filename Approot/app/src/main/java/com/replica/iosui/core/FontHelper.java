package com.replica.iosui.core;

import android.content.Context;
import android.graphics.Typeface;
import android.widget.TextView;

import androidx.core.content.res.ResourcesCompat;

import com.replica.iosui.R;

public class FontHelper {

    private static Typeface regular;
    private static Typeface semibold;
    private static Typeface bold;

    public static Typeface getRegular(Context context) {
        if (regular == null) {
            regular = loadSafe(context, R.font.sf_pro_regular, Typeface.NORMAL);
        }
        return regular;
    }

    public static Typeface getSemibold(Context context) {
        if (semibold == null) {
            semibold = loadSafe(context, R.font.sf_pro_semibold, Typeface.BOLD);
        }
        return semibold;
    }

    public static Typeface getBold(Context context) {
        if (bold == null) {
            bold = loadSafe(context, R.font.sf_pro_bold, Typeface.BOLD);
        }
        return bold;
    }

    public static void applyRegular(TextView tv) {
        if (tv != null) tv.setTypeface(getRegular(tv.getContext()));
    }

    public static void applySemibold(TextView tv) {
        if (tv != null) tv.setTypeface(getSemibold(tv.getContext()));
    }

    public static void applyBold(TextView tv) {
        if (tv != null) tv.setTypeface(getBold(tv.getContext()));
    }

    private static Typeface loadSafe(Context context, int resId, int fallbackStyle) {
        try {
            Typeface tf = ResourcesCompat.getFont(context, resId);
            if (tf != null) return tf;
        } catch (Throwable ignored) {
        }
        try {
            return Typeface.create("sans-serif", fallbackStyle);
        } catch (Throwable ignored) {
            return Typeface.DEFAULT;
        }
    }
}
