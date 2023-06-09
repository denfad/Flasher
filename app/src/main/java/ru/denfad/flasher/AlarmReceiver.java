package ru.denfad.flasher;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Environment;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.util.Log;

import com.yandex.disk.rest.Credentials;
import com.yandex.disk.rest.ProgressListener;
import com.yandex.disk.rest.RestClient;
import com.yandex.disk.rest.exceptions.ServerException;

import java.io.File;
import java.io.IOException;

public class AlarmReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {

        // For our recurring task, we'll just display a message
        Log.d("Reciver", "OK");
        Thread gfgThread = new Thread(new Runnable() {
            @Override
            public void run() {
                SaveImage(context, null);
                Intent intent2 = new Intent(context, AppWidget.class);
                intent2.setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE);
                int[] ids = AppWidgetManager.getInstance(context).getAppWidgetIds(new ComponentName(context, AppWidget.class));
                intent2.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids);
                context.sendBroadcast(intent2);
            }
        });
        gfgThread.start();

        Intent alarmIntent = new Intent(context, AlarmReceiver.class);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(context, 0, alarmIntent, PendingIntent.FLAG_IMMUTABLE);
        AlarmManager manager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        int interval = 3600000;

        manager.setInexactRepeating(AlarmManager.RTC_WAKEUP, System.currentTimeMillis(), interval, pendingIntent);
    }

    //Функция сохранения фото
    private void SaveImage(Context context, Bitmap finalBitmap) {
        String root = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES).toString();
        File myDir = new File(root + "/Widget");
        if (!myDir.exists()) {
            myDir.mkdirs();
        }
        String fname = "private.jpeg";
        File file = new File(myDir, fname);
        file.delete();
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(context);
        String sFrieId = settings.getString("friend", null);
        try {
            try {
                Credentials credentials = new Credentials("denfad2003", "AQAAAAArZCXAAADLWwB7Pep9mEcuhrs_TcTcdUs");
                RestClient client = new RestClient(credentials);
                Log.e("API", "Create");
                try {
                    Log.e("Error", sFrieId);
                    client.downloadFile(sFrieId+".jpg", file, new DownloadProgressListener());
                } catch (ServerException | IOException e) {
                    e.printStackTrace();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        // Tell the media scanner about the new file so that it is
        // immediately available to the user.
        IBinder binder = peekService(context, new Intent(context, MediaScannerConnection.class));
        ((MediaScannerConnection) binder).scanFile(context, new String[] { file.toString() }, null,
                new MediaScannerConnection.OnScanCompletedListener() {
                    public void onScanCompleted(String path, Uri uri) {
                        Log.i("ExternalStorage", "Scanned " + path + ":");
                        Log.i("ExternalStorage", "-> uri=" + uri);
                    }
                });
    }

    //слушатель прогресса загрузки
    public class DownloadProgressListener implements ProgressListener {

        boolean cancel = false;

        @Override
        public void updateProgress(long loaded, long total) {
            if(loaded >= total) {
                cancel = true;
                Log.d("Download image", "stop");
            }
        }

        @Override
        public boolean hasCancelled() {
            return cancel;
        }
    }
}
