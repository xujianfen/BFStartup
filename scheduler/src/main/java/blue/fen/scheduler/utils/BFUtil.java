package blue.fen.scheduler.utils;

import android.os.Looper;

import java.util.Map;
import java.util.Objects;

/**
 * <p>创建时间：2023/12/31 （星期日｝</p>
 * <p>作者： blue_fen</p>
 * <p>描述：</p>
 */
public class BFUtil {
    /**
     * 摘取自{@link   Objects#requireNonNullElse(Object, Object)}
     */
    public static <T> T requireNonNullElse(T obj, T defaultObj) {
        return (obj != null) ? obj : Objects.requireNonNull(defaultObj, "defaultObj");
    }

    /**
     * 获取当前线程ID
     */
    public static long getThreadId() {
        return Thread.currentThread().getId();
    }

    /**
     * 判断目标线程是否为当前线程
     *
     * @param threadId 目标线程ID
     */
    public static boolean isCurrentThread(long threadId) {
        return getThreadId() == threadId;
    }

    /**
     * 判断目标线程是否为当前线程
     *
     * @param thread 目标线程
     */
    public static boolean isCurrentThread(Thread thread) {
        return Thread.currentThread().equals(thread);
    }

    /**
     * 判断当前线程是否为主线程
     */
    public static boolean isMainThread() {
        return Looper.getMainLooper() == Looper.myLooper();
    }

    public interface Function<T, R> {
        R apply(T t);
    }

    /**
     * 摘取自{@link   Map#computeIfAbsent(Object, java.util.function.Function)}
     */
    public static <K, V> V computeIfAbsent(Map<K, V> map,
                                           K key,
                                           Function<? super K, ? extends V> mappingFunction) {
        Objects.requireNonNull(mappingFunction);
        V v;
        if ((v = map.get(key)) == null) {
            V newValue;
            if ((newValue = mappingFunction.apply(key)) != null) {
                map.put(key, newValue);
                return newValue;
            }
        }
        return v;
    }
}
