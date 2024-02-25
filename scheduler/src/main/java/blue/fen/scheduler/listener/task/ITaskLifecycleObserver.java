package blue.fen.scheduler.listener.task;

import blue.fen.scheduler.scheduler.ISchedulerTask;

/**
 * <p>创建时间：2024/02/16 （星期五｝</p>
 * <p>作者： blue_fen</p>
 * <p>描述：任务生命周期观察者</p>
 */
public interface ITaskLifecycleObserver {
    void finish();

    void before(ISchedulerTask task);

    void after(ISchedulerTask task);

    /**
     * 任务执行过程中发生的异常会发送到该方法
     *
     * @param task      发生异常的任务
     * @param throwable 异常信息，若当前{@code task}的依赖任务发生的异常且异常未忽略，则{@code throwable=null}
     */
    void error(ISchedulerTask task, Throwable throwable);

    /**
     * 监听处理发生的异常会发送到该方法
     *
     * @param task 若为{@link ITaskLifecycleObserver#finish()}发生的异常，则{@code task=null}
     */
    void handleError(ISchedulerTask task, Exception e);

    /**
     * 项目收尾
     * 项目完成并处理完所有完成监听的时候调用
     */
    default void closeout() {
    }
}
