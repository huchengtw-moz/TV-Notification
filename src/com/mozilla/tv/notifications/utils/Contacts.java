package com.mozilla.tv.notifications.utils;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.ContactsContract.PhoneLookup;

public class Contacts {
  public static String querySenderName(Context ctx, String number) {
    ContentResolver resolver = ctx.getContentResolver();
    Uri uri = Uri.withAppendedPath(PhoneLookup.CONTENT_FILTER_URI, Uri.encode(number));
    Cursor cursor = resolver.query(uri, new String[] { PhoneLookup.DISPLAY_NAME }, null, null, null);
    try {
      return cursor.moveToFirst() ? cursor.getString(0) : null;
    } finally {
      cursor.close();
    }
  }
}
