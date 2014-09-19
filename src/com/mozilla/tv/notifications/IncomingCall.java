package com.mozilla.tv.notifications;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.telephony.TelephonyManager;

import com.mozilla.tv.notifications.utils.Contacts;
import com.mozilla.tv.notifications.utils.IntentUtils;

public class IncomingCall extends BroadcastReceiver {

  private static String ringingNumber;

  @Override
  public void onReceive(final Context context, Intent intent) {
    String state = intent.getStringExtra(TelephonyManager.EXTRA_STATE);
    if (state.equals(TelephonyManager.EXTRA_STATE_RINGING)) {
      ringingNumber = intent.getStringExtra(TelephonyManager.EXTRA_INCOMING_NUMBER);
      String ringingName = Contacts.querySenderName(context, ringingNumber);

      IntentUtils.sendTVIntent(context, "start-ringing", "中文字", ringingName,
              ringingNumber);
    } else if (null != ringingNumber &&
            (TelephonyManager.EXTRA_STATE_OFFHOOK.equals(state) ||
            TelephonyManager.EXTRA_STATE_IDLE.equals(state))) {
      String ringingName = Contacts.querySenderName(context, ringingNumber);
      IntentUtils.sendTVIntent(context, "stop-ringing", null, ringingName,
              ringingNumber);
      ringingNumber = null;
    }
  }
}
