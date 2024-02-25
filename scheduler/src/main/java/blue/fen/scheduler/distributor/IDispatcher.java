package blue.fen.scheduler.distributor;

import blue.fen.scheduler.BFScheduler;
import blue.fen.scheduler.TaskFlow;
import blue.fen.scheduler.scheduler.ISchedulerTask;
import blue.fen.scheduler.scheduler.ITaskProvider;

/**
 * <p>创建时间：2023/12/31 （星期日｝</p>
 * <p>作者： blue_fen</p>
 * <p>描述：任务调度接口</p>
 */
public interface IDispatcher extends ITaskProvider {
    TaskFlow flow();

    /**
     * 直接执行
     */
    void perform();

    /**
     * 派遣到对应线程执行
     */
    void dispatch();

    BFScheduler.Node taskNode();

    @Override
    default ISchedulerTask task() {
        return taskNode().task();
    }

    @Override
    default String name() {
        return flow().name();
    }

    boolean isAsync();
}
