package com.example.zhouwc.utils;

import android.content.Context;
import android.widget.Toast;

/**
 * 吐司显示类
 */
public final class showToast {
    private static Toast toast = null;

    public static void showText(Context context, String msg) {
        showMessage(context, msg, Toast.LENGTH_SHORT);
    }

    public static void showText(Context context, String msg, int len) {
        showMessage(context, msg, len);
    }

    private synchronized static void showMessage(Context context, String msg,
                                                 int len) {
        //由于至始至终只使用一个toast,所以不执行cancel  否则不显示
        if (toast != null) {
            toast.setText(msg);
            toast.setDuration(len);
        } else {
            toast = Toast.makeText(context, msg, len);
        }
        toast.show();
    }
}