package com.example.zhouwc.networklibs.Phone;



import com.example.zhouwc.networklibs.ConnectUtils.DeviceInfo;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by zhouwenchao on 2017-08-18.
 */
public class PhoneSocketManager {
    private final Map<DeviceInfo, PhoneSocket> socketMap = new HashMap<>();
    public static final Object Lock = new Object();

    private static class DeviceSocketManagerInstance {
        private static PhoneSocketManager instance = new PhoneSocketManager();
    }

    private PhoneSocketManager() {
    }

    public static PhoneSocketManager getInstance() {
        return DeviceSocketManagerInstance.instance;
    }

    public Map<DeviceInfo, PhoneSocket> getSocketMap() {
        synchronized (Lock) {
            return socketMap;
        }
    }

    public Object getLock() {
        return Lock;
    }

    public boolean removeSocket(DeviceInfo info) {
        synchronized (Lock) {
            if (checkCont(info)) {
                socketMap.remove(info);
                return true;
            } else {
                return false;
            }
        }
    }

    public boolean addSocket(DeviceInfo info, PhoneSocket runnableBase) {
        synchronized (Lock) {
            if (!checkCont(info)) {
                socketMap.put(info, runnableBase);
                return true;
            } else {
                return false;
            }
        }
    }

    /**
     * 手机端 可以通过 ID deviceIp token 三种方式判断设备信息，因为这三者肯定都是不同的
     * 但是建议还是使用 deviceIP来判断，因为 token 有可能是空的，不可靠，只有ip在局域网中是唯一的
     *
     * @return
     */
    public List<DeviceInfo> getSocketList() {
        List<DeviceInfo> list = new ArrayList<>();
        synchronized (Lock) {
            for (Map.Entry<DeviceInfo, PhoneSocket> entry : socketMap.entrySet()) {
                list.add(entry.getKey());
            }
        }
        return list;
    }

    public boolean checkCont(DeviceInfo info) {
        synchronized (Lock) {
            for (Map.Entry<DeviceInfo, PhoneSocket> entry : socketMap.entrySet()) {
                if (entry.getKey().getDeviceIP().equals(info.getDeviceIP())) {  //手机端比较 ip
                    return true;
                }
            }
        }
        return false;
    }

    public boolean checkCont(String adress) {
        synchronized (Lock) {
            for (Map.Entry<DeviceInfo, PhoneSocket> entry : socketMap.entrySet()) {
                if (entry.getKey().getDeviceIP().equals(adress)) {  //手机端比较 ip
                    return true;
                }
            }
        }
        return false;
    }

    public PhoneSocket getPhoneSocket(DeviceInfo info) {
        synchronized (Lock) {
            for (Map.Entry<DeviceInfo, PhoneSocket> entry : socketMap.entrySet()) {
                if (entry.getKey().getDeviceIP().equals(info.getDeviceIP())) {  //设备端因为 服务器 ip都相同，所以比较 token
                    return entry.getValue();
                }
            }
            return null;
        }
    }

    /**
     * @param info 设备端端IP地址
     * @return
     */
    public PhoneSocket getPhoneSocket(String info) {
        synchronized (Lock) {
            for (Map.Entry<DeviceInfo, PhoneSocket> entry : socketMap.entrySet()) {
                if (entry.getKey().getDeviceIP().equals(info)) {  //设备端因为 服务器 ip都相同，所以比较 token
                    return entry.getValue();
                }
            }
            return null;
        }
    }
}
