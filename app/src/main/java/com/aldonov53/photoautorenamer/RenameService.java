package com.aldonov53.photoautorenamer;

import android.app.*;
import android.content.*;
import android.os.*;
import java.io.File;
import java.util.Locale;

public class RenameService extends Service {
    private FileObserver observer;
    private final String CHANNEL_ID = "renamer";

    @Override public void onCreate() {
        super.onCreate();
        createNotification();
        startForeground(1, notification("Следит за DCIM/Camera"));
        startWatching();
    }

    private void startWatching() {
        File cameraDir = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM), "Camera");
        if (!cameraDir.exists()) cameraDir.mkdirs();

        observer = new FileObserver(cameraDir.getAbsolutePath(), FileObserver.CLOSE_WRITE | FileObserver.MOVED_TO) {
            @Override public void onEvent(int event, String name) {
                if (name == null) return;
                String lower = name.toLowerCase(Locale.ROOT);
                if (!(lower.endsWith(".jpg") || lower.endsWith(".jpeg") || lower.endsWith(".png") || lower.endsWith(".heic"))) return;
                if (name.matches("\\d{6}\\..+")) return; // уже переименовано
                File src = new File(cameraDir, name);
                new Thread(() -> renameWhenReady(src)).start();
            }
        };
        observer.startWatching();
    }

    private void renameWhenReady(File src) {
        try { Thread.sleep(2500); } catch (Exception ignored) {}
        if (!src.exists()) return;

        String ext = ".jpg";
        String n = src.getName();
        int dot = n.lastIndexOf('.');
        if (dot >= 0) ext = n.substring(dot).toLowerCase(Locale.ROOT);

        synchronized (this) {
            int counter = getSharedPreferences("prefs", MODE_PRIVATE).getInt("counter", 1);
            File dst;
            do {
                dst = new File(src.getParentFile(), String.format(Locale.ROOT, "%06d%s", counter, ext));
                counter++;
            } while (dst.exists());

            boolean ok = src.renameTo(dst);
            if (ok) {
                getSharedPreferences("prefs", MODE_PRIVATE).edit().putInt("counter", counter).apply();
                sendBroadcast(new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, android.net.Uri.fromFile(dst)));
                sendBroadcast(new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, android.net.Uri.fromFile(src)));
            }
        }
    }

    private void createNotification() {
        if (Build.VERSION.SDK_INT >= 26) {
            NotificationChannel ch = new NotificationChannel(CHANNEL_ID, "Photo renamer", NotificationManager.IMPORTANCE_LOW);
            getSystemService(NotificationManager.class).createNotificationChannel(ch);
        }
    }

    private Notification notification(String text) {
        Notification.Builder b = Build.VERSION.SDK_INT >= 26 ? new Notification.Builder(this, CHANNEL_ID) : new Notification.Builder(this);
        return b.setContentTitle("Photo Auto Renamer")
                .setContentText(text)
                .setSmallIcon(android.R.drawable.ic_menu_camera)
                .build();
    }

    @Override public int onStartCommand(Intent i, int flags, int startId) { return START_STICKY; }
    @Override public void onDestroy() { if (observer != null) observer.stopWatching(); super.onDestroy(); }
    @Override public IBinder onBind(Intent intent) { return null; }
}
