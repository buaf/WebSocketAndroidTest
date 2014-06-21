package com.example;

import android.app.Activity;
import android.app.ActivityManager;
import android.content.Context;
import android.util.Log;

public class Utils {

    public static boolean isMyServiceRunning(Class<?> serviceClass, Activity activity) {
        ActivityManager manager = (ActivityManager) activity.getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (serviceClass.getName().equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }

    public static void debug(String msg) {
        Log.d("UtilsDebug", getLocation() + "DebugMessage:{" + msg + '}');
    }

    public static void error(String msg) {
        Log.e("UtilsError", getLocation() + "ErrorMessage:{" + msg + '}');
    }

    public static String getLocation() {
        StackTraceElement[] elements = Thread.currentThread().getStackTrace();
        StackTraceElement current = elements[4];
        StringBuilder builder = new StringBuilder();
        builder.append("Location:{");
        builder.append('(').append(current.getClassName()).append(':').append(current.getLineNumber()).append(')');
        builder.append("{ in ").append(current.getMethodName()).append(" }} ");
        return builder.toString();
    }

}
