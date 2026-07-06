package com.aldonov53.photoautorenamer;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.Settings;
import android.widget.Button;
import android.widget.TextView;
import android.view.ViewGroup;
import android.graphics.Typeface;
import android.content.pm.PackageManager;

public class MainActivity extends Activity {
    TextView status;

    @Override public void onCreate(Bundle b) {
        super.onCreate(b);

        status = new TextView(this);
        status.setTextSize(18);
        status.setPadding(40, 60, 40, 20);

        Button grant = new Button(this);
        grant.setText("1. Дать доступ к файлам");

        Button start = new Button(this);
        start.setText("2. Запустить переименование");

        Button stop = new Button(this);
        stop.setText("Остановить");

        android.widget.LinearLayout layout = new android.widget.LinearLayout(this);
        layout.setOrientation(android.widget.LinearLayout.VERTICAL);
        layout.setPadding(20, 20, 20, 20);
        layout.addView(status, new ViewGroup.LayoutParams(-1, -2));
        layout.addView(grant);
        layout.addView(start);
        layout.addView(stop);
        setContentView(layout);

        grant.setOnClickListener(v -> requestPermissionsNow());
        start.setOnClickListener(v -> {
            Intent i = new Intent(this, RenameService.class);
            if (Build.VERSION.SDK_INT >= 26) startForegroundService(i); else startService(i);
            updateStatus();
        });
        stop.setOnClickListener(v -> {
            stopService(new Intent(this, RenameService.class));
            updateStatus();
        });

        updateStatus();
    }

    void requestPermissionsNow() {
        if (Build.VERSION.SDK_INT >= 30 && !Environment.isExternalStorageManager()) {
            Intent i = new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION);
            i.setData(Uri.parse("package:" + getPackageName()));
            startActivity(i);
        } else if (Build.VERSION.SDK_INT >= 23) {
            requestPermissions(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, 10);
        }
        if (Build.VERSION.SDK_INT >= 33 && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, 11);
        }
    }

    void updateStatus() {
        boolean files = Build.VERSION.SDK_INT < 30 || Environment.isExternalStorageManager();
        status.setTypeface(Typeface.DEFAULT_BOLD);
        status.setText("Папка: DCIM/Camera\nИмена: 000001.jpg, 000002.jpg...\nДоступ к файлам: " + (files ? "есть" : "нет"));
    }

    @Override protected void onResume() { super.onResume(); updateStatus(); }
}
