package com.example.zhouwc.utils;

import android.text.TextUtils;

import java.util.Collection;

/**
 * Created by zhouwenchao on 2017-10-10.
 *
 */
public class ArrayUtil {
    private static ArrayUtil instans;

    private ArrayUtil() {
    }

    public static boolean isEmpty(Collection<?> list) {
        if (list == null) {
            return true;
        }
        if ((list.size() == 0)) return true;

        return false;

    }


    public static boolean Contain(String[] strings, String str) {
        if (strings == null || strings.length == 0) {
            return false;
        }
        for (String tmp : strings) {
            if (tmp.equals(str)) {
                return true;
            }
        }
        return false;
    }

    public static boolean Contain(Collection<?> list, Object str) {
        if (list == null || list.size() == 0) {
            return false;
        }
        for (Object tmp : list) {
            if (tmp == str || tmp.equals(str)) {
                return true;
            }
        }
        return false;
    }

    public static boolean equelse(Collection<?> list, Collection<?> list2) {
        if (list == null || list2 == null) return false;
        if (list.size() != list2.size()) return false;

        if (list == list2) return true;
        for (Object object : list) {
            boolean flag = false;
            for (Object object2 : list2) {
                if (object.equals(object2)) {
                    flag = true;
                    break;
                }
            }
            if (!flag) return false;
        }
        return true;
    }
}
