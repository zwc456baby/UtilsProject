package com.example.zhouwc.utils;

import android.os.Environment;
import android.text.TextUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.LinkedList;

/**
 * Created by zhouwenchao on 2016-12-01.
 */
public class SearchFile implements Runnable {
    ArrayList<String> supportList;
    String path;
    FileListCall call;
    LinkedList<String> AllFiles;

    public SearchFile(String path, ArrayList<String> supportList, FileListCall f) { //回调
        this.supportList = supportList;
        this.path = path;
        this.call = f;
    }

    @Override
    public void run() {
        AllFiles = new LinkedList<String>();
        getAllFiles(path, AllFiles, supportList);
        call.getFileList(path, AllFiles, true);
    }

    private void getAllFiles(String path, LinkedList<String> allfiles, ArrayList<String> supportList) {
        ArrayList<String> files = getFiles(path);  //得到文件夹下所有文件列表
        if (files != null && files.size() > 0) {  //如果路径下有文件,调用自己继续扫描
            for (String filepath : files) {
                getAllFiles(filepath, allfiles, supportList);
            }
        } else if (new File(path).isFile()) {  //如果路径是一个文件，添加到集合中
            if (supportFile(path, supportList)) {  //如果文件后缀在支持列表中,  则添加到集合中并返回
                allfiles.add(path);
            }
        }
    }

    /**
     * 返回文件夹下文件目录，如果是文件，返回空
     *
     * @param filepath
     * @return
     */
    private ArrayList<String> getFiles(String filepath) {
        if (TextUtils.isEmpty(filepath)) {
            return null;
        }
        File f = new File(filepath);
        ArrayList<String> strings = null;
        if (Environment.getExternalStorageState().equals(
                Environment.MEDIA_MOUNTED)) {
            if (f.exists() && f.isDirectory()) {  //如果文件路径存在，且是文件夹
                File[] files;
                files = f.listFiles();  //获得文件夹下所有文件列表
                if (files != null) {    //如果列表不为空
                    strings = new ArrayList<String>();
                    for (File file : files) {
                        strings.add(file.getAbsolutePath());
                    }
                }
            }
        }
        return strings;
    }

    //判断是否是支持的文件列表
    private boolean supportFile(String fileSufix, ArrayList<String> list) {
        if (list == null) {  //如果 支持文件列表为空,则 默认 全部支持
            return true;
        }
        String str[] = fileSufix.split("\\.");
        if (str.length > 0) {   //如果传入的是文件夹路径，判断路径下文件是否支持
            String st = str[(str.length - 1)];   //获得文件后缀
            for (String filesufix : list) {
                if (filesufix.equals(st)) {
                    return true;
                }
            }
        }
//        else {   //如果是直接传入文件后缀
//            for (String Sufix : list) {
//                if (fileSufix.equals(Sufix)) {
//                    return true;
//                }
//            }
//        }
        return false;
    }

    public interface FileListCall {
         void getFileList(String searchDir, LinkedList<String> filelist, boolean boo);
    }
}
