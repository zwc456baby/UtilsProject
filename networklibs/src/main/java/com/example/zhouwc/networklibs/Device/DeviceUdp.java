package com.example.zhouwc.networklibs.Device;

import android.content.Context;
import android.net.wifi.WifiManager;

import com.example.zhouwc.networklibs.ConnectUtils.Constans;
import com.example.zhouwc.networklibs.ConnectUtils.DeviceInfo;
import com.example.zhouwc.networklibs.ConnectUtils.RunnableBase;
import com.example.zhouwc.networklibs.ConnectUtils.UdpDataJson;
import com.example.zhouwc.utils.ByteUtils;
import com.example.zhouwc.utils.Log;
import com.example.zhouwc.utils.ThreadUtils;
import com.google.gson.Gson;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.List;

/**
 * udp 主要负责用来发送当前设备的 ip 和端口信息，主要的连接方法放在tcp中
 * Created by zhouwenchao on 2017-08-18.
 */
public class DeviceUdp extends RunnableBase {


    private final String LOG_TAG = "UdpServer";

    private DatagramSocket UdpSocket;
    private byte[] receive;//接收数据的数组
    private DeviceInfo deviceInfo;
    private String deviceID;
    private UdpDataJson connectDataJson;
    private DatagramPacket ReceivePacket;
    private Gson gson;

    private final int ErrorOutCount = 3; //如果异常超过一定的次数
    private DeviceUdpCallback callback;

    private WifiManager.MulticastLock UdpLock;

    //    public DeviceUdp(String deviceID, Context context) {
//        this.deviceID = deviceID;
//        WifiManager manager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
//        UdpLock = manager.createMulticastLock("_UDP lock");
//    }
    public DeviceUdp(String deviceID, Context context, DeviceUdpCallback callback) {
        this.deviceID = deviceID;
        this.callback = callback;
        WifiManager manager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
        UdpLock = manager.createMulticastLock("_UDP lock");
    }

    public void closeUDPServer() {
        isExit = true;
        clearUdp();
    }

    @Override
    public void run() {
        initUdp();
        while (!isExit) { //标记当前程序未退出
            try {
//                ThreadUtils.sleep(Constans.OneMessageSleep);  //线程短暂休眠，避免扫描局域网设备带来的性能损耗
                ReceivePacket = new DatagramPacket(receive, receive.length);
                ReceivePacket.setPort(Constans.SDAT_PHONE_PORT);
                Log.d(LOG_TAG, "UDP: Wait the client connect......：");
                UdpSocket.receive(ReceivePacket);      /* 阻塞等待接收 */
                byte[] data = decryption_1(ReceivePacket.getData(), ReceivePacket.getLength());
                try {
                    connectDataJson = gson.fromJson(new String(data, Constans.CODEC), UdpDataJson.class);
                    if (connectDataJson.getType().equals(UdpDataJson.TYPE_REQUESTHOST)) {
                        if (!checkDeviceListContains(connectDataJson.getExcludeDevice(), deviceInfo)) {
                            sendDeviceInfo();
                        }
                    } else {
                        Log.e("当前数据不是请求连接的");
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    Log.w(LOG_TAG, "data数组解析失败：");
                    Log.v(data);
                }
                if (currentErrorCount != 0) currentErrorCount = 0;
            } catch (Exception e) {
                e.printStackTrace();
                currentErrorCount++;
                if (currentErrorCount < ErrorOutCount) {
                    restart();
                }
            }
        }
        clearUdp();
    }

    private void sendDeviceInfo() {
        try {
          /* 发送设备信息（安装位置和唯一识别码） */
            String StrSend = gson.toJson(deviceInfo);
            byte[] send = StrSend.getBytes(Constans.CODEC);
            byte[] EncryptedSend = encryption_1(send, send.length);
            DatagramPacket SendPacket =
                    new DatagramPacket(EncryptedSend,
                            EncryptedSend.length,
                            InetAddress.getByName(ReceivePacket.getAddress().getHostAddress()),  //发送到指定端口的消息
                            ReceivePacket.getPort());
            UdpSocket.send(SendPacket);
              /* 等待确认信息 */
            Log.d(LOG_TAG, "Waiting to confirm whether choose the current device......");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    int currentErrorCount = 0;

    private void restart() {
        ThreadUtils.sleep(Constans.RestartTime);
        clearUdp();
        initUdp();
    }


    private void initUdp() {
        try {
            if (UdpSocket == null) {
                UdpSocket = new DatagramSocket(null);
                UdpSocket.setReuseAddress(true);
                UdpSocket.bind(new InetSocketAddress(Constans.SDAT_SERVER_PORT)); // <-- now bind it
            }
            if (gson == null) gson = new Gson();
            if (receive == null) receive = new byte[Constans.DATA_MAX_LENGTH]; //接收数据的数组
            if (deviceInfo == null) {
                deviceInfo = new DeviceInfo();
                deviceInfo.setDeviceID(deviceID);
            }
            if (callback != null) callback.startUDP();
            UdpLock.acquire();
        } catch (Exception e) {
            e.printStackTrace();
//            isExit = true;  //如果创建UDP通讯失败，退出当前线程
        }
    }


    private void clearUdp() {
        UdpLock.release();
        if (UdpSocket != null) {
            if (UdpSocket.isConnected()) UdpSocket.disconnect();
            UdpSocket.close();
            if (callback != null) callback.closeUDP();
        }
        UdpSocket = null;
    }

    private boolean checkDeviceListContains(List<DeviceInfo> list, DeviceInfo info) {
        if (list == null || list.size() == 0) return false;
        for (DeviceInfo deviceInfo : list) {
            if (deviceInfo.getDeviceID().equals(info.getDeviceID())) return true;
        }
        return false;
    }

    /******************
     * 加密和解密算法
     ***********************/
    /* 用于UDP传输中的数据解密 */
    private byte[] decryption_1(byte[] data, int length) {
        byte[] ret = new byte[length];
        int i;
        for (i = 0; i < length; i++) {
            ret[i] = data[i];
        }
        return ByteUtils.decrypt(ret);
//        return ret;//TODO, 需要加入算法
    }

    /* 用于UDP传输中的数据加密 */
    private byte[] encryption_1(byte[] data, int length) {
        byte[] ret = new byte[length];
        int i;
        for (i = 0; i < length; i++) {
            ret[i] = data[i];
        }
        return ByteUtils.encrypt(ret);
//        return ret;//TODO, 需要加入算法
    }
}
