package blue.fen.scheduler;

import blue.fen.scheduler.distributor.BFTaskExecutor;
import blue.fen.scheduler.distributor.IDispatcher;
import blue.fen.scheduler.distributor.IDistributor;
import blue.fen.scheduler.listener.task.SchedulerListener;
import blue.fen.scheduler.priority.PriorityAdapter;
import blue.fen.scheduler.priority.PriorityQueue;
import blue.fen.scheduler.utils.BFClass;

/**
 * <p>创建时间：2023/12/31 （星期日｝</p>
 * <p>作者： blue_fen</p>
 * <p>描述：任务流水线</p>
 */
public interface TaskFlow extends BFClass.BFCleanable {
    String name();

    PriorityAdapter priorityAdapter();

    IDistributor getDistributor();

    PriorityQueue<?> readyQueue();

    Thread thread();

    BFTaskExecutor executor();

    int remaining();

    void addReady(BFScheduler.Node node);

    /**
     * 异步执行
     */
    void enqueue();

    /**
     * 同步执行
     */
    void execute();

    boolean active();

    void callComplete(IDispatcher dispatcher);

    void complete(BFScheduler.Node node);

    /**
     * 任务流水线提供者
     */
    interface Provider {
        TaskFlow get(BFTaskConfig config, BFProject project, SchedulerListener listen);

        BFClass.Default<Provider> DEFAULT = BFClass.defaultImpl(Impl::new);

        final class Impl implements Provider {
            @Override
            public TaskFlow get(BFTaskConfig config, BFProject project, SchedulerListener listener) {
                return new TaskFlowManager(config, project, listener);
            }
        }
    }
}
