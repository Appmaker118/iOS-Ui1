package com.replica.iosui;

import android.Manifest;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.widget.Button;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.replica.iosui.lockscreen.LockScreenActivity;
import com.replica.iosui.lockscreen.LockScreenService;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Button btnPermissions = findViewById(R.id.btn_grant_permissions);
        Button btnNotifAccess = findViewById(R.id.btn_notification_access);
        Button btnStartService = findViewById(R.id.btn_start_lock_service);
        Button btnPreviewLock = findViewById(R.id.btn_preview_lock_screen);

        btnPermissions.setOnClickListener(v -> requestCorePermissions());

        btnNotifAccess.setOnClickListener(v -> {
            startActivity(new Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS));
        });

        btnStartService.setOnClickListener(v -> {
            Intent serviceIntent = new Intent(this, LockScreenService.class);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(serviceIntent);
            } else {
                startService(serviceIntent);
            }
            Toast.makeText(this, "iOS Lock Screen Service Started!", Toast.LENGTH_SHORT).show();
        });

        btnPreviewLock.setOnClickListener(v -> {
            startActivity(new Intent(this, LockScreenActivity.class));
        });
    }

    private void requestCorePermissions() {
        List<String> perms = new ArrayList<>();
        perms.add(Manifest.permission.CAMERA);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            perms.add(Manifest.permission.READ_MEDIA_IMAGES);
            perms.add(Manifest.permission.POST_NOTIFICATIONS);
        } else {
            perms.add(Manifest.permission.READ_EXTERNAL_STORAGE);
        }

        ActivityCompat.requestPermissions(this, perms.toArray(new String[0]), 101);

        if (!Settings.canDrawOverlays(this)) {
            Intent overlayIntent = new Intent(
                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:" + getPackageName())
            );
            startActivity(overlayIntent);
        }
    }
}
