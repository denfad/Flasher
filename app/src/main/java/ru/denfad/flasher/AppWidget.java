package ru.denfad.flasher;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.appwidget.AppWidgetProviderInfo;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.widget.RemoteViews;


public class AppWidget extends AppWidgetProvider {

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        // Perform this loop procedure for each widget that belongs to this
        // provider.
        for (int i = 0; i < appWidgetIds.length; i++) {

            int appWidgetId = appWidgetIds[i];
            // Create an Intent to launch ExampleActivity
            Intent intent = new Intent(context, ImageService.class);
            PendingIntent pendingIntent = PendingIntent.getForegroundService(
                    context,
                    0,
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
            );

            // Get the layout for the widget and attach an on-click listener
            // to the button.

            AppWidgetProviderInfo info = appWidgetManager.getAppWidgetInfo(appWidgetId);
            RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.app_widget);
            views.setOnClickPendingIntent(R.id.imageView, pendingIntent);
            SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(context);



            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inSampleSize = 2;
            try {
                String root = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES).toString();
                Bitmap icon = BitmapFactory.decodeFile(root + "/Widget/private.jpeg", options);
                views.setImageViewBitmap(R.id.imageView, icon);

            } catch (Exception e) {
                e.printStackTrace();
                Bitmap icon = BitmapFactory.decodeResource(context.getResources(),
                        R.drawable.black, options);
                views.setImageViewBitmap(R.id.imageView, icon);

            }
            // Tell the AppWidgetManager to perform an update on the current app widget.
            appWidgetManager.updateAppWidget(appWidgetId, views);
        }
    }


}