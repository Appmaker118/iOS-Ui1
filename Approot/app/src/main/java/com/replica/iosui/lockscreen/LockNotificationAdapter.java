package com.replica.iosui.lockscreen;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.replica.iosui.R;

import java.util.ArrayList;
import java.util.List;

public class LockNotificationAdapter extends RecyclerView.Adapter<LockNotificationAdapter.NotifViewHolder> {

    public interface OnNotificationClickListener {
        void onNotificationClicked(LockNotificationItem item);
    }

    private final List<LockNotificationItem> items = new ArrayList<>();
    private final OnNotificationClickListener clickListener;

    public LockNotificationAdapter(OnNotificationClickListener clickListener) {
        this.clickListener = clickListener;
    }

    public void updateItems(List<LockNotificationItem> newItems) {
        items.clear();
        items.addAll(newItems);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public NotifViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_lock_notification, parent, false);
        return new NotifViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull NotifViewHolder holder, int position) {
        LockNotificationItem item = items.get(position);
        holder.tvTitle.setText(item.title);
        holder.tvBody.setText(item.text);
        holder.tvTime.setText(formatIosTime(item.postTime));
        com.replica.iosui.core.FontHelper.applySemibold(holder.tvTitle);
        com.replica.iosui.core.FontHelper.applyRegular(holder.tvBody);
        com.replica.iosui.core.FontHelper.applyRegular(holder.tvTime);


        if (item.appIcon != null) {
            holder.ivIcon.setImageBitmap(getRoundedIcon(item.appIcon));
        }

        holder.itemView.setOnClickListener(v -> {
            if (clickListener != null) {
                clickListener.onNotificationClicked(item);
            }
        });
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    private String formatIosTime(long postTime) {
        long diff = System.currentTimeMillis() - postTime;
        if (diff < DateUtils.MINUTE_IN_MILLIS) {
            return "now";
        }
        CharSequence rel = DateUtils.getRelativeTimeSpanString(
                postTime,
                System.currentTimeMillis(),
                DateUtils.MINUTE_IN_MILLIS,
                DateUtils.FORMAT_ABBREV_RELATIVE
        );
        return rel.toString();
    }

    private Bitmap getRoundedIcon(Drawable drawable) {
        int size = 96;
        Bitmap output = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(output);

        final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        final Rect rect = new Rect(0, 0, size, size);
        final RectF rectF = new RectF(rect);
        float radius = size * 0.225f; // Authentic iOS icon corner radius

        canvas.drawRoundRect(rectF, radius, radius, paint);
        paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_IN));

        Bitmap src = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888);
        Canvas srcCanvas = new Canvas(src);
        drawable.setBounds(0, 0, size, size);
        drawable.draw(srcCanvas);

        canvas.drawBitmap(src, rect, rect, paint);
        return output;
    }

    static class NotifViewHolder extends RecyclerView.ViewHolder {
        final ImageView ivIcon;
        final TextView tvTitle;
        final TextView tvTime;
        final TextView tvBody;

        NotifViewHolder(@NonNull View itemView) {
            super(itemView);
            ivIcon = itemView.findViewById(R.id.iv_notif_app_icon);
            tvTitle = itemView.findViewById(R.id.tv_notif_title);
            tvTime = itemView.findViewById(R.id.tv_notif_time);
            tvBody = itemView.findViewById(R.id.tv_notif_body);
        }
    }
}
