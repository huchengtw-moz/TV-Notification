package com.mozilla.tv.notifications.tv;

import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.support.v4.app.NotificationCompat;

import com.mozilla.tv.notifications.MainActivity;
import com.mozilla.tv.notifications.R;

public class TVConnService extends Service {

  public static boolean hasServiceRunning;

  public class TVConnBinder extends Binder {
    public TVConnService getService() {
      return TVConnService.this;
    }
  }

  @Override
  public IBinder onBind(Intent arg0) {
    return new TVConnBinder();
  }

  @Override
  public int onStartCommand(Intent intent, int flags, int startId) {
    Intent appIntent = new Intent(this, MainActivity.class);
    PendingIntent contentIntent = PendingIntent.getActivity(this, 0, appIntent,
            PendingIntent.FLAG_CANCEL_CURRENT);

    NotificationCompat.Builder builder = new NotificationCompat.Builder(this);
    builder.setSmallIcon(R.drawable.ic_launcher);
    builder.setContentTitle("Notifications to TV");
    builder.setContentText("Bridging");
    builder.setContentIntent(contentIntent);

    this.startForeground(1, builder.build());

    TVConn.get().start();
    hasServiceRunning = true;
    return Service.START_STICKY;
  }

  @Override
  public void onDestroy() {
    hasServiceRunning = false;
    TVConn.get().stop();
    this.stopForeground(true);
    super.onDestroy();
  }

}
