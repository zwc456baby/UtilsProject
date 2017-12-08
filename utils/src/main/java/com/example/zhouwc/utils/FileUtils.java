package com.example.zhouwc.utils;

import android.content.Context;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.text.TextUtils;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.LinkedList;
import java.util.zip.CRC32;
import java.util.zip.CheckedOutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

/**
 * Created by zhouwenchao on 2017-07-24.
 */
public class FileUtils {
    private final String enter = "\n";
    public final String error0 = "发生了未知异常";
    public final String error1 = "无读写文件权限或文件不存在";
    public final String error2 = "有正在复制的线程，请等待复制完成";
    public final String error3 = "创建文件或文件夹异常";
    public final String error4 = "文件流异常";
    public final String error5 = "关闭文件流异常";
    // 本地根目录
    public final static String rootDir = Environment.getExternalStorageDirectory().getAbsolutePath();


    private static FileUtils instans;

    public static FileUtils instans() {
        if (instans == null) {
            synchronized (FileUtils.class) {
                if (instans == null) {
                    instans = new FileUtils();
                }
            }
        }
        return instans;
    }

    /**
     * 是否有SD卡
     *
     * @return
     */
    public boolean hasSDCard() {
        return Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED);
    }

    /**
     * 判断根目录文件夹是否可读写
     *
     * @return 文件可读可写
     */
    public boolean hasRootDirPermmiss() {
        File rootDirFile = new File(rootDir);
        return rootDirFile.canWrite() && rootDirFile.canRead();
    }

    /**
     * 判断目录文件夹是否可读写
     *
     * @return 文件可读可写
     */
    public boolean hasFilePermmiss(String path) {
        File rootDirFile = new File(path);
        return hasFilePermmiss(rootDirFile);

    }

    /**
     * 判断根目录文件夹是否可读写
     *
     * @return 文件可读可写
     */
    public boolean hasFilePermmiss(File pathFile) {
        if (FileExist(pathFile)) {
            return pathFile.canWrite() && pathFile.canRead();
        } else {
            return hasRootDirPermmiss();
        }
    }


    /**
     * @param path     原文件路径
     * @param toPath   要复制到某个目录，只需要路径 不需要加 “/” 也不需要文件名，会自动生成并加入
     * @param callBack 回调。
     */
    public synchronized void copyFileToPath(String path, String toPath, CopyFileCallBack callBack) {
        copyFileToPath(path, clearStr(toPath, File.separator), 4, callBack);
    }

    /**
     * @param path        原文件路径
     * @param toPath      要复制到某个目录，只需要路径 不需要加 “/” 也不需要文件名，会自动生成并加入
     * @param threadCount 线程数，如果线程数小于1，则会报出未知异常
     * @param callBack    回调。
     */
    public synchronized void copyFileToPath(String path, String toPath, int threadCount, CopyFileCallBack callBack) {
        final File sourceFile = new File(path);
        if (sourceFile.exists() && hasFilePermmiss(sourceFile)) {  // 有权限且文件存在
            ThreadUtils.execute(new CopyRunnable(path, clearStr(toPath, File.separator), threadCount, callBack));
        } else {
            if (callBack != null) {
                callBack.CopyOver(false, error1);
            }
        }
    }

    public synchronized void copyRawFileToPath(Context context, int resourceID, String toPath, CopyFileCallBack callBack) {
        copyRawFileToPath(context, resourceID, new File(toPath), callBack);
    }

    public synchronized void copyRawFileToPath(Context context, int resourceID, File toFile, CopyFileCallBack callBack) {
        if (hasRootDirPermmiss()) {  // 有读写文件夹权限
            ThreadUtils.execute(new CopyResourceRunnable(context, resourceID, toFile, callBack));
        } else {
            if (callBack != null) {
                callBack.CopyOver(false, error1);
            }
        }
    }

    public synchronized void copyAssasFileToPath(Context context, String sourceName, String topath, CopyFileCallBack callBack) {
        copyAssasFileToPath(context, sourceName, new File(topath), callBack);
    }

    public synchronized void copyAssasFileToPath(Context context, String sourceName, File tofile, CopyFileCallBack callBack) {
        if (hasRootDirPermmiss()) {  // 有读写文件夹权限
            ThreadUtils.execute(new CopyResourceRunnable(context, sourceName, tofile, callBack));
        } else {
            if (callBack != null) {
                callBack.CopyOver(false, error1);
            }
        }
    }

    /**
     * 压缩文件,使用迭代的方式,  支持自定义压缩格式
     *
     * @param srcPathName  资源路径
     * @param SavaFilePath 保存路径
     */
    public void createZipFiles(String srcPathName, String SavaFilePath, CreateZipFileCallBack callBack) {
        if (hasFilePermmiss(srcPathName)) {  // 有读写文件夹权限
            ThreadUtils.execute(new CreateZipFileRunnable(clearStr(srcPathName, File.separator), SavaFilePath, callBack));
        } else {
            if (callBack != null) {
                callBack.CreateOver(false, error1);
            }
        }
    }

    /**
     * zip解压缩
     *
     * @param zipfile File 需要解压缩的文件
     * @param descDir String 解压后的目标目录
     */
    public void unZipFiles(File zipfile, String descDir, UnZipFileCallBack callBack) {
        if (hasFilePermmiss(zipfile)) {  // 有读写文件夹权限
            ThreadUtils.execute(new unZipFileRunnable(zipfile, clearStr(descDir, File.separator), callBack));
        } else {
            if (callBack != null) {
                callBack.UnZipFileOver(false, error1);
            }
        }
    }

    class unZipFileRunnable implements Runnable {
        String descDir;
        File zipfile;
        private UnZipFileCallBack callBack;
        private String errorTag = "";

        private boolean isExit = false;
        private long progress = 0;

        private unZipFileRunnable(File zipfile, String descDir, UnZipFileCallBack callBack) {
            this.descDir = descDir;
            this.zipfile = zipfile;
            this.callBack = callBack;
        }

        @Override
        public void run() {
            try {
                isExit = false;
//                String toPath = descDir + File.separator + zipfile.getName();
                if (FileExist(descDir)) {
                    if (isFile(descDir)) {   //如果传入的目标目录不是文件夹,则跳出
                        errorTag = apendError(errorTag, error1);
                        return;
                    }
                } else {
                    if (!mkDir(descDir)) {
                        errorTag = apendError(errorTag, error3);
                        return;
                    }
                }
                if (!zipfile.exists()) {  //如果传入的文件File不存在  跳出
                    errorTag = apendError(errorTag, error1);
                    return;
                }
                ThreadUtils.execute(new notifyRunnable(getFileOrDirLenght(zipfile), callBack));
                ZipFile zf = new ZipFile(zipfile);  //设置解压编码
                for (Enumeration entries = zf.entries(); entries
                        .hasMoreElements(); ) {
                    ZipEntry entry = ((ZipEntry) entries.nextElement());
                    InputStream in = zf.getInputStream(entry);
                    if (entry.isDirectory()) {
                        File fileOut = new File(descDir, entry.getName());
                        if (!fileOut.exists()) {
                            fileOut.mkdirs();   //如果是一个空目录 ,则创建目录
                        }
                    } else {
                        File fileOut = new File(descDir, entry.getName());
                        if (!fileOut.exists()) {
                            (new File(fileOut.getParent())).mkdirs();
                        }
                        FileOutputStream out1 = new FileOutputStream(fileOut);
                        BufferedOutputStream bos = new BufferedOutputStream(
                                out1);
                        int length;
                        byte buffer[] = new byte[8192];
                        while ((length = in.read(buffer)) != -1) {
                            bos.write(buffer, 0, length);
                            progress += length;
                        }
                        //关闭文件流
                        bos.flush();
                        bos.close();
                        out1.close();
                    }
                    in.close(); //关闭文件流
                }
                zf.close();//关闭文件流
            } catch (IOException e) {
                errorTag = apendError(errorTag, error4);
            } catch (Exception e) {
                errorTag = apendError(errorTag, error0);
            } finally {
                isExit = true;
                if (callBack != null) {
                    callBack.UnZipFileOver(TextUtils.isEmpty(errorTag), errorTag);
                }
            }
        }

        class notifyRunnable implements Runnable {
            private UnZipFileCallBack callBack;
            private long aLong;

            private notifyRunnable(long length, UnZipFileCallBack callBack) {
                this.aLong = length;
                this.callBack = callBack;
            }


            @Override
            public void run() {
                while (!isExit) {
                    if (callBack != null) callBack.unzipProgress(progress, aLong);
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }

    class CreateZipFileRunnable implements Runnable {
        private String SavaFilePath;
        private String srcPathName;
        private CreateZipFileCallBack callBack;
        private String errorTag = "";

        private boolean isExit = false;
        private long progress = 0;

        private CreateZipFileRunnable(String srcPathName, String SavaFilePath, CreateZipFileCallBack callBack) {
            this.srcPathName = srcPathName;
            this.SavaFilePath = SavaFilePath;
            this.callBack = callBack;
        }

        @Override
        public void run() {
            isExit = false;
            File zipFile = new File(SavaFilePath);
            try {
                if (!FileExist(srcPathName)) {  //如果资源文件不存在
                    errorTag = apendError(errorTag, error1);
                    return;
                }
                ThreadUtils.execute(new notifyRunnable(getFileOrDirLenght(srcPathName), callBack));
                compress(zipFile, srcPathName);
            } catch (Exception e) {
                errorTag = apendError(errorTag, error1);
//                e.printStackTrace();
            } finally {
                isExit = true;
                if (callBack != null)
                    callBack.CreateOver(TextUtils.isEmpty(errorTag), errorTag);
            }
        }

        private void compress(File zipFile, String srcPathName) {
            File file = new File(srcPathName);
            try {
                FileOutputStream fileOutputStream = new FileOutputStream(zipFile);
                CheckedOutputStream cos = new CheckedOutputStream(fileOutputStream,
                        new CRC32());
                ZipOutputStream out = new ZipOutputStream(cos);
//                out.setEncoding(Encoding);
                String basedir = "";
                if (file.isDirectory()) {    //在进入递归方法前,使用 if 判断 可做到去除外层文件夹
                    File[] files = file.listFiles();
                    for (int i = 0; i < files.length; i++) {
            /* 递归 */
                        compress(files[i], out, basedir);
                    }
                } else {
                    compress(file, out, basedir);
                }
                out.close();
                fileOutputStream.close();
            } catch (IOException e) {
                errorTag = apendError(errorTag, error4);
            }
        }

        /* 判断是目录还是文件 */
        private void compress(File file, ZipOutputStream out, String basedir) {
            if (file.isDirectory()) {
                compressDirectory(file, out, basedir);
            } else {
                compressFile(file, out, basedir);
            }
        }

        /**
         * 压缩一个目录
         */
        private void compressDirectory(File dir, ZipOutputStream out, String basedir) {
            if (!dir.exists())
                return;
            File[] files = dir.listFiles();
            for (int i = 0; i < files.length; i++) {
            /* 递归 */
                compress(files[i], out, basedir + dir.getName() + File.separator);
            }
        }

        /**
         * 压缩一个文件
         */
        private void compressFile(File file, ZipOutputStream out, String basedir) {
            if (!file.exists()) {
                return;
            }
            try {
                BufferedInputStream bis = new BufferedInputStream(
                        new FileInputStream(file));
                ZipEntry entry = new ZipEntry(basedir + file.getName());
                out.putNextEntry(entry);
                int count;
                byte data[] = new byte[8192];
                while ((count = bis.read(data)) != -1) {
                    out.write(data, 0, count);
                    progress += count;
                }
                bis.close();
            } catch (IOException e) {
                errorTag = apendError(errorTag, error4);
            }
        }

        class notifyRunnable implements Runnable {
            private CreateZipFileCallBack callBack;
            private long aLong;

            private notifyRunnable(long length, CreateZipFileCallBack callBack) {
                this.aLong = length;
                this.callBack = callBack;
            }


            @Override
            public void run() {
                while (!isExit) {
                    if (callBack != null) callBack.createProgress(progress, aLong);
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }

    private String clearStr(String str, String clearStr) {
        byte[] bytes = str.getBytes();
        byte[] clearStrBytes = clearStr.getBytes();
        byte[] StrTitleBytes = new byte[clearStrBytes.length];
        //截取指定的 clearStrBytes的长度
        System.arraycopy(bytes, bytes.length - StrTitleBytes.length, StrTitleBytes, 0, StrTitleBytes.length);
        byte[] newByteTmp = null;
        if (Arrays.equals(clearStrBytes, StrTitleBytes)) {  //如果是指定的字符串
            newByteTmp = new byte[bytes.length - clearStrBytes.length];
            System.arraycopy(bytes, 0, newByteTmp, 0, newByteTmp.length);
        }
        if (newByteTmp != null) {
            return new String(newByteTmp);
        } else {
            return str;
        }
    }


    public boolean FileExist(String path) {
        return FileExist(new File(path));
    }

    public boolean FileExist(File path) {
        return path.exists();
    }

    public boolean isDir(String path) {
        return isDir(new File(path));
    }

    public boolean isDir(File path) {
        return path.isDirectory();
    }

    public boolean isFile(String path) {
        return isFile(new File(path));
    }

    public boolean isFile(File path) {
        return path.isFile();
    }

    public boolean renameFile(String path, String name) {
        return renameFile(new File(path), name);
    }

    public boolean renameFile(File file, String name) {
        if (FileExist(file)) {
            String toName = file.getParent() + File.separator + name;
            if (!FileExist(toName)) {
                File toFileName = new File(toName);
                return file.renameTo(toFileName);
            } else {
                return false;
            }
        } else {
            return false;
        }
    }

    public long getFileOrDirLenght(String path) {
        return getFileOrDirLenght(new File(path));
    }

    public long getFileOrDirLenght(File sourceFile) {
        if (!FileExist(sourceFile)) {  //如果文件不存在
            return 0;
        }
        if (sourceFile.isDirectory()) {  // 如果是文件夹，递归
            return getdirLenght(sourceFile, 0);
        } else {
            return sourceFile.length();
        }
    }

    /**
     * 获取文件夹大小
     *
     * @param sourceFile 原始路径
     */
    private long getdirLenght(File sourceFile, long lenght) {
        File[] files = sourceFile.listFiles();
        for (int i = 0; i < files.length; i++) {
            lenght += getFileOrDirLenght(files[i].getAbsolutePath());
        }
        return lenght;
    }

    public ArrayList<String> getAllFiles(String path) {
        return getFiles(path, new ArrayList<String>());
    }

    /**
     * 返回文件夹下文件目录，如果是文件，返回空
     *
     * @param filepath
     * @return
     */
    private ArrayList<String> getFiles(String filepath, @NonNull ArrayList<String> arrayList) {
        if (TextUtils.isEmpty(filepath)) {
            return null;
        }
        File f = new File(filepath);
        if (f.exists() && f.isDirectory()) {  //如果文件路径存在，且是文件夹
            File[] files = f.listFiles();  //获得文件夹下所有文件列表
            if (files != null) {    //如果列表不为空
                for (File file : files) {
                    getFiles(file.getAbsolutePath(), arrayList);
//                    arrayList.add(file.getAbsolutePath());
                }
            }
        } else if (f.exists() && f.isFile()) {
            arrayList.add(filepath);
        }
        return arrayList;
    }

    class CopyResourceRunnable implements Runnable {
        Context context;
        int resourID;
        String sourcename;
        File toFile;
        CopyFileCallBack callBack;
        String errorTag = "";
        int copyMode = -1;
        boolean notify = true;

        long copyFileProgress = 0;

        private CopyResourceRunnable(Context context, int resourceID, File tofile, CopyFileCallBack callBack) {
            this.context = context;
            this.resourID = resourceID;
            this.toFile = tofile;
            this.callBack = callBack;
            copyMode = 1;
        }

        private CopyResourceRunnable(Context context, String sourceName, File tofile, CopyFileCallBack callBack) {
            this.context = context;
            this.sourcename = sourceName;
            this.toFile = tofile;
            this.callBack = callBack;
            copyMode = 2;
        }

        @Override
        public void run() {
//            FileOutputStream lOutputStream = context.openFileOutput(target, 0);
            notify = true;
            copyFileProgress = 0;
            copyImpl();
            System.gc();
            notify = false;
            if (callBack != null) {
                callBack.CopyOver(TextUtils.isEmpty(errorTag), errorTag);
            }
        }

        private void copyImpl() {
            OutputStream outputStream = null;
            InputStream lInputStream = null;
            try {
                if (copyMode == 1 | copyMode == 2) {
                    if (!toFile.exists()) {  //  如果文件不存在
                        try {
                            if (mkDir(toFile.getParent())) {
                                errorTag = apendError(errorTag, error3);
                                return;
                            }
                            toFile.createNewFile();
                        } catch (RuntimeException | IOException e) {
                            errorTag = apendError(errorTag, error3);
                        }
                    } else {
                        deleteFileSafely(toFile);
                        copyImpl();
                        return;
                    }
                    outputStream = new FileOutputStream(toFile);
                    if (copyMode == 1) {
                        lInputStream = context.getResources().openRawResource(resourID);
                    } else if (copyMode == 2) {
                        lInputStream = context.getAssets().open(sourcename);
                    }
                    ThreadUtils.execute(new notifyProgressRunnable(0, callBack)); //启用通知线程
                    int readByte;
                    byte[] buff = new byte[8048];
                    while ((readByte = lInputStream.read(buff)) > 0) {
                        outputStream.write(buff, 0, readByte);
                        copyFileProgress += readByte;
                    }
                } else {
                    errorTag = apendError(errorTag, error0);
                }
            } catch (IOException e) {
                errorTag = apendError(errorTag, error4);
            } catch (Exception e) {
                errorTag = apendError(errorTag, error0);
            } finally {
                try {
                    if (outputStream != null) {
                        outputStream.flush();
                        outputStream.close();
                    }
                    if (lInputStream != null)
                        lInputStream.close();
                } catch (IOException e) {
                    errorTag = apendError(errorTag, error5);
                }
            }
        }

        class notifyProgressRunnable implements Runnable {
            long fileLenght = 0;
            CopyFileCallBack copyFileCallBack;

            private notifyProgressRunnable(long fileLenght, CopyFileCallBack callBack) {
                this.fileLenght = fileLenght;
                this.copyFileCallBack = callBack;
            }

            @Override
            public void run() {
                while (notify) {
                    if (copyFileCallBack != null) {
                        copyFileCallBack.CopyProgress(copyFileProgress, fileLenght); //因为是复制资源文件，无法读取到文件总长度，故为0
                    }
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }


    class CopyRunnable implements Runnable {
        private final Object threadLock = new Object();
        String path;
        String toPath;
        int threadCount;
        CopyFileCallBack callBack;
        private int currentThreadCount = 0; //使用 int 来计数，线程数
        //        private final List<CopyImpl> threads = Collections.synchronizedList(new ArrayList<CopyImpl>());
        private String errorTag = "";  // 默认没有异常
        long copyFileProgress = 0;
        boolean notify = false;

        private CopyRunnable(String pathTmp, String toPathTmp, int threadCount, CopyFileCallBack callBackTmp) {
            this.path = pathTmp;
            this.toPath = toPathTmp;
            this.threadCount = threadCount;
            this.callBack = callBackTmp;
        }

        @Override
        public void run() {
            try {
                long fileLenght = getFileOrDirLenght(path);
                copyFileProgress = 0;
                notify = true;
                if (callBack != null) {
                    callBack.startCopy(fileLenght);
                }
                ThreadUtils.execute(new notifyProgressRunnable(fileLenght, callBack));
                File sourceFile = new File(path);
                if (sourceFile.isDirectory()) {  // 如果是文件夹，迭代
                    File[] files = sourceFile.listFiles();
                    for (int i = 0; i < files.length; i++) {
                        copyDir(files[i].getAbsolutePath(), toPath + File.separator, fileLenght);  // 第一次不用加Name
                    }
                } else {
                    copyFile(path, toPath + File.separator); // 如果是文件的话，直接传入路径复制文件
                }
            } catch (Exception e) {
                errorTag = apendError(errorTag, error0);
            } finally {
                System.gc();  //手动调用一次，因为有可能读写文件后，因为占用文件导致文件无法访问。
                notify = false;
                if (callBack != null) {
                    callBack.CopyOver(TextUtils.isEmpty(errorTag), errorTag);
                }
                errorTag = "";
            }
        }


        /**
         * 复制文件夹 迭代
         *
         * @param path   原始路径
         * @param toPath 复制到哪里。。
         */
        private void copyDir(String path, String toPath, long filelenght) {
            mkDir(toPath);
            File sourceFile = new File(path);
            if (sourceFile.isDirectory()) {  // 如果是文件夹，迭代
                File[] files = sourceFile.listFiles();
                for (int i = 0; i < files.length; i++) {
                    copyDir(files[i].getAbsolutePath(), toPath + sourceFile.getName() + File.separator, filelenght);
                }
            } else {
                copyFile(path, toPath); // 如果是文件的话，直接传入路径复制文件
            }
        }

        private void copyFile(String path, String toPath) {
            File file = new File(path);
            File toFile = new File(toPath + file.getName());
            if (hasFilePermmiss(file) && file.exists()) {  // 有权限且文件存在
                if (!toFile.exists()) {  //  如果文件不存在
                    try {
                        if (mkDir(toFile.getParent())) {
                            errorTag = apendError(errorTag, error3);
                            return;
                        }
                        toFile.createNewFile();
                    } catch (RuntimeException | IOException e) {
                        errorTag = apendError(errorTag, error3);
                    }
                } else {
                    deleteFileSafely(toFile);
                    copyFile(path, toPath);
                    return;
                }
                long len = file.length();
                long oneNum = len / threadCount;//每个线程负责的文件长度，强制转换成int类型
                long currentCopyLenght = 0; //当前已分配的复制长度
                for (int i = 0; i < threadCount; i++) {
                    newCopyThread(path, toPath + file.getName(), currentCopyLenght, i == (threadCount - 1) ? len : (oneNum + currentCopyLenght), threadCount);
//                    newCopyThread(path, toPath + file.getName(), currentCopyLenght, oneNum + currentCopyLenght, threadCount);
                    currentCopyLenght += oneNum;
                }
                //文件长度不能整除的部分放到最后一段处理
//                newCopyThread(path, toPath + file.getName(), currentCopyLenght, len, threadCount);
                JudgeCopyFileOver();
            } else {
                errorTag = apendError(errorTag, error1);
            }
        }

        class notifyProgressRunnable implements Runnable {
            long fileLenght = 0;
            CopyFileCallBack copyFileCallBack;

            private notifyProgressRunnable(long fileLenght, CopyFileCallBack callBack) {
                this.fileLenght = fileLenght;
                this.copyFileCallBack = callBack;
            }

            @Override
            public void run() {
                while (notify) {
                    if (copyFileCallBack != null) {
                        copyFileCallBack.CopyProgress(copyFileProgress, fileLenght);
                    }
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        }


        private void JudgeCopyFileOver() {
            synchronized (threadLock) {
                while (true) {
                    if (currentThreadCount == 0)  //只有当复制线程的数量为0 时，代表复制完成，退出等待
                        break;
//                    if (threads.size() == 0)  //只有当复制线程的数量为0 时，代表复制完成，退出等待
//                        break;
                    try {
                        threadLock.wait();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        }

        /**
         * 判断是否已经达到了最大线程
         *
         * @param count
         */
        private void JudgeThreadCount(int count) {
            synchronized (threadLock) {
                //oneNum * i 起始位置， oneNum * (i + 1)要复制数据的长度
                while (true) {
                    if (currentThreadCount < count)  //只有当小于当前线程时，才跳出循环
                        break;
//                    if (threads.size() < count)  //只有当小于当前线程时，才跳出循环
//                        break;
                    try {
                        threadLock.wait();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        }

        long lenght = 0;

        private void newCopyThread(String path, String toPath, long start, long end, int count) {
            synchronized (threadLock) {
                JudgeThreadCount(count);
                CopyImpl copy = new CopyImpl(path, toPath, start, end);
                currentThreadCount++;
//            threads.add(copy); //
                ThreadUtils.execute(copy);  //
            }
        }

        class CopyImpl implements Runnable {
            private String path;
            private String toPath;
            private long startIndex;
            private long endIndex;

            private CopyImpl(String path, String toPath, long start, long end) {
                this.path = path;
                this.toPath = toPath;
                this.startIndex = start;
                this.endIndex = end;
            }

            @Override
            public void run() {
                RandomAccessFile in = null;
                RandomAccessFile out = null;
//                FileLock lock = null;
                try {
                    //创建一个只读的随机访问文件
                    in = new RandomAccessFile(path, "r");
                    //创建一个可读可写的随机访问文件
                    out = new RandomAccessFile(toPath, "rw");
                    in.seek(startIndex);// 将输入跳转到指定位置
                    out.seek(startIndex);// 从指定位置开始写
//                    FileChannel inChannel = in.getChannel(); //文件输入通道
//                    FileChannel outChannel = out.getChannel();//文件输出通道
                    byte buff[] = new byte[8196];
                    int readindex;
                    long copyindex = endIndex - startIndex;
                    if (copyindex < buff.length) {
                        buff = new byte[(int) copyindex];
                    }
                    boolean breakwhile = false;
                    while ((readindex = in.read(buff)) > 0) {
                        out.write(buff, 0, readindex);
                        copyFileProgress += readindex;
                        copyindex -= readindex;
                        if (breakwhile) {
                            break;
                        }
                        if (copyindex < buff.length) {
                            buff = new byte[(int) copyindex];
                            breakwhile = true;
                        }
                    }
//                    //锁住需要操作的区域,false代表锁住
//                    lock = outChannel.lock(startIndex, (endIndex - startIndex), false);
//                    //将字节从此通道的文件传输到给定的可写入字节的outChannel通道。
//                    inChannel.transferTo(startIndex, (endIndex - startIndex), outChannel);
                } catch (Exception e) {
                    errorTag = apendError(errorTag, error4);
//                e.printStackTrace();
                } finally {
                    try {
//                        if (lock != null)
//                            lock.release();//释放锁
                        if (out != null)
                            out.close();//从里到外关闭文件
                        if (in != null)
                            in.close();//关闭文件
                    } catch (Exception e) {
                        errorTag = apendError(errorTag, error5);
                    } finally {
                        synchronized (threadLock) {
                            currentThreadCount--;
//                            threads.remove(this);
                            try {
                                threadLock.notifyAll();
                            } catch (Exception e) {
                            }
                        }
                    }
                }
            }
        }
    }

    /**
     * 将异常信息进行拼接
     *
     * @param error
     */
    private String apendError(String errorTag, String error) {
        if (TextUtils.isEmpty(errorTag)) {
            errorTag += error;
            return errorTag;
        } else {
            errorTag += enter;
            errorTag += error;
            return errorTag;
        }
    }

    private void writeStream() {

    }

    public boolean mkDir(String path) {
        File dir = new File(path);
        return dir.mkdirs();
    }

    public boolean deleteDirFileSafely(File path) {
        if (path != null && path.exists()) {
            if (path.isDirectory()) {
                File[] files = path.listFiles();
                for (int i = 0; i < files.length; i++) {
                    deleteDirFileSafely(files[i]);
                }
            }
            return deleteFileSafely(path);
        } else {
            return false;
        }
    }

    public boolean deleteTimeOldFile(String path, long time) {
        File dirOrFile = new File(path);
        if (dirOrFile.exists() && dirOrFile.isDirectory()) {
            File[] files = dirOrFile.listFiles();
            if (files == null || files.length == 0) {
                return false;
            }
            for (File file : files) {
                if (file.isDirectory()) {
                    deleteTimeOldFile(file.getAbsolutePath(), time);
//                    FileUtils.instans().deleteDirFileSafely(file);
                } else if (file.isFile() && file.lastModified() < System.currentTimeMillis() - time) {
                    FileUtils.instans().deleteDirFileSafely(file);
                }
            }
            return true;
        } else if (dirOrFile.exists() && dirOrFile.isFile()) {
            if (dirOrFile.lastModified() < System.currentTimeMillis() - time) {
                return FileUtils.instans().deleteDirFileSafely(dirOrFile);
            } else {
                return false;
            }
        } else {
            return false;
        }
    }

    public boolean deleteSizeOldFile(String path, long allFileSize) {
        long currentSize = getFileOrDirLenght(path);
        ArrayList<String> arrayList = getAllFiles(path);
        if (currentSize <= allFileSize) return false;
        if (arrayList == null || arrayList.size() == 0) return false;

        while (currentSize > allFileSize) {
            if (arrayList.size() <= 0) break;
            String oldFile = getListTimeOldPath(arrayList);
            arrayList.remove(oldFile);
            File file = new File(oldFile);
            if (file.exists()) {
                long length = file.length();
                deleteDirFileSafely(file);
                currentSize -= length;
            }
        }
        return true;
    }

    public boolean deleteCountOutFile(String path, int count) {
        ArrayList<String> arrayList = getAllFiles(path);
        if (arrayList == null || arrayList.size() <= count) return false;

        while (arrayList.size() > count) {
            if (arrayList.size() <= 0) break;
            String oldFile = getListTimeOldPath(arrayList);
            arrayList.remove(oldFile);
            File file = new File(oldFile);
            if (file.exists()) {
                deleteDirFileSafely(file);
            }
        }
        return true;
    }

    /**
     * 获取整个list中，时间最早的文件路径
     *
     * @return
     */
    public String getListTimeOldPath(ArrayList<String> arrayList) {
        long time = -1;
        String path = null;
        for (String str : arrayList) {
            File file = new File(str);
            if (file.isFile()) {
                if (time == -1) {
                    time = file.lastModified();
                    path = file.getAbsolutePath();
                } else if (file.lastModified() < time) {
                    time = file.lastModified();
                    path = file.getAbsolutePath();
                }
            }
        }
        return path;
    }


    /**
     * @param file 要删除的文件
     */
    private boolean deleteFileSafely(File file) {
        if (file != null && file.exists()) {
            File tmp = getTmpFile(file, System.currentTimeMillis(), -1);
            if (file.renameTo(tmp)) {    // 将源文件重命名
                return tmp.delete();  //  删除重命名后的文件
            } else {
                return file.delete();
            }
        }
        return false;
    }


    private File getTmpFile(File file, long time, int index) {
        File tmp;
        if (index == -1) {
            tmp = new File(file.getParent() + File.separator + time);
        } else {
            tmp = new File(file.getParent() + File.separator + time + "(" + index + ")");
        }
        if (!tmp.exists() || index >= 1000) {
            return tmp;
        } else {
            return getTmpFile(file, time, index >= 1000 ? index : ++index);
        }
    }


    public interface CopyFileCallBack {
        void startCopy(long filelenght);

        void CopyProgress(long progress, long fileLenght);

        void CopyOver(boolean over, String error);
    }

    public interface CreateZipFileCallBack {
        void createProgress(long progress, long fileLenght);

        void CreateOver(boolean over, String error);
    }

    public interface UnZipFileCallBack {
        void unzipProgress(long progress, long fileLenght);

        void UnZipFileOver(boolean over, String error);
    }
}
