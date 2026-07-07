package com.numbercamera.app;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.view.Gravity;
import android.view.View;
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
    private static final int REQ_PHOTO = 1001;
    private static final int REQ_PERM = 1002;
    private File currentFile;
    private TextView info;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setGravity(Gravity.CENTER);
        root.setPadding(40, 40, 40, 40);

        TextView title = new TextView(this);
        title.setText("Number Camera");
        title.setTextSize(26);
        title.setGravity(Gravity.CENTER);

        info = new TextView(this);
        info.setTextSize(16);
        info.setGravity(Gravity.CENTER);
        updateInfo();

        Button btn = new Button(this);
        btn.setText("Сделать фото");
        btn.setTextSize(20);
        btn.setOnClickListener(v -> startPhoto());

        root.addView(title);
        root.addView(info);
        root.addView(btn);
        setContentView(root);
    }

    private File photosDir() {
        File dir = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), "NumberCamera");
        if (!dir.exists()) dir.mkdirs();
        return dir;
    }

    private int nextNumber() {
        SharedPreferences sp = getSharedPreferences("counter", MODE_PRIVATE);
        int n = sp.getInt("next", 1);
        File dir = photosDir();
        while (new File(dir, String.format("%03d.jpg", n)).exists()) n++;
        sp.edit().putInt("next", n).apply();
        return n;
    }

    private void updateInfo() {
        int n = nextNumber();
        info.setText("Следующее имя: " + String.format("%03d.jpg", n) + "\nПапка: Pictures/NumberCamera");
    }

    private void startPhoto() {
        if (android.os.Build.VERSION.SDK_INT < 29 && ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, REQ_PERM);
            return;
        }
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (intent.resolveActivity(getPackageManager()) == null) {
            Toast.makeText(this, "Камера не найдена", Toast.LENGTH_LONG).show();
            return;
        }
        int n = nextNumber();
        currentFile = new File(photosDir(), String.format("%03d.jpg", n));
        try { currentFile.createNewFile(); } catch (IOException e) { Toast.makeText(this, "Не удалось создать файл", Toast.LENGTH_LONG).show(); return; }
        Uri uri = FileProvider.getUriForFile(this, getPackageName() + ".provider", currentFile);
        intent.putExtra(MediaStore.EXTRA_OUTPUT, uri);
        intent.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION | Intent.FLAG_GRANT_READ_URI_PERMISSION);
        startActivityForResult(intent, REQ_PHOTO);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQ_PHOTO) {
            if (resultCode == RESULT_OK && currentFile != null && currentFile.exists() && currentFile.length() > 0) {
                int used = Integer.parseInt(currentFile.getName().replace(".jpg", ""));
                getSharedPreferences("counter", MODE_PRIVATE).edit().putInt("next", used + 1).apply();
                Toast.makeText(this, "Сохранено: " + currentFile.getName(), Toast.LENGTH_LONG).show();
            } else if (currentFile != null && currentFile.exists()) {
                currentFile.delete();
            }
            updateInfo();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grants) {
        super.onRequestPermissionsResult(requestCode, permissions, grants);
        if (requestCode == REQ_PERM && grants.length > 0 && grants[0] == PackageManager.PERMISSION_GRANTED) startPhoto();
        else Toast.makeText(this, "Нужно разрешение на сохранение файлов", Toast.LENGTH_LONG).show();
    }
}
