package com.example;

import android.net.Uri;
import android.provider.BaseColumns;

public class UnsentMessagestTableConstants implements BaseColumns {

    public static final String _TABLE_NAME = "unsent_messages";

    public static final Uri CONTENT_URI = Uri.parse("content://" + MessagesProvider.AUTHORITY + "/" + _TABLE_NAME);
    public static final Uri CONTENT_ID_URI_BASE = Uri.parse("content://" + MessagesProvider.AUTHORITY + "/" + _TABLE_NAME + "/");

    public static final String CONTENT_TYPE = "unsentMessages.dir";
    public static final String CONTENT_ITEM_TYPE = "unsentMessages.item";
    public static final String DEFAULT_SORT_ORDER = _ID + " COLLATE LOCALIZED ASC";
}
