package com.example.zhouwc.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import android.preference.PreferenceManager;
import android.text.TextUtils;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * Created by hasee on 2017/6/10.
 */
public class PreUtils {
    private static final String User = "user_preference";
    private static final PreUtilsImpl preImpl = new PreUtilsImpl(User);
    private static PreUtilsImpl sysPreImpl;
//    private static final SysPreUtilsImpl sysPreImpl = new SysPreUtilsImpl();

    public static boolean putInt(Context context, String key, int value) {
        return getDefaultImpl().putInt(context, key, value);
    }

    public static int getInt(Context context, String key, int defaultInt) {
        return getDefaultImpl().getInt(context, key, defaultInt);
    }

    public static boolean putString(Context context, String key, String value) {
        return getDefaultImpl().putString(context, key, value);
    }

    public static String getString(Context context, String key, String defaultStr) {
        return getDefaultImpl().getString(context, key, defaultStr);
    }

    public static boolean putBooble(Context context, String key, boolean value) {
        return getDefaultImpl().putBooble(context, key, value);
    }

    public static boolean getBooble(Context context, String key, boolean defaultStr) {
        return getDefaultImpl().getBooble(context, key, defaultStr);
    }

    public static boolean putMap(Context context, String key, Map<String, String> value) {
        return getDefaultImpl().putMap(context, key, value);
    }

    public static Map<String, String> getMap(Context context, String key) {
        return getDefaultImpl().getMap(context, key);
    }


    public static boolean putIntToSys(Context context, String key, int value) {
        return getSysPreference(context).putInt(context, key, value);
    }

    public static int getIntFromSys(Context context, String key, int defaultInt) {
        return getSysPreference(context).getInt(context, key, defaultInt);
    }

    public static boolean putStringToSys(Context context, String key, String value) {
        return getSysPreference(context).putString(context, key, value);
    }

    public static String getStringFromSys(Context context, String key, String defaultStr) {
        return getSysPreference(context).getString(context, key, defaultStr);
    }

    public static boolean putBoobleToSys(Context context, String key, boolean value) {
        return getSysPreference(context).putBooble(context, key, value);
    }

    public static boolean getBoobleFromSys(Context context, String key, boolean defaultStr) {
        return getSysPreference(context).getBooble(context, key, defaultStr);
    }

    public static boolean putMapToSys(Context context, String key, Map<String, String> value) {
        return getSysPreference(context).putMap(context, key, value);
    }

    public static Map<String, String> getMapFromSys(Context context, String key) {
        return getSysPreference(context).getMap(context, key);
    }


    private static PreUtilsImpl getDefaultImpl() {
        return preImpl;
    }

    private static PreUtilsImpl getSysPreference(Context context) {
        if (sysPreImpl == null) {
            synchronized (PreUtils.class) {
                if (sysPreImpl == null) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                        sysPreImpl = new PreUtilsImpl(PreferenceManager.getDefaultSharedPreferencesName(context));
                    } else {
                        sysPreImpl = new PreUtilsImpl(context.getPackageName() + "_preferences");
                    }
                }
            }
        }
        return sysPreImpl;
    }

    private static class PreUtilsImpl implements PreferenceInterface {
        String name;

        private PreUtilsImpl(String name) {
            this.name = name;
        }

        @Override
        public boolean putInt(Context context, String key, int value) {
            if (TextUtils.isEmpty(key)) return false;
            SharedPreferences preferences = context.getSharedPreferences(name, Context.MODE_PRIVATE);
            SharedPreferences.Editor editor = preferences.edit();
            editor.putInt(key, value);
            return editor.commit();
        }

        @Override
        public int getInt(Context context, String key, int defaultInt) {
            if (TextUtils.isEmpty(key)) return defaultInt;
            SharedPreferences preferences = context.getSharedPreferences(name, Context.MODE_PRIVATE);
            return preferences.getInt(key, defaultInt);
        }

        @Override
        public boolean putString(Context context, String key, String value) {
            if (TextUtils.isEmpty(key)) return false;
            SharedPreferences preferences = context.getSharedPreferences(name, Context.MODE_PRIVATE);
            SharedPreferences.Editor editor = preferences.edit();
            editor.putString(key, value);
            return editor.commit();
        }

        @Override
        public String getString(Context context, String key, String defaultStr) {
            if (TextUtils.isEmpty(key)) return defaultStr;
            SharedPreferences preferences = context.getSharedPreferences(name, Context.MODE_PRIVATE);
            return preferences.getString(key, defaultStr);
        }

        @Override
        public boolean putBooble(Context context, String key, boolean value) {
            if (TextUtils.isEmpty(key)) return false;
            SharedPreferences preferences = context.getSharedPreferences(name, Context.MODE_PRIVATE);
            SharedPreferences.Editor editor = preferences.edit();
            editor.putBoolean(key, value);
            return editor.commit();
        }

        @Override
        public boolean getBooble(Context context, String key, boolean defaultStr) {
            if (TextUtils.isEmpty(key)) return defaultStr;
            SharedPreferences preferences = context.getSharedPreferences(name, Context.MODE_PRIVATE);

            return preferences.getBoolean(key, defaultStr);
        }

        @Override
        public boolean putMap(Context context, String key, Map<String, String> value) {
            if (TextUtils.isEmpty(key)) return false;
            JSONObject object = new JSONObject();
            for (Map.Entry<String, String> entry : value.entrySet()) {
                try {
                    object.put(entry.getKey(), entry.getValue());
                } catch (JSONException e) {
                    return false;
                }
            }
            return putString(context, key, object.toString());
        }

        @Override
        public Map<String, String> getMap(Context context, String key) {
            String data = getString(context, key, "");
            if (TextUtils.isEmpty(data)) {
                return null;
            }
            Map<String, String> map = new HashMap<>();
            try {
                JSONObject jsonObject = new JSONObject(data);
                Iterator<String> sIterator = jsonObject.keys();
                while (sIterator.hasNext()) {
                    String mapKey = sIterator.next();
                    map.put(mapKey, jsonObject.getString(mapKey));
                }
                return map;
            } catch (JSONException e) {
                e.printStackTrace();
                return null;
            }
        }
    }

    interface PreferenceInterface {
        boolean putInt(Context context, String key, int value);

        int getInt(Context context, String key, int defaultInt);

        boolean putString(Context context, String key, String value);

        String getString(Context context, String key, String defaultStr);

        boolean putBooble(Context context, String key, boolean value);

        boolean getBooble(Context context, String key, boolean defaultStr);

        boolean putMap(Context context, String key, Map<String, String> value);

        Map<String, String> getMap(Context context, String key);
    }
}
