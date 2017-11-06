package com.example.zhouwc.networklibs.Phone;


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
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketException;

/**
 * Created by zhouwenchao on 2017-08-18.
 * 客户端
 */
public final class PhoneSocket extends SocketRunnableBase {
    private final String LOG_TAG = "SocketServer";

//    private int heatOutCount = (int) (Constans.outTime / Constans.heatTime);
//    private int heatCount = 0;


    private String Name;
    private String Password;
    private ConnectParm connectParm; //连接参数 每次在连接成功后，每次请求都要带有这串数据


    private PhoneSocket.PhoneSocketCallBack callBack;

    public PhoneSocket(DeviceInfo deviceInfo, String userName, String password, PhoneSocketCallBack callBack) {
        deviceinfo = deviceInfo;
        deviceinfo.setUserName(userName);
        connectToken = new ConnectToken();
        connectToken.setNextToken(RandomUtil.getRandomString());
        connectToken.setCurrentToken(connectToken.getNextToken());
        this.Name = userName;
        this.Password = password;
        this.callBack = callBack;

        sendRunnable = new SendRunnableImpl();
        dealRunnable = new DealRunnable();
        healtRunnable = new HeatRunnable();
    }

    @Override
    public void run() {
        initSocketServer();
        try {
            if (!isExit) {
                socket.connect(new InetSocketAddress(deviceinfo.getDeviceIP(), Constans.SDAT_SERVER_PORT), (int) Constans.outTime);
                socket.setTcpNoDelay(true);
                deviceinfo.setPhoneIP(socket.getLocalAddress().getHostAddress());
                input = socket.getInputStream();
                output = socket.getOutputStream();
                int ReceiveNumber;
                ThreadUtils.execute(dealRunnable);
                ThreadUtils.execute(sendRunnable);
                ThreadUtils.execute(healtRunnable);
                sendRequestConnect(); //发送请求连接的帧
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
            Log.e(LOG_TAG, "出现读写socket异常");
            isClear = true;
            isExit = true;
        }
        Log.w("线程结束循环：" + isExit);
        clearSocketServer();
    }

    public boolean sendOriginalData(String data) {
        try {
            byte dataByte[] = data.getBytes(Constans.CODEC);
            return sendFrame((byte) 51, dataByte, dataByte.length);
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean sendOriginalData(byte[] data) {
        return sendFrame((byte) 53, data, data.length);
    }

    /**
     * 关闭socket 连接，发送断开连接数据
     */
    public void Disconnect() {
        sendDisconnect();
        Log.v("主动断开");
        clearSocketServer();
    }

    public DeviceInfo getDeviceInfo() {
        return deviceinfo;
    }


    private void sendHeatFrame() {
        byte[] nullByte = new byte[0];
//        byte[] tokenByte = gson.toJson(connectToken).getBytes();
        sendHeatRequestFrame((byte) 5, nullByte, nullByte.length);
    }

    @Override
    protected void dealFrame(Frame frame) {
        if (!isConnect()) {
            NotConnectDeal(frame);
            return;
        }
        if (!checkToken(frame)) {  //用户Token不正确，执行断开操作
            Log.v("用户Token与当前Token不相同，退出...");
            isExit = true;
            clearSocketServer();
            return;
        }
        ConnectSuccusDeal(frame);
    }

    private void InitToken(Frame frame) {
        try {
            ConnectToken Info = gson.fromJson(new String(frame.getLoad(), Constans.CODEC), ConnectToken.class);
            connectToken.setCurrentToken(Info.getCurrentToken());
            connectToken.setNextToken(Info.getNextToken());
            isResetToken = false;
            sendCacheMessage();
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
    }

    private void NotConnectDeal(Frame frame) {
        if (frame.getFrameType() == 4) {
            InitToken(frame);
            InitConnectParm();
            sendUserParam();
        } else if (frame.getFrameType() == 2) {
            if (frame.getLoad()[0] == 0) {
                isConnect = true;   //表示鉴权成功。
                boolean addsuccuss;
                addsuccuss = PhoneSocketManager.getInstance().addSocket(deviceinfo, this); // 将当前线程加入到管理器中
                if (callBack != null) this.callBack.connectStatus(frame.getLoad()[0], this);  //
                if (!addsuccuss) {
                    isClear = false;
                    isConnect = false;
                    isExit = true;  //如果添加失败，说明已经存在一个相同的连接，则退出当前连接，但不关闭连接
                }
            } else if (frame.getLoad()[0] == 1) {
                Log.v("不允许连接");
                isConnect = false;
                isExit = true;
                clearSocketServer();
            }
        } else {
            sendRequestConnect();
        }
    }

    private void ConnectSuccusDeal(Frame frame) {
        if (frame.getFrameType() == 6) {
            heatTime = System.currentTimeMillis();
//            heatCount = 0;  //重置心跳时间
            isResetToken = false;
            sendCacheMessage();
        } else if (frame.getFrameType() == 8) {
            Log.v("响应断开连接");
            clearSocketServer();
        } else if (frame.getFrameType() == 52 || frame.getFrameType() == 54) {
            dealRunnable.addMassage(frame.getLoad());
//            callBack.ReceiveData(frame.getLoad(), this);
        } else {
            Log.e("未知type");
        }
    }

    private boolean checkToken(Frame frame) {
        try {
            ConnectToken Info = gson.fromJson(frame.getToken(), ConnectToken.class);
            if (frame.getFrameType() == 6) {
                if (Info.getCurrentToken().equals(connectToken.getNextToken())) {
                    connectToken.setCurrentToken(Info.getCurrentToken());
                    connectToken.setNextToken(Info.getNextToken());
                    return true;
                } else {
                    return false;
                }
            }
            if (Info.getCurrentToken().equals(connectToken.getCurrentToken()) &&
                    Info.getNextToken().equals(connectToken.getNextToken())) {
                return true;
            } else {
                Log.v("当前帧：" + Info.getCurrentToken());
                return false;
            }
        } catch (Exception e) {
            return false;
        }
    }

    private void InitConnectParm() {
        connectParm = new ConnectParm();
//        connectParm.setToken(deviceinfo.getDeviceToken());
        connectParm.setUserName(Name);
        connectParm.setPassword(Password);
    }

    /**
     * 在此处应该将密码也传入
     */
    private void sendUserParam() {
        try {
            byte[] tokenByte;
            tokenByte = gson.toJson(connectParm).getBytes(Constans.CODEC);
            sendFrame((byte) 1, tokenByte, tokenByte.length);
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
    }

    private void sendRequestConnect() {
        String requst = gson.toJson(connectToken);
        try {
            byte[] requestByte = requst.getBytes(Constans.CODEC);
            sendHeatRequestFrame((byte) 3, requestByte, requestByte.length);
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
    }

    private boolean sendDisconnect() {
        byte dataByte[] = new byte[1];
        return sendFrame((byte) 7, dataByte, dataByte.length);
    }

    private void initSocketServer() {
        if (PhoneSocketManager.getInstance().checkCont(deviceinfo)) {
            Log.v("发现重复的 服务器连接，手机端主动关闭连接");
            isExit = true;   //如果管理器已经存在一个服务端连接  退出当前连接
            isClear = false;
            if (callBack != null) callBack.connectStatus(2, this);
            return;
        }
        receive = new byte[Constans.DATA_MAX_LENGTH];
        socket = new Socket();
        try {
            socket.setSoTimeout((int) Constans.outTime);
        } catch (SocketException e) {
            e.printStackTrace();
            isExit = true;
        }
    }

    private void clearSocketServer() {
        Log.v("手机端关闭socket连接");
        if (!isClear) {
            return;
        }
        try {
            if (input != null) input.close();
            if (output != null) output.close();
            if (socket != null) socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            input = null;
            output = null;
            socket = null;
            isConnect = false;
            isExit = true;
            sendRunnable.clear();
            dealRunnable.clear();
            PhoneSocketManager.getInstance().removeSocket(deviceinfo);
            if (callBack != null) callBack.disconnect(deviceinfo);
        }
    }

    private boolean sendHeatRequestFrame(byte type, byte[] load, int LoadLength) {
        if (type == 5 || type == 3) {
            if (isResetToken) {   /*如果已经发送过一次重置指令，则不再发送，因为两次重复发送会导致服务端无法正常识别指令*/
                synchronized (messageQueue) {
                    messageQueue.addFirst(new MessageEntity(type, load, LoadLength));
                    return true;
                }
            } else {
                isResetToken = true;
                synchronized (messageQueue) {
                    return send(type, load, LoadLength);
                }
            }
        } else {
            return false;
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
                        if (messageEntity.getType() == 5 || messageEntity.getType() == 3) {
                            sendHeatRequestFrame(messageEntity.getType(), messageEntity.getLoad(), messageEntity.getLength());
                        } else {
                            cacheLength -= messageEntity.getLength();
                            send(messageEntity.getType(), messageEntity.getLoad(), messageEntity.getLength());
                        }
                    } catch (Exception e) {
                    }
                }
            }
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
                        if (callBack != null) callBack.ReceiveData(bytes, PhoneSocket.this);
                    } else {
                        Lock();
                    }
                } catch (Exception e) {
//                    cacheByte = null;
                    Log.e("处理数据出错");
                    Log.e(e);
                }
            }
        }
    }

    private class HeatRunnable extends HealtRunnable {
        @Override
        public void run() {
            while (!isExit) {
                ThreadUtils.sleep(Constans.heatTime);
                if (isConnect) {
                    sendHeatFrame();
                } else {
                    sendRequestConnect();
                }
//                heatCount++;
                if (isOutTime()) {
                    Log.v(LOG_TAG, "连接超时");
                    isExit = true;
                    clearSocketServer();
                }
            }
        }
    }

    public interface PhoneSocketCallBack {
        /*0 连接成功 1 密码错误 2 重复连接*/
        void connectStatus(int status, PhoneSocket socket);

        void ReceiveData(byte[] data, PhoneSocket socket);

        void disconnect(DeviceInfo deviceInfo);
    }
}
