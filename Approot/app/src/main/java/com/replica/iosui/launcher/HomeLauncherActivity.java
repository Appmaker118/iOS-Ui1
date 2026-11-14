package com.replica.iosui.launcher;

import android.content.Intent;
import android.os.Bundle;
import android.view.Gravity;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.replica.iosui.MainActivity;

public class HomeLauncherActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setGravity(Gravity.CENTER);
        root.setPadding(48, 48, 48, 48);

        TextView label = new TextView(this);
        label.setText("iOS 27 Home Launcher Ready\n(Home Screen Icons & Dock will be added in their dedicated step)");
        label.setTextColor(0xFFFFFFFF);
        label.setTextSize(16f);
        label.setGravity(Gravity.CENTER);
        root.addView(label);

        Button btnOpenSetup = new Button(this);
        btnOpenSetup.setText("Open iOS 27 Setup Hub");
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        params.topMargin = 32;
        btnOpenSetup.setLayoutParams(params);
        btnOpenSetup.setOnClickListener(v -> startActivity(new Intent(this, MainActivity.class)));
        root.addView(btnOpenSetup);

        setContentView(root);
    }

    @Override
    public void onBackPressed() {
        // Home screen never closes on Back press
    }
}
