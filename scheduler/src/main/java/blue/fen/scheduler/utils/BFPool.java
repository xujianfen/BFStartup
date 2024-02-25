package blue.fen.scheduler.utils;

import androidx.annotation.NonNull;
import androidx.core.util.Pools;

/**
 * 创建时间：2023/12/31 （星期日｝
 * 作者： blue_fen
 * 描述：
 */
public class BFPool<T> implements Pools.Pool<T> {
    private final Object[] mPool;

    private int mPoolSize;

    public BFPool(int maxPoolSize) {
        if (maxPoolSize <= 0) {
            throw new IllegalArgumentException("The max pool size must be > 0");
        }
        mPool = new Object[maxPoolSize];
    }

    @Override
    @SuppressWarnings("unchecked")
    public T acquire() {
        if (mPoolSize > 0) {
            final int lastPooledIndex = mPoolSize - 1;
            T instance = (T) mPool[lastPooledIndex];
            mPool[lastPooledIndex] = null;
            mPoolSize--;
            return instance;
        }
        return null;
    }

    @Override
    public boolean release(@NonNull T instance) {
        if (isInPool(instance)) {
            throw new IllegalStateException("Already in the pool!");
        }
        if (mPoolSize < mPool.length) {
            mPool[mPoolSize] = instance;
            mPoolSize++;
            return true;
        }
        return false;
    }

    private boolean isInPool(@NonNull T instance) {
        for (int i = 0; i < mPoolSize; i++) {
            if (mPool[i] == instance) {
                return true;
            }
        }
        return false;
    }
}
