package com.example.zhouwc.utils;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;

/**
 * Created by zhouwenchao on 2017-03-17.
 */
public class AlarmUtil {

    /**
     * 重启主程序，应该在发生异常或者是anr时调用
     *
     * @param context 上下文
     */
    public static final void restart(Context context, Class activity, int delayTime) {
        Log.e("程序异常重启");
        Intent intent = new Intent(context.getApplicationContext(), activity);
        PendingIntent restartIntent = PendingIntent.getActivity(
                context.getApplicationContext(), 0, intent,
                0);
        // 退出程序
        AlarmManager mgr = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        mgr.set(AlarmManager.RTC, System.currentTimeMillis() + delayTime, restartIntent); // 20毫秒钟后重启应用
        android.os.Process.killProcess(android.os.Process.myPid()); //结束本进程
    }
}
