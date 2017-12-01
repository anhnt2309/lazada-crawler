package us.originally.lazadacrawler;

import android.app.Application;
import android.content.Context;

/**
 * Created by TuanAnh on 11/18/17.
 */

public class BaseApplication extends Application {

    private static Context context;

    public void onCreate() {
        super.onCreate();
        BaseApplication.context = getApplicationContext();
    }

    public static Context getAppContext() {
        return BaseApplication.context;
    }
}

