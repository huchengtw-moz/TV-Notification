package com.mozilla.tv.notifications.tv;

import org.json.JSONException;
import org.json.JSONObject;

import android.app.IntentService;
import android.content.Intent;
import android.util.Log;

public class TVBridgeService extends IntentService {

  public static final String TAG = "TVBridgeService";

  public TVBridgeService() {
    super("TVBridgeService");
  }

  @Override
  protected void onHandleIntent(Intent arg0) {
    Log.d(TAG, "bridge notification to tv");
    try {
      JSONObject jsObj = new JSONObject();
      jsObj.put("type", arg0.getStringExtra("type"));
      if (arg0.hasExtra("sender")) {
        jsObj.put("name", arg0.getStringExtra("sender"));
      }
      jsObj.put("call", arg0.getStringExtra("number"));
      if (arg0.hasExtra("body")) {
        jsObj.put("body", arg0.getStringExtra("body"));
      }
      TVConn.get().send(jsObj.toString());
    } catch (JSONException e) {
      e.printStackTrace();
    }
  }

}
