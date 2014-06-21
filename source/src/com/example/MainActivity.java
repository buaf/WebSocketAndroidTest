package com.example;


import android.app.Activity;
import android.content.ComponentName;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;

import java.util.concurrent.Semaphore;

public class MainActivity extends Activity {

    private final Messenger mMessenger = new Messenger(getMessengerHandler());
    private final ServiceConnection mConnection = getServiceConnection();
    private Messenger mService = null;
    private boolean mBound = false;

    private ListView mMsgListView;
    private EditText mSendMsgEdit;
    private Button mSendMsgButton;
    private MessagesListAdapter mMsgListAdapter;

    void doBindService() {
        Utils.debug("Bind service");
        mBound = bindService(new Intent(this, ConnectionService.class), mConnection, Context.BIND_AUTO_CREATE);
    }

    void doUnbindService() {
        Utils.debug("Unbind service");
        if (mBound) {
            if (mService != null) {
                try {
                    Message msg = Message.obtain(null, ConnectionService.MSG_UNREGISTER_CLIENT);
                    msg.replyTo = mMessenger;
                    mService.send(msg);
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
            }

            unbindService(mConnection);
            mBound = false;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Utils.debug("");

        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.main);

        if (!Utils.isMyServiceRunning(ConnectionService.class, this)) {
            startService(new Intent(this, ConnectionService.class));
        }

        initUi();
    }

    private void initUi() {
        mMsgListView = (ListView) findViewById(R.id.msgListView);
        mMsgListAdapter = new MessagesListAdapter(this, null, 0);
        mMsgListView.setAdapter(mMsgListAdapter);

        mSendMsgEdit = (EditText) findViewById(R.id.sendMsgEdit);
        mSendMsgButton = (Button) findViewById(R.id.sendMsgButton);
        mSendMsgButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String textMessage = mSendMsgEdit.getText().toString();

                long unixTime = System.currentTimeMillis() / 1000L;
                ContentValues messageValues = new ContentValues();
                messageValues.put(MessagesTableConstants._DATE, unixTime);
                messageValues.put(MessagesTableConstants._TEXT, textMessage);
                Uri createdRow = getContentResolver().insert(MessagesTableConstants.CONTENT_URI, messageValues);

                long rowId = ContentUris.parseId(createdRow);
                ContentValues unsentMessageValues = new ContentValues();
                unsentMessageValues.put(UnsentMessagestTableConstants._ID, rowId);
                getContentResolver().insert(UnsentMessagestTableConstants.CONTENT_URI, unsentMessageValues);

                Message msg = Message.obtain(null, ConnectionService.MSG_SEND_SOCKET, 0, 0);
                Bundle bundle = msg.getData();
                bundle.putString(ConnectionService.BUNDLE_MESSAGE_TEXT, textMessage);
                bundle.putLong(ConnectionService.BUNDLE_MESSAGE_ID, rowId);
                try {
                    mService.send(msg);
                } catch (RemoteException e) {
                    e.printStackTrace();
                }

                Utils.debug("Write:" + textMessage);
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        doBindService();
    }

    @Override
    protected void onPause() {
        doUnbindService();
        super.onPause();
    }

    private ServiceConnection getServiceConnection() {
        return new ServiceConnection() {
            public void onServiceConnected(ComponentName className,
                                           IBinder service) {
                Utils.debug("");
                mService = new Messenger(service);
                try {
                    Message msg = Message.obtain(null, ConnectionService.MSG_REGISTER_CLIENT);
                    msg.replyTo = mMessenger;
                    mService.send(msg);
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
            }

            public void onServiceDisconnected(ComponentName className) {
                Utils.debug("");
                mService = null;
            }
        };
    }

    private Handler getMessengerHandler() {
        return new Handler() {
            @Override
            public void handleMessage(Message msg) {
                switch (msg.what) {
                    case ConnectionService.MSG_SEND_SOCKET:
                        Bundle bundle = msg.getData();
                        String msgStr = bundle.getString(ConnectionService.BUNDLE_MESSAGE_TEXT);
                        long msgId = bundle.getLong(ConnectionService.BUNDLE_MESSAGE_ID);

                        String where = UnsentMessagestTableConstants._ID + "=?";
                        getContentResolver().delete(UnsentMessagestTableConstants.CONTENT_URI, where, new String[]{String.valueOf(msgId)});

                        Utils.debug("Server message:" + msgStr + "Id:" + msgId + " Remove from:" + UnsentMessagestTableConstants._TABLE_NAME);
                        mMsgListAdapter.notifyDataSetChanged();

                        break;
                    default:
                        super.handleMessage(msg);
                }
            }
        };
    }

}