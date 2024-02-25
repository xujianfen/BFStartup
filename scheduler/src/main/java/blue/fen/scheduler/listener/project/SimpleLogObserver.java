package blue.fen.scheduler.listener.project;

import blue.fen.scheduler.scheduler.ISchedulerTask;
import blue.fen.scheduler.utils.BFClass;
import blue.fen.scheduler.utils.BFLog;
import blue.fen.scheduler.utils.BFSingleton;

/**
 * <p>创建时间：2024/02/16 （星期五｝</p>
 * <p>作者： blue_fen</p>
 * <p>描述：简单日志</p>
 */
public class SimpleLogObserver implements IProjectLifecycleObserver {
    private static final BFSingleton<SimpleLogObserver> sInstance =
            BFClass.singleton(SimpleLogObserver::new);

    private SimpleLogObserver() {
    }

    public static SimpleLogObserver getInstance() {
        return sInstance.get();
    }

    @Override
    public void prepare(String project) {
        print(Method.PREPARE, project, null);
    }

    @Override
    public void finish(String project) {
        print(Method.FINISH, project, null);
    }

    @Override
    public void before(String project, ISchedulerTask task) {
        print(Method.BEFORE, project, task);
    }

    @Override
    public void after(String project, ISchedulerTask task) {
        print(Method.AFTER, project, task);
    }

    @Override
    public void error(String project, ISchedulerTask task, Throwable throwable) {
        err(Method.ERROR, project, task, throwable);
    }

    @Override
    public void handleError(String project, ISchedulerTask task, Exception e) {
        err(Method.HANDLE_ERROR, project, task, e);
    }

    private void print(Method method, String project, ISchedulerTask task) {
        BFLog.d("SimpleLog[" + project + "] " + method.name() +
                (task != null ? "(" + task.name() + ")" : ""));
    }

    private void err(Method method, String project, ISchedulerTask task, Throwable throwable) {
        BFLog.err("SimpleLog[" + project + "] " + method.name() +
                "(" + task.name() + ") throw " + throwable, throwable);
    }
}
