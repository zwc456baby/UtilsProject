package com.example.zhouwc.networklibs.ConnectUtils;


import android.content.Context;

import com.example.zhouwc.networklibs.Device.DeviceSocketCallback;
import com.example.zhouwc.networklibs.Device.DeviceSocketManager;
import com.example.zhouwc.networklibs.Device.DeviceUdp;
import com.example.zhouwc.networklibs.Device.DeviceUdpCallback;
import com.example.zhouwc.networklibs.Device.TcpServer;
import com.example.zhouwc.networklibs.Phone.PhoneSocket;
import com.example.zhouwc.networklibs.Phone.PhoneSocketManager;
import com.example.zhouwc.networklibs.Phone.PhoneUdp;
import com.example.zhouwc.utils.ThreadUtils;

/**
 * Created by zhouwenchao on 2017-08-18.
 */
public class ConnectManager {

    private static class ConnectManagerIns {
        private static ConnectManager connectManager = new ConnectManager();
    }

    private ConnectManager() {
    }

    public static ConnectManager getInstance() {
        return ConnectManagerIns.connectManager;
    }


    private TcpServer tcpServer;

    public void startDeviceTCPServer(String deviceID, DeviceSocketCallback callBack) {
        if (tcpServer != null) {
            throw new RuntimeException("tcp 服务器已经开启，请关闭后重试");
        }
        tcpServer = new TcpServer(deviceID, callBack);
        ThreadUtils.execute(tcpServer);
    }

    public void closeDeviceTCPServer() {
        tcpServer.closeServerSocket();
        tcpServer = null;
    }

    private DeviceUdp deviceUdp;

    public void startDeviceUDPServer(String deviceID, Context context, DeviceUdpCallback callback) {
        if (deviceUdp != null) {
            throw new RuntimeException("udp 服务器已经开启，请关闭后重试");
        }
        deviceUdp = new DeviceUdp(deviceID,context, callback);  //启动服务器端的udp服务
        ThreadUtils.execute(deviceUdp);
    }

    public void closeDeviceUDPServer() {
        deviceUdp.closeUDPServer();
        deviceUdp = null;
    }

    public void deviceDisconnect(DeviceInfo deviceInfo) {
        synchronized (DeviceSocketManager.Lock) {
            if (DeviceSocketManager.getInstance().checkCont(deviceInfo)) {
                DeviceSocketManager.getInstance().getDeviceSocket(deviceInfo).Disconnect();
            }
        }
    }

    public void deviceDisconnect(String address) {
        synchronized (DeviceSocketManager.Lock) {
            if (DeviceSocketManager.getInstance().checkCont(address)) {
                DeviceSocketManager.getInstance().getDeviceSocket(address).Disconnect();
            }
        }
    }

    public void serchDeviceList(PhoneUdp.SearchListCallBack call) {
        PhoneUdp phoneUdp = new PhoneUdp(call);
        ThreadUtils.execute(phoneUdp);
    }

    public void connectDevice(DeviceInfo deviceInfo, String username, String password, PhoneSocket.PhoneSocketCallBack callBack) {
        PhoneSocket phoneSocket = new PhoneSocket(deviceInfo, username, password, callBack);
        ThreadUtils.execute(phoneSocket);
    }


    public void phoneDisconnect(DeviceInfo deviceInfo) {
        synchronized (PhoneSocketManager.Lock) {
            if (PhoneSocketManager.getInstance().checkCont(deviceInfo)) {
                PhoneSocketManager.getInstance().getPhoneSocket(deviceInfo).Disconnect();
            }
        }
    }

    public void phoneDisconnect(String adress) {
        synchronized (PhoneSocketManager.Lock) {
            if (PhoneSocketManager.getInstance().checkCont(adress)) {
                PhoneSocketManager.getInstance().getPhoneSocket(adress).Disconnect();
            }
        }
    }

    public boolean sendMessageToDevice(DeviceInfo deviceInfo, String message) {
        synchronized (PhoneSocketManager.Lock) {
            if (PhoneSocketManager.getInstance().checkCont(deviceInfo)) {
                return PhoneSocketManager.getInstance().getPhoneSocket(deviceInfo).sendOriginalData(message);
            } else {
                return false;
            }
        }
    }

    public boolean sendMessageToDevice(DeviceInfo deviceInfo, byte[] message) {
        synchronized (PhoneSocketManager.Lock) {
            if (PhoneSocketManager.getInstance().checkCont(deviceInfo)) {
                return PhoneSocketManager.getInstance().getPhoneSocket(deviceInfo).sendOriginalData(message);
            } else {
                return false;
            }
        }
    }

    public boolean sendMessageToDevice(String adress, String message) {
        synchronized (PhoneSocketManager.Lock) {
            if (PhoneSocketManager.getInstance().checkCont(adress)) {
                return PhoneSocketManager.getInstance().getPhoneSocket(adress).sendOriginalData(message);
            } else {
                return false;
            }
        }
    }

    public boolean sendMessageToDevice(String adress, byte[] message) {
        synchronized (PhoneSocketManager.Lock) {
            if (PhoneSocketManager.getInstance().checkCont(adress)) {
                return PhoneSocketManager.getInstance().getPhoneSocket(adress).sendOriginalData(message);
            } else {
                return false;
            }
        }
    }

    /* 发送消息到 手机端*/

    public boolean sendMessageToPhone(DeviceInfo deviceInfo, String message) {
        synchronized (DeviceSocketManager.Lock) {
            if (DeviceSocketManager.getInstance().checkCont(deviceInfo)) {
                return DeviceSocketManager.getInstance().getDeviceSocket(deviceInfo).sendOriginalData(message);
            } else {
                return false;
            }
        }
    }

    public boolean sendMessageToPhone(DeviceInfo deviceInfo, byte[] message) {
        synchronized (DeviceSocketManager.Lock) {
            if (DeviceSocketManager.getInstance().checkCont(deviceInfo)) {
                return DeviceSocketManager.getInstance().getDeviceSocket(deviceInfo).sendOriginalData(message);
            } else {
                return false;
            }
        }
    }

    public boolean sendMessageToPhone(String adress, String message) {
        synchronized (DeviceSocketManager.Lock) {
            if (DeviceSocketManager.getInstance().checkCont(adress)) {
                return DeviceSocketManager.getInstance().getDeviceSocket(adress).sendOriginalData(message);
            } else {
                return false;
            }
        }
    }

    public boolean sendMessageToPhone(String adress, byte[] message) {
        synchronized (DeviceSocketManager.Lock) {
            if (DeviceSocketManager.getInstance().checkCont(adress)) {
                return DeviceSocketManager.getInstance().getDeviceSocket(adress).sendOriginalData(message);
            } else {
                return false;
            }
        }
    }


}
