package com.example.zhouwc.networklibs.ConnectUtils;


/**
 * Created by zhouwenchao on 2017-08-18.
 */
public abstract class RunnableBase implements Runnable {

    protected boolean isExit = false;
    protected boolean isConnect = false;
    protected boolean isClear = true;  //断开连接后是否要清空线程信息

}
