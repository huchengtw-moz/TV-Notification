package com.mozilla.tv.notifications.utils;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

public class SimpleDB {

  private Context ctx;

  public SimpleDB(Context ctx) {
    this.ctx = ctx;
  }

  public SQLiteDatabase openDatabase(String name) {
    return ctx.openOrCreateDatabase(name, Context.MODE_PRIVATE, null);
  }

  public void insert(SQLiteDatabase db, String table, ContentValues values) {
    db.insert(table, null, values);
  }

  public void update(SQLiteDatabase db, String table, ContentValues values, int id) {
    db.update(table, values, "_id=?", new String[] { "" + id });
  }

  public void ensureTextTable(SQLiteDatabase db, String name, String[] fields) {
    if (!checkTableExist(db, name)) {
      createTextTable(db, name, fields);
    }
  }

  public void ensureIntTable(SQLiteDatabase db, String name, String[] fields) {
    if (!checkTableExist(db, name)) {
      createIntTable(db, name, fields);
    }
  }

  public Cursor list(SQLiteDatabase db, String name) {
    return db.query(name, null, null, null, null, null, null);
  }

  public Cursor get(SQLiteDatabase db, String name, int id) {
    return db.query(name, null, "_id=?", new String[] { "" + id }, null, null, null);
  }

  public void deleteRows(SQLiteDatabase db, String table, int[] ids) {
    db.beginTransaction();
    try {
      for (int id : ids) {
        db.delete(table, "_id=?", new String[] { "" + id });
      }
      db.setTransactionSuccessful();
    } finally {
      db.endTransaction();
    }
  }

  public void deleteRows(SQLiteDatabase db, String table, Integer[] ids) {
    db.beginTransaction();
    try {
      for (int id : ids) {
        db.delete(table, "_id=?", new String[] { "" + id });
      }
      db.setTransactionSuccessful();
    } finally {
      db.endTransaction();
    }
  }

  public void deleteRow(SQLiteDatabase db, String table, int id) {
    db.delete(table, "_id=?", new String[] { "" + id });
  }

  public void createTextTable(SQLiteDatabase db, String name, String[] fields) {
    String sql = "CREATE TABLE " + name +
            " (_id INTEGER PRIMARY KEY AUTOINCREMENT,";

    for (int i = 0; i < fields.length; i++) {
      sql += fields[i] + " TEXT" + (i == (fields.length - 1) ? ")" : ",");
    }
    db.execSQL(sql);
  }

  public void createIntTable(SQLiteDatabase db, String name, String[] fields) {
    String sql = "CREATE TABLE " + name +
            " (_id INTEGER PRIMARY KEY AUTOINCREMENT,";

    for (int i = 0; i < fields.length; i++) {
      sql += fields[i] + " INTEGER" + (i == (fields.length - 1) ? ")" : ",");
    }
    db.execSQL(sql);
  }

  public boolean checkTableExist(SQLiteDatabase db, String tableName) {
    Cursor c = db.rawQuery("SELECT * FROM sqlite_master WHERE type='table' " +
            "AND name='" + tableName + "';", null);
    try {
      return c.getCount() > 0;
    } finally {
      c.close();
    }
  }

}
