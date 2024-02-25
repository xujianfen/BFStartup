package blue.fen.scheduler.listener.task;

import blue.fen.scheduler.listener.task.ITaskLifecycleObserver;
import blue.fen.scheduler.scheduler.ISchedulerTask;

/**
 * <p>创建时间：2024/01/01 （星期一｝</p>
 * <p>作者： blue_fen</p>
 * <p>描述：</p>
 */
public abstract class ATaskLifecycleObserver implements ITaskLifecycleObserver {
    @Override
    public void finish() {

    }

    @Override
    public void before(ISchedulerTask task) {

    }

    @Override
    public void after(ISchedulerTask task) {

    }

    @Override
    public void error(ISchedulerTask task, Throwable throwable) {

    }

    @Override
    public void handleError(ISchedulerTask task, Exception e) {
    }
}
