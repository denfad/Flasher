package ru.denfad.flasher;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.os.IBinder;
import android.preference.PreferenceManager;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import android.util.Log;

import com.yandex.disk.rest.Credentials;
import com.yandex.disk.rest.ProgressListener;
import com.yandex.disk.rest.ResourcesArgs;
import com.yandex.disk.rest.RestClient;
import com.yandex.disk.rest.exceptions.ServerException;

import java.io.File;
import java.io.IOException;
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;

public class ImageService extends Service {

    private static Timer timer = new Timer();
    private Context ctx;
    private final String IP = "http://10.0.2.2:8080/?login=";
    private static final int NOTIF_ID = 1;
    public static final String NOTIF_CHANNEL_ID = "2";
    public Date date;

    @Override
    public void onCreate() {
        super.onCreate();
        timer = new Timer();
        ctx = this;
    }

    private class MainTask extends TimerTask
    {
        public void run()
        {
            try {
                Thread gfgThread = new Thread(new Runnable() {
                    @Override
                    public void run() {
                        SaveImage(getApplicationContext(), null);

                    }
                });
                gfgThread.start();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }


    @Override
    public void onDestroy() {
        timer.cancel();
        super.onDestroy();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        timer.scheduleAtFixedRate(new MainTask(), 0, 600000);
        startForeground();
        return super.onStartCommand(intent, flags, startId);
    }

    private void startForeground() {
        createNotificationChannel();
        Intent notificationIntent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0,
                notificationIntent, PendingIntent.FLAG_MUTABLE);

        startForeground(NOTIF_ID, new NotificationCompat.Builder(this,
                NOTIF_CHANNEL_ID) // don't forget create a notification channel first
                .setOngoing(true)
                .setDefaults(0)
                .setSound(null)
                .setSmallIcon(R.drawable.flash)
                .setContentTitle(getString(R.string.app_name))
                .setContentIntent(pendingIntent)
                .build());
    }

    private void createNotificationChannel() {
        // Create the NotificationChannel, but only on API 26+ because
        // the NotificationChannel class is new and not in the support library
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = NOTIF_CHANNEL_ID;
            String description = "Widget Chanel";
            int importance = NotificationManager.IMPORTANCE_NONE;
            NotificationChannel channel = new NotificationChannel(NOTIF_CHANNEL_ID, name, importance);
            channel.setDescription(description);
            channel.setSound(null,null);
            // Register the channel with the system; you can't change the importance
            // or other notification behaviors after this
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
    }

    //Функция сохранения фото
    private void SaveImage(Context context, Bitmap finalBitmap) {

        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        String sFrieId = settings.getString("friend", null);
        Log.d("FRIEND NAME", sFrieId);
        try {
            try {
                Credentials credentials = new Credentials(getString(R.string.username), getString(R.string.api_key));
                RestClient client = new RestClient(credentials);
                try {
                    date = client.getResources(new ResourcesArgs.Builder().setPath(sFrieId+".jpg").build()).getModified();
                    long time = settings.getLong("date", 0);
                    if(time == 0 || time+10 < date.getTime()) {
                        Log.d("Image", "Download");
                        String root = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES).toString();
                        File myDir = new File(root + "/Widget");
                        if (!myDir.exists()) {
                            myDir.mkdirs();
                        }
                        String fname = "private.jpeg";
                        File file = new File(myDir, fname);
                        file.delete();
                        client.downloadFile(sFrieId + ".jpg", file, new DownloadProgressListener());

                        // Tell the media scanner about the new file so that it is
                        // immediately available to the user.
                        MediaScannerConnection.scanFile(context, new String[] { file.toString() }, null,
                                new MediaScannerConnection.OnScanCompletedListener() {
                                    public void onScanCompleted(String path, Uri uri) {
                                        Log.i("ExternalStorage", "Scanned " + path + ":");
                                        Log.i("ExternalStorage", "-> uri=" + uri);
                                    }
                                });
                    }
                    else {
                        Log.d("Image", "Not updated");
                    }
                } catch (ServerException | IOException e) {
                    e.printStackTrace();
                    Log.e("Server Error", "Not downloaded");
                    SaveImage(getApplicationContext(), null);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }

        } catch (Exception e) {
            e.printStackTrace();
        }


    }

    //слушатель прогресса загрузки
    public class DownloadProgressListener implements ProgressListener {

        boolean cancel = false;

        @Override
        public void updateProgress(long loaded, long total) {
            Log.d("Download progress", loaded + " total: "+total);
            if(loaded >= total) {
                cancel = true;
                SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
                settings.edit().putLong("date", date.getTime()).apply();
                Intent intent2 = new Intent(ctx, AppWidget.class);
                intent2.setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE);
                int[] ids = AppWidgetManager.getInstance(getApplication()).getAppWidgetIds(new ComponentName(getApplication(), AppWidget.class));
                intent2.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids);
                sendBroadcast(intent2);
            }
        }

        @Override
        public boolean hasCancelled() {

            return cancel;
        }




    }
}