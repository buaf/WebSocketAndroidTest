package com.example;

import android.net.Uri;
import android.provider.BaseColumns;

public class MessagesTableConstants implements BaseColumns {

    public static final String _TABLE_NAME = "messages";
    public static final String _DATE = "date";
    public static final String _TEXT = "text";

    public static final Uri CONTENT_URI = Uri.parse("content://" + MessagesProvider.AUTHORITY + "/" + _TABLE_NAME);
    public static final Uri CONTENT_ID_URI_BASE = Uri.parse("content://" + MessagesProvider.AUTHORITY + "/" + _TABLE_NAME + "/");

    public static final String CONTENT_TYPE = "messages.dir";
    public static final String CONTENT_ITEM_TYPE = "messages.item";
    public static final String DEFAULT_SORT_ORDER = _DATE + " COLLATE LOCALIZED ASC";
}
