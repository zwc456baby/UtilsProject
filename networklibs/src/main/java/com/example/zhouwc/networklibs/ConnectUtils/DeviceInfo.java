package com.example.zhouwc.networklibs.ConnectUtils;

import java.io.Serializable;

/**
 * Created by zhouwenchao on 2017-08-18.
 */
public class DeviceInfo implements Serializable {
    private String deviceID;  /*UDP 阶段就可以获取到的*/
    private String deviceIP;  /* UDP 通讯成功后，设置idevice IP*/
//    private String deviceToken; /* TCP通讯尚未鉴权阶段即需要交换token*/
    private String phoneIP;  /* tcp 阶段获取 phoneIP*/
    private String userName; /*鉴权成功之后，才会获得 userName */

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getPhoneIP() {
        return phoneIP;
    }

    public void setPhoneIP(String phoneIP) {
        this.phoneIP = phoneIP;
    }

    public String getDeviceIP() {
        return deviceIP;
    }

    public void setDeviceIP(String deviceIP) {
        this.deviceIP = deviceIP;
    }

    public String getDeviceID() {
        return deviceID;
    }

    public void setDeviceID(String deviceID) {
        this.deviceID = deviceID;
    }

//    public String getDeviceToken() {
//        return deviceToken;
//    }
//
//    public void setDeviceToken(String deviceToken) {
//        this.deviceToken = deviceToken;
//    }
}
