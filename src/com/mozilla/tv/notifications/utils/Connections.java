package com.mozilla.tv.notifications.utils;

import android.content.Context;
import android.net.ConnectivityManager;

public class Connections {
  public static boolean isNetworkConnected(Context ctx) {
    ConnectivityManager cm = (ConnectivityManager) ctx.getSystemService(
            Context.CONNECTIVITY_SERVICE);
    return null != cm.getActiveNetworkInfo();
  }
}
