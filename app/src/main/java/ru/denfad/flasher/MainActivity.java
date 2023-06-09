package ru.denfad.flasher;

import android.Manifest;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.preference.PreferenceManager;
import android.provider.MediaStore;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.animation.AnimationUtils;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import com.yandex.disk.rest.Credentials;
import com.yandex.disk.rest.ProgressListener;
import com.yandex.disk.rest.RestClient;
import com.yandex.disk.rest.exceptions.ServerException;
import com.yandex.disk.rest.json.Link;

import java.io.File;
import java.io.IOException;

import id.zelory.compressor.Compressor;

public class MainActivity extends AppCompatActivity {

    private ImageButton load;
    private TextView progress;
    private static final int PICK_IMAGE = 100;
    private static final int READ_EXTERNAL_STORAGE = 200;
    private Uri imageUri;
    private String sUsrId;
    private String sFrieId;
    private SharedPreferences settings;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        settings = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());

        //поле ввода имени пользователя
        EditText usrId = findViewById(R.id.usr_id);
        usrId.setOnKeyListener((view, i, keyEvent) -> {
            if (keyEvent.getAction() == KeyEvent.ACTION_DOWN && (i == KeyEvent.KEYCODE_ENTER)) {
                SharedPreferences.Editor editor = settings.edit();
                editor.putString("user", usrId.getText().toString());
                editor.apply();
                editor.commit();
                sUsrId = usrId.getText().toString();
                return true;
            }
            return false;
        });

        //поле ввода имени друга
        EditText frieId = findViewById(R.id.friend_id);
        frieId.setOnKeyListener((view, i, keyEvent) -> {
            if (keyEvent.getAction() == KeyEvent.ACTION_DOWN && (i == KeyEvent.KEYCODE_ENTER)) {
                SharedPreferences.Editor editor = settings.edit();
                editor.putString("friend", frieId.getText().toString());
                editor.apply();
                editor.commit();
                sFrieId = frieId.getText().toString();
                return true;
            }
            return false;
        });


        //собираем сохранённую информацию, заполняем ими поля
        sUsrId = settings.getString("user", null);
        sFrieId = settings.getString("friend", null);
        if (sUsrId != null) usrId.setText(sUsrId);
        if (sFrieId != null) frieId.setText(sFrieId);

        //создаём сервис закачки новых фото
        if (sUsrId != null) {
            startForegroundService(new Intent(this, ImageService.class));
            Log.d("Service", "Start");
           /* Intent alarmIntent = new Intent(MainActivity.this, AlarmReceiver.class);
            PendingIntent pendingIntent = PendingIntent.getBroadcast(MainActivity.this, 0, alarmIntent, PendingIntent.FLAG_IMMUTABLE);
            AlarmManager manager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
            int interval = 3600000;
            manager.setInexactRepeating(AlarmManager.RTC_WAKEUP, System.currentTimeMillis(), interval, pendingIntent);*/
        }

        //кнопка загрузки фото
        load = findViewById(R.id.load_image);
        load.setOnClickListener(view -> {
            //запрос на доступ к галлереи
            view.startAnimation(AnimationUtils.loadAnimation(getApplicationContext(), R.anim.image_button));
            int permissionStatus = ContextCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.READ_EXTERNAL_STORAGE);
            if (permissionStatus == PackageManager.PERMISSION_GRANTED) {
                openGallery();
            } else {
                ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                        READ_EXTERNAL_STORAGE);
            }

        });

        progress = findViewById(R.id.progress);
    }

    private void openGallery() {
        Intent gallery = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.INTERNAL_CONTENT_URI);
        startActivityForResult(gallery, PICK_IMAGE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK && requestCode == PICK_IMAGE) {
            imageUri = data.getData();
            String filePath = getRealPathFromURI(imageUri);
            load.setImageURI(imageUri);
            SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
            SharedPreferences.Editor editor = settings.edit();
            editor.putString("image", filePath);
            Log.e("Error", filePath);
            // Commit the edits!
            editor.apply();
            editor.commit();

            progress.setText("Сжимаем фото...");
            File file = new File(filePath);
            File compress = null;
            try {
                compress = new Compressor(getApplicationContext()).compressToFile(file);
            } catch (IOException e) {
                e.printStackTrace();
            }

            //restApi
            File finalCompress = compress;
            new Thread(new Runnable() {

                @Override
                public void run() {
                    Credentials credentials = new Credentials("denfad2003", "AQAAAAArZCXAAADLWwB7Pep9mEcuhrs_TcTcdUs");
                    RestClient client = new RestClient(credentials);
                    Log.e("API", "Create");
                    String serverPath = sUsrId + ".jpg"; //путь на самом диске
                    try {
                        Link link = client.getUploadLink(serverPath, true);
                        client.uploadFile(link, true, finalCompress, new UploadProgressListener());
                    } catch (ServerException | IOException e) {
                        e.printStackTrace();
                    }
                }
            }).start();
        } else if (resultCode == RESULT_CANCELED && requestCode == READ_EXTERNAL_STORAGE) {
            Toast.makeText(getApplicationContext(), "Нам нужен доступ для магии :)", Toast.LENGTH_SHORT);
        }
    }


    //метод поиска полного пути до выбранного фото
    private String getRealPathFromURI(Uri contentURI) {
        String result;
        Cursor cursor = getContentResolver().query(contentURI, null, null, null, null);
        if (cursor == null) { // Source is Dropbox or other similar local file path
            result = contentURI.getPath();
        } else {
            cursor.moveToFirst();
            int idx = cursor.getColumnIndex(MediaStore.Images.ImageColumns.DATA);
            result = cursor.getString(idx);
            cursor.close();
        }
        return result;
    }

    //слушатель прогресса подкачки
    public class UploadProgressListener implements ProgressListener {

        boolean cancel = false;

        @Override
        public void updateProgress(long loaded, long total) {
            new Handler(Looper.getMainLooper()).post(new Runnable() {
                @Override
                public void run() {
                    progress.setText("Загружено "+ loaded/ total * 100 + "%");
                }
            });
            if (loaded >= total) {
                cancel = true;
                Log.d("Load image", "stop");
                new Handler(Looper.getMainLooper()).post(new Runnable() {
                    @Override
                    public void run() {
                        progress.setText("Загрузите фото");
                    }
                });

            }
        }

        @Override
        public boolean hasCancelled() {
            return cancel;
        }
    }


}