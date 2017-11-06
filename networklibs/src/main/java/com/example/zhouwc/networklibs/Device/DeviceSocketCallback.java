package com.example.zhouwc.networklibs.Device;

/**
 * 将多个回调放在一起
 * Created by hasee on 2017/8/19.
 */
public interface DeviceSocketCallback extends TcpServer.TCPServerCallBack, DeviceSocket.DSocketCallBack {
}
