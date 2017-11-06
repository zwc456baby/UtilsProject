package com.example.zhouwc.utils;

import android.content.Context;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AlphaAnimation;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

/**
 * Created by zhouwenchao on 2017-08-01.
 */
public class LogView extends ListView {
    private static final int LIST_MAX_LENGHT = 4000;       //控件最多显示条数  4000条以30毫秒速度需要两分钟才会填满
    private static final int CLEAR_LENGHT = 1000;     //当超出显示条数时，清空多少条
    private static final int STR_MAX_LENGHT = 2048; //一个string 最长的允许的字节

    private static boolean isDebug = false;

    private final int TEXT_VIEW_MIN_HEIGHT = 25;      // 单条信息最小高度
    private final int TEXT_VIEW_TEXT_SIZE = -1;  //textView 文字大小  // -1 即为系统默认
    private final boolean ViewEnable = true; //允许拖动listview显示内容

//    private final Object objLock = new Object();     //由于  有清除和添加访问操作，为防止多线程访问造成list异常，使用同步锁


    private static final LinkedList<String> list = new LinkedList<>();
    private static BaseAdapter arrayAdapter;

    public LogView(Context context) {
        super(context);
        InitView(context);
    }

    public LogView(Context context, AttributeSet attrs) {
        super(context, attrs);
        InitView(context);
    }

    public LogView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        InitView(context);
    }

    // AbsListView.TRANSCRIPT_MODE_DISABLED    // 禁用
    //AbsListView.TRANSCRIPT_MODE_NORMAL   // 正常状态
    //AbsListView.TRANSCRIPT_MODE_ALWAYS_SCROLL   // 总是滚动到最新一条
    private void InitView(Context context) {
//        arrayAdapter = new ArrayAdapter<String>(context, android.R.layout.simple_list_item_1, list);
        arrayAdapter = new Adapter(context, list);
        this.setAdapter(arrayAdapter);
        this.setTranscriptMode(ListView.TRANSCRIPT_MODE_NORMAL);
        this.setVerticalScrollBarEnabled(true);  //显示滑动条
        this.setEnabled(ViewEnable);

//        ndroid:scrollbarFadeDuration="0"
//        SCROLLBAR_POSITION_DEFAULT
//        this.setScrollBarFadeDuration(0);
    }

    public static void addInfo(final String str) {
        if (!isDebug) {
            return;
        }
        ThreadUtils.post(new Runnable() {
            @Override
            public void run() {
                synchronized (list) {
                    if (str == null) {
                        return;
                    }
                    if (str.length() > STR_MAX_LENGHT) {
                        for (String subMsg : largeStringToList(str)) {
                            list.addLast(subMsg);  //添加一条到最前面
                        }
                    } else {
                        list.addLast(str);  //添加一条到最前面
                    }
                    if (list.size() > LIST_MAX_LENGHT) {
                        for (int index = 0; index <= CLEAR_LENGHT; index++) { //循环删除指定条数
                            list.removeFirst(); //删除最后一条
                        }
                    }
                }
                notifyAdapter();
            }
        });
    }

    public static void clearInfo() {
        ThreadUtils.post(new Runnable() {
            @Override
            public void run() {
                synchronized (list) {
                    list.clear();
                }
                notifyAdapter();
            }
        });
    }

    public static void setDebug(boolean debug) {
        isDebug = debug;
        if (!isDebug) {
            clearInfo();
        }
    }

    private static void notifyAdapter() {
        if (arrayAdapter != null) arrayAdapter.notifyDataSetChanged();
    }


    /**
     * 长字符串转化为List
     *
     * @param msg
     * @return
     */
    private static List<String> largeStringToList(String msg) {
        List<String> stringList = new ArrayList<String>();
        int index = 0;
        int countOfSub = msg.length() / STR_MAX_LENGHT;
        if (countOfSub > 0) {
            for (int i = 0; i < countOfSub; i++) {
                String sub = msg.substring(index, index + STR_MAX_LENGHT);
                stringList.add(sub);
                index += STR_MAX_LENGHT;
            }
            stringList.add(msg.substring(index, msg.length()));
        } else {
            stringList.add(msg);
        }
        return stringList;
    }

    private class Adapter extends BaseAdapter {
        private LinkedList linkedList;
        private Context context;
        //        private ScaleAnimation sAnima = new ScaleAnimation(0, 5, 0, 5);//横向放大5倍，纵向放大5
        private AlphaAnimation aAnima = new AlphaAnimation(0.0f, 1.0f);//从不透明到透明

        private Adapter(Context contextTmp, LinkedList list) {
            this.context = contextTmp;
            this.linkedList = list;
            aAnima.setDuration(300);
        }

        @Override
        public boolean isEnabled(int position) {
            return false;
        }

        @Override
        public int getCount() {
            return linkedList.size();
        }

        @Override
        public Object getItem(int position) {
            return linkedList.get(position);
        }

        @Override
        public long getItemId(int position) {
            return linkedList.get(position).hashCode();
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
//                convertView = new TextView(this.context);
                TextView textView = new TextView(this.context);
                textView.setGravity(Gravity.CENTER_VERTICAL);
                textView.setMinHeight(TEXT_VIEW_MIN_HEIGHT);
                if (TEXT_VIEW_TEXT_SIZE > -1) {
                    textView.setTextSize(TEXT_VIEW_TEXT_SIZE);
                }
                convertView = textView;
            }
            if (position == getCount() - 1) {
                convertView.startAnimation(aAnima);
            }
            ((TextView) convertView).setText(getItem(position).toString());
            return convertView;
        }
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        synchronized (list) {
            arrayAdapter = null;
        }
    }
}
