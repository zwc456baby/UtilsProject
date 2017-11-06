package com.example.zhouwc.networklibs.Device;


import com.example.zhouwc.networklibs.ConnectUtils.ConnectParm;
import com.example.zhouwc.networklibs.ConnectUtils.ConnectToken;
import com.example.zhouwc.networklibs.ConnectUtils.Constans;
import com.example.zhouwc.networklibs.ConnectUtils.DeviceInfo;
import com.example.zhouwc.networklibs.ConnectUtils.Frame;
import com.example.zhouwc.networklibs.ConnectUtils.MessageEntity;
import com.example.zhouwc.networklibs.ConnectUtils.SocketRunnableBase;
import com.example.zhouwc.utils.Log;
import com.example.zhouwc.utils.RandomUtil;
import com.example.zhouwc.utils.ThreadUtils;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.Socket;
import java.net.SocketException;

/**
 * Created by zhouwenchao on 2017-08-18.
 * 服务端 socket
 */
public final class DeviceSocket extends SocketRunnableBase {

    private final String LOG_TAG = "SocketServer";

    private static final int sendTokenCount = 3;

    private int currentSendTokenCount = 0;


    private DeviceSocketCallback callBack;

    public DeviceSocket(String deviceID, Socket socket1, DeviceSocketCallback callBack) {
        this.socket = socket1;
        this.callBack = callBack;
        try {
            this.socket.setTcpNoDelay(true);
        } catch (SocketException e) {
            e.printStackTrace();
        }
        deviceinfo = new DeviceInfo();
        deviceinfo.setPhoneIP(socket.getInetAddress().getHostAddress());
        deviceinfo.setDeviceIP(socket.getLocalAddress().getHostAddress());
        deviceinfo.setDeviceID(deviceID);

        connectToken = new ConnectToken();
        connectToken.setCurrentToken(RandomUtil.getRandomString());
        connectToken.setNextToken(RandomUtil.getRandomString());

        sendRunnable = new SendRunnableImpl();
        dealRunnable = new DealRunnable();
        healtRunnable = new HeatRunnable();
    }


    @Override
    public void run() {
        initSocke();
        int ReceiveNumber;

        try {
            if (!isExit) {
                ThreadUtils.execute(dealRunnable);
                ThreadUtils.execute(sendRunnable);
                ThreadUtils.execute(healtRunnable);
                while (!isExit && (ReceiveNumber = input.read(receive)) != -1 && !isExit) {
                    byte[] newBytes = new byte[ReceiveNumber + (cacheByte == null ? 0 : cacheByte.length)];
                    if (cacheByte != null && cacheByte.length > 0) {
                        System.arraycopy(cacheByte, 0, newBytes, 0, cacheByte.length);
                    }
                    System.arraycopy(receive, 0, newBytes, (cacheByte == null ? 0 : cacheByte.length), ReceiveNumber);
                    cacheByte = ReadBytes(0, newBytes, newBytes.length);

                }
            }
        } catch (Exception e) {
            Log.e(LOG_TAG, "程序出现读写socket异常");
            Log.e(e);
            isClear = true;
            isExit = true;
        }
        Log.v(LOG_TAG, "退出线程");
        clearSocket();
    }

    public boolean sendOriginalData(byte[] bytes) {
        return sendFrame((byte) 54, bytes, bytes.length);
    }

    public boolean sendOriginalData(String message) {
        try {
            byte[] messageByte;
            messageByte = message.getBytes(Constans.CODEC);
            return sendFrame((byte) 52, messageByte, messageByte.length);
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
            return false;
        }
    }

    public DeviceInfo getDeviceInfo() {
        return deviceinfo;
    }


    @Override
    protected void dealFrame(Frame frameTmp) {
        if (!isConnect()) {  //如果还未连接成功收到的请求是鉴权消息
            NotConnectDeal(frameTmp);
            return;
        } else if (currentSendTokenCount != 0) {
            currentSendTokenCount = 0;
        }
        if (!checkToken(frameTmp)) {  //用户Token不正确，执行断开操作
            Log.v("用户Token与当前Token不相同，退出...");
            isExit = true;
            clearSocket();
            return;
        }
        ConnectSuccusDeal(frameTmp);
    }

    private void NotConnectDeal(Frame frameTmp) {
        if (frameTmp.getFrameType() == 1) { // 在此处应该同时判断token中的密码
            byte byt[] = new byte[1];
            if (checkUser(frameTmp)) {
                isConnect = true;
                byt[0] = 0;
                sendFrame((byte) 2, byt, byt.length);
                if (callBack != null) callBack.connectStatus(0, this);
            } else {
                isConnect = false;
                byt[0] = 1;
                sendFrame((byte) 2, byt, byt.length);
                if (callBack != null) callBack.connectStatus(1, this);
                Log.w(LOG_TAG, "手机端传入的用户名和密码错误，断开连接");
                clearSocket();
            }
        } else if (frameTmp.getFrameType() == 3) {
            sendToken(frameTmp);
        } else {
            currentSendTokenCount++; //不论什么type，只要在未连接状态下，只允许三次尝试
        }
        if ((currentSendTokenCount > sendTokenCount)) { //如果连续三次都未连接成功，则断开当前连接
            Log.v(LOG_TAG, "手机端已经尝试三次获取token，但都未成功，主动断开连接");
            disConnect();
        }
    }

    private void ConnectSuccusDeal(Frame frame) {
        if (frame.getFrameType() == 5) {   //
//            Log.v(LOG_TAG, "心跳连接成功");
            heatTime = System.currentTimeMillis();
            sendHeatFrame();
            isResetToken = false;
            sendCacheMessage();
        } else if (frame.getFrameType() == 7) {
            disConnect();  /*客户端主动要求断开连接，此时发送响应并断开连接*/
        } else if (frame.getFrameType() == 51 || frame.getFrameType() == 53) {
            dealRunnable.addMassage(frame.getLoad());
//            callBack.ReceiveData(frame.getLoad(), this);
        } else {
            Log.e("未知type");
        }
    }

    private boolean checkToken(Frame frame) {
        try {
            ConnectToken Info = gson.fromJson(frame.getToken(), ConnectToken.class);

            if (Info.getCurrentToken().equals(connectToken.getCurrentToken()) &&
                    Info.getNextToken().equals(connectToken.getNextToken())) {
                if (frame.getFrameType() == 5) {
                    isResetToken = true;
                    synchronized (messageQueue) {
                        connectToken.setCurrentToken(Info.getNextToken());
                        connectToken.setNextToken(RandomUtil.getRandomString());
                    }
                }
                return true;
            } else {
                Log.v("Token 不正确");
                return false;
            }
        } catch (Exception e) {
            return false;
        }
    }

    private boolean checkUser(Frame frame) {
        try {
            ConnectParm connectParm = gson.fromJson(new String(frame.getLoad(), Constans.CODEC), ConnectParm.class);
            if (callBack != null && callBack.requestConnect(connectParm)) {
                deviceinfo.setUserName(connectParm.getUserName());
                return true;
            } else {
                return false;
            }
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
            return false;
        }
    }

    private void disConnect() {
        sendDisconnect();
        isExit = true;
        clearSocket();
    }


    private void sendHeatFrame() {
        byte[] nullByte = new byte[0];
//        byte[] tokenByte = gson.toJson(connectToken).getBytes();
//        sendFrame((byte) 6, nullByte, nullByte.length);
        synchronized (messageQueue) {
            send((byte) 6, nullByte, nullByte.length);
        }
    }


    private void sendToken(Frame frame) {
        ConnectToken Info = gson.fromJson(frame.getToken(), ConnectToken.class);
        connectToken.setCurrentToken(Info.getNextToken());
        connectToken.setNextToken(RandomUtil.getRandomString());
        byte[] tokenByte = gson.toJson(connectToken).getBytes();
        sendFrame((byte) 4, tokenByte, tokenByte.length);
        currentSendTokenCount++; //不论什么type，只要在未连接状态下，只允许三次尝试
    }

    private boolean sendDisconnect() {
        byte dataByte[] = new byte[1];
        return sendFrame((byte) 8, dataByte, dataByte.length);
    }

    /**
     * 关闭socket 连接，发送断开连接数据
     */
    public void Disconnect() {
        disConnect();
    }


    private void initSocke() {
        if (DeviceSocketManager.getInstance().checkCont(deviceinfo)) {
            Log.v("设备端发现重复连接的设备，关闭当前连接");
            isExit = true;   //如果设备端已经存在一个连接  退出当前连接
            isClear = false;
            if (callBack != null) callBack.connectStatus(2, this);
            return;
        }
        DeviceSocketManager.getInstance().addSocket(deviceinfo, this);
        try {
            if (socket.isConnected()) {
                socket.setSoTimeout((int) Constans.outTime);
                input = socket.getInputStream();
                output = socket.getOutputStream();
                receive = new byte[Constans.DATA_MAX_LENGTH];
            }
        } catch (IOException e) {
            e.printStackTrace();
            isExit = true;
        }
    }

    private void clearSocket() {
        Log.v("设备端关闭 socket 连接 ");
        if (!isClear) {
            return;
        }
        try {
            if (output != null) output.close();
            if (input != null) input.close();
            if (socket != null) socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            output = null;
            input = null;
            socket = null;
            isConnect = false;
            isExit = true;
            sendRunnable.clear();
            dealRunnable.clear();
            sendRunnable = null;
            dealRunnable = null;
            DeviceSocketManager.getInstance().removeSocket(deviceinfo);
            if (callBack != null) callBack.disconnect(deviceinfo);
        }
    }

    private final class DealRunnable extends SocketRunnableBase.DealRunnable {

        @Override
        public void run() {
            while (!isExit) {
                try {
                    if (linkedList.size() > 0) {
                        byte[] bytes = linkedList.removeFirst();
                        dealCacheLength -= bytes.length;
                        if (callBack != null) callBack.ReceiveData(bytes, DeviceSocket.this);
                    } else {
                        Lock();
                    }
                } catch (Exception e) {
//                    cacheByte = null;
//                    linkedList.clear();
                    Log.e("处理数据出错");
                    Log.e(e);
                }
            }
        }
    }

    private final class SendRunnableImpl extends SendRunnable {

        @Override
        public void run() {
            while (!isExit) {
                if (isResetToken || messageQueue.size() == 0) {
                    Lock();
                    continue;
                }
                synchronized (messageQueue) {
                    try {
                        if (isResetToken) continue;
                        MessageEntity messageEntity = messageQueue.removeFirst();
                        if (messageEntity == null) continue;
                        cacheLength -= messageEntity.getLength();
                        send(messageEntity.getType(), messageEntity.getLoad(), messageEntity.getLength());
                    } catch (Exception e) {
                    }
                }
            }
        }
    }

    private class HeatRunnable extends HealtRunnable {
        @Override
        public void run() {
            while (!isExit) {
                ThreadUtils.sleep(Constans.heatTime);
//                heatCount++;
                if (isOutTime()) {
                    Log.w("连接超时");
                    isExit = true;
                    clearSocket();
                }
            }
        }
    }

    protected interface DSocketCallBack {
        boolean requestConnect(ConnectParm connectParm);

        /*0 连接成功 1 密码错误 2 重复连接*/
        void connectStatus(int status, DeviceSocket deviceSocket);

        void ReceiveData(byte[] data, DeviceSocket deviceSocket);

        void disconnect(DeviceInfo deviceInfo);
    }
}
