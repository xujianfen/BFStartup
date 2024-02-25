package blue.fen.scheduler.listener.task;

import blue.fen.scheduler.distributor.IDispatcher;
import blue.fen.scheduler.scheduler.ISchedulerTask;
import blue.fen.scheduler.utils.BFLog;
import blue.fen.scheduler.utils.BFObservable;

/**
 * <p>创建时间：2024/01/01 （星期一｝</p>
 * <p>作者： blue_fen</p>
 * <p>描述：任务生命周期管理者</p>
 */
public class TaskLifecycleRegistry extends BFObservable<ITaskLifecycleObserver> implements TaskListener {
    @Override
    public void before(IDispatcher distributor) {
        for (ITaskLifecycleObserver observer : mObservers) {
            try {
                observer.before(distributor.task());
            } catch (Exception e) {
                error(distributor.task(), observer, e);
            }
        }
    }

    @Override
    public void after(IDispatcher distributor) {
        for (ITaskLifecycleObserver observer : mObservers) {
            try {
                observer.after(distributor.task());
            } catch (Exception e) {
                error(distributor.task(), observer, e);
            }
        }
    }

    @Override
    public void throwable(IDispatcher distributor, Throwable throwable) {
        for (ITaskLifecycleObserver observer : mObservers) {
            try {
                observer.error(distributor.task(), throwable);
            } catch (Exception e) {
                error(distributor.task(), observer, e);
            }
        }
    }

    private void error(ISchedulerTask task, ITaskLifecycleObserver observer, Exception e) {
        try {
            observer.handleError(task, e);
        } catch (Exception err) {
            BFLog.err(err);
            throw err;
        }
    }

    @Override
    public void finish() {
        for (ITaskLifecycleObserver observer : mObservers) {
            try {
                observer.finish();
            } catch (Exception e) {
                error(null, observer, e);
            }
        }
        closeout();
    }

    private void closeout() {
        for (ITaskLifecycleObserver observer : mObservers) {
            try {
                observer.closeout();
            } catch (Exception e) {
                error(null, observer, e);
            }
        }
    }

    @Override
    public void clean() {
        mObservers.clear();
    }
}
