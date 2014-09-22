package com.mozilla.tv.notifications.utils;

import android.content.Context;
import android.content.Intent;

import com.mozilla.tv.notifications.email.MailBridgeService;
import com.mozilla.tv.notifications.tv.TVBridgeService;
import com.mozilla.tv.notifications.tv.TVConnService;

public class IntentUtils {

  public static final String REMOTE_URL = "app://tv-notification-receiver.gaiamobile.org/index.html";

  public static void sendEmailIntent(Context ctx, String type, String body,
          String sender, String number) {
    Intent i = new Intent(ctx, MailBridgeService.class);
    i.putExtra("type", type);
    if (null != sender) {
      i.putExtra("sender", sender);
    }
    i.putExtra("number", number);
    if (null != sender) {
      i.putExtra("body", body);
    }
    ctx.startService(i);
  }

  public static void sendTVIntent(Context ctx, String type, String body,
          String sender, String number) {
    sendTVIntent(ctx, REMOTE_URL, type, body, sender, number);
  }

  public static void sendTVIntent(Context ctx, String url, String type, String body,
          String sender, String number) {
    Intent i = new Intent(ctx, TVBridgeService.class);
    i.putExtra("url", url);
    i.putExtra("type", type);
    if (null != sender) {
      i.putExtra("sender", sender);
    }
    if (null != number) {
      i.putExtra("number", number);
    }
    if (null != sender) {
      i.putExtra("body", body);
    }
    ctx.startService(i);
  }

  public static void startTVConnService(Context ctx) {
    Intent i = new Intent(ctx, TVConnService.class);
    ctx.startService(i);
  }

  public static void stopTVConnService(Context ctx) {
    Intent i = new Intent(ctx, TVConnService.class);
    ctx.stopService(i);
  }

}
