package com.mozilla.tv.notifications;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import com.mozilla.tv.notifications.utils.Contacts;
import com.mozilla.tv.notifications.utils.IntentUtils;

public class MMSReceiver extends BroadcastReceiver {
  private static final String ACTION_MMS_RECEIVED = "android.provider.Telephony.WAP_PUSH_RECEIVED";
  private static final String MMS_DATA_TYPE = "application/vnd.wap.mms-message";

  // Retrieve MMS
  public void onReceive(Context context, Intent intent) {

    String action = intent.getAction();
    String type = intent.getType();
    // the following code haven't been not tested, just make a porting from
    // others.
    if (action.equals(ACTION_MMS_RECEIVED) && type.equals(MMS_DATA_TYPE)) {

      Bundle bundle = intent.getExtras();
      if (bundle != null) {

        byte[] buffer = bundle.getByteArray("data");
        String number = new String(buffer);
        int indx = number.indexOf("/TYPE");
        if (indx > 0 && (indx - 15) > 0) {
          int newIndx = indx - 15;
          number = number.substring(newIndx, indx);
          indx = number.indexOf("+");
          if (indx > 0) {
            number = number.substring(indx);
          }
        }
        IntentUtils.sendTVIntent(context, "mms", null,
                Contacts.querySenderName(context, number),
                number);

      }
    }
  }
}
