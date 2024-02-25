package blue.fen.scheduler.listener.project;

import blue.fen.scheduler.listener.task.ITaskLifecycleObserver;
import blue.fen.scheduler.scheduler.ISchedulerTask;

/**
 * <p>创建时间：2024/01/01 （星期一｝</p>
 * <p>作者： blue_fen</p>
 * <p>描述：项目生命周期观察者</p>
 */
public interface IProjectLifecycleObserver {
    void prepare(String project);

    void finish(String project);

    void before(String project, ISchedulerTask task);

    void after(String project, ISchedulerTask task);

    /**
     * 详见：{@linkplain ITaskLifecycleObserver#error(ISchedulerTask, Throwable)}
     *
     * @param project 项目名
     */
    void error(String project, ISchedulerTask task, Throwable throwable);

    /**
     * 监听处理发生的异常会发送到该方法
     *
     * @param task    若为{@link IProjectLifecycleObserver#prepare(String)}
     *                或者{@link IProjectLifecycleObserver#finish(String)}
     *                发生的异常，则{@code task=null}
     * @param project 项目名
     */
    void handleError(String project, ISchedulerTask task, Exception e);


    /**
     * 详见：{@linkplain ITaskLifecycleObserver#closeout()}
     */
    default void closeout(String project) {
    }

    enum Method {
        PREPARE,
        FINISH,
        BEFORE,
        AFTER,
        ERROR,
        HANDLE_ERROR;

        public boolean isError() {
            return ERROR == this || HANDLE_ERROR == this;
        }
    }
}
