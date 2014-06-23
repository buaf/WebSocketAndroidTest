package com.example;

import android.app.Service;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.Semaphore;

import de.tavendo.autobahn.WebSocketConnection;
import de.tavendo.autobahn.WebSocketException;
import de.tavendo.autobahn.WebSocketHandler;

public class ConnectionService extends Service {

    public static final String HOST = "ws://echo.websocket.org";

    public static final int MSG_REGISTER_CLIENT = 1;
    public static final int MSG_UNREGISTER_CLIENT = 2;
    public static final int MSG_SEND_SOCKET = 3;

    public static final String BUNDLE_MESSAGE_TEXT = "msg";
    public static final String BUNDLE_MESSAGE_ID = "id";

    private final WebSocketConnection mConnection = new WebSocketConnection();
    private final Messenger mMessenger = new Messenger(getMessengerHandler());
    private final ArrayList<Messenger> mClients = new ArrayList<Messenger>();

    private final Thread mSendMessageThread = getMessageThread();
    private final Semaphore mWaitConnectionSemaphore = new Semaphore(0);
    private final Semaphore mSendMessageSemaphore = new Semaphore(0);

    private boolean mIsConnected = false;
    private final LinkedBlockingDeque<TextMessage> mMessageQueue = new LinkedBlockingDeque<TextMessage>();
    private TextMessage mLastMessage;

    static class TextMessage {
        public int id;
        public String data;

        @Override
        public boolean equals(Object obj) {
            if (obj instanceof TextMessage) {
                TextMessage msg = (TextMessage)obj;
                return id == msg.id;
            } else {
                return false;
            }
        }
    }


    @Override
    public void onCreate() {
        super.onCreate();
        mSendMessageThread.start();
        Utils.debug("Create service");
    }

    void loadUnsentMessages() {
        Uri unsetMsgUri = UnsentMessagestTableConstants.CONTENT_URI;
        String[] unsetMsgProjection = new String[]{UnsentMessagestTableConstants._ID};

        Cursor unsetMsgCursor = getContentResolver().query(unsetMsgUri, unsetMsgProjection, null, null, null);
        for (unsetMsgCursor.moveToFirst(); !unsetMsgCursor.isAfterLast(); unsetMsgCursor.moveToNext()) {
            String unsetMsgIndex = unsetMsgCursor.getString(unsetMsgCursor.getColumnIndexOrThrow(UnsentMessagestTableConstants._ID));

            Uri msgUri = MessagesTableConstants.CONTENT_URI;
            String[] msgProjection = new String[]{MessagesTableConstants._ID, MessagesTableConstants._DATE, MessagesTableConstants._TEXT};
            String msgSelection = UnsentMessagestTableConstants._ID + "=?";
            String[] msgSelectionArgs = new String[]{unsetMsgIndex};
            Cursor msgCursor = getContentResolver().query(msgUri, msgProjection, msgSelection, msgSelectionArgs, null);

            if (msgCursor.getCount() > 0) {
                msgCursor.moveToFirst();
                String text = msgCursor.getString(msgCursor.getColumnIndexOrThrow(MessagesTableConstants._TEXT));
                String index = msgCursor.getString(msgCursor.getColumnIndexOrThrow(MessagesTableConstants._ID));

                Utils.debug("Load unsent message:" + text + " Id:" + index);

                TextMessage message = new TextMessage();
                message.data = text;
                message.id = Integer.parseInt(index);

                if (!mMessageQueue.contains(message)) {
                    mMessageQueue.add(message);
                }
            }
            msgCursor.close();
        }
        unsetMsgCursor.close();
    }

    int getUnsentMessagesCount() {
        Uri unsetMsgUri = UnsentMessagestTableConstants.CONTENT_URI;
        String[] unsetMsgProjection = new String[]{UnsentMessagestTableConstants._ID};
        Cursor cursor = getContentResolver().query(unsetMsgUri, unsetMsgProjection, null, null, null);
        try {
            return cursor.getCount();
        } finally {
            cursor.close();
        }
    }


    public void connect(String host) {
        Utils.debug("Start connect.");

        if (mIsConnected) {
            mWaitConnectionSemaphore.release();
            return;
        }

        try {
            mConnection.connect(host, new WebSocketHandler() {

                @Override
                public void onOpen() {
                    String openConnectionMsg = "Connected to:" + HOST;
                    mIsConnected = true;
                    Utils.debug(openConnectionMsg);
                    mWaitConnectionSemaphore.release();
                    mSendMessageThread.interrupt();
                }

                @Override
                public void onTextMessage(String msg) {
                    String textMsg = " Server message:" + msg + " ";
                    Utils.debug(textMsg);
                    sendMessageToClients(mLastMessage.id, textMsg);
                    mMessageQueue.remove(mLastMessage);
                    mSendMessageSemaphore.release();
                }

                @Override
                public void onClose(int code, String reason) {
                    String fullError = "Connection close. Code:" + code + " Reason:" + reason;
                    mIsConnected = false;
                    Utils.debug(fullError);
                    if (code == 2) {
                        Utils.debug("You need to enable internet!");
                    }
                    mWaitConnectionSemaphore.release();
                    mSendMessageSemaphore.release();
                }
            });

        } catch (WebSocketException e) {
            Utils.debug("WebSocketException:" + e.toString());
        }
    }

    @Override
    public void onDestroy() {
        mConnection.disconnect();
        Utils.debug("Disconnect!");
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mMessenger.getBinder();
    }

    public void sendMessageToClients(long id, String msg) {
        Message message = Message.obtain(null, ConnectionService.MSG_SEND_SOCKET);
        Bundle bundle = message.getData();
        bundle.putString(BUNDLE_MESSAGE_TEXT, msg + '\n');
        bundle.putLong(BUNDLE_MESSAGE_ID, id);

        try {
            for (Messenger messenger : mClients) {
                messenger.send(message);
            }
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    private Handler getMessengerHandler() {
        return new Handler() {
            @Override
            public void handleMessage(Message msg) {
                switch (msg.what) {
                    case MSG_REGISTER_CLIENT:
                        mClients.add(msg.replyTo);
                        break;
                    case MSG_UNREGISTER_CLIENT:
                        mClients.remove(msg.replyTo);
                        break;
                    case MSG_SEND_SOCKET:
                        Bundle data = msg.getData();
                        String msgData = data.getString(BUNDLE_MESSAGE_TEXT);
                        int msgId = (int)data.getLong(BUNDLE_MESSAGE_ID);
                        Utils.debug("Handle message:" + msgData + " Id:" + msgId);

                        TextMessage textMessage = new TextMessage();
                        textMessage.data = msgData;
                        textMessage.id = msgId;
                        if (!mMessageQueue.contains(textMessage)) {
                            mMessageQueue.add(textMessage);
                        }
                        mSendMessageThread.interrupt();
                        break;
                    default:
                        super.handleMessage(msg);
                }
            }
        };
    }

    private Thread getMessageThread() {
        return new Thread() {
            @Override
            public void run() {
                while (true) {
                    try {
                        connect(HOST);
                        Utils.debug("Wait to connect");
                        mWaitConnectionSemaphore.acquire();

                        if (mIsConnected) {
                            Utils.debug("Messages to send:" + mMessageQueue.size());

                            for (TextMessage aMMessageQueue : mMessageQueue) {
                                mLastMessage = aMMessageQueue;
                                mConnection.sendTextMessage(mLastMessage.data + " ");
                                mSendMessageSemaphore.acquire();
                            }

                            Utils.debug("Messages has send, wait to next part");
                        } else {
                            Utils.error("Can't connect to server.... retry after 60sec");
                            Thread.sleep(30000);
                            continue;
                        }

                        if (getUnsentMessagesCount() > 0) {
                            loadUnsentMessages();
                        } else if (mMessageQueue.size() < 1){
                            Thread.sleep(120000);
                        }
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        };
    }


}