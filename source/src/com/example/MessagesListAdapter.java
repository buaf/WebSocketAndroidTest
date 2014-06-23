package com.example;

import android.app.LoaderManager;
import android.content.Context;
import android.content.CursorLoader;
import android.content.Loader;
import android.database.Cursor;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CursorAdapter;
import android.widget.TextView;

import java.text.DateFormat;
import java.util.ArrayDeque;
import java.util.Date;
import java.util.Deque;
import java.util.Locale;

public class MessagesListAdapter extends CursorAdapter
        implements LoaderManager.LoaderCallbacks<Cursor> {

    private MainActivity mMainActivity;
    private LayoutInflater mInflater;
    private final Deque<Integer> mUnsentMessageCache = new ArrayDeque<Integer>();

    static final String[] MESSAGES_PROJECTION = new String[]{
            MessagesTableConstants._ID,
            MessagesTableConstants._DATE,
            MessagesTableConstants._TEXT
    };

    public MessagesListAdapter(Context context, Cursor c, int flags) {
        super(context, c, flags);
        Utils.debug("");
        mInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        if (context instanceof MainActivity) {
            mMainActivity = (MainActivity)context;
            mMainActivity.getLoaderManager().initLoader(0, null, this);
        } else {
            Utils.error("Error, can't cast context to MainActivity!");
        }
    }

    @Override
    public View newView(Context context, Cursor cursor, ViewGroup parent) {
        Utils.debug("");
        return mInflater.inflate(R.layout.dataview, parent, false);
    }

    @Override
    public void bindView(View view, Context context, Cursor cursor) {
        String index = cursor.getString(cursor.getColumnIndexOrThrow(MessagesTableConstants._ID));
        String formatData = cursor.getString(cursor.getColumnIndexOrThrow(MessagesTableConstants._DATE));
        String text = cursor.getString(cursor.getColumnIndexOrThrow(MessagesTableConstants._TEXT));

        TextView columnView = (TextView) view.findViewById(R.id.columnIdText);
        columnView.setText(index);

        TextView dataView = (TextView) view.findViewById(R.id.dataText);
        dataView.setText(stringToTextData(formatData));

        TextView textView = (TextView) view.findViewById(R.id.messageText);
        textView.setText(text);

        Integer value = Integer.parseInt(index);
        if (mUnsentMessageCache.contains(value)) {
            view.setBackgroundColor(Color.parseColor("#FFA3A1"));
        } else {
            view.setBackgroundColor(Color.parseColor("#AEFF7E"));
        }
    }

    @Override
    public void notifyDataSetChanged() {
        mUnsentMessageCache.clear();
        Uri uri = UnsentMessagestTableConstants.CONTENT_URI;
        String[] projection = new String[]{UnsentMessagestTableConstants._ID};
        Cursor unsetMsgCursor = mMainActivity.getContentResolver().query(uri, projection, null, null, null);
        for (unsetMsgCursor.moveToFirst(); !unsetMsgCursor.isAfterLast(); unsetMsgCursor.moveToNext()) {
            String unsetMsgIndex = unsetMsgCursor.getString(unsetMsgCursor.getColumnIndexOrThrow(UnsentMessagestTableConstants._ID));
            Integer id = Integer.parseInt(unsetMsgIndex);
            if (!mUnsentMessageCache.contains(id)) {
                mUnsentMessageCache.add(id);
            }
        }
        unsetMsgCursor.close();

        super.notifyDataSetChanged();
    }

    public String stringToTextData(String text) {
        long value = Long.parseLong(text) * 1000L;
        Date date = new Date(value);
        DateFormat dateFormat = DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.MEDIUM, Locale.getDefault());
        return dateFormat.format(date);
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        Utils.debug("");
        CursorLoader cursorLoader = new CursorLoader(mMainActivity, MessagesTableConstants.CONTENT_URI,
                MESSAGES_PROJECTION, null, null, null);
        cursorLoader.setUpdateThrottle(1000);
        return cursorLoader;
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        Utils.debug("");
        swapCursor(data);
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        Utils.debug("");
        swapCursor(null);
    }
}
