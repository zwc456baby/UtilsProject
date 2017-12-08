package com.example.zhouwc.utilsproject;

import android.os.Bundle;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;

import com.example.zhouwc.utils.ACache;
import com.example.zhouwc.utils.LayoutUtil;
import com.example.zhouwc.utils.Log;
import com.example.zhouwc.utils.PreUtils;

import java.io.File;


public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Log.getLogConfig().savaLogFile(true);


//        String path = this.getFilesDir().getAbsolutePath() + File.separator + "APreDate";
//        String path = Environment.getExternalStorageDirectory().getAbsolutePath() + File.separator + "tmpfiledir" + File.separator;
//        Log.getLogConfig().setLogFileDir(path);
//        File file = new File(path);
//        Log.e("new File:" + file.getAbsolutePath());
////        ACache.get(file);
////        Log.v("File:" + path);
////        ThreadUtils.sleep(3000);
//        APreData.setDefaultDir(path);
////
//        boolean flage = APreData.putString("test", "第一次写入");
//        Log.v(flage);
//        long time = System.currentTimeMillis();
//        for (int i = 0; i < 220; i++) {
//            APreData.putString("test", "这是测试数据:" + i);
//        }
//        Log.v(System.currentTimeMillis() - time);
////        Log.v("这是测试数据".getBytes() + "  ");
//        String data = APreData.getString("test");
//        Log.v(data);
//        Log.v(data.getBytes());


//        Log.v("test");
//        String hex = "0xFF0000";
//        String intt = "-65536";
//        int value = -65536;
//
//        Log.v("16进制转10进制：");
//        Log.v(ArithUtil.HexToInt(hex));
//        Log.v("10进制转16进制");
//        Log.v(ArithUtil.IntToHex(value));
//
//        int i = 0;
//        long time = System.currentTimeMillis();
//        for (; i < 10; i++) {
//            PreUtils.putString(this, "fhduaofdussasof" + i, "次数：" + i);
//            Log.v(PreUtils.getString(this, "fhduaofdussasof" + i, null));
//        }
//        Log.v(System.currentTimeMillis() - time);
//        time = System.currentTimeMillis();
//        for (i = 0; i < 10; i++) ACache.get(this).put("fhduaofduasof", "次数：" + i);
//
//        Log.v(System.currentTimeMillis() - time);
    }
}
