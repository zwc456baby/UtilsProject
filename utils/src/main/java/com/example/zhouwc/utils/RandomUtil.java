package com.example.zhouwc.utils;

/**
 * Created by zhouwenchao on 2017-10-18.
 * 产生随机数或随机字符串
 */
public class RandomUtil {
    private static final String string = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";

    public static int getRandom(int count) {
        return (int) Math.round(ArithUtil.mul(Math.random(), count));
    }

    public static String getRandomString(int length) {
        StringBuilder sb = new StringBuilder();
        int len = string.length();
        for (int i = 0; i < length; i++) {
            sb.append(string.charAt(getRandom(len - 1)));
        }
        return sb.toString();
    }

    public static String getRandomString() {
        return getRandomString(20);
    }
}
