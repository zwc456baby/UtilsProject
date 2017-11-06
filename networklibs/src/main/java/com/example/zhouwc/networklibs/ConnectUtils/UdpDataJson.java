package com.example.zhouwc.networklibs.ConnectUtils;

import java.util.List;

/**
 * Created by zhouwenchao on 2017-08-18.
 */
public class UdpDataJson {
    /* type 定义*/
    public static final String TYPE_REQUESTHOST = "Do you request host";
    String type;
    List<DeviceInfo> excludeDevice;

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public List<DeviceInfo> getExcludeDevice() {
        return excludeDevice;
    }

    public void setExcludeDevice(List<DeviceInfo> excludeDevice) {
        this.excludeDevice = excludeDevice;
    }
}
