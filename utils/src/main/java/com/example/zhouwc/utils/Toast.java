package com.example.zhouwc.utils;

import android.content.Context;

/**
 * 吐司显示类
 */
public final class Toast {
    private static android.widget.Toast toast = null;
    private static Object lock = new Object();

    public static void showText(Context context, String msg) {
        showMessage(context, msg, android.widget.Toast.LENGTH_SHORT);
    }

    public static void showText(Context context, String msg, int len) {
        showMessage(context, msg, len);
    }

    private static void showMessage(final Context context, final String msg,
                                    final int len) {
        //由于至始至终只使用一个toast,所以不执行cancel  否则不显示
        ThreadUtils.post(new Runnable() {
            @Override
            public void run() {
                synchronized (lock) {
                    if (toast != null) {
                        toast.setText(msg);
                        toast.setDuration(len);
                    } else {
                        toast = android.widget.Toast.makeText(context, msg, len);
                    }
                    toast.show();
                }
            }
        });
    }
}