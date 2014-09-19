package com.mozilla.tv.notifications;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;

import com.mozilla.tv.notifications.utils.Contacts;
import com.mozilla.tv.notifications.utils.IntentUtils;

public class OutgoingCall extends BroadcastReceiver {

  private boolean isEmpty(String s) {
    return null == s || "".equals(s);
  }

  @Override
  public void onReceive(final Context context, Intent intent) {
    final String number = intent.getStringExtra(Intent.EXTRA_PHONE_NUMBER);
    final TelephonyManager telephony = (TelephonyManager)
            context.getSystemService(Context.TELEPHONY_SERVICE);
    PhoneStateListener listener = new PhoneStateListener() {
      private boolean callingNow = false;

      @Override
      public void onCallStateChanged(int state, String incomingNumber) {
        if (TelephonyManager.CALL_STATE_OFFHOOK == state &&
                isEmpty(incomingNumber)) {

          callingNow = true;
          IntentUtils.sendTVIntent(context, "call-out", null,
                  Contacts.querySenderName(context, number), number);
        } else if (callingNow && isEmpty(incomingNumber) && (
                TelephonyManager.CALL_STATE_IDLE == state)) {

          callingNow = false;
          telephony.listen(this, PhoneStateListener.LISTEN_NONE);
          IntentUtils.sendTVIntent(context, "call-out-end", null,
                  Contacts.querySenderName(context, number), number);
        }
      }
    };
    telephony.listen(listener, PhoneStateListener.LISTEN_CALL_STATE);
  }
}
