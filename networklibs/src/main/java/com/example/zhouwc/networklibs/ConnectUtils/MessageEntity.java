package com.example.zhouwc.networklibs.ConnectUtils;

/**
 * Created by zhouwenchao on 2017-10-19.
 */
public class MessageEntity {
    byte type;
    byte[] load;
    int length;

    public MessageEntity(byte type, byte[] load, int length) {
        this.type = type;
        this.load = load;
        this.length = length;
    }

    public byte getType() {
        return type;
    }

    public void setType(byte type) {
        this.type = type;
    }

    public byte[] getLoad() {
        return load;
    }

    public void setLoad(byte[] load) {
        this.load = load;
    }

    public int getLength() {
        return length;
    }

    public void setLength(int length) {
        this.length = length;
    }
}
