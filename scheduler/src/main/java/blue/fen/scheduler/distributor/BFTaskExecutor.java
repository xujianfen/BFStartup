package blue.fen.scheduler.distributor;

import androidx.annotation.NonNull;

import blue.fen.scheduler.utils.BFClass;

/**
 * <p>创建时间：2024/01/01 （星期一｝</p>
 * <p>作者： blue_fen</p>
 * <p>描述：执行任务的线程池</p>
 */
public interface BFTaskExecutor {
    /**
     * 执行io线程
     * @param dispatcher 任务调度器
     * @param runnable 执行的方法
     */
    void executeOnDiskIO(@NonNull IDispatcher dispatcher, @NonNull Runnable runnable);

    /**
     * 执行main线程
     * @param dispatcher 任务调度器
     * @param runnable 执行的方法
     */
    void executeOnMainThread(@NonNull IDispatcher dispatcher, @NonNull Runnable runnable);

    BFClass.Default<BFTaskExecutor> DEFAULT = BFClass.defaultImpl(BFDefaultTaskExecutor::getInstance);
}
