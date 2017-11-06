package com.example.zhouwc.networklibs.Device;



import com.example.zhouwc.networklibs.ConnectUtils.DeviceInfo;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by zhouwenchao on 2017-08-18.
 */
public class DeviceSocketManager {
    private final Map<DeviceInfo, DeviceSocket> socketMap = new HashMap<>();
    public static final Object Lock = new Object();

    private static class DeviceSocketManagerInstance {
        private static DeviceSocketManager instance = new DeviceSocketManager();
    }

    private DeviceSocketManager() {
    }

    public static DeviceSocketManager getInstance() {
        return DeviceSocketManagerInstance.instance;
    }

    public boolean removeSocket(DeviceInfo token) {
        synchronized (Lock) {
            if (checkCont(token)) {
                socketMap.remove(token);
                return true;
            } else {
                return false;
            }
        }
    }

    public Map<DeviceInfo, DeviceSocket> getSocketMap() {
        synchronized (Lock) {
            return socketMap;
        }
    }


    public boolean addSocket(DeviceInfo token, DeviceSocket runnableBase) {
        synchronized (Lock) {
            if (!checkCont(token)) {
                socketMap.put(token, runnableBase);
                return true;
            } else {
                return false;
            }
        }
    }

    /**
     * 设备端，因为保存的三个信息， 一个是自己的IP，一个是自己的name,所以只能通过 Token 判断 手机端连接情况
     * Token 在设备端肯定不为空
     *
     * @return
     */
    public List<DeviceInfo> getSocketList() {
        List<DeviceInfo> list = new ArrayList<>();
        synchronized (Lock) {
            for (Map.Entry<DeviceInfo, DeviceSocket> entry : socketMap.entrySet()) {
                list.add(entry.getKey());
            }
        }
        return list;
    }

    public boolean checkCont(DeviceInfo info) {
        synchronized (Lock) {
            for (Map.Entry<DeviceInfo, DeviceSocket> entry : socketMap.entrySet()) {
                if (entry.getKey().getPhoneIP().equals(info.getPhoneIP())) {  //设备端因为 服务器 ip都相同，所以比较 手机端IP
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * @param adress 手机端IP地址
     * @return
     */
    public boolean checkCont(String adress) {
        synchronized (Lock) {
            for (Map.Entry<DeviceInfo, DeviceSocket> entry : socketMap.entrySet()) {
                if (entry.getKey().getPhoneIP().equals(adress)) {  //设备端因为 服务器 ip都相同，所以比较 手机端IP
                    return true;
                }
            }
        }
        return false;
    }

    public DeviceSocket getDeviceSocket(DeviceInfo info) {
        synchronized (Lock) {
            for (Map.Entry<DeviceInfo, DeviceSocket> entry : socketMap.entrySet()) {
                if (entry.getKey().getPhoneIP().equals(info.getPhoneIP())) {  //设备端因为 服务器 ip都相同，所以比较 token
                    return entry.getValue();
                }
            }
            return null;
        }
    }

    public DeviceSocket getDeviceSocket(String info) {
        synchronized (Lock) {
            for (Map.Entry<DeviceInfo, DeviceSocket> entry : socketMap.entrySet()) {
                if (entry.getKey().getPhoneIP().equals(info)) {  //设备端因为 服务器 ip都相同，所以比较 token
                    return entry.getValue();
                }
            }
            return null;
        }
    }
}
