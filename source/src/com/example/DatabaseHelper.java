package com.example;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class DatabaseHelper extends SQLiteOpenHelper {

    public static final String DATABASE_NAME = "messages_test.db";
    public static final int DATABASE_VERSION = 2;

    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        Utils.debug("Create table " + DATABASE_NAME);

        StringBuilder messagesTableBuilder = new StringBuilder();
        messagesTableBuilder.append("CREATE TABLE ").append(MessagesTableConstants._TABLE_NAME).append(" (");
        messagesTableBuilder.append(MessagesTableConstants._ID).append(" INTEGER PRIMARY KEY,");
        messagesTableBuilder.append(MessagesTableConstants._DATE).append(" INTEGER,");
        messagesTableBuilder.append(MessagesTableConstants._TEXT).append(" TEXT");
        messagesTableBuilder.append("); ");

        db.execSQL(messagesTableBuilder.toString());


        StringBuilder unsentMessagesTableBuilder = new StringBuilder();
        unsentMessagesTableBuilder.append("CREATE TABLE ").append(UnsentMessagestTableConstants._TABLE_NAME).append(" (");
        unsentMessagesTableBuilder.append(UnsentMessagestTableConstants._ID).append(" INTEGER PRIMARY KEY");
        unsentMessagesTableBuilder.append(");");

        db.execSQL(unsentMessagesTableBuilder.toString());
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        Utils.debug("");

        db.execSQL("DROP TABLE IF EXISTS " + MessagesTableConstants._TABLE_NAME);
        db.execSQL("DROP TABLE IF EXISTS " + UnsentMessagestTableConstants._TABLE_NAME);

        onCreate(db);
    }
}
