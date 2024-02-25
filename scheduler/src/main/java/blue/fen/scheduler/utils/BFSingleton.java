package blue.fen.scheduler.utils;

/**
 * <p>创建时间：2023/12/31 （星期日｝</p>
 * <p>作者： blue_fen</p>
 * <p>描述：摘取自{@link android.util.Singleton}</p>
 */
public abstract class BFSingleton<T> {
    public BFSingleton() {
    }

    private T mInstance;

    protected abstract T create();

    public final T get() {
        synchronized (this) {
            if (mInstance == null) {
                mInstance = create();
            }
            return mInstance;
        }
    }
}

