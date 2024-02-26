package blue.fen.demo.executoasr;

import androidx.annotation.NonNull;

import blue.fen.scheduler.distributor.BFDefaultTaskExecutor;
import blue.fen.scheduler.distributor.BFTaskExecutor;
import blue.fen.scheduler.distributor.IDispatcher;
import blue.fen.scheduler.utils.BFLog;

/**
 * <p>创建时间：2024/02/27 （星期二｝</p>
 * <p>作者： blue_fen</p>
 * <p>描述：</p>
 */
public class MyExecutoasr implements BFTaskExecutor {
    @Override
    public void executeOnDiskIO(@NonNull IDispatcher dispatcher, @NonNull Runnable runnable) {
        BFLog.d("我是MyExecutoasr的executeOnDiskIO方法");
        BFDefaultTaskExecutor.getInstance().executeOnDiskIO(dispatcher, runnable);
    }

    @Override
    public void executeOnMainThread(@NonNull IDispatcher dispatcher, @NonNull Runnable runnable) {
        BFLog.d("我是MyExecutoasr的executeOnMainThread方法");
        BFDefaultTaskExecutor.getInstance().executeOnMainThread(dispatcher, runnable);
    }
}
