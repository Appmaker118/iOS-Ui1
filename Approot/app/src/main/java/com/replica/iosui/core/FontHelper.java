package com.replica.iosui.core;

import android.content.Context;
import android.graphics.Typeface;
import android.widget.TextView;

public class FontHelper {

    public static Typeface getRegular(Context context) {
        return Typeface.create("sans-serif", Typeface.NORMAL);
    }

    public static Typeface getSemibold(Context context) {
        return Typeface.create("sans-serif-medium", Typeface.NORMAL);
    }

    public static Typeface getBold(Context context) {
        return Typeface.create("sans-serif", Typeface.BOLD);
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
}
