package com.example.zhouwc.networklibs.ConnectUtils;


import com.example.zhouwc.utils.ByteUtils;

import java.io.UnsupportedEncodingException;

/**
 * Created by zhouwenchao on 2017-08-18.
 */
public class Frame {
    private final String LOG_TAG = "Frame";

    private byte[] data;

    private String Token;

    private byte[] loadData;

    public Frame() {
    }

    public synchronized boolean setFrame(byte[] data) {
//        byte tmpbyte[] = new byte[length];
//        System.arraycopy(data, 0, tmpbyte, 0, tmpbyte.length);  //将数组通过这种方式，去除无用空byte
        this.data = ByteUtils.decrypt(data);  /*将数据解密*/
        if (this.data == data) return false;  /*解密失败会导致前后数据一样*/
        if (data.length < 9) return false;  /*最小数据应该是一个 type +4byte 的长度信息+4byte 的data 长度信息，共9位*/
//        InitToken();
//        InitLoadData();
        return InitToken() && InitLoadData();
    }

    private boolean InitToken() {
        int userNameLength = getTokenLength();
        if (userNameLength == -1 || userNameLength > Constans.DATA_MAX_LENGTH) return false;

        int userNameBeginPosition = getTokenBeginPosition();

        if ((data.length - userNameBeginPosition) < userNameLength) {  /*数据剩下的长度，小于信息长度*/
            return false;
        }

        byte[] ret = new byte[userNameLength];
        System.arraycopy(data, userNameBeginPosition, ret, 0, ret.length);
//        for (int i = 0; i < userNameLength; i++) {
//            ret[i] = data[userNameBeginPosition++];
//        }
        try {
            Token = new String(ret, Constans.CODEC);
            return true;
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
            return false;
        }
    }

    private boolean InitLoadData() {
        int LoadLength = getLoadLength();
        if (LoadLength == -1 || LoadLength > Constans.LOAD_DATA_MAX_LENGTH) return false;

        int LoadBegin = getLoadBeginPosition();
        if ((data.length - LoadBegin) < LoadLength) return false; /*数据剩余长度，小于加载长度*/

        byte[] ret = new byte[LoadLength];
        System.arraycopy(data, LoadBegin, ret, 0, ret.length);

        loadData = ret;
        return true;
    }


    /**
     * 数据总长度
     *
     * @return
     */
    private int getLoadLength() {
        byte[] loadLengthByte = new byte[4];
        System.arraycopy(data, 5, loadLengthByte, 0, loadLengthByte.length);
        int length = ByteUtils.getInt(loadLengthByte);
        if (length < 0) return -1;

        return length;
//        int multiplier = 1;
//        int value = 0;
//        int index = getTokenDataEndIndex();
//        byte encodedByte;
//
//        do {
//            encodedByte = data[index++];
//            value += (encodedByte & 127) * multiplier;
//            multiplier *= 128;
//            if (multiplier > 128 * 128 * 128) {
//                Log.e(LOG_TAG, "Invalid LoadLength!");
//                value = 0;
//                return value;
//            }
//        } while ((encodedByte & 128) != 0);
//
//        return value;
    }

    /**
     * 数据起始位置
     *
     * @return
     */
    private int getLoadBeginPosition() {
        int tokenStartIndex = getTokenBeginPosition();
        int tokenLength = getTokenLength();
        if (tokenLength == -1 || tokenLength > Constans.DATA_MAX_LENGTH) return -1;

        return tokenStartIndex + tokenLength;

//        int multiplier = 1;
//        int index = getTokenDataEndIndex();
//        byte encodedByte;
//        do {
//            encodedByte = data[index++];
//            multiplier *= 128;
//            if (multiplier > 128 * 128 * 128) {
//                Log.e(LOG_TAG, "Invalid LoadLength!");
//                return index;
//            }
//        } while ((encodedByte & 128) != 0);
//        return index;
    }

//    /**
//     * 获取用户名占用的数据的结束下标
//     * 结束下标 等于 携带的名字长度 加上 长度数组起始下标
//     *
//     * @return
//     */
//    private int getTokenDataEndIndex() {
//        int index = 0;
//        index += getTokenLength();
//        index += getTokenBeginPosition();
//        return index;
//
//    }

    /**
     * 获取token长度
     *
     * @return
     */
    private int getTokenLength() {
        byte[] tokenLengthByte = new byte[4];
        System.arraycopy(data, 1, tokenLengthByte, 0, tokenLengthByte.length);
        int length = ByteUtils.getInt(tokenLengthByte);
        if (length < 0) return -1;

        return length;

//        int multiplier = 1;
//        int value = 0;
//        int index = 1;
//        byte encodedByte;
//        do {
//            encodedByte = data[index++];
//            value += (encodedByte & 127) * multiplier;
//            multiplier *= 128;
//            if (multiplier > 128 * 128 * 128) {
//                Log.e(LOG_TAG, "Invalid LoadLength!");
//                value = 0;
//                return value;
//            }
//        } while ((encodedByte & 128) != 0);
//        return value;
    }

    /**
     * 获取Token的起始下标
     *
     * @return Token数据的起始下标
     */
    private int getTokenBeginPosition() {
        return 9;
//        int multiplier = 1;
//        int index = 1;
//        byte encodedByte;
//        do {
//            encodedByte = data[index++];
//            multiplier *= 128;
//            if (multiplier > 128 * 128 * 128) {
//                Log.e(LOG_TAG, "Invalid LoadLength!");
//                return index;
//            }
//        } while ((encodedByte & 128) != 0);
//        return index;
    }


    public byte getFrameType() {
        return data[0];
    }

    /**
     * 获取用户名
     *
     * @return 用户名的byte数组
     */
    public String getToken() {
        return Token;
    }

    public byte[] getLoad() {
        return loadData;
    }
}
