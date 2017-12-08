package com.example.zhouwc.utils;

import android.os.Handler;
import android.os.Looper;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Created by zhouwenchao on 2017-04-27.
 * 线程工具，支持post线程到主Handler中
 * 支持 execute 子线程
 * 支持 executeDelayed 子线程延时
 */
public final class ThreadUtils {
    private static final ExecutorService threadCache = Executors.newCachedThreadPool();
    //    private static final ScheduledExecutorService timerTask = Executors.newSingleThreadScheduledExecutor();
    private static final Handler mHandler = new Handler(Looper.getMainLooper());
    private static final List<RunnableIm> RUNNABLE_LIST = new ArrayList<>();
    private static final Object listLock = new Object();

    static {
        execute(new TimerRunnable());
    }

    public static void postDelayed(Runnable r, long time) {
        mHandler.postDelayed(r, time);
    }

    public static void post(Runnable r) {  //主线程
        mHandler.post(r);
    }

    public static void removeCallbacks(Runnable r) {
        mHandler.removeCallbacks(r);
    }

    public static void sleep(long time) {
        try {
            Thread.sleep(time);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public static boolean isMainThread() {
        return Thread.currentThread() == Looper.getMainLooper().getThread();
    }

    private ThreadUtils() {
    }

    public static void execute(Runnable runnable) {
        threadCache.execute(runnable);
    }

    public static void executeDelayed(Runnable runnable, long time) {
        synchronized (listLock) {
            RunnableIm im = new RunnableIm(time, runnable);
            RUNNABLE_LIST.add(im);
            try {
                listLock.notifyAll();
            } catch (Exception ignored) {
            }
        }
    }

    public static void removeExecute(Runnable runnable) {
        synchronized (listLock) {
            for (int index = 0; index < RUNNABLE_LIST.size(); index++) {
                if (RUNNABLE_LIST.get(index).getRunnable().equals(runnable)) {
                    RUNNABLE_LIST.remove(index);
                    index--;  //移除后下标减 1
                }
            }
        }
    }

    /**
     * 使用该线程实现定时器功能
     */
    private static final class TimerRunnable implements Runnable {
        private RunnableIm runnableIm;

        @Override
        public void run() {
            while (true) {
                synchronized (listLock) {
                    if (RUNNABLE_LIST.size() == 0) {
                        ThreadWait();
                        continue;
                    }
                    runnableIm = getListMinTime(System.currentTimeMillis());
                    if (runnableIm == null) {
                        continue;
                    }
                    long sleepTime = runnableIm.getTime() - (System.currentTimeMillis() - runnableIm.getSysTime());
                    if (sleepTime <= 0) {
                        removeRunnableIm(runnableIm);  //如果线程已经到了或超过执行事件，立即从列表移除并添加线程到执行器中
                        execute(runnableIm.getRunnable());
                        runnableIm = null;
                        continue;
                    }
                    ThreadWait(sleepTime);//在线程等待指定时间后，执行 任务
                }
            }
        }

        private void ThreadWait(long time) {
            try {
                listLock.wait(time);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        private void ThreadWait() {
            try {
                listLock.wait();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * 获取线程列表中最小的等待时间
     *
     * @return
     */
    private static RunnableIm getListMinTime(long time) {
        synchronized (listLock) {
            int index = getMinTimeImIndex(time);
            return index == -1 ? null : RUNNABLE_LIST.get(index);
        }
    }

    private static boolean removeRunnableIm(RunnableIm im) {
        synchronized (listLock) {
            return RUNNABLE_LIST.remove(im);
        }
    }

    private static int getMinTimeImIndex(long time) {
        synchronized (listLock) {
            int minIndex = -1;
            for (int index = 0; index < RUNNABLE_LIST.size(); index++) {
                if (minIndex == -1) {  //将下标指向 0
                    minIndex = index;
                } else {
                    //如果遍历的时候找到一个更小的，则重新指向这个最小的
                    RunnableIm imTmp = RUNNABLE_LIST.get(index);
                    RunnableIm imMin = RUNNABLE_LIST.get(minIndex);
                    if ((imTmp.getTime() - (time - imTmp.getSysTime())) < (imMin.getTime() - (time - imMin.getSysTime()))) {
                        minIndex = index;
                    }
                }
            }
            return minIndex;
        }
    }

    private static class RunnableIm {
        long time;
        Runnable runnable;
        long sysTime;

        private RunnableIm(long ti, Runnable run) {
            this.time = ti;
            this.runnable = run;
            sysTime = System.currentTimeMillis();
        }

        public long getTime() {
            return time;
        }

        public long getSysTime() {
            return sysTime;
        }

        public Runnable getRunnable() {
            return runnable;
        }

    }
}
