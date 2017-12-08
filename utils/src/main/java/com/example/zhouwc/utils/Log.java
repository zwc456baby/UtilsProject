package com.example.zhouwc.utils;

import android.annotation.SuppressLint;
import android.app.Application;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.os.Bundle;
import android.os.Message;
import android.support.annotation.NonNull;
import android.text.TextUtils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.StringReader;
import java.io.StringWriter;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.ref.Reference;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.xml.transform.OutputKeys;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

import static java.lang.annotation.ElementType.ANNOTATION_TYPE;
import static java.lang.annotation.RetentionPolicy.SOURCE;

//引用到其他包时，可能导入其他包时会异常，更改全包路径


/**
 * Created by zhouwenchao on 2017/6/9.
 * 入口类
 */
public final class Log {
    private Log() {
    }

    private static Logger printer = new Logger();
    private static LogConfigImpl logConfig = LogConfigImpl.getInstance();

    /**
     * 选项配置
     *
     * @return 配置类
     */
    public static LogConfig getLogConfig() {
        return logConfig;
    }

    public static void v(Object object) {
        printer.v(object);
    }


    public static void d(Object object) {
        printer.d(object);
    }


    public static void i(Object object) {
        printer.i(object);
    }


    public static void w(Object object) {
        printer.w(object);
    }


    public static void e(Object object) {
        printer.e(object);
    }


    public static void wtf(Object object) {
        printer.wtf(object);
    }

    /**
     * 打印json
     *
     * @param json json数据
     */
    public static void json(String json) {
        printer.json(json);
    }

    /**
     * 输出xml
     *
     * @param xml xml数据
     */
    public static void xml(String xml) {
        printer.xml(xml);
    }

    public static String get(Object object) {
        return printer.get(object);
    }

    public static void flushLogFile() {
        printer.flushLogFile();
    }

    public static void v(String tag, Object object) {
        printer.setTag(tag).v(object);
    }


    public static void d(String tag, Object object) {
        printer.setTag(tag).d(object);
    }


    public static void i(String tag, Object object) {
        printer.setTag(tag).i(object);
    }


    public static void w(String tag, Object object) {
        printer.setTag(tag).w(object);
    }


    public static void e(String tag, Object object) {
        printer.setTag(tag).e(object);
    }


    public static void wtf(String tag, Object object) {
        printer.setTag(tag).wtf(object);
    }

    /**
     * 打印json
     *
     * @param json json数据
     */
    public static void json(String tag, String json) {
        printer.setTag(tag).json(json);
    }

    /**
     * 输出xml
     *
     * @param xml xml 数据
     */
    public static void xml(String tag, String xml) {
        printer.setTag(tag).xml(xml);
    }


    /**
     * Created by pengwei08 on 2015/7/20.
     */
// TODO: 16/3/22 泛型支持
    private static class Logger implements Printer {
        private LogConfigImpl mLogConfig;
        private final ThreadLocal<String> localTags = new ThreadLocal<String>();
//        private final int STACKOFFSET = 9;

        private Logger() {
            mLogConfig = LogConfigImpl.getInstance();
            mLogConfig.addParserClass(Constant.DEFAULT_PARSE_CLASS);
        }

        /**
         * 设置临时tag
         *
         * @param tag 临时tag
         * @return 返回打印类
         */
        private Printer setTag(String tag) {
            if (!TextUtils.isEmpty(tag) && mLogConfig.isEnable()) {
                localTags.set(tag);
            }
            return this;
        }

        /**
         * 打印字符串
         *
         * @param type
         * @param msg
         */
        private void logString(@LogLevel.LogLevelType int type, String tag, String msg) {
            if (msg.length() > Constant.LINE_MAX) {
                for (String subMsg : Utils.largeStringToList(msg)) {
                    printLog(type, tag, subMsg);
//                logString(type, tag, subMsg);
                }
                return;
            }
            printLog(type, tag, msg);
        }


        /**
         * 打印对象
         *
         * @param type
         * @param objecttmp
         */
        private void logObject(@LogLevel.LogLevelType int type, Object objecttmp) {
            if (!mLogConfig.isEnable()) {
                return;
            }
            if (type < mLogConfig.getLogLevel()) {
                return;
            }
//        String tag = generateTag();
            String tempTag = localTags.get();
            if (!TextUtils.isEmpty(tempTag)) {
                localTags.remove();
                logString(type, tempTag, ObjectUtil.objectToString(objecttmp));
            } else {
                logString(type, mLogConfig.getTagPrefix(), ObjectUtil.objectToString(objecttmp));
            }
        }

        /**
         * 获取当前activity栈信息
         *
         * @return
         */
        private StackTraceElement getCurrentStackTrace() {
            StackTraceElement[] trace = Thread.currentThread().getStackTrace();
            int stackOffset = getStackOffset(trace, Log.class);
            return stackOffset == -1 ? null : trace[stackOffset];
        }

        private int getStackOffset(StackTraceElement[] trace, Class cla) {
            String claName = cla.getName();
            for (int i = Constant.MIN_STACK_OFFSET; i < trace.length; i++) {
                if (trace[i].getClassName().equals(claName)) return ++i;
            }
            return -1;
        }

        /**
         * 获取最顶部stack信息
         *
         * @return tasckInfo
         */
        private String getTopStackInfo() {
            StackTraceElement caller = getCurrentStackTrace();
            if (caller == null) return "Null Stack Trace";
            String stackTrace = caller.toString();
            stackTrace = stackTrace.substring(stackTrace.lastIndexOf('('), stackTrace.length());
            String tag = "%s.%s%s";
            String callerClazzName = caller.getClassName();
            callerClazzName = callerClazzName.substring(callerClazzName.lastIndexOf(".") + 1);
            tag = String.format(tag, callerClazzName, caller.getMethodName(), stackTrace);
            return tag;
        }

        @Override
        public String get(Object object) {
            return ObjectUtil.objectToString(object);
        }

        @Override
        public void flushLogFile() {
            if (LogPrint.isInstance()) {
                LogPrint.getInstance().flushCache();
            }
            if (!mLogConfig.isSavaLog()) {
                LogPrint.clear();
            }
        }

        @Override
        public void d(Object object) {
            logObject(LogLevel.TYPE_DEBUG, object);
        }


        @Override
        public void e(Object object) {
            logObject(LogLevel.TYPE_ERROR, object);
        }


        @Override
        public void w(Object object) {
            logObject(LogLevel.TYPE_WARM, object);
        }


        @Override
        public void i(Object object) {
            logObject(LogLevel.TYPE_INFO, object);
        }


        @Override
        public void v(Object object) {
            logObject(LogLevel.TYPE_VERBOSE, object);
        }


        @Override
        public void wtf(Object object) {
            logObject(LogLevel.TYPE_WTF, object);
        }

        /**
         * 采用orhanobut/logger的json解析方案
         * source:https://github.com/orhanobut/logger/blob/master/logger/src/main/java/com/orhanobut/logger/LoggerPrinter.java#L152
         *
         * @param json
         */
        @Override
        public void json(String json) {
            int indent = 4;
            if (TextUtils.isEmpty(json)) {
                logObject(LogLevel.TYPE_DEBUG, "JSON{json is empty}");
                return;
            }
            try {
                if (json.startsWith("{")) {
                    JSONObject jsonObject = new JSONObject(json);
                    String msg = jsonObject.toString(indent);
                    logObject(LogLevel.TYPE_DEBUG, msg);
                } else if (json.startsWith("[")) {
                    JSONArray jsonArray = new JSONArray(json);
                    String msg = jsonArray.toString(indent);
                    logObject(LogLevel.TYPE_DEBUG, msg);
                }
            } catch (JSONException e) {
                logObject(LogLevel.TYPE_ERROR, e.toString() + "\n\njson = " + json);
            }
        }

        /**
         * 采用orhanobut/logger的xml解析方案
         * source:https://github.com/orhanobut/logger/blob/master/logger/src/main/java/com/orhanobut/logger/LoggerPrinter.java#L180
         *
         * @param xml
         */
        @Override
        public void xml(String xml) {
            if (TextUtils.isEmpty(xml)) {
                logObject(LogLevel.TYPE_DEBUG, "XML{xml is empty}");
                return;
            }
            try {
                Source xmlInput = new StreamSource(new StringReader(xml));
                StreamResult xmlOutput = new StreamResult(new StringWriter());
                Transformer transformer = TransformerFactory.newInstance().newTransformer();
                transformer.setOutputProperty(OutputKeys.INDENT, "yes");
                transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");
                transformer.transform(xmlInput, xmlOutput);
                logObject(LogLevel.TYPE_DEBUG, xmlOutput.getWriter().toString().replaceFirst(">", ">\n"));
            } catch (TransformerException e) {
                logObject(LogLevel.TYPE_ERROR, e.toString() + "\n\nxml = " + xml);
            }
        }

        /**
         * 打印日志
         *
         * @param type
         * @param tag
         * @param msg
         */
        private void printLog(@LogLevel.LogLevelType int type, String tag, String msg) {
            msg = getTopStackInfo() + ": " + msg;

            switch (type) {
                case LogLevel.TYPE_VERBOSE:
                    android.util.Log.v(tag, msg);
                    break;
                case LogLevel.TYPE_DEBUG:
                    android.util.Log.d(tag, msg);
                    break;
                case LogLevel.TYPE_INFO:
                    android.util.Log.i(tag, msg);
                    break;
                case LogLevel.TYPE_WARM:
                    android.util.Log.w(tag, msg);
                    break;
                case LogLevel.TYPE_ERROR:
                    android.util.Log.e(tag, msg);
                    break;
                case LogLevel.TYPE_WTF:
                    android.util.Log.wtf(tag, msg);
                    break;
                default:
                    break;
            }
            LogView.addInfo(msg);
            if (mLogConfig.isSavaLog()) {
                savaLogToFile(type, tag, msg);
            }
        }

        private void savaLogToFile(int level, String tag, String msg) {
            LogPrint.getInstance().addOnlineLog(level, tag, msg);
        }
    }

    //********************** **********************************************************************
    //**********************  以下为配置类  ********************************************
    //********************** **********************************************************************

    /**
     * Created by pengwei on 16/3/4.
     * Log config
     */
    private static final class LogConfigImpl implements LogConfig {

        private boolean enable = appIsDebug();
        private String tagPrefix;
        @LogLevel.LogLevelType
        private int logLevel = LogLevel.TYPE_VERBOSE;
        private List<Parser> parseList;
        private static LogConfigImpl singleton;
        private boolean savaLog = enable;

        private LogConfigImpl() {
            parseList = new ArrayList<Parser>();
        }

        private static LogConfigImpl getInstance() {
            if (singleton == null) {
                synchronized (LogConfigImpl.class) {
                    if (singleton == null) {
                        singleton = new LogConfigImpl();
                    }
                }
            }
            return singleton;
        }

        @Override
        public LogConfig configAllowLog(boolean allowLog) {
            this.enable = allowLog;
            return this;
        }

        @Override
        public LogConfig configTagPrefix(String prefix) {
            this.tagPrefix = prefix;
            return this;
        }


        @Override
        public LogConfig configLevel(@LogLevel.LogLevelType int logLevel) {
            this.logLevel = logLevel;
            return this;
        }

        @SafeVarargs
        @Override
        public final LogConfig addParserClass(Class<? extends Parser>... classes) {
            // TODO: 16/3/12 判断解析类的继承关系
            for (Class<? extends Parser> cla : classes) {
                try {
                    parseList.add(0, cla.newInstance());
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            return this;
        }


        private boolean isSavaLog() {
            return savaLog;
        }

//        public String getLogFilePath() {
//            return logFilePath;
//        }

        @Override
        public LogConfig savaLogFile(boolean sava) {
            this.savaLog = sava;

            return this;
        }

        @Override
        public LogConfig setLogFileDir(String logFilePath) {
//            this.logFilePath = logFilePath;
            if (TextUtils.isEmpty(logFilePath)) return this;
            if (LogPrint.isInstance()) {
                LogPrint.clear();
            }
            LogPrint.getInstance().setLogDir(logFilePath);
//            if (!TextUtils.isEmpty(logFilePath)) {
//                LogPrint.getInstance(logFilePath);
//            }
            return this;
        }

        /**
         * 返回应用是否处于debug模式
         * 如果反射应用状态失败，则使用自身的BuildConfig数据
         *
         * @return app是否处于debug模式
         */
        @Override
        public boolean appIsDebug() {
            Application application = getAppLication();
            if (application == null) return BuildConfig.DEBUG;
            ApplicationInfo info = application.getApplicationInfo();
            return (info.flags & ApplicationInfo.FLAG_DEBUGGABLE) != 0;
        }

        private Application getAppLication() {
            try {
                @SuppressLint("PrivateApi")
                Application application = (Application) Class.forName("android.app.ActivityThread")
                        .getMethod("currentApplication").invoke(null, (Object[]) null);
                return application;
            } catch (Exception e) {
                try {
                    @SuppressLint("PrivateApi")
                    Application application = (Application) Class.forName("android.app.AppGlobals").
                            getMethod("getInitialApplication").invoke(null, (Object[]) null);
                    return application;
                } catch (Exception e2) {
                    return null;
                }
            }
        }

        private boolean isEnable() {
            return enable;
        }

        private String getTagPrefix() {
            if (TextUtils.isEmpty(tagPrefix)) {
                return "LogUtils";
            }

            return tagPrefix;
        }


        private int getLogLevel() {
            return logLevel;
        }

        private List<Parser> getParseList() {
            return parseList;
        }

    }
    //********************** **********************************************************************
    //**********************  以下为常量类  ********************************************
    //********************** **********************************************************************

    /**
     * Created by pengwei on 16/4/18.
     */
    private static final class Constant {

        private static final String STRING_OBJECT_NULL = "Object[object is null]";

        // 每行最大日志长度
        private static final int LINE_MAX = 1024 * 3;

        // 解析属性最大层级
        /*最大层级只能是1 否则如果层级大于2  数组 1 引用数组 2 ，数组 2 又引用数组1 会导致无限循环*/
        private static final int MAX_CHILD_LEVEL = 1;

        private static final int MIN_STACK_OFFSET = 5;

        // 换行符
        private static final String BR = System.getProperty("line.separator");


        // 默认支持解析库
        private static final Class[] DEFAULT_PARSE_CLASS = new Class[]{
                BundleParse.class, IntentParse.class, CollectionParse.class,
                MapParse.class, ThrowableParse.class, ReferenceParse.class, MessageParse.class
        };


        /**
         * 获取默认解析类
         *
         * @return 返回默认揭西裤
         */
        private static List<Parser> getParsers() {
            return LogConfigImpl.getInstance().getParseList();
        }
    }
    //********************** **********************************************************************
    //**********************  以下为工具类  ********************************************
    //********************** **********************************************************************

    /**
     * Created by pengwei08 on 2015/7/20.
     */
    private static final class ObjectUtil {

        /**
         * 将对象转化为String
         *
         * @param object
         * @return
         */
        private static String objectToString(Object object) {
            return objectToString(object, 0);
        }

        /**
         * 是否为静态内部类
         *
         * @param cla
         * @return
         */
        private static boolean isStaticInnerClass(Class cla) {
            if (cla != null && cla.isMemberClass()) {
                int modifiers = cla.getModifiers();
                if ((modifiers & Modifier.STATIC) == Modifier.STATIC) {
                    return true;
                }
            }
            return false;
        }

        private static String objectToString(Object object, int childLevel) {
            if (object == null) {
                return Constant.STRING_OBJECT_NULL;
            }
            if (childLevel > Constant.MAX_CHILD_LEVEL) {
                return object.toString();
            }
            if (Constant.getParsers() != null && Constant.getParsers().size() > 0) {
                for (Parser parser : Constant.getParsers()) {
                    if (parser.parseClassType().isAssignableFrom(object.getClass())) {
                        return parser.parseString(object);
                    }
                }
            }
            if (ArrayUtil.isArray(object)) {
                return ArrayUtil.parseArray(object);
            }
            if (object.toString().startsWith(object.getClass().getName() + "@")) {
                StringBuilder builder = new StringBuilder();
                getClassFields(object.getClass(), builder, object, false, childLevel);
                Class superClass = object.getClass().getSuperclass();
                while (!superClass.equals(Object.class)) {
                    getClassFields(superClass, builder, object, true, childLevel);
                    superClass = superClass.getSuperclass();
                }
                return builder.toString();
            } else {
                // 若对象重写toString()方法默认走toString()
                return object.toString();
            }
        }

        /**
         * 拼接class的字段和值
         *
         * @param cla         类
         * @param builder     String
         * @param o           对象
         * @param isSubClass  死否为子class
         * @param childOffset 递归解析属性的层级
         */
        private static void getClassFields(Class cla, StringBuilder builder, Object o, boolean isSubClass,
                                           int childOffset) {
            if (cla.equals(Object.class)) {
                return;
            }
            if (isSubClass) {
                builder.append(Constant.BR).append(Constant.BR).append("=> ");
            }
//        String breakLine = childOffset == 0 ? BR : "";
            String breakLine = "";
            //TODO
//            builder.append(cla.getSimpleName()).append(" {");
            builder.append(cla.getName()).append(" {");
            Field[] fields = cla.getDeclaredFields();
            for (int i = 0; i < fields.length; ++i) {
                Field field = fields[i];
                field.setAccessible(true);
//                if (cla.isMemberClass() && !isStaticInnerClass(cla) && i == 0) {
//                    continue;
//                }
                if (!isStaticInnerClass(cla) && i == 0) {
                    continue;
                }
                Object subObject = null;
                try {
                    subObject = field.get(o);
                } catch (IllegalAccessException e) {
                    subObject = e;
                } finally {
                    if (subObject != null) {
                        // 解决Instant Run情况下内部类死循环的问题
//                    System.out.println(field.getName()+ "***" +subObject.getClass() + "啊啊啊啊啊啊" + cla);
                        if (!isStaticInnerClass(cla) && (field.getName().equals("$change") || field.getName().equalsIgnoreCase("this$0"))) {
                            //noinspection ContinueOrBreakFromFinallyBlock
                            continue;
                        }
                        if (subObject instanceof String) {
                            subObject = "\"" + subObject + "\"";
                        } else if (subObject instanceof Character) {
                            subObject = "\'" + subObject + "\'";
                        }
                        if (childOffset < Constant.MAX_CHILD_LEVEL) {
                            subObject = objectToString(subObject, childOffset + 1);
                        }
                    }
                    String formatString = breakLine + "%s = %s, ";
//                System.out.println(field.getName() + "**" + cla.getName() + "**" + isSubClass + "**" + o.toString());
                    builder.append(String.format(formatString, field.getName(),
                            subObject == null ? "null" : subObject.toString()));
                }
            }
            if (builder.toString().endsWith("{")) {
                builder.append("}");
            } else {
                builder.replace(builder.length() - 2, builder.length() - 1, breakLine + "}");
            }
        }
    }

    /**
     * Created by pengwei on 16/4/19.
     */
    private static final class Utils {

        /**
         * 长字符串转化为List
         *
         * @param msg
         * @return
         */
        public static List<String> largeStringToList(String msg) {
            List<String> stringList = new ArrayList<String>();
            int index = 0;
            int maxLength = Constant.LINE_MAX;
            int countOfSub = msg.length() / maxLength;
            if (countOfSub > 0) {
                for (int i = 0; i < countOfSub; i++) {
                    String sub = msg.substring(index, index + maxLength);
                    stringList.add(sub);
                    index += maxLength;
                }
                stringList.add(msg.substring(index, msg.length()));
            } else {
                stringList.add(msg);
            }
            return stringList;
        }
    }
//***********************  ArrayUtil  ****************************

    /**
     * Created by pengwei08 on 2015/7/25.
     * Thanks to zhutiantao for providing an array of analytical methods.
     */
    private static final class ArrayUtil {

        /**
         * 获取数组的纬度
         *
         * @param object
         * @return
         */
        private static int getArrayDimension(Object object) {
            int dim = 0;
            for (int i = 0; i < object.toString().length(); ++i) {
                if (object.toString().charAt(i) == '[') {
                    ++dim;
                } else {
                    break;
                }
            }
            return dim;
        }

        /**
         * 是否为数组
         *
         * @param object object
         * @return 是否为数组
         */
        private static boolean isArray(Object object) {
            return object.getClass().isArray();
        }

        /**
         * 获取数组类型
         *
         * @param object 如L为int型
         * @return
         */
        private static char getType(Object object) {
            if (isArray(object)) {
                String str = object.toString();
                return str.substring(str.lastIndexOf("[") + 1, str.lastIndexOf("[") + 2).charAt(0);
            }
            return 0;
        }

        /**
         * 遍历数组
         *
         * @param result
         * @param array
         */
        private static void traverseArray(StringBuilder result, Object array) {
            if (isArray(array)) {
                if (getArrayDimension(array) == 1) {
                    switch (getType(array)) {
                        case 'I':
                            result.append(Arrays.toString((int[]) array));
                            break;
                        case 'D':
                            result.append(Arrays.toString((double[]) array));
                            break;
                        case 'Z':
                            result.append(Arrays.toString((boolean[]) array));
                            break;
                        case 'B':
                            result.append(Arrays.toString((byte[]) array));
                            break;
                        case 'S':
                            result.append(Arrays.toString((short[]) array));
                            break;
                        case 'J':
                            result.append(Arrays.toString((long[]) array));
                            break;
                        case 'F':
                            result.append(Arrays.toString((float[]) array));
                            break;
                        case 'L':
                            Object[] objects = (Object[]) array;
                            result.append("[");
                            for (int i = 0; i < objects.length; ++i) {
                                result.append(ObjectUtil.objectToString(objects[i]));
                                if (i != objects.length - 1) {
                                    result.append(",");
                                }
                            }
                            result.append("]");
                            break;
                        default:
                            result.append(Arrays.toString((Object[]) array));
                            break;
                    }
                } else {
                    result.append("[");
                    for (int i = 0; i < ((Object[]) array).length; i++) {
                        traverseArray(result, ((Object[]) array)[i]);
                        if (i != ((Object[]) array).length - 1) {
                            result.append(",");
                        }
                    }
                    result.append("]");
                }
            } else {
                result.append("not a array!!");
            }
        }

        /**
         * 将数组内容转化为字符串
         *
         * @param array 数组
         * @return 字符串
         */
        private static String parseArray(Object array) {
            StringBuilder result = new StringBuilder();
            traverseArray(result, array);
            return result.toString();
        }
    }
    //********************** **********************************************************************
    //**********************  以下为接口类  ********************************************
    //********************** **********************************************************************

    /**
     * Created by pengwei on 16/3/4.
     */
    public interface LogConfig {

        LogConfig configAllowLog(boolean allowLog);

        LogConfig configTagPrefix(String prefix);


        LogConfig configLevel(@LogLevel.LogLevelType int logLevel);

        LogConfig addParserClass(Class<? extends Parser>... classes);

        LogConfig savaLogFile(boolean sava);

        LogConfig setLogFileDir(String logFilePath);

        boolean appIsDebug();

    }

//**********************************  Printer ************************************

    /**
     * Created by pengwei08 on 2015/7/20.
     */
    public interface Printer {

        String get(Object object);

        void flushLogFile();

        void d(Object object);

        void e(Object object);

        void w(Object object);

        void i(Object object);

        void v(Object object);

        void wtf(Object object);

        void json(String json);

        void xml(String xml);


    }

    /**
     * Created by pengwei on 16/3/8.
     * 格式化对象
     */
    public interface Parser<T> {

        String LINE_SEPARATOR = Constant.BR;

        Class<T> parseClassType();

        String parseString(T t);
    }

    /**
     * Created by pengwei on 16/3/3.
     */
    public static final class LogLevel {
        public static final int TYPE_VERBOSE = 1;
        public static final int TYPE_DEBUG = 2;
        public static final int TYPE_INFO = 3;
        public static final int TYPE_WARM = 4;
        public static final int TYPE_ERROR = 5;
        public static final int TYPE_WTF = 6;

        @mIntDef({TYPE_VERBOSE, TYPE_DEBUG, TYPE_INFO, TYPE_WARM, TYPE_ERROR, TYPE_WTF})
        @Retention(RetentionPolicy.SOURCE)
        public @interface LogLevelType {
        }
    }


    @Retention(SOURCE)
    @Target({ANNOTATION_TYPE})
    private @interface mIntDef {
        /**
         * Defines the allowed constants for this element
         */
        long[] value() default {};

        /**
         * Defines whether the constants can be used as a flag, or just as an enum (the default)
         */
        boolean flag() default false;
    }


    //********************** **********************************************************************
    //**********************  以下为默认支持的解析类  ********************************************
    //********************** **********************************************************************

    /**
     * Created by pengwei on 16/3/8.
     */
    static final class BundleParse implements Parser<Bundle> {

        @Override
        public Class<Bundle> parseClassType() {
            return Bundle.class;
        }

        @Override
        public String parseString(Bundle bundle) {
            if (bundle != null) {
                StringBuilder builder = new StringBuilder(bundle.getClass().getName() + " [" + LINE_SEPARATOR);
                for (String key : bundle.keySet()) {
                    builder.append(String.format("'%s' => %s " + LINE_SEPARATOR,
                            key, ObjectUtil.objectToString(bundle.get(key))));
                }
                builder.append("]");
                return builder.toString();
            }
            return null;
        }
    }

    /**
     * Created by pengwei on 16/3/8.
     */
    static final class CollectionParse implements Parser<Collection> {

        @Override
        public Class<Collection> parseClassType() {
            return Collection.class;
        }

        @SuppressLint("DefaultLocale")
        @Override
        public String parseString(Collection collection) {
            String simpleName = collection.getClass().getName();
            String msg = "%s size = %d [" + Constant.BR;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append(String.format(msg, simpleName, collection.size()));
//            msg = String.format(msg, simpleName, collection.size());

            if (!collection.isEmpty()) {
                Iterator iterator = collection.iterator();
                int flag = 0;
                String itemString = "[%d]:%s%s";
                while (iterator.hasNext()) {
                    Object item = iterator.next();
                    stringBuilder.append(String.format(itemString, flag, ObjectUtil.objectToString(item),
                            flag++ < collection.size() - 1 ? "," + LINE_SEPARATOR : LINE_SEPARATOR));
//                    msg += String.format(itemString, flag, ObjectUtil.objectToString(item),
//                            flag++ < collection.size() - 1 ? "," + LINE_SEPARATOR : LINE_SEPARATOR);
                }
            }
            return stringBuilder.toString() + "]";
        }
    }

    /**
     * Created by pengwei on 16/3/8.
     */
    static final class IntentParse implements Parser<Intent> {
        @SuppressLint("UseSparseArrays")
        private Map<Integer, String> flagMap = new HashMap<>();

        {
            Class cla = Intent.class;
            Field[] fields = cla.getDeclaredFields();
            for (Field field : fields) {
                field.setAccessible(true);
                if (field.getName().startsWith("FLAG_")) {
                    int value = 0;
                    try {
                        Object object = field.get(cla);
                        if (object instanceof Integer || object.getClass().getSimpleName().equals("int")) {
                            value = (int) object;
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    if (flagMap.get(value) == null) {
                        flagMap.put(value, field.getName());
                    }
                }
            }
        }

        @Override
        public Class<Intent> parseClassType() {
            return Intent.class;
        }

        @Override
        public String parseString(Intent intent) {
            StringBuilder builder = new StringBuilder(parseClassType().getSimpleName() + " [" + LINE_SEPARATOR);
            builder.append(String.format("%s = %s" + LINE_SEPARATOR, "Scheme", intent.getScheme()));
            builder.append(String.format("%s = %s" + LINE_SEPARATOR, "Action", intent.getAction()));
            builder.append(String.format("%s = %s" + LINE_SEPARATOR, "DataString", intent.getDataString()));
            builder.append(String.format("%s = %s" + LINE_SEPARATOR, "Type", intent.getType()));
            builder.append(String.format("%s = %s" + LINE_SEPARATOR, "Package", intent.getPackage()));
            builder.append(String.format("%s = %s" + LINE_SEPARATOR, "ComponentInfo", intent.getComponent()));
            builder.append(String.format("%s = %s" + LINE_SEPARATOR, "Flags", getFlags(intent.getFlags())));
            builder.append(String.format("%s = %s" + LINE_SEPARATOR, "Categories", intent.getCategories()));
            builder.append(String.format("%s = %s" + LINE_SEPARATOR, "Extras",
                    new BundleParse().parseString(intent.getExtras())));
            builder.append("]");
            return builder.toString();
        }

        /**
         * 获取flag的值
         * 感谢涛哥提供的方法(*^__^*)
         *
         * @param flags
         * @return
         */
        private String getFlags(int flags) {
            StringBuilder builder = new StringBuilder();
            for (int flagKey : flagMap.keySet()) {
                if ((flagKey & flags) == flagKey) {
                    builder.append(flagMap.get(flagKey));
                    builder.append(" | ");
                }
            }
            if (TextUtils.isEmpty(builder.toString())) {
                builder.append(flags);
            } else if (builder.indexOf("|") != -1) {
                builder.delete(builder.length() - 2, builder.length());
            }
            return builder.toString();
        }
    }

    /**
     * Created by pengwei on 16/3/8.
     */
    static final class MapParse implements Parser<Map> {
        @Override
        public Class<Map> parseClassType() {
            return Map.class;
        }

        @Override
        public String parseString(Map map) {
            StringBuilder msg = new StringBuilder(map.getClass().getName() + " [" + LINE_SEPARATOR);
            Set keys = map.keySet();
            for (Object key : keys) {
                String itemString = "%s -> %s" + LINE_SEPARATOR;
                Object value = map.get(key);
                if (value != null) {
                    if (value instanceof String) {
                        value = "\"" + value + "\"";
                    } else if (value instanceof Character) {
                        value = "\'" + value + "\'";
                    }
                }
                msg.append(String.format(itemString, ObjectUtil.objectToString(key),
                        ObjectUtil.objectToString(value)));
            }
            return msg + "]";
        }
    }


    /**
     * Created by pengwei on 2017/3/29.
     */

    static final class MessageParse implements Parser<Message> {
        @Override
        public Class<Message> parseClassType() {
            return Message.class;
        }

        @Override
        public String parseString(Message message) {
            if (message == null) {
                return null;
            }
            String stringBuilder = message.getClass().getName() + " [" + LINE_SEPARATOR + String.format("%s = %s", "what", message.what) + LINE_SEPARATOR +
                    String.format("%s = %s", "when", message.getWhen()) + LINE_SEPARATOR +
                    String.format("%s = %s", "arg1", message.arg1) + LINE_SEPARATOR +
                    String.format("%s = %s", "arg2", message.arg2) + LINE_SEPARATOR +
                    String.format("%s = %s", "data",
                            new BundleParse().parseString(message.getData())) +
                    LINE_SEPARATOR +
                    String.format("%s = %s", "obj",
                            ObjectUtil.objectToString(message.obj)) +
                    LINE_SEPARATOR +
                    "]";
            return stringBuilder;
        }
    }


    /**
     * Created by pengwei on 16/3/22.
     */
    static final class ReferenceParse implements Parser<Reference> {
        @Override
        public Class<Reference> parseClassType() {
            return Reference.class;
        }

        @Override
        public String parseString(Reference reference) {
            Object actual = reference.get();
            String builder = reference.getClass().getSimpleName() + "<" + actual.getClass().getSimpleName() + "> {" +
                    "→" + ObjectUtil.objectToString(actual);
            return builder + "}";
        }
    }

    /**
     * Created by pengwei on 16/3/8.
     */
    static final class ThrowableParse implements Parser<Throwable> {
        @Override
        public Class<Throwable> parseClassType() {
            return Throwable.class;
        }

        @Override
        public String parseString(Throwable throwable) {
            return android.util.Log.getStackTraceString(throwable);
        }
    }

    private static class LogPrint implements Runnable {
        private final LinkedList<LogEntity> logCache = new LinkedList<>();

        private FileOutputStream outStream = null;
        private final Object obj = new Object();
        private static LogPrint logclass;
        private long currentFileTime = 0;
        private final long newFileTime = 86400000;
        private String dayType = "yyyy-MM-dd";
        private SimpleDateFormat day = new SimpleDateFormat(dayType);

        private static String LogDir = FileUtils.rootDir + File.separator + "LogUtilsFile" + File.separator;
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

        private static LogPrint getInstance() {
            if (logclass == null) {
                synchronized (LogPrint.class) {
                    if (logclass == null) {
                        logclass = new LogPrint();
                    }
                }
            }
            return logclass;
        }

        private void setLogDir(String path) {
            LogDir = path;
        }

        private static boolean isInstance() {
            return logclass != null;
        }

        private static void clear() {
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

        private void addOnlineLog(int level, @NonNull String tag, @NonNull String msg) {
            logCache.addLast(new LogPrint.LogEntity(System.currentTimeMillis(), level, tag, msg));
            threadStart();
        }


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
//        boolean hasSDCard = Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED);
            if (!FileUtils.instans().hasRootDirPermmiss()) return null;
            try {
                deleteFiles(LogDir, 7, 0);
//                String filePath = LogDir + ;
                currentLogFile = new File(LogDir, createfileTime + "Log.txt");
                if (!currentLogFile.exists()) {
                    File dir = new File(currentLogFile.getParent());
                    createDir(dir);
                    if (!currentLogFile.createNewFile()) {
                        return null;
                    }
                }
                if (currentLogFile.isDirectory()) {
                    FileUtils.instans().deleteDirFileSafely(currentLogFile);
                    if (!currentLogFile.createNewFile()) {
                        return null;
                    }
                }
                return new FileOutputStream(currentLogFile, true);  //追加模式
            } catch (Exception ignored) {
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
            if (dir == null) return;
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
            } catch (ParseException ignored) {
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

        private void flushCache() {
            synchronized (obj) {
                while (logCache.size() > 0) {
                    LogPrint.LogEntity entity = null;
                    try {
                        entity = logCache.removeFirst();
                    } catch (Exception ignored) {
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
                    String stringBuilder = String.valueOf(c.get(Calendar.YEAR)) + "/" +
                            (c.get(Calendar.MONTH) + 1) + "/" +
                            c.get(Calendar.DATE) + " " +
                            c.get(Calendar.HOUR_OF_DAY) + ":" +
                            c.get(Calendar.MINUTE) + ":" +
                            c.get(Calendar.SECOND) + ":" +
                            c.get(Calendar.MILLISECOND) + separator +
                            getLevel(entity.getLevel()) + separator +
                            entity.getTag() + speter +
                            entity.getMsg() + enter;
                    //                String logs = timeTitle + + + + + + +;
                    savaLineLog(stringBuilder.getBytes());
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
                } catch (Exception ignored) {
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
                    } catch (Exception ignored) {
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
}

















