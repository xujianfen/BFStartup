package blue.fen.scheduler.listener.project;

import blue.fen.scheduler.scheduler.ISchedulerTask;

/**
 * <p>创建时间：2024/02/17 （星期六｝</p>
 * <p>作者： blue_fen</p>
 * <p>描述：</p>
 */
public abstract class AProjectLifecycleObserver implements IProjectLifecycleObserver {
    @Override
    public void prepare(String project) {

    }

    @Override
    public void finish(String project) {

    }

    @Override
    public void before(String project, ISchedulerTask task) {

    }

    @Override
    public void after(String project, ISchedulerTask task) {

    }

    @Override
    public void error(String project, ISchedulerTask task, Throwable throwable) {

    }

    @Override
    public void handleError(String project, ISchedulerTask task, Exception e) {

    }
}
