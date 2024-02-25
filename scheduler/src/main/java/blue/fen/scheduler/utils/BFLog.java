package blue.fen.scheduler.utils;

import android.util.Log;

import blue.fen.scheduler.BFConfig;

/**
 * <p>创建时间：2023/12/31 （星期日｝</p>
 * <p>作者： blue_fen</p>
 * <p>描述：</p>
 *
 * @noinspection unused
 */
public class BFLog {
    private final static String TAG = "BFScheduler";

    public static boolean isDebug() {
        return BFConfig.isDebug();
    }

    private static String handlePrefix(String prefix) {
        return prefix == null ? "" : prefix + "：";
    }

    public static void d(Object msg) {
        d(null, msg);
    }

    public static void d(String prefix, Object msg) {
        if (isDebug()) {
            Log.d(TAG, handlePrefix(prefix) + msg);
        }
    }

    public static void w(Object msg) {
        w(null, msg);
    }

    public static void w(String prefix, Object msg) {
        if (isDebug()) {
            Log.w(TAG, handlePrefix(prefix) + msg);
        }
    }

    public static void err(Object obj) {
        if (isDebug()) {
            System.err.println(obj);
        }
    }

    public static void err(String msg) {
        err(null, msg, null);
    }

    public static void err(String msg, Throwable e) {
        err(null, msg, e);
    }

    public static void err(String prefix, String msg) {
        err(prefix, msg, null);
    }

    public static void err(String prefix, String msg, Throwable e) {
        if (isDebug()) {
            if (e != null) e.printStackTrace();
            Log.e(TAG, handlePrefix(prefix) + msg);
        }
    }
}
