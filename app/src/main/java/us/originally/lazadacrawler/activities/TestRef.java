package us.originally.lazadacrawler.activities;

import android.util.Log;

import com.furture.react.JSRef;

/**
 * Created by TuanAnh on 12/10/17.
 */

public class TestRef {

    public static void showData(JSRef ref) {
        int count = ((Number) ref.call("count")).intValue();
        for (int i = 0; i < count; i++) {
            Log.d("DataUtils", "Call JavaScript getItem Method :  " + ref.call("getItem", i));
        }
    }
}
