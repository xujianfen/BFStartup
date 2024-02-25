package blue.fen.scheduler.distributor;

import android.annotation.SuppressLint;

import androidx.annotation.NonNull;


import androidx.arch.core.executor.ArchTaskExecutor;
import androidx.arch.core.executor.TaskExecutor;

import blue.fen.scheduler.utils.BFClass;
import blue.fen.scheduler.utils.BFSingleton;

/**
 * <p>创建时间：2024/01/01 （星期一｝</p>
 * <p>作者： blue_fen</p>
 * <p>描述：执行任务的默认线程池</p>
 */
public class BFDefaultTaskExecutor implements BFTaskExecutor {
    private static final BFSingleton<BFDefaultTaskExecutor> sInstance =
            BFClass.singleton(BFDefaultTaskExecutor::new);

    public static BFDefaultTaskExecutor getInstance() {
        return sInstance.get();
    }

    TaskExecutor executor;

    @SuppressLint("RestrictedApi")
    private BFDefaultTaskExecutor() {
        executor = ArchTaskExecutor.getInstance();
    }

    @SuppressLint("RestrictedApi")
    @Override
    public void executeOnDiskIO(@NonNull IDispatcher dispatcher, @NonNull Runnable runnable) {
        executor.executeOnDiskIO(runnable);
    }

    @SuppressLint("RestrictedApi")
    @Override
    public void executeOnMainThread(@NonNull IDispatcher dispatcher, @NonNull Runnable runnable) {
        executor.executeOnMainThread(runnable);
    }

    @SuppressLint("RestrictedApi")
    public void executeOnMainThread(@NonNull Runnable runnable) {
        executor.executeOnMainThread(runnable);
    }
}
