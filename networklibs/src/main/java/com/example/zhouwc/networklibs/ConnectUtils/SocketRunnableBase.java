package com.example.zhouwc.networklibs.ConnectUtils;

import com.example.zhouwc.utils.ByteUtils;
import com.example.zhouwc.utils.Log;
import com.google.gson.Gson;

import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.Socket;
import java.util.LinkedList;

/**
 * Created by zhouwenchao on 2017-10-20.
 */
public abstract class SocketRunnableBase extends RunnableBase {

    protected final LinkedList<MessageEntity> messageQueue = new LinkedList<>();

    protected final Frame frame = new Frame();

    protected byte[] receive;
    protected boolean isResetToken = false;

    protected long heatTime = System.currentTimeMillis();


    protected Socket socket;
    protected DeviceInfo deviceinfo;

    protected ConnectToken connectToken;

    protected InputStream input;
    protected OutputStream output;

    protected final Gson gson = new Gson();

    protected DealRunnable dealRunnable;

    protected SendRunnable sendRunnable;

    protected HealtRunnable healtRunnable;

    protected long cacheLength = 0;

    protected byte[] cacheByte;

    public boolean isConnect() {
        return isConnect;
    }

//    protected abstract boolean sendCacheMessage();

    protected abstract void dealFrame(Frame frame);

    /**
     * 对要发送的帧数据进行处理
     *
     * @param type   类型
     * @param load   数据
     * @param length 数据长度
     * @return 包装处理后的数据
     */
    protected byte[] getSendByteData(byte type, byte[] load, int length) {
        byte[] types = new byte[1];
        byte[] tokenInfoByte = new byte[0];   //将用户名转换成byte
        byte[] tokenInfoLengthByte;
        byte[] loadLengthByte;
        if (length > Constans.LOAD_DATA_MAX_LENGTH) {
            Log.e("一次发送数据长度大于指定长度,请修改到指定长度：" + Constans.LOAD_DATA_MAX_LENGTH + " current length:" + length);
            return null;  /*一次发送的长度不能大于指定长度*/
        }
        if (load.length != length) {
            Log.e("数据实际长度不等于声明长度，指定长度：" + load.length + " 声明长度：" + length);
            return null;
        }
        String tokenInfo = gson.toJson(connectToken);
        try {
            tokenInfoByte = tokenInfo.getBytes(Constans.CODEC);
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        tokenInfoLengthByte = ByteUtils.getBytes(tokenInfoByte.length);  /*将长度信息转换成 byte*/
        loadLengthByte = ByteUtils.getBytes(length);
        types[0] = type;
        byte[] originalByte = new byte[tokenInfoByte.length + length + 9];
        int index = 0;
        System.arraycopy(types, 0, originalByte, index, types.length);
        index += types.length;
        System.arraycopy(tokenInfoLengthByte, 0, originalByte, index, tokenInfoLengthByte.length);
        index += tokenInfoLengthByte.length;
        System.arraycopy(loadLengthByte, 0, originalByte, index, loadLengthByte.length);
        index += loadLengthByte.length;
        System.arraycopy(tokenInfoByte, 0, originalByte, index, tokenInfoByte.length);
        index += tokenInfoByte.length;
        System.arraycopy(load, 0, originalByte, index, load.length);
//        index += load.length;
        byte[] encryptByte = ByteUtils.encrypt(originalByte);
        byte[] encryptByteLength = ByteUtils.getBytes(encryptByte.length);
        byte[] sendByte = new byte[encryptByte.length + encryptByteLength.length];
        System.arraycopy(encryptByteLength, 0, sendByte, 0, encryptByteLength.length);
        System.arraycopy(encryptByte, 0, sendByte, encryptByteLength.length, encryptByte.length);
//        System.arraycopy(encryptByte, 0, sendByte, 0, encryptByte.length);
        if (sendByte.length > Constans.DATA_MAX_LENGTH) {
            Log.e("经过处理后的数据大于最大长度：" + Constans.DATA_MAX_LENGTH + " current length:" + sendByte.length);
            return null;
        }
        return sendByte;
    }

    protected byte[] ReadBytes(int start, byte[] bytes, int length) {
//        length < Constans.IntByteLength ||
        if (length == start || start > length) {
            return null;
        }
        if (length - start < Constans.IntByteLength) {
            byte[] nextByteCache = new byte[length - start];
            System.arraycopy(bytes, start, nextByteCache, 0, nextByteCache.length);
            return nextByteCache;
        }
        byte[] frameLengthByte = new byte[Constans.IntByteLength];
        System.arraycopy(bytes, start, frameLengthByte, 0, frameLengthByte.length);
        int frameLength = ByteUtils.getInt(frameLengthByte); //TODO 需要将byte转换成int
        if (frameLength == 0 || frameLength > (Constans.DATA_MAX_LENGTH - Constans.IntByteLength)) {  //如果读取的数据长度是0，其实是有问题的，无论何种情况，帧肯定不为0
            return null;
        }
        if ((length - start - Constans.IntByteLength) < frameLength) {
            byte[] nextByteCache = new byte[length - start];
            System.arraycopy(bytes, start, nextByteCache, 0, nextByteCache.length);
            return nextByteCache;
        } else {
            byte[] data = new byte[frameLength];
            System.arraycopy(bytes, start + Constans.IntByteLength, data, 0, data.length);
            if (frame.setFrame(data)) {
                dealFrame(frame);
//                ThreadUtils.sleep(Constans.OneMessageSleep);
            } else {
                Log.v("解析一包数据失败");
                return null;
            }
        }
        return ReadBytes(start + Constans.IntByteLength + frameLength, bytes, length);
    }

    protected final boolean isOutTime() {
        return System.currentTimeMillis() > (heatTime + Constans.outTime);
    }

    protected synchronized boolean sendFrame(byte type, byte[] load, int LoadLength) {
        if (isExit) {
            return false;
        }
        if ((LoadLength + cacheLength) > Constans.CacheMaxLength) {
            Log.e("当前消息发送缓存列表条目超出 " + Constans.CacheMaxLength + " 请等待消息发送完成再继续添加，剩余条目：" + messageQueue.size());
            return false;
        }
//        if (isResetToken) {
//            messageQueue.addLast(new MessageEntity(type, load, LoadLength));
//            return true;
//        }
        messageQueue.addLast(new MessageEntity(type, load, LoadLength));
        cacheLength += LoadLength;
        return sendCacheMessage();
    }

    protected boolean sendCacheMessage() {
        sendRunnable.UnLock();
        return true;
    }

    protected boolean send(byte type, byte[] load, int length) {
        synchronized (messageQueue) {
            if (isExit) {
                return false;
            }
            try {
                byte data[] = getSendByteData(type, load, length);
                if (data == null) {
                    return false;
                }
                output.write(data, 0, data.length);
                output.flush();
                return true;
//            clearPingCnt();
            } catch (Exception e) {
                e.printStackTrace();
                return false;
            }
        }
    }

    protected abstract class DealRunnable implements Runnable {
        private final Object lock = new Object();
        protected final LinkedList<byte[]> linkedList = new LinkedList();

        protected long dealCacheLength = 0;


        protected void Lock() {
            synchronized (lock) {
                try {
                    if (linkedList.size() == 0) {
                        lock.wait();
                    }
                } catch (Exception e) {
                }
            }
        }

        private void UnLock() {
            synchronized (lock) {
                try {
                    lock.notifyAll();
                } catch (Exception e) {
                }
            }
        }

        public final void addMassage(byte[] bytes) {
            if ((bytes.length + dealCacheLength) > Constans.CacheMaxLength) {
                Log.e("当前接收消息缓存列表条目超出 " + Constans.CacheMaxLength + " 请尽快处理消息接收队列，剩余条目：" + linkedList.size());
                UnLock();
                return;
            }
            linkedList.addLast(bytes);
            dealCacheLength += bytes.length;
            UnLock();
        }

        public final void clear() {
            linkedList.clear();
            UnLock();
        }
    }

    protected abstract class SendRunnable implements Runnable {
        private final Object lock = new Object();

        protected void Lock() {
            synchronized (lock) {
                try {
                    lock.wait();
                } catch (Exception e) {
                }
            }
        }

        public void UnLock() {
            synchronized (lock) {
                try {
                    lock.notifyAll();
                } catch (Exception e) {
                }
            }
        }

        public final void clear() {
            messageQueue.clear();
            UnLock();
        }
    }

    protected abstract class HealtRunnable implements Runnable {
    }
}
