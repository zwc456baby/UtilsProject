package com.example.zhouwc.utils;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by zhouwenchao on 2017-09-05.
 * 获取一个不重复的ID
 */
public class GeneratedID {
    private static final AtomicInteger sNextGeneratedId = new AtomicInteger(1);

    private GeneratedID() {
    }

    public static int getID() {
        while (true) {
            final int result = sNextGeneratedId.get();
            // aapt-generated IDs have the high byte nonzero; clamp to the range under that.
            int newValue = result + 1;
            if (newValue > 0x00FFFFFF) newValue = 1; // Roll over to 1, not 0.
            if (sNextGeneratedId.compareAndSet(result, newValue)) {
                return result;
            }
        }
    }
    
}
