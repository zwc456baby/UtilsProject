package com.example.zhouwc.networklibs.Device;


import com.example.zhouwc.networklibs.ConnectUtils.Constans;
import com.example.zhouwc.networklibs.ConnectUtils.RunnableBase;
import com.example.zhouwc.utils.Log;
import com.example.zhouwc.utils.ThreadUtils;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * Created by zhouwenchao on 2017-08-18.
 */
public class TcpServer extends RunnableBase {
    private ServerSocket server;

    private final int ErrorOutCount = 3; //如果异常超过一定的次数
    private int currentErrorCount = 0;
    private DeviceSocketCallback callBack;
    private String deviceID;

    private int port;

    public TcpServer(String deviceID, DeviceSocketCallback callBack) {
        this.port = Constans.SDAT_SERVER_PORT;
        this.callBack = callBack;
        this.deviceID = deviceID;
    }

    public TcpServer(int port, String deviceID, DeviceSocketCallback callBack) {
        this.port = port;
        this.callBack = callBack;
        this.deviceID = deviceID;
    }

    @Override
    public void run() {
        initServerSocket();
        currentErrorCount = 0;
        while (!isExit) {
//            ThreadUtils.sleep(Constans.OneMessageSleep); //休眠指定时间，避免死线程带来的性能损耗
            try {
                Log.v("等待 socket 连接请求");
                Socket socket = server.accept();
                if (isExit) {
                    Log.v("已经退出了");
                    break;
                }

                DeviceSocket socketServer = new DeviceSocket(deviceID, socket, callBack);
                if (this.callBack != null) {
                    this.callBack.createSocket(socketServer);
                }
                ThreadUtils.execute(socketServer);  //运行socket通讯
                if (currentErrorCount != 0) currentErrorCount = 0;
            } catch (Exception e) {
                e.printStackTrace();
                currentErrorCount++;
                if (currentErrorCount < ErrorOutCount) {
                    reStart();
                } else {
                    isExit = true;
                }
            }
        }
        clearServerSocket();
        if (isExit) {
            if (callBack != null) callBack.closeServer();
        }
    }

    private void reStart() {
        ThreadUtils.sleep(Constans.RestartTime);
        clearServerSocket();
        initServerSocket();
    }

    private void initServerSocket() {
        try {
//            server = new ServerSocket(Constans.SDAT_SERVER_PORT);   /* 用户有可能会更换 网络，导致网络端口变化，或者代理端口变化 */
            server = new ServerSocket();   /* 用户有可能会更换 网络，导致网络端口变化，或者代理端口变化 */
            server.setReuseAddress(true);
            server.bind(new InetSocketAddress(this.port));
        } catch (IOException e) {
            e.printStackTrace();
            isExit = true;  //初始化失败则退出程序
        } finally {
            if (isExit) {
                if (callBack != null) callBack.closeServer();
            }
        }
    }

    private void clearServerSocket() {
        try {
            Log.v("关闭 socket 连接服务 ");
            if (server != null) {
                server.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            server = null;
        }
    }

    public void closeServerSocket() {
        isExit = true;  //初始化失败则退出程序
        clearServerSocket();
        if (isExit) {
            if (callBack != null) callBack.closeServer();
        }
    }

    protected interface TCPServerCallBack {
        void createSocket(DeviceSocket deviceSocket);

        void closeServer();
    }
}
