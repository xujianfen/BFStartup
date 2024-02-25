package blue.fen.scheduler;

import blue.fen.scheduler.block.Block;
import blue.fen.scheduler.distributor.BFTaskExecutor;
import blue.fen.scheduler.distributor.IDistributor;
import blue.fen.scheduler.priority.PriorityProvider;
import blue.fen.scheduler.BFConfig.CheckCycleMode;

/**
 * <p>创建时间：2023/12/31 （星期日｝</p>
 * <p>作者： blue_fen</p>
 * <p>描述：项目任务配置信息</p>
 *
 * @noinspection unused
 */
public class   BFTaskConfig {
    /**
     * 该值为true时，会在初始化结束时，检查循环依赖，默认为true
     */
    boolean autoCheckDependencies = true;

    /**
     * 是否排除自循环，默认为true
     */
    boolean excludeSelfCircular = true;

    /**
     * 循环依赖检查模式，优先级大于{@linkplain BFConfig}的环检查模式
     */
    @CheckCycleMode
    int checkCycleMode;

    /**
     * 等待超时时间，单位微妙
     */
    int awaitOutTime;

    PriorityProvider priorityProvider;

    IDistributor distributor;

    Block block;

    BFTaskExecutor executor;

    TaskFlow.Provider flowProvider;

    /**
     * @see BFTaskConfig#autoCheckDependencies
     */
    public BFTaskConfig autoCheckDependencies(boolean checkDependencies) {
        this.autoCheckDependencies = checkDependencies;
        return this;
    }

    /**
     * @see BFTaskConfig#excludeSelfCircular
     */
    public BFTaskConfig excludeSelfCircular(boolean excludeSelfCircular) {
        this.excludeSelfCircular = excludeSelfCircular;
        return this;
    }

    /**
     * @see BFTaskConfig#checkCycleMode
     */
    public BFTaskConfig checkCycleMode(@CheckCycleMode int checkCycleMode) {
        this.checkCycleMode = checkCycleMode;
        return this;
    }

    /**
     * 设置任务超时时间
     */
    public BFTaskConfig awaitOutTime(int awaitOutTime) {
        this.awaitOutTime = awaitOutTime;
        return this;
    }

    /**
     * 自定义优先级队列
     */
    public BFTaskConfig priorityProvider(PriorityProvider priorityProvider) {
        this.priorityProvider = priorityProvider;
        return this;
    }

    /**
     * 自定义任务分发器
     */
    public BFTaskConfig distributor(IDistributor distributor) {
        this.distributor = distributor;
        return this;
    }

    /**
     * 自定义任务阻塞器
     */
    public BFTaskConfig block(Block block) {
        this.block = block;
        return this;
    }

    /**
     * 自定义任务线程池
     */
    public BFTaskConfig executor(BFTaskExecutor executor) {
        this.executor = executor;
        return this;
    }

    /**
     * 自定义任务流水线
     */
    public BFTaskConfig flowProvider(TaskFlow.Provider flowProvider) {
        this.flowProvider = flowProvider;
        return this;
    }

    public BFTaskConfig copy(BFTaskConfig config) {
        if(config == null) return this;
        autoCheckDependencies = config.autoCheckDependencies;
        excludeSelfCircular = config.excludeSelfCircular;
        checkCycleMode = config.checkCycleMode;
        awaitOutTime = config.awaitOutTime;
        priorityProvider = config.priorityProvider;
        distributor = config.distributor;
        executor = config.executor;
        flowProvider = config.flowProvider;
        return this;
    }

    public BFScheduler.Build commit() {
        throw new UnsupportedOperationException("只有内部生成的BFTaskConfig才可以调用commit方法");
    }

    static class BFTaskConfigBridge extends BFTaskConfig {
        private final BFScheduler.Build task;

        BFTaskConfigBridge(BFScheduler.Build task) {
            this.task = task;
        }

        public BFScheduler.Build commit() {
            configDefault();
            return task.config(this);
        }

        private void configDefault() {
            block = Block.DEFAULT.factory(block);
            distributor = IDistributor.DEFAULT.factory(distributor);
            priorityProvider = PriorityProvider.DEFAULT.factory(priorityProvider);
            flowProvider = TaskFlow.Provider.DEFAULT.factory(flowProvider);
            executor = BFTaskExecutor.DEFAULT.factory(executor);
            checkCycleMode = getCycleMode();
        }

        @CheckCycleMode
        private int getCycleMode() {
            if (checkCycleMode == 0) { //默认值为0，是为了静态值不为DEFAULT_CHECK时，允许设置为DEFAULT_CHECK
                checkCycleMode = BFConfig.checkCycleMode();
            }
            if (checkCycleMode == BFConfig.DEFAULT_CHECK) {
                checkCycleMode = BFConfig.isDebug() ? BFConfig.CYCLE_CHECK : BFConfig.NO_CHECK;
            }
            return checkCycleMode;
        }
    }
}
