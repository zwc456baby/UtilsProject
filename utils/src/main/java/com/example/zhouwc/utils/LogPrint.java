package com.example.zhouwc.utils;

import android.support.annotation.NonNull;

import java.io.File;
import java.io.FileOutputStream;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.LinkedList;

/**
 * Created by zhouwenchao on 2017-11-01.
 * 日志保存类
 */
class LogPrint implements Runnable {
    private final LinkedList<LogEntity> logCache = new LinkedList();

    private FileOutputStream outStream = null;
    private final Object obj = new Object();
    private static LogPrint logclass;
    private long currentFileTime = 0;
    private final long newFileTime = 86400000;
    private String dayType = "yyyy年MM月dd日";
    private SimpleDateFormat day = new SimpleDateFormat(dayType);

    private static final String LogDir = FileUtils.rootDir + File.separator + "LogUtilsFile" + File.separator;
    private static final long MaxFileSize = 100 * 1024 * 1024;  /*100MB*/
    private Calendar c = Calendar.getInstance();
    private File currentLogFile;
    //    private String timeTitle;
    private static final String enter = "\n";
    private static final String separator = File.separator;
    private static final String speter = " ";

    private boolean isExit = false;

    private boolean isWait = false;

//    private byte[] errorLog;

    private LogPrint() {
//        InitStream();
        ThreadUtils.execute(this);
    }

    protected static LogPrint getInstance() {
        if (logclass == null) {
            synchronized (LogPrint.class) {
                if (logclass == null) {
                    logclass = new LogPrint();
                }
            }
        }
        return logclass;
    }

    protected static boolean isInstance() {
        return logclass != null;
    }

    protected static void clear() {
        synchronized (LogPrint.class) {
            if (isInstance()) {
                logclass.flushCache();
                logclass.isExit = true;
                logclass.closeOutPutStream(); //如果需要,则关闭当前文件流
                logclass.threadStart();
                logclass = null;
            }
        }
    }

    private void InitStream() {
        if (JudgeNewStream()) {  //循环判断是否需要创建新的文件流
            synchronized (obj) {
                if (JudgeNewStream()) {
                    closeOutPutStream(); //如果需要,则关闭当前文件流
                    outStream = newOutPutStream();  //重新创建文件流
                }
            }
        }
    }

    protected void addOnlineLog(@NonNull int level, @NonNull String tag, @NonNull String msg) {
//        if (createStreamTime < (System.currentTimeMillis() - 1000)) {
//            InitStream();
//        }
        logCache.addLast(new LogEntity(System.currentTimeMillis(), level, tag, msg));
        threadStart();
//        String logs = timeTitle + separator + level + separator + tag + speter + msg + enter;
//        savaLineLog(logs.getBytes());
    }

//    private long createStreamTime = System.currentTimeMillis();

    private void savaLineLog(byte[] logs) {
        try {
            InitStream();
            if (outStream != null) {
                outStream.write(logs);
                outStream.flush();
            }
        } catch (Exception e) {
            System.out.println("保存一条Log日志失败：" + new String(logs));
        }
    }

    private synchronized FileOutputStream newOutPutStream() {
        c.setTimeInMillis(System.currentTimeMillis());
        String createfileTime = day.format(c.getTime());
        currentFileTime = stringToLong(createfileTime);
        String filePath;
//        boolean hasSDCard = Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED);
        FileOutputStream outStreamTmp = null;
        if (FileUtils.instans().hasRootDirPermmiss()) { // SD卡根目录的hello.text
            try {
                deleteFiles(LogDir, 7, 0);
                filePath = LogDir + createfileTime + "Log.txt";
                currentLogFile = new File(filePath);
                if (!currentLogFile.exists()) {
                    File dir = new File(currentLogFile.getParent());
                    createDir(dir);
                    currentLogFile.createNewFile();
                }
                outStreamTmp = new FileOutputStream(currentLogFile, true);  //追加模式
            } catch (Exception ignored) {
            }
            return outStreamTmp;
        } else {
            return null;
        }
    }

    private boolean JudgeNewStream() {
        return currentFileTime < (System.currentTimeMillis() - newFileTime) || outStream == null || JudgeLogFileState();
    }

    private boolean JudgeLogFileState() {
        return currentLogFile == null || !currentLogFile.exists() || currentLogFile != null && currentLogFile.length() > MaxFileSize;
    }

    private void closeOutPutStream() {
        synchronized (obj) {
            if (outStream != null) {
                try {
                    outStream.flush();
                    outStream.close();
                } catch (Exception ignored) {
                } finally {
                    outStream = null;
                }
            }
        }
    }

    private void deleteFiles(String path, int day, long allFileSize) {
        File dir = new File(path);
        if (dir.exists() && dir.isDirectory()) {
            File[] files = dir.listFiles();
            if (files == null || files.length == 0) {
                return;
            }
            for (File file : files) {
                //                if (path.equals(LogDir)) {
//                    if (file.getName().equals("true") && file.isFile() || file.getName().equals("false") && file.isFile()) {
//                        continue;
//                    }
//                }
                if (file.isDirectory()) {
                    deleteFiles(file.getAbsolutePath(), day, allFileSize);
                    FileUtils.instans().deleteDirFileSafely(file);

                } else if (file.isFile() && file.lastModified() < System.currentTimeMillis() - (newFileTime * day)) {
                    FileUtils.instans().deleteDirFileSafely(file);
                } else {
                    allFileSize += file.length();
                }
            }
            if (allFileSize > MaxFileSize) {  //限定总文件大小为100Mb
                deleteFiles(path, --day, 0);
            }
        }
    }

    private void createDir(File dir) {
        if (!dir.exists()) {
            dir.mkdirs();
        } else if (dir.isFile()) {
            FileUtils.instans().deleteDirFileSafely(dir);
//            deleteFileSafely(dir);
            dir.mkdirs();
        }
    }

    private long stringToLong(String strTime) {
        Date date = stringToDate(strTime, dayType); // String类型转成date类型
        if (date == null) {
            return 0;
        } else {
            return date.getTime();
        }
    }

    private Date stringToDate(String strTime, String formatType) {
        SimpleDateFormat formatter = new SimpleDateFormat(formatType);
        Date date = null;
        try {
            date = formatter.parse(strTime);
        } catch (ParseException e) {
        }
        return date;
    }

    @Override
    public void run() {
        while (!isExit) {
            if (logCache.size() > 0) {
                flushCache();
            } else {
                sleepThread();
            }
        }
    }

    protected void flushCache() {
        synchronized (obj) {
            while (logCache.size() > 0) {
                LogEntity entity = null;
                try {
                    entity = logCache.removeFirst();
                } catch (Exception e) {
                }
                if (entity == null) {
                    continue;
                }
                if (logCache.size() > 1000) {  /*当缓存数据大于一千条，不保存info等级以下的信息*/
                    if (entity.getLevel() < Log.LogLevel.TYPE_INFO) {
                        continue;
                    }
                }
                if (logCache.size() > 5000) {/*当缓存数据大于五千条，不保存异常等级以下的信息*/
                    if (entity.getLevel() < Log.LogLevel.TYPE_ERROR) {
                        continue;
                    }
                }
                if (logCache.size() > 10000) { /*当缓存数据大于一万时，完全清空缓存*/
                    logCache.clear();
                }
                c.setTimeInMillis(entity.getTime());
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append(c.get(Calendar.YEAR)).append("/")
                        .append((c.get(Calendar.MONTH) + 1)).append("/")
                        .append(c.get(Calendar.DATE)).append(" ")
                        .append(c.get(Calendar.HOUR_OF_DAY)).append(":")
                        .append(c.get(Calendar.MINUTE)).append(":")
                        .append(c.get(Calendar.SECOND)).append(":")
                        .append(c.get(Calendar.MILLISECOND)).append(separator)
                        .append(getLevel(entity.getLevel())).append(separator)
                        .append(entity.getTag()).append(speter)
                        .append(entity.getMsg()).append(enter);
//                String logs = timeTitle + + + + + + +;
                savaLineLog(stringBuilder.toString().getBytes());
            }
        }
    }

    private void sleepThread() {
        synchronized (obj) {
            isWait = true;
            try {
                if (logCache.size() == 0) {
                    obj.wait();
                }
            } catch (Exception e) {
            } finally {
                isWait = false;
            }
        }
    }

    private void threadStart() {
        if (isWait) {
            synchronized (obj) {
                try {
                    obj.notifyAll();
                } catch (Exception e) {
                }
            }
        }
    }

    private String getLevel(int level) {
        String leve;
        switch (level) {
            case Log.LogLevel.TYPE_VERBOSE:
                leve = "V";
                break;
            case Log.LogLevel.TYPE_DEBUG:
                leve = "D";
                break;
            case Log.LogLevel.TYPE_INFO:
                leve = "I";
                break;
            case Log.LogLevel.TYPE_WARM:
                leve = "W";
                break;
            case Log.LogLevel.TYPE_ERROR:
                leve = "E";
                break;
            case Log.LogLevel.TYPE_WTF:
                leve = "WTF";
                break;
            default:
                leve = "V";
                break;
        }
        return leve;
    }

    private class LogEntity {
        long time;
        int level;
        String tag;
        String msg;

        private LogEntity(long time, int level, String tag, String msg) {
            this.time = time;
            this.level = level;
            this.tag = tag;
            this.msg = msg;
        }

        private long getTime() {
            return time;
        }

        private int getLevel() {
            return level;
        }

        private String getTag() {
            return tag;
        }

        private String getMsg() {
            return msg;
        }
    }
}
