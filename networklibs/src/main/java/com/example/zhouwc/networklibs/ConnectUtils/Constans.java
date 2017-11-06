package com.example.zhouwc.networklibs.ConnectUtils;

/**
 * Created by zhouwenchao on 2017-08-18.
 */
public class Constans {
    public static final int SDAT_SERVER_PORT = 8000; //端口
    public static final int SDAT_PHONE_PORT = 9000; //端口
    public static final String CODEC = "UTF-8";  //编解码格式   byte 编解码
    /* 一个终端的 id 应该是固定的，如果需要更改id，则要 另写代码，且要在启动tcp和udp服务 之前，否则会导致连接失败*/
//    public static final String deviceID = "7895613249874651321";

    public static final String UDP_BROADCAST_ADDR = "255.255.255.255";
    //
    public static final int RECEIVE_TIMEOUT = 300;    /* udp 扫描局域网设备等待响应时间，单位 ms */
    public static final int TRY_AGAIN_NUMBER = 5;    /* udp 扫描重复尝试次数，同时这也是局域网设备列表的上限  */

    public static final long outTime = 6000; /* tcp 连接超时时间 */
    public static final long heatTime = 2000;   /* tcp 连接心跳间隔时间 */

    public static final int LOAD_DATA_MAX_LENGTH = 1048576;/* 帧长度，目前 1MB */
    public static final int DATA_MAX_LENGTH = LOAD_DATA_MAX_LENGTH * 2;

    public final static int IntByteLength = 4;

    /* 接收到一次 消息后，线程等待时间，太长会影响通讯，太短可能导致性能消耗，该值类至于 网络延时，如果不考虑电量，设置为 0*/
//    public static final long OneMessageSleep = 0;

    public static final long RestartTime = 2000;
    /*如果此时一帧长度为1M，则限制为 100条*/
    /*消息缓存列表的最大长度，一条消息最大长度为 8K ，一万条即为80MB，为防止内存溢出，限制消息列表最大长度为一万条*/
    /*限制为 100 MB*/
    public static final int CacheMaxLength = 50 * DATA_MAX_LENGTH;
}
