package blue.fen.scheduler.listener.project;

import static blue.fen.scheduler.BFConfig.FLAG_DETAIL_LOG_MODE;
import static blue.fen.scheduler.BFConfig.FLAG_EXTRA_DATA_ENABLE;
import static blue.fen.scheduler.BFConfig.FLAG_SIMPLE_LOG_MODE;

import blue.fen.scheduler.BFConfig;
import blue.fen.scheduler.listener.task.ITaskLifecycleObserver;
import blue.fen.scheduler.scheduler.ISchedulerTask;
import blue.fen.scheduler.utils.BFClass;
import blue.fen.scheduler.utils.BFLog;
import blue.fen.scheduler.utils.BFObservable;
import blue.fen.scheduler.utils.BFSingleton;

/**
 * <p>创建时间：2024/01/01 （星期一｝</p>
 * <p>作者： blue_fen</p>
 * <p>描述：项目生命周期观察者</p>
 */
public class ProjectLifecycleObservable extends BFObservable<IProjectLifecycleObserver>
        implements IProjectLifecycleObserver {
    private static final BFSingleton<ProjectLifecycleObservable> sInstance =
            BFClass.singleton(ProjectLifecycleObservable::new);

    private ProjectLifecycleObservable() {
        if(BFConfig.isMatchFlag(FLAG_EXTRA_DATA_ENABLE)) {
            registerObserver(ExtraDataRecords.getInstance());
        }
        switch (BFConfig.logMode()) {
            case FLAG_DETAIL_LOG_MODE:
                registerObserver(DetailLogObserver.getInstance());
                break;
            case FLAG_SIMPLE_LOG_MODE:
                registerObserver(SimpleLogObserver.getInstance());
                break;
        }
    }

    /**
     * @noinspection UnusedReturnValue, unused
     */
    public boolean tryRegisterObserver(IProjectLifecycleObserver observer) {
        try {
            registerObserver(observer);
            return true;
        } catch (IllegalStateException e) {
            e.printStackTrace();
            return false;
        }
    }

    public static ProjectLifecycleObservable getInstance() {
        return sInstance.get();
    }

    @Override
    public void prepare(String project) {
        for (IProjectLifecycleObserver observer : mObservers) {
            try {
                observer.prepare(project);
            } catch (Exception e) {
                handleError(project, observer, null, e);
            }
        }
    }

    @Override
    public void finish(String project) {
        for (IProjectLifecycleObserver observer : mObservers) {
            try {
                observer.finish(project);
            } catch (Exception e) {
                handleError(project, observer, null, e);
            }
        }
    }

    @Override
    public void closeout(String project) {
        for (IProjectLifecycleObserver observer : mObservers) {
            try {
                observer.closeout(project);
            } catch (Exception e) {
                handleError(project, observer, null, e);
            }
        }
    }

    @Override
    public void before(String project, ISchedulerTask task) {
        for (IProjectLifecycleObserver observer : mObservers) {
            try {
                observer.before(project, task);
            } catch (Exception e) {
                handleError(project, observer, task, e);
            }
        }
    }

    @Override
    public void after(String project, ISchedulerTask task) {
        for (IProjectLifecycleObserver observer : mObservers) {
            try {
                observer.after(project, task);
            } catch (Exception e) {
                handleError(project, observer, task, e);
            }
        }
    }

    @Override
    public void error(String project, ISchedulerTask task, Throwable throwable) {
        for (IProjectLifecycleObserver observer : mObservers) {
            try {
                observer.error(project, task, throwable);
            } catch (Exception err) {
                handleError(project, observer, task, err);
            }
        }
    }

    @Override
    public void handleError(String project, ISchedulerTask task, Exception e) {
        for (IProjectLifecycleObserver observer : mObservers) {
            handleError(project, observer, task, e);
        }
    }

    private void handleError(
            String project,
            IProjectLifecycleObserver observer,
            ISchedulerTask task,
            Exception e
    ) {
        try {
            observer.handleError(project, task, e);
        } catch (Exception err) {
            BFLog.err(err);
            throw err;
        }
    }


    public static ITaskLifecycleObserver taskAdapter(String name) {
        Adapter adapter = new Adapter(name);
        adapter.prepare();
        return adapter;
    }

    public static class Adapter implements ITaskLifecycleObserver {
        private final String name;

        public Adapter(String name) {
            this.name = name;
        }

        public ProjectLifecycleObservable get() {
            return ProjectLifecycleObservable.getInstance();
        }

        public void prepare() {
            get().prepare(name);
        }

        @Override
        public void finish() {
            get().finish(name);
        }

        @Override
        public void before(ISchedulerTask task) {
            get().before(name, task);
        }

        @Override
        public void after(ISchedulerTask task) {
            get().after(name, task);
        }

        @Override
        public void error(ISchedulerTask task, Throwable throwable) {
            get().error(name, task, throwable);
        }

        @Override
        public void handleError(ISchedulerTask task, Exception e) {
            get().handleError(name, task, e);
        }

        @Override
        public void closeout() {
            get().closeout(name);
        }
    }
}
