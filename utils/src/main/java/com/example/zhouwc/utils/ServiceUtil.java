package com.example.zhouwc.utils;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;

/**
 * Created by zhouwenchao on 2017-10-25.
 * 服务工具类
 */
public class ServiceUtil {
    /**
     * 用来判断服务是否运行.
     *
     * @param mContext  上下文
     * @param className 判断的服务名字
     * @param count     判断条目，建议 200 及以上
     * @return true 在运行 false 不在运行
     */
    public static boolean isServiceRunning(Context mContext, String className, int count) {
        ActivityManager activityManager = (ActivityManager)
                mContext.getSystemService(Context.ACTIVITY_SERVICE);
        List<ActivityManager.RunningServiceInfo> serviceList
                = activityManager.getRunningServices(count);  /* 获取条数，太少了会导致判断失败*/
        if (!(serviceList.size() > 0)) {
            return false;
        }
        for (int i = 0; i < serviceList.size(); i++) {
            if (serviceList.get(i).service.getClassName().equals(className)) {
                return true;
            }
        }
        return false;
    }

    public static void startForeground(Service context, Class<? extends Service> classs, int NOTIFICATION_ID, Class<? extends Activity> startActivity) {
        final Class<?>[] mSetForegroundSignature = new Class[]{
                boolean.class};
        final Class<?>[] mStartForegroundSignature = new Class[]{
                int.class, Notification.class};

        NotificationManager mNM = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        Method mStartForeground;
        Method mSetForeground;

        Object[] mSetForegroundArgs = new Object[1];
        Object[] mStartForegroundArgs = new Object[2];


        try {
            mStartForeground = classs.getMethod("startForeground", mStartForegroundSignature);
        } catch (NoSuchMethodException e) {
            mStartForeground = null;
        }

        try {
            mSetForeground = context.getClass().getMethod("setForeground",
                    mSetForegroundSignature);
        } catch (NoSuchMethodException e) {
            throw new IllegalStateException(
                    "OS doesn't have Service.startForeground OR Service.setForeground!");
        }
        Notification.Builder builder = new Notification.Builder(context);
        PendingIntent contentIntent = PendingIntent.getActivity(context, 0,
                new Intent(context, startActivity), 0);
        builder.setContentIntent(contentIntent);
//        builder.setSmallIcon(R.drawable.icon);
        builder.setTicker("Foreground Service Start");
        builder.setContentTitle("BVT_TouchScreen Service");
        builder.setContentText("常驻前台服务");
        Notification notification = builder.build();

        if (mStartForeground != null) {
            mStartForegroundArgs[0] = NOTIFICATION_ID;
            mStartForegroundArgs[1] = notification;
            invokeMethod(context, mStartForeground, mStartForegroundArgs);
            return;
        }
//
//            // Fall back on the old API.
//            mSetForegroundArgs[0] = Boolean.TRUE;
//            invokeMethod(mSetForeground, mSetForegroundArgs);
//            mNM.notify(id, notification);
//        } else {
            /* 还可以使用以下方法，当sdk大于等于5时，调用sdk现有的方法startForeground设置前台运行，
             * 否则调用反射取得的sdk level 5（对应Android 2.0）以下才有的旧方法setForeground设置前台运行 */

        if (Build.VERSION.SDK_INT >= 5) {
            context.startForeground(NOTIFICATION_ID, notification);
        } else {
            // Fall back on the old API.
            mSetForegroundArgs[0] = Boolean.TRUE;
            invokeMethod(context, mSetForeground, mSetForegroundArgs);
            mNM.notify(NOTIFICATION_ID, notification);
        }
    }

    public static void stopForeground(Service context, Class<? extends Service> classs, int NOTIFICATION_ID) {
        final Class<?>[] mSetForegroundSignature = new Class[]{
                boolean.class};
        final Class<?>[] mStopForegroundSignature = new Class[]{
                boolean.class};

        NotificationManager mNM = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        Method mStopForeground;
        Method mSetForeground;

        Object[] mSetForegroundArgs = new Object[1];
        Object[] mStopForegroundArgs = new Object[1];


        try {
            mStopForeground = classs.getMethod("stopForeground", mStopForegroundSignature);
        } catch (NoSuchMethodException e) {
            mStopForeground = null;
        }

        try {
            mSetForeground = context.getClass().getMethod("setForeground",
                    mSetForegroundSignature);
        } catch (NoSuchMethodException e) {
            throw new IllegalStateException(
                    "OS doesn't have Service.startForeground OR Service.setForeground!");
        }

        if (mStopForeground != null) {
            mStopForegroundArgs[0] = Boolean.TRUE;
            invokeMethod(context, mStopForeground, mStopForegroundArgs);
            return;
        }
            /* 还可以使用以下方法，当sdk大于等于5时，调用sdk现有的方法stopForeground停止前台运行，
             * 否则调用反射取得的sdk level 5（对应Android 2.0）以下才有的旧方法setForeground停止前台运行 */
        if (Build.VERSION.SDK_INT >= 5) {
            context.stopForeground(true);
        } else {
            // Fall back on the old API.  Note to cancel BEFORE changing the
            // foreground state, since we could be killed at that point.
            mNM.cancel(NOTIFICATION_ID);
            mSetForegroundArgs[0] = Boolean.FALSE;
            invokeMethod(context, mSetForeground, mSetForegroundArgs);
        }
    }


    private static void invokeMethod(Service service, Method method, Object[] args) {
        try {
            method.invoke(service, args);
        } catch (InvocationTargetException e) {
            // Should not happen.
            Log.w("ApiDemos", e);
        } catch (IllegalAccessException e) {
            // Should not happen.
            Log.w("ApiDemos", e);
        }
    }
}
