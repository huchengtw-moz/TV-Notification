package com.mozilla.tv.notifications.email;

import java.util.ArrayList;

import android.app.IntentService;
import android.content.BroadcastReceiver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.ConnectivityManager;
import android.util.Log;

import com.mozilla.tv.notifications.utils.Connections;
import com.mozilla.tv.notifications.utils.SimpleDB;

public class MailBridgeService extends IntentService {

  private static final String TAG = "MailBridgeService";
  private static int MAXIMUM_RETRY_COUNT = 5;

  private static class Record {
    int id = -1;
    String type;
    String sender;
    String number;
    String body;

    public static Record fromCursor(Cursor c) {
      Record r = new Record();
      r.id = c.getInt(0);
      r.type = c.getString(1);
      r.sender = c.getString(2);
      r.number = c.getString(3);
      r.body = c.getString(4);
      return r;
    }
  }

  private BroadcastReceiver networkReceiver;
  private boolean receiverHooked;
  private SimpleDB simpleDB; // for offline mode

  public MailBridgeService() {
    super("MailBridgeService");
  }

  private BroadcastReceiver getNetworkReceiver() {
    if (null == networkReceiver) {
      networkReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
          notifyNetworkUpdate(!intent.getBooleanExtra(
                  ConnectivityManager.EXTRA_NO_CONNECTIVITY, true));
        }
      };
    }
    return networkReceiver;
  }

  private SimpleDB getSimpleDB() {
    if (null == simpleDB) {
      simpleDB = new SimpleDB(this);
    }
    return simpleDB;
  }

  private void flashUnsent() {
    SimpleDB sdb = getSimpleDB();
    SQLiteDatabase db = sdb.openDatabase("mail-bridge");
    try {
      sdb.ensureTextTable(db, "unsent",
              new String[] { "type", "sender", "number", "body" });
      // list all unsent
      Cursor c = sdb.list(db, "unsent");
      if (c.moveToFirst()) {
        ArrayList<Record> badList = new ArrayList<Record>();
        try {
          // start to retry
          while (c.moveToNext()) {
            try {
              Log.i(TAG, "retry id: #" + c.getInt(0));
              sendEmail(c.getString(1), c.getString(2), c.getString(3), c.getString(4));
            } catch (Exception e) {
              e.printStackTrace();
              Log.e(TAG, "unable to send email #" + c.getInt(0));
              badList.add(Record.fromCursor(c));
            }
          }
        } finally {
          c.close();
        }
        // process bad email. we need to do it here to maintain only one
        // connection at the same time per thread.
        for (Record r : badList) {
          if (increaseRetryCount(sdb, db, r.id) >= MAXIMUM_RETRY_COUNT) {
            Log.e(TAG, "bad email #" + r.id);
            sdb.deleteRow(db, "unsent", r.id);
            putToBad(sdb, db, r.type, r.sender, r.number, r.body);
          }
        }
      }
    } finally {
      db.close();
    }
  }

  private void notifyNetworkUpdate(boolean online) {
    if (!online) {
      return;
    }
    synchronized (MailBridgeService.class) {
      if (online && receiverHooked) {
        this.unregisterReceiver(getNetworkReceiver());
        receiverHooked = false;
      }
    }
    // we need to flash all unsent when internet is back.
    flashUnsent();
  }

  private void put(SimpleDB sdb, SQLiteDatabase db, String table, String type,
          String sender, String number, String body) {
    sdb.ensureTextTable(db, table, new String[] { "type", "sender", "number", "body" });
    ContentValues values = new ContentValues();
    values.put("type", type);
    values.put("sender", sender);
    values.put("number", number);
    values.put("body", body);
    sdb.insert(db, table, values);
  }

  private int increaseRetryCount(SimpleDB sdb, SQLiteDatabase db, int id) {
    sdb.ensureIntTable(db, "retryCount", new String[] { "count" });
    Cursor c = sdb.get(db, "retryCount", id);
    int count = 1;
    if (c.moveToFirst()) {
      count = c.getInt(1) + 1;
      ContentValues values = new ContentValues();
      values.put("retryCount", count);
      sdb.update(db, "retryCount", values, id);
    } else {
      ContentValues values = new ContentValues();
      values.put("retryCount", count);
      sdb.insert(db, "retryCount", values);
    }
    Log.i(TAG, "still fail #" + id + ", retry count: " + count);
    return count;
  }

  private void putToBad(SimpleDB sdb, SQLiteDatabase db, String type, String sender, String number, String body) {
    put(sdb, db, "bad", type, sender, number, body);
    Log.i(TAG, "put to bad");
  }

  private void sendEmail(String type, String sender, String number, String body) throws Exception {
    // use java mail to send email.
    Mail mail = new Mail("huchengtw@gmail.com", "xxxxxxx");
    mail.setSubject("You have an " + type + " " + sender);
    mail.setBody("A new " + type + " " + sender +
            "(" + number + ")" + ":\n" + body);
    mail.setTo(new String[] { "im@john.hu", "huchengtw@gmail.com" });
    mail.setFrom("hsuncheng.hu@gmail.com");
    if (mail.send()) {
      Log.i(TAG, "bridge message to email: ok");
    } else {
      Log.i(TAG, "bridge message to email: fail");
      throw new Exception("unable to send");
    }
  }

  private void persist(String type, String sender, String number, String body) {
    SimpleDB sdb = getSimpleDB();
    SQLiteDatabase db = sdb.openDatabase("mail-bridge");
    try {
      put(sdb, db, "unsent", type, sender, number, body);
    } finally {
      db.close();
    }
  }

  @Override
  protected void onHandleIntent(Intent arg0) {
    if (!Connections.isNetworkConnected(this)) {
      Log.i(TAG, "no internet found, save it to storage.");
      persist(arg0.getStringExtra("type"), arg0.getStringExtra("sender"),
              arg0.getStringExtra("number"), arg0.getStringExtra("body"));
    } else {
      Log.i(TAG, "bridge message to email.");
      try {
        sendEmail(arg0.getStringExtra("type"), arg0.getStringExtra("sender"),
                arg0.getStringExtra("number"), arg0.getStringExtra("body"));
        // flash unsent when a new request is in.
        flashUnsent();
      } catch (Exception e) {
        e.printStackTrace();
        persist(arg0.getStringExtra("type"), arg0.getStringExtra("sender"),
                arg0.getStringExtra("number"), arg0.getStringExtra("body"));
      }
    }
  }
}
