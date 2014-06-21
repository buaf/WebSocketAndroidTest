package com.example;

import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.text.TextUtils;

import java.util.HashMap;

public class MessagesProvider  extends ContentProvider {

    public static final String AUTHORITY = "com.example.MessagesProvider";

    public static final HashMap<String, String> MESSAGES_PROJECTION_MAP;
    public static final HashMap<String, String> UNSENT_MESSAGES_PROJECTION_MAP;

    static {
        MESSAGES_PROJECTION_MAP = new HashMap<String, String>();
        MESSAGES_PROJECTION_MAP.put(MessagesTableConstants._ID, MessagesTableConstants._ID);
        MESSAGES_PROJECTION_MAP.put(MessagesTableConstants._DATE, MessagesTableConstants._DATE);
        MESSAGES_PROJECTION_MAP.put(MessagesTableConstants._TEXT, MessagesTableConstants._TEXT);

        UNSENT_MESSAGES_PROJECTION_MAP = new HashMap<String, String>();
        UNSENT_MESSAGES_PROJECTION_MAP.put(UnsentMessagestTableConstants._ID, UnsentMessagestTableConstants._ID);
    }

    private final UriMatcher mUriMatcher;

    private static final int MESSAGES = 1;
    private static final int MESSAGES_ID = 2;
    private static final int UNSENT_MESSAGES = 3;
    private static final int UNSENT_MESSAGES_ID = 4;

    private DatabaseHelper mOpenHelper;

    public MessagesProvider() {
        mUriMatcher = new UriMatcher(UriMatcher.NO_MATCH);
        mUriMatcher.addURI(AUTHORITY, MessagesTableConstants._TABLE_NAME, MESSAGES);
        mUriMatcher.addURI(AUTHORITY, MessagesTableConstants._TABLE_NAME + "/#", MESSAGES_ID);

        mUriMatcher.addURI(AUTHORITY, UnsentMessagestTableConstants._TABLE_NAME, UNSENT_MESSAGES);
        mUriMatcher.addURI(AUTHORITY, UnsentMessagestTableConstants._TABLE_NAME + "/#", UNSENT_MESSAGES_ID);

        Utils.debug("Init SimpleProvider");
    }

    @Override
    public boolean onCreate() {
        mOpenHelper = new DatabaseHelper(getContext());
        return true;
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection,
                        String[] selectionArgs, String sortOrder) {

        SQLiteQueryBuilder qb = new SQLiteQueryBuilder();

        switch (mUriMatcher.match(uri)) {
            case MESSAGES:
                qb.setTables(MessagesTableConstants._TABLE_NAME);
                qb.setProjectionMap(MESSAGES_PROJECTION_MAP);
                if (TextUtils.isEmpty(sortOrder)) {
                    sortOrder = MessagesTableConstants.DEFAULT_SORT_ORDER;
                }
                break;

            case MESSAGES_ID:
                qb.setTables(MessagesTableConstants._TABLE_NAME);
                qb.setProjectionMap(MESSAGES_PROJECTION_MAP);
                qb.appendWhere(MessagesTableConstants._ID + "=?");
                selectionArgs = DatabaseUtils.appendSelectionArgs(selectionArgs,
                        new String[]{uri.getLastPathSegment()});
                if (TextUtils.isEmpty(sortOrder)) {
                    sortOrder = MessagesTableConstants.DEFAULT_SORT_ORDER;
                }
                break;

            case UNSENT_MESSAGES:
                qb.setTables(UnsentMessagestTableConstants._TABLE_NAME);
                qb.setProjectionMap(UNSENT_MESSAGES_PROJECTION_MAP);
                if (TextUtils.isEmpty(sortOrder)) {
                    sortOrder = UnsentMessagestTableConstants.DEFAULT_SORT_ORDER;
                }
                break;

            case UNSENT_MESSAGES_ID:
                qb.setTables(UnsentMessagestTableConstants._TABLE_NAME);
                qb.setProjectionMap(UNSENT_MESSAGES_PROJECTION_MAP);
                qb.appendWhere(UnsentMessagestTableConstants._ID + "=?");
                selectionArgs = DatabaseUtils.appendSelectionArgs(selectionArgs,
                        new String[]{uri.getLastPathSegment()});
                if (TextUtils.isEmpty(sortOrder)) {
                    sortOrder = UnsentMessagestTableConstants.DEFAULT_SORT_ORDER;
                }
                break;

            default:
                throw new IllegalArgumentException("Unknown URI " + uri);
        }


        SQLiteDatabase db = mOpenHelper.getReadableDatabase();

        Cursor c = qb.query(db, projection, selection, selectionArgs,
                null /* no group */, null /* no filter */, sortOrder);

        c.setNotificationUri(getContext().getContentResolver(), uri);
        return c;
    }

    @Override
    public String getType(Uri uri) {
        switch (mUriMatcher.match(uri)) {
            case MESSAGES:
                return MessagesTableConstants.CONTENT_TYPE;
            case MESSAGES_ID:
                return MessagesTableConstants.CONTENT_ITEM_TYPE;
            case UNSENT_MESSAGES:
                return UnsentMessagestTableConstants.CONTENT_TYPE;
            case UNSENT_MESSAGES_ID:
                return UnsentMessagestTableConstants.CONTENT_ITEM_TYPE;
            default:
                throw new IllegalArgumentException("Unknown URI " + uri);
        }
    }

    @Override
    public Uri insert(Uri uri, ContentValues initialValues) {
        if (initialValues == null) {
            throw new IllegalArgumentException("Null insert data");
        }

        int uriType = mUriMatcher.match(uri);
        SQLiteDatabase sqlDB = mOpenHelper.getWritableDatabase();
        long id;
        switch (uriType) {
            case MESSAGES:
                id = sqlDB.insert(MessagesTableConstants._TABLE_NAME, null, initialValues);
                Uri msgNoteUri = ContentUris.withAppendedId(MessagesTableConstants.CONTENT_ID_URI_BASE, id);
                getContext().getContentResolver().notifyChange(msgNoteUri, null);
                return msgNoteUri;

            case UNSENT_MESSAGES:
                id = sqlDB.insert(UnsentMessagestTableConstants._TABLE_NAME, null, initialValues);
                Uri unsentMsgNoteUri = ContentUris.withAppendedId(UnsentMessagestTableConstants.CONTENT_ID_URI_BASE, id);
                getContext().getContentResolver().notifyChange(unsentMsgNoteUri, null);
                return unsentMsgNoteUri;
        }

        throw new IllegalArgumentException("Unknown URI: " + uri);
    }

    @Override
    public int delete(Uri uri, String where, String[] whereArgs) {
        Utils.debug("");

        SQLiteDatabase db = mOpenHelper.getWritableDatabase();
        String finalWhere;

        int count;

        switch (mUriMatcher.match(uri)) {
            case MESSAGES:
                count = db.delete(MessagesTableConstants._TABLE_NAME, where, whereArgs);
                break;

            case MESSAGES_ID:
                finalWhere = DatabaseUtils.concatenateWhere(
                        MessagesTableConstants._ID + " = " + ContentUris.parseId(uri), where);
                count = db.delete(MessagesTableConstants._TABLE_NAME, finalWhere, whereArgs);
                break;

            case UNSENT_MESSAGES:
                count = db.delete(UnsentMessagestTableConstants._TABLE_NAME, where, whereArgs);
                break;

            case UNSENT_MESSAGES_ID:
                finalWhere = DatabaseUtils.concatenateWhere(
                        UnsentMessagestTableConstants._ID + " = " + ContentUris.parseId(uri), where);
                count = db.delete(UnsentMessagestTableConstants._TABLE_NAME, finalWhere, whereArgs);
                break;

            default:
                throw new IllegalArgumentException("Unknown URI " + uri);
        }

        getContext().getContentResolver().notifyChange(uri, null);

        return count;
    }

    @Override
    public int update(Uri uri, ContentValues values, String where, String[] whereArgs) {
        Utils.debug("");

        SQLiteDatabase db = mOpenHelper.getWritableDatabase();
        int count;
        String finalWhere;

        switch (mUriMatcher.match(uri)) {
            case MESSAGES:
                count = db.update(MessagesTableConstants._TABLE_NAME, values, where, whereArgs);
                break;

            case MESSAGES_ID:
                finalWhere = DatabaseUtils.concatenateWhere(
                        MessagesTableConstants._ID + " = " + ContentUris.parseId(uri), where);
                count = db.update(MessagesTableConstants._TABLE_NAME, values, finalWhere, whereArgs);
                break;

            case UNSENT_MESSAGES:
                count = db.update(UnsentMessagestTableConstants._TABLE_NAME, values, where, whereArgs);
                break;

            case UNSENT_MESSAGES_ID:
                finalWhere = DatabaseUtils.concatenateWhere(
                        UnsentMessagestTableConstants._ID + " = " + ContentUris.parseId(uri), where);
                count = db.update(UnsentMessagestTableConstants._TABLE_NAME, values, finalWhere, whereArgs);
                break;

            default:
                throw new IllegalArgumentException("Unknown URI " + uri);
        }

        getContext().getContentResolver().notifyChange(uri, null);

        return count;
    }
}