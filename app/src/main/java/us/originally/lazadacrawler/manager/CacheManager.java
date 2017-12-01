package us.originally.lazadacrawler.manager;

import android.content.Context;
import android.content.SharedPreferences;

import com.google.gson.Gson;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;

import us.originally.lazadacrawler.BaseApplication;


public class CacheManager {

    private static final String SHARED_PREF_CACHE_DATA = "SHARED_PREF_CACHE_DATA";
    private static final String KEY_CREATED_TIMESTAMP = "_created_at";

    public static SharedPreferences getSharedPreferences() {
        Context ctx = BaseApplication.getAppContext();
        if (ctx == null)
            return null;
        SharedPreferences settings = ctx.getSharedPreferences(SHARED_PREF_CACHE_DATA, 0);
        return settings;
    }

    //*************************************************************************
    // List
    //*************************************************************************

    public static void saveListCacheData(final String key, ArrayList list) {
        saveObjectCacheData(key, list);
    }

    public static void removeListCacheData(final String key) {
        removeObjectCacheData(key);
    }

    public static <T> ArrayList<T> getListCacheData(final String key, Type type, int expiry_minutes) {
        if (key == null)
            return null;
        if (expiry_minutes <= 0)
            return getListCacheData(key, type);

        //When was it created
        long createdTimestamp = getSharedPreferences().getLong(key + KEY_CREATED_TIMESTAMP, 0);
        if (createdTimestamp <= 0)
            return null;

        //Not expired yet
        long secondDelta = (System.currentTimeMillis() - createdTimestamp) / 1000;
        if (secondDelta < expiry_minutes * 60)
            return getListCacheData(key, type);

        //Cache miss
        return null;
    }

    public static <T> ArrayList<T> getListCacheData(final String key, Type type) {
        if (key == null)
            return null;
        SharedPreferences settings = getSharedPreferences();
        if (settings == null)
            return null;

        String jsonString = settings.getString(key, null);
        if (jsonString == null || jsonString.length() <= 0)
            return null;

        Gson gson = new Gson();
        Object object = null;

        try {
            object = gson.fromJson(jsonString, type);

        } catch (Exception e) {
            String message = e.getMessage();
            object = null;
        }

        final ArrayList<T> finalObject = (ArrayList<T>) object;
        return finalObject;
    }

    public static <T> HashMap<String, T> getHashMapCacheData(final String key, Type type) {
        if (key == null)
            return null;
        SharedPreferences settings = getSharedPreferences();
        if (settings == null)
            return null;

        String jsonString = settings.getString(key, null);
        if (jsonString == null || jsonString.length() <= 0)
            return null;

        Gson gson = new Gson();
        Object object = null;

        try {
            object = gson.fromJson(jsonString, type);

        } catch (Exception e) {
            String message = e.getMessage();
            object = null;
        }

        final HashMap<String, T> finalObject = (HashMap<String, T>) object;
        return finalObject;
    }

    //*************************************************************************
    // String
    //*************************************************************************

    public static void saveStringCacheData(final String key, String data) {
        if (key == null || data == null)
            return;

        SharedPreferences settings = getSharedPreferences();
        if (settings == null)
            return;
        SharedPreferences.Editor editor = settings.edit();
        if (editor == null)
            return;

        editor.putString(key, data);
        editor.commit();
    }

    public static String getStringCacheData(String key) {
        if (key == null)
            return null;

        SharedPreferences settings = getSharedPreferences();
        if (settings == null)
            return null;

        return settings.getString(key, null);
    }

    //*************************************************************************
    // Object
    //*************************************************************************

    public static void saveObjectCacheData(final String key, Object object) {
        if (key == null || object == null)
            return;

        SharedPreferences settings = getSharedPreferences();
        if (settings == null)
            return;
        SharedPreferences.Editor editor = settings.edit();
        if (editor == null)
            return;

        //Serialize the list
        Gson mJson = new Gson();
        String jsonString = null;
        try {
            jsonString = mJson.toJson(object);
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (jsonString == null)
            return;

        //Save it & the timestamp it was save
        if (jsonString.length() > 0) {
            editor.putString(key, jsonString);
            editor.putLong(key + KEY_CREATED_TIMESTAMP, System.currentTimeMillis());
        }

        editor.commit();
    }

    public static void removeObjectCacheData(final String key) {
        if (key == null)
            return;
        SharedPreferences settings = getSharedPreferences();
        if (settings == null)
            return;
        SharedPreferences.Editor editor = settings.edit();
        if (editor == null)
            return;

        editor.remove(key);
        editor.remove(key + KEY_CREATED_TIMESTAMP);
        editor.commit();
    }

    public static <T extends Object> T getObjectCacheData(final String key, Class<T> type, int expiry_minutes) {
        if (key == null)
            return null;
        if (expiry_minutes <= 0)
            return getObjectCacheData(key, type);

        //When was it created
        long createdTimestamp = getSharedPreferences().getLong(key + KEY_CREATED_TIMESTAMP, 0);
        if (createdTimestamp <= 0)
            return null;

        //Not expired yet
        long secondDelta = (System.currentTimeMillis() - createdTimestamp) / 1000;
        if (secondDelta < expiry_minutes * 60)
            return getObjectCacheData(key, type);

        //Cache miss
        return null;
    }

    public static <T extends Object> T getObjectCacheData(final String key, Class<T> type) {
        if (key == null)
            return null;
        SharedPreferences settings = getSharedPreferences();
        if (settings == null)
            return null;

        String jsonString = settings.getString(key, null);
        if (jsonString == null || jsonString.length() <= 0)
            return null;

        Gson gson = new Gson();
        Object object = null;

        try {
            object = gson.fromJson(jsonString, type);

        } catch (Exception e) {
            object = null;
            e.printStackTrace();
        }
        if (object == null)
            return null;

        return type.cast(object);
    }


}
