package blue.fen.scheduler.distributor;

import blue.fen.scheduler.BFScheduler;
import blue.fen.scheduler.TaskFlow;
import blue.fen.scheduler.listener.task.TaskListener;
import blue.fen.scheduler.utils.BFClass;

/**
 * <p>创建时间：2023/12/31 （星期日｝</p>
 * <p>作者： blue_fen</p>
 * <p>描述：任务分发器</p>
 */
public interface IDistributor {
    void execute(TaskFlow flow);

    void dispatch(IDispatcher dispatcher);

    void complete(IDispatcher dispatcher);

    IDispatcher createDispatcher(TaskFlow flow, TaskListener listener, BFScheduler.Node node);

    void remove(TaskFlow flow);

    BFClass.Default<IDistributor> DEFAULT = BFClass.defaultImpl(DistributorHandler::getInstance);
}
