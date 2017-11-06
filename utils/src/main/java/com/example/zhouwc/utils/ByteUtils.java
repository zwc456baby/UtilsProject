package com.example.zhouwc.utils;

import java.nio.ByteOrder;
import java.security.SecureRandom;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

/**
 * Created by zhouwenchao on 2017-09-28.
 */
public class ByteUtils {
    private static final String AES_KEY = "zhouwenchao-wyPolQfAsJ2DZejVqBnqpOGJ1ijTg7gTs7rs5AcuFcn6zlsfxYNv4nOOiHBl";   /*用于加密的密钥*/

    private ByteUtils() {
    }


    //index从0开始
    //获取取第index位
    public static int GetBit(byte b, int index) {
        return ((b & (1 << index)) > 0) ? 1 : 0;
    }

    //将第index位设为1
    public static byte SetBitOne(byte b, int index) {
        return (byte) (b | (1 << index));
    }

    //将第index位设为0
    public static byte SetBitZero(byte b, int index) {
        return (byte) (b & (Byte.MAX_VALUE - (1 << index)));
    }

    //将第index位取反
    public static byte ReverseBit(byte b, int index) {
        return (byte) (b ^ (byte) (1 << index));
    }

    /**
     * @param original 原始byte数组
     * @param tobyte   目标byte数组
     * @param start    原始byte数组开始位置
     * @param toStart  目标byte数组开始位置 一般为 0
     * @param toend    目标byte 数组复制结束位置 ， 一般为 lenght
     * @return 返回复制后的byte
     */
    public static byte[] ByteArrayCopy(byte[] original, byte tobyte[], int start, int toStart, int toend) {
        System.arraycopy(original, start, tobyte, toStart, toend - toStart);
        return tobyte;
    }

    /**
     * @param original 原始byte数组
     * @param start    原始byte数组开始位置
     * @param toStart  目标byte数组开始位置 一般为 0
     * @param toend    目标byte 数组复制结束位置 ， 一般为 lenght
     * @return 返回复制后的byte
     */
    public static byte[] ByteArrayCopy(byte[] original, int start, int toStart, int toend) {
        byte[] newByte = new byte[toend];
        return ByteArrayCopy(original, newByte, start, toStart, toend);
    }

    public static boolean equelsByte(byte[] byte1, byte[] byte2) {
        if (byte1 == byte2) {
            return true;
        }
        if (byte1.length != byte2.length) {
            return false;
        }
        for (int i = 0; i < byte1.length; i++) {
            if (byte1[i] != byte2[i]) {
                return false;
            }
        }
        return true;
    }


    /***********
     * 对字符串进行AES加解密
     ***************/
    private static final String SHA1PRNG = "SHA1PRNG";
    private static final String AES = "AES";//AES 加密
    private static final String CBC_PKCS5_PADDING = "AES/CBC/PKCS5Padding";//AES是加密方式 CBC是工作模式 PKCS5Padding是填充模式

    // 对密钥进行处理
    private static byte[] getRawKey(byte[] seed) throws Exception {
        KeyGenerator kgen = KeyGenerator.getInstance(AES);
        //for android
        SecureRandom sr;
        // 在4.2以上版本中，SecureRandom获取方式发生了改变
        if (android.os.Build.VERSION.SDK_INT >= 17) {
            sr = SecureRandom.getInstance(SHA1PRNG, "Crypto");
        } else {
            sr = SecureRandom.getInstance(SHA1PRNG);
        }
        sr.setSeed(seed);
        kgen.init(128, sr); //256 bits or 128 bits,192bits
        //AES中128位密钥版本有10个加密循环，192比特密钥版本有12个加密循环，256比特密钥版本则有14个加密循环。
        SecretKey skey = kgen.generateKey();
        return skey.getEncoded();
    }


    /*
 * 加密
 */
    public static byte[] encrypt(byte[] clear) {
        return encrypt(AES_KEY, clear);
    }

    public static byte[] encrypt(String key, byte[] bytes) {
        if (bytes == null || bytes.length == 0) return null;
        try {
            SecretKeySpec skeySpec = new SecretKeySpec(getRawKey(key.getBytes()), AES);
            Cipher cipher = Cipher.getInstance(CBC_PKCS5_PADDING);
            cipher.init(Cipher.ENCRYPT_MODE, skeySpec, new IvParameterSpec(new byte[cipher.getBlockSize()]));
//            return cipher.doFinal(clear);
            return cipher.doFinal(bytes);
        } catch (Exception e) {
            e.printStackTrace();
            return bytes;
        }
    }

    /*
     * 解密
     */
    public static byte[] decrypt(byte[] encrypted) {
        return decrypt(AES_KEY, encrypted);
//        if (encrypted == null || encrypted.length == 0) return null;
//        try {
//            encrypted = Base64.decode(encrypted, Base64.NO_WRAP);
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//        try {
//            Cipher cipher = Cipher.getInstance(CBC_PKCS5_PADDING);
//            cipher.init(Cipher.DECRYPT_MODE, skeySpec, new IvParameterSpec(new byte[cipher.getBlockSize()]));
//            return cipher.doFinal(encrypted);
//        } catch (Exception e) {
//            e.printStackTrace();
//            return encrypted;
//        }
    }

    public static byte[] decrypt(String key, byte[] encrypted) {
        if (encrypted == null || encrypted.length == 0) return null;
        try {
            SecretKeySpec skeySpec = new SecretKeySpec(getRawKey(key.getBytes()), AES);
            Cipher cipher = Cipher.getInstance(CBC_PKCS5_PADDING);
            cipher.init(Cipher.DECRYPT_MODE, skeySpec, new IvParameterSpec(new byte[cipher.getBlockSize()]));
            return cipher.doFinal(encrypted);
        } catch (Exception e) {
            e.printStackTrace();
            return encrypted;
        }
    }

    public static boolean testCPU() {
        if (ByteOrder.nativeOrder() == ByteOrder.BIG_ENDIAN) {
            // System.out.println("is big ending");
            return true;
        } else {
            // System.out.println("is little ending");
            return false;
        }
    }

    public static byte[] getBytes(short s, boolean bBigEnding) {
        byte[] buf = new byte[2];

        if (bBigEnding) {
            for (int i = buf.length - 1; i >= 0; i--) {
                buf[i] = (byte) (s & 0x00ff);
                s >>= 8;
            }
        } else {
            for (int i = 0; i < buf.length; i++) {
                buf[i] = (byte) (s & 0x00ff);
                s >>= 8;
            }
        }

        return buf;
    }

    public static byte[] getBytes(int s, boolean bBigEnding) {
        byte[] buf = new byte[4];

        if (bBigEnding) {
            for (int i = buf.length - 1; i >= 0; i--) {
                buf[i] = (byte) (s & 0x000000ff);
                s >>= 8;
            }
        } else {
            System.out.println("1");
            for (int i = 0; i < buf.length; i++) {
                buf[i] = (byte) (s & 0x000000ff);
                s >>= 8;
            }
        }

        return buf;
    }

    public static byte[] getBytes(long s, boolean bBigEnding) {
        byte[] buf = new byte[8];

        if (bBigEnding) {
            for (int i = buf.length - 1; i >= 0; i--) {
                buf[i] = (byte) (s & 0x00000000000000ff);
                s >>= 8;
            }
        } else {
            for (int i = 0; i < buf.length; i++) {
                buf[i] = (byte) (s & 0x00000000000000ff);
                s >>= 8;
            }
        }

        return buf;
    }

    public static short getShort(byte[] buf, boolean bBigEnding) {
        if (buf == null) {
            throw new IllegalArgumentException("byte array is null!");
        }

        if (buf.length > 2) {
            throw new IllegalArgumentException("byte array size > 2 !");
        }

        short r = 0;
        if (bBigEnding) {
            for (int i = 0; i < buf.length; i++) {
                r <<= 8;
                r |= (buf[i] & 0x00ff);
            }
        } else {
            for (int i = buf.length - 1; i >= 0; i--) {
                r <<= 8;
                r |= (buf[i] & 0x00ff);
            }
        }

        return r;
    }

    public static int getInt(byte[] buf, boolean bBigEnding) {
        if (buf == null) {
            throw new IllegalArgumentException("byte array is null!");
        }

        if (buf.length > 4) {
            throw new IllegalArgumentException("byte array size > 4 !");
        }

        int r = 0;
        if (bBigEnding) {
            for (int i = 0; i < buf.length; i++) {
                r <<= 8;
                r |= (buf[i] & 0x000000ff);
            }
        } else {
            for (int i = buf.length - 1; i >= 0; i--) {
                r <<= 8;
                r |= (buf[i] & 0x000000ff);
            }
        }

        return r;
    }

    public static long getLong(byte[] buf, boolean bBigEnding) {
        if (buf == null) {
            throw new IllegalArgumentException("byte array is null!");
        }

        if (buf.length > 8) {
            throw new IllegalArgumentException("byte array size > 8 !");
        }

        long r = 0;
        if (bBigEnding) {
            for (int i = 0; i < buf.length; i++) {
                r <<= 8;
                r |= (buf[i] & 0x00000000000000ff);
            }
        } else {
            for (int i = buf.length - 1; i >= 0; i--) {
                r <<= 8;
                r |= (buf[i] & 0x00000000000000ff);
            }
        }

        return r;
    }

    /*----------------------------------------------------------*/
     /* 对转换进行一个简单的封装 */
     /*----------------------------------------------------------*/
    public static byte[] getBytes(int i) {
        return getBytes(i, testCPU());
    }

    public static byte[] getBytes(short s) {
        return getBytes(s, testCPU());
    }

    public static byte[] getBytes(long l) {
        return getBytes(l, testCPU());
    }

    public static int getInt(byte[] buf) {
        return getInt(buf, testCPU());
    }

    public static short getShort(byte[] buf) {
        return getShort(buf, testCPU());
    }

    public static long getLong(byte[] buf) {
        return getLong(buf, testCPU());
    }
/*只提供int long short 单个转换，不提供 数组转换*/
//    /****************************************/
//    public short[] Bytes2Shorts(byte[] buf) {
//        byte bLength = 2;
//        short[] s = new short[buf.length / bLength];
//
//        for (int iLoop = 0; iLoop < s.length; iLoop++) {
//            byte[] temp = new byte[bLength];
//
//            for (int jLoop = 0; jLoop < bLength; jLoop++) {
//                temp[jLoop] = buf[iLoop * bLength + jLoop];
//            }
//
//            s[iLoop] = getShort(temp);
//        }
//
//        return s;
//    }
//
//    public byte[] Shorts2Bytes(short[] s) {
//        byte bLength = 2;
//        byte[] buf = new byte[s.length * bLength];
//
//        for (int iLoop = 0; iLoop < s.length; iLoop++) {
//            byte[] temp = getBytes(s[iLoop]);
//
//            for (int jLoop = 0; jLoop < bLength; jLoop++) {
//                buf[iLoop * bLength + jLoop] = temp[jLoop];
//            }
//        }
//
//        return buf;
//    }
//
//    /****************************************/
//    public int[] Bytes2Ints(byte[] buf) {
//        byte bLength = 4;
//        int[] s = new int[buf.length / bLength];
//
//        for (int iLoop = 0; iLoop < s.length; iLoop++) {
//            byte[] temp = new byte[bLength];
//
//            for (int jLoop = 0; jLoop < bLength; jLoop++) {
//                temp[jLoop] = buf[iLoop * bLength + jLoop];
//            }
//
//            s[iLoop] = getInt(temp);
//
//            System.out.println("2out->"+s[iLoop]);
//        }
//
//        return s;
//    }
//
//    public byte[] Ints2Bytes(int[] s) {
//        byte bLength = 4;
//        byte[] buf = new byte[s.length * bLength];
//
//        for (int iLoop = 0; iLoop < s.length; iLoop++) {
//            byte[] temp = getBytes(s[iLoop]);
//
//            System.out.println("1out->"+s[iLoop]);
//
//            for (int jLoop = 0; jLoop < bLength; jLoop++) {
//                buf[iLoop * bLength + jLoop] = temp[jLoop];
//            }
//        }
//
//        return buf;
//    }
//
//    /****************************************/
//    public long[] Bytes2Longs(byte[] buf) {
//        byte bLength = 8;
//        long[] s = new long[buf.length / bLength];
//
//        for (int iLoop = 0; iLoop < s.length; iLoop++) {
//            byte[] temp = new byte[bLength];
//
//            for (int jLoop = 0; jLoop < bLength; jLoop++) {
//                temp[jLoop] = buf[iLoop * bLength + jLoop];
//            }
//
//            s[iLoop] = getLong(temp);
//        }
//
//        return s;
//    }
//
//    public byte[] Longs2Bytes(long[] s) {
//        byte bLength = 8;
//        byte[] buf = new byte[s.length * bLength];
//
//        for (int iLoop = 0; iLoop < s.length; iLoop++) {
//            byte[] temp = getBytes(s[iLoop]);
//
//            for (int jLoop = 0; jLoop < bLength; jLoop++) {
//                buf[iLoop * bLength + jLoop] = temp[jLoop];
//            }
//        }
//
//        return buf;
//    }


}
