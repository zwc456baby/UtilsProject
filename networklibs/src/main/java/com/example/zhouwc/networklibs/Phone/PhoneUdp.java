package com.example.zhouwc.networklibs.Phone;

import com.example.zhouwc.networklibs.ConnectUtils.Constans;
import com.example.zhouwc.networklibs.ConnectUtils.DeviceInfo;
import com.example.zhouwc.networklibs.ConnectUtils.RunnableBase;
import com.example.zhouwc.networklibs.ConnectUtils.UdpDataJson;
import com.example.zhouwc.utils.ByteUtils;
import com.example.zhouwc.utils.Log;
import com.google.gson.Gson;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by zhouwenchao on 2017-08-18.
 */
public class PhoneUdp extends RunnableBase {

    private final String LOG_TAG = "UdpServer";

    private UdpDataJson dataJson;
    private final List<DeviceInfo> deviceList = new ArrayList<>();
    private DatagramSocket datagramSocket;
    DatagramPacket SendPacket;
    byte[] send;
    byte[] EncryptedSend;

    private Gson gson;

    private SearchListCallBack callBack;

    public PhoneUdp(SearchListCallBack searchList) {
        this.callBack = searchList;
    }

    @Override
    public void run() {
        initUdpServer();
        int TryCount = 0;
        while (!isExit) {
            try {
   /* 等待接收device端的回复设备信息 */
                byte[] receive = new byte[256];
                byte[] data;
                DatagramPacket ReceivePacket = new DatagramPacket(receive, receive.length);
                ReceivePacket.setPort(Constans.SDAT_SERVER_PORT);
                datagramSocket.setSoTimeout(Constans.RECEIVE_TIMEOUT);
                dataJson.setExcludeDevice(deviceList); //重置排除列表
                send = gson.toJson(dataJson).getBytes(Constans.CODEC);
                EncryptedSend = encryption_1(send, send.length);
                SendPacket =
                        new DatagramPacket(EncryptedSend,
                                EncryptedSend.length,
                                InetAddress.getByName(Constans.UDP_BROADCAST_ADDR),
                                Constans.SDAT_SERVER_PORT);       /* 查找device端的固定端口号 */
                datagramSocket.send(SendPacket);
                datagramSocket.receive(ReceivePacket);
                data = decryption_1(ReceivePacket.getData(), ReceivePacket.getLength());
                DeviceInfo deviceInfo = gson.fromJson(new String(data, Constans.CODEC), DeviceInfo.class);
                deviceInfo.setDeviceIP(ReceivePacket.getAddress().getHostAddress());
                if (deviceInfo.getDeviceID() != null && deviceInfo.getDeviceID().length() > 0) {
                    if (!checkDeviceListContains(deviceList, deviceInfo)) {  //如果不包涵当前UID，则添加到列表
                        deviceList.add(deviceInfo);
                        Log.v("deviceID:" + deviceInfo.getDeviceID());
                    }
                }
            } catch (Exception e) {
            }
            TryCount++;
            if (TryCount > Constans.TRY_AGAIN_NUMBER) {
                break;
            }
        }
        clearUdpServer();
    }

    private void initUdpServer() {
        try {
            gson = new Gson();
            dataJson = new UdpDataJson();
            dataJson.setType(UdpDataJson.TYPE_REQUESTHOST);
            dataJson.setExcludeDevice(deviceList);
//            datagramSocket = new DatagramSocket(Constans.SDAT_PHONE_PORT); // <-- create an unbound socket first
            datagramSocket = new DatagramSocket(null); // <-- create an unbound socket first
            datagramSocket.setReuseAddress(true);
            datagramSocket.bind(new InetSocketAddress(Constans.SDAT_PHONE_PORT)); // <-- now bind it
            if (!datagramSocket.getBroadcast()) {
                Log.e(LOG_TAG, "UDP: The socket is not broadcast socket!");
            }
        } catch (SocketException e) {
            e.printStackTrace();
            isExit = true;
        }
    }

    private void clearUdpServer() {
        if (datagramSocket != null) {
            datagramSocket.disconnect();
            datagramSocket.close();
        }
        datagramSocket = null;
        if (callBack != null) {
            callBack.getDeviceList(deviceList);
        }
    }

    private boolean checkDeviceListContains(List<DeviceInfo> list, DeviceInfo info) {
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
//        byte[] ret = new byte[length];
//        int i;
//        for (i = 0; i < length; i++) {
//            ret[i] = data[i];
//        }
//
//        return ret;//TODO, 需要加入算法
        byte[] ret = new byte[length];
        int i;
        for (i = 0; i < length; i++) {
            ret[i] = data[i];
        }

        return ByteUtils.decrypt(ret);
    }

    /* 用于UDP传输中的数据加密 */
    private byte[] encryption_1(byte[] data, int length) {
//        byte[] ret = new byte[length];
//        int i;
//        for (i = 0; i < length; i++) {
//            ret[i] = data[i];
//        }

//        return ret;//TODO, 需要加入算法
        byte[] ret = new byte[length];
        int i;
        for (i = 0; i < length; i++) {
            ret[i] = data[i];
        }

        return ByteUtils.encrypt(ret);
    }

    public interface SearchListCallBack {
        void getDeviceList(List<DeviceInfo> list);
    }
}
