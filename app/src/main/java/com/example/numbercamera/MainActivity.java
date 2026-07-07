package com.example.numbercamera;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.Gravity;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;

import java.io.File;
import java.io.IOException;

public class MainActivity extends Activity {
    private static final int REQ_CAMERA = 10;
    private static final int REQ_PERMISSION = 11;
    private File currentPhotoFile;
    private TextView info;
    private SharedPreferences prefs;

    @Override public void onCreate(Bundle b) {
        super.onCreate(b);
        prefs = getSharedPreferences("counter", MODE_PRIVATE);
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setGravity(Gravity.CENTER);
        layout.setPadding(40,40,40,40);
        info = new TextView(this);
        info.setTextSize(20);
        info.setGravity(Gravity.CENTER);
        updateInfo();
        Button shoot = new Button(this);
        shoot.setText("Сделать фото");
        shoot.setTextSize(22);
        shoot.setOnClickListener(v -> checkAndTakePhoto());
        Button reset = new Button(this);
        reset.setText("Сбросить счетчик на 001");
        reset.setOnClickListener(v -> { prefs.edit().putInt("next",1).apply(); updateInfo(); });
        layout.addView(info);
        layout.addView(shoot);
        layout.addView(reset);
        setContentView(layout);
    }

    private void updateInfo() {
        int next = prefs.getInt("next", 1);
        info.setText("Следующее фото: " + String.format("%03d.jpg", next) + "\nПапка: Android/data/com.example.numbercamera/files/Pictures/NumberCamera");
    }

    private void checkAndTakePhoto() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, REQ_PERMISSION);
            return;
        }
        takePhoto();
    }

    @Override public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grants) {
        super.onRequestPermissionsResult(requestCode, permissions, grants);
        if (requestCode == REQ_PERMISSION && grants.length > 0 && grants[0] == PackageManager.PERMISSION_GRANTED) takePhoto();
        else Toast.makeText(this, "Нужно разрешение камеры", Toast.LENGTH_SHORT).show();
    }

    private void takePhoto() {
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (intent.resolveActivity(getPackageManager()) == null) { Toast.makeText(this, "Камера не найдена", Toast.LENGTH_SHORT).show(); return; }
        try {
            currentPhotoFile = createNextFile();
            Uri uri = FileProvider.getUriForFile(this, getPackageName() + ".fileprovider", currentPhotoFile);
            intent.putExtra(MediaStore.EXTRA_OUTPUT, uri);
            intent.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION | Intent.FLAG_GRANT_READ_URI_PERMISSION);
            startActivityForResult(intent, REQ_CAMERA);
        } catch (IOException e) {
            Toast.makeText(this, "Ошибка файла: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private File createNextFile() throws IOException {
        File dir = new File(getExternalFilesDir(null), "Pictures/NumberCamera");
        if (!dir.exists() && !dir.mkdirs()) throw new IOException("не удалось создать папку");
        int n = prefs.getInt("next", 1);
        File f;
        do { f = new File(dir, String.format("%03d.jpg", n)); n++; } while (f.exists());
        return f;
    }

    @Override protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQ_CAMERA) {
            if (resultCode == RESULT_OK && currentPhotoFile != null && currentPhotoFile.exists()) {
                String name = currentPhotoFile.getName();
                int num = Integer.parseInt(name.substring(0,3));
                prefs.edit().putInt("next", num + 1).apply();
                Toast.makeText(this, "Сохранено: " + name, Toast.LENGTH_LONG).show();
            } else if (currentPhotoFile != null) currentPhotoFile.delete();
            updateInfo();
        }
    }
}
