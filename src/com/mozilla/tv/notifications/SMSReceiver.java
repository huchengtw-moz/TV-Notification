package com.mozilla.tv.notifications;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.telephony.SmsMessage;

import com.mozilla.tv.notifications.utils.Contacts;
import com.mozilla.tv.notifications.utils.IntentUtils;

public class SMSReceiver extends BroadcastReceiver {

  @Override
  public void onReceive(Context context, Intent arg1) {
    Object[] msgs = (Object[]) arg1.getExtras().get("pdus");

    for (int i = 0; i < msgs.length; i++) {
      SmsMessage sms = SmsMessage.createFromPdu((byte[]) msgs[i]);
      IntentUtils.sendTVIntent(context, "sms", sms.getMessageBody(),
              Contacts.querySenderName(context, sms.getDisplayOriginatingAddress()),
              sms.getDisplayOriginatingAddress());

    }
  }

}
