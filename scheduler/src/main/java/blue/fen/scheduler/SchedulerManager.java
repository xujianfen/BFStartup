package blue.fen.scheduler;

import android.database.Observable;
import android.text.TextUtils;

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

import blue.fen.scheduler.block.Block;
import blue.fen.scheduler.check.CycleCheck;
import blue.fen.scheduler.check.SccCheck;
import blue.fen.scheduler.check.SimpleCheck;
import blue.fen.scheduler.distributor.IDispatcher;
import blue.fen.scheduler.listener.task.ITaskLifecycleObserver;
import blue.fen.scheduler.listener.task.SchedulerListener;
import blue.fen.scheduler.listener.task.TaskLifecycleRegistry;
import blue.fen.scheduler.listener.project.ProjectLifecycleObservable;
import blue.fen.scheduler.priority.PriorityAdapter;
import blue.fen.scheduler.scheduler.ISchedulerTask;
import blue.fen.scheduler.scheduler.SchedulerTask;

/**
 * <p>创建时间：2023/12/31 （星期日｝</p>
 * <p>作者： blue_fen</p>
 * <p>描述：任务调度者</p>
 */
class SchedulerManager implements BFScheduler, SchedulerListener {
    private final Block block;

    private final TaskFlow flow;

    private final TaskLifecycleRegistry lifecycle;

    private final String name;

    private final AtomicBoolean alreadyCalled = new AtomicBoolean(false);

    @Override
    public String name() {
        return name;
    }

    public static class Factory implements BFScheduler.Factory {
        @Override
        public SchedulerManager factory(BFTaskConfig config, BFProject project) {
            SchedulerManager flow = new SchedulerManager(config, project);

            flow.lifecycle.registerObserver(ProjectLifecycleObservable.taskAdapter(flow.name));
            Block block = config.block;
            block.setName(flow.name());
            block.setTimeout(config.awaitOutTime);
            flow.generateSource(config, project);
            block.tryGenerateLatch();

            if (config.autoCheckDependencies) {
                flow.checkDependencies(config, project);
            }
            return flow;
        }

        @Override
        public Node createNode(ISchedulerTask task) {
            return new SchedulerManager.TaskNode(task);
        }
    }

    private SchedulerManager(BFTaskConfig config, BFProject project) {
        block = config.block;
        flow = config.flowProvider.get(config, project, this);
        name = TextUtils.isEmpty(project.name) ? String.valueOf(hashCode()) : project.name;
        lifecycle = new TaskLifecycleRegistry();
    }

    private void generateSource(BFTaskConfig config, BFProject project) {
        Map<String, Node> mapping = project.mapping;
        for (Node n : mapping.values()) {
            TaskNode node = (TaskNode) n;
            block.tryIncrement(n);
            List<String> dependencies = node.dependencies();
            node.refreshDependentSize(dependencies);
            clampPriority(flow.priorityAdapter(), node);
            if (node.isReady()) {
                flow.addReady(node);
            } else {
                for (String dependentName : dependencies) {
                    TaskNode dependentNode = (TaskNode) mapping.get(dependentName);
                    if (config.excludeSelfCircular && dependentName.equals(node.name())) {
                        node.dependenciesComplete();
                        if (node.isReady()) {
                            flow.addReady(node);
                        }
                        continue;
                    }
                    if (dependentNode != null) {
                        dependentNode.addSuccessors(node);
                    } else {
                        throw new IllegalArgumentException("不存在的依赖：" + dependentName);
                    }
                }
            }
        }
    }

    private void clampPriority(PriorityAdapter priority, TaskNode node) {
        int p = node.priority;
        if (p >= BFConfig.PRIORITY_DEFAULT && p < BFConfig.PRIORITY_MAX) {
            p = priority.defaultPriority() + (p - BFConfig.PRIORITY_DEFAULT);
        } else if (node.task instanceof SchedulerTask) {
            SchedulerTask schedulerTask = (SchedulerTask) node.task;
            if (schedulerTask.relativePriority()) {
                p += priority.defaultPriority();
            }
        }
        p = priority.clampPriority(p);
        node.priority = p;
    }

    private void checkDependencies(BFTaskConfig config, BFProject project) {
        Map<String, Node> mapping = project.mapping;
        switch (config.checkCycleMode) {
            case BFConfig.NO_CHECK:
            case BFConfig.DEFAULT_CHECK:
                break;
            case BFConfig.SIMPLE_CHECK:
                SimpleCheck.checkDependencies(flow);
                break;
            case BFConfig.SCC_CHECK:
                SccCheck.checkDependencies(mapping);
                break;
            case BFConfig.CYCLE_CHECK:
                CycleCheck.checkDependencies(mapping);
                break;
        }
    }

    @Override
    public Observable<ITaskLifecycleObserver> getLifecycle() {
        return lifecycle;
    }

    @Override
    public void execute() {
        if (alreadyCalled.weakCompareAndSet(false, true)) flow.execute();
        else throw new IllegalStateException("工作被重复执行！！！");
    }

    @Override
    public void enqueue() {
        if (alreadyCalled.weakCompareAndSet(false, true)) flow.enqueue();
        else throw new IllegalStateException("工作被重复执行！！！");
    }

    @Override
    public void destroy() {
        flow.clean();
    }

    @Override
    public void clean() {
        lifecycle.clean();
        block.clean();
    }

    @Override
    public void before(IDispatcher distributor) {
        lifecycle.before(distributor);
    }

    @Override
    public void after(IDispatcher distributor) {
        lifecycle.after(distributor);
    }

    @Override
    public void throwable(IDispatcher distributor, Throwable throwable) {
        lifecycle.throwable(distributor, throwable);
    }

    @Override
    public void finish() {
        lifecycle.finish();
    }

    /**
     * 不同于{@linkplain SchedulerManager#after}，这里会影响分发过程，只能用于内部处理
     */
    @Override
    public void complete(Node node) {
        block.tryDecrement(node);
    }

    @Override
    public void await() {
        if (flow.active()) block.await();
    }

    private static class TaskNode extends BFScheduler.Node.Impl {
        private ISchedulerTask task;

        /**
         * 后继任务列表
         */
        private Set<Node> successors;

        int priority;

        private int dependentSize;

        private TaskNode(ISchedulerTask task) {
            this.task = task;
            this.priority = task.priority();
        }

        void addSuccessors(TaskNode node) {
            if (successors == null) {
                successors = new LinkedHashSet<>();
            }
            successors.add(node);
        }

        @Override
        public Set<Node> getSuccessors() {
            return successors;
        }

        @Override
        public List<String> dependencies() {
            return task.dependencies();
        }

        @Override
        public void dependenciesComplete() {
            dependentSize--;
        }

        public void refreshDependentSize(List<String> dependencies) {
            dependentSize = dependencies == null ? 0 : dependencies.size();
        }

        @Override
        public boolean isReady() {
            return dependentSize == 0;
        }

        @Override
        public int dependentSize() {
            return dependentSize;
        }

        @Override
        public int priority() {
            return priority;
        }

        @Override
        public String name() {
            return task.name();
        }

        @Override
        public ISchedulerTask task() {
            return task;
        }

        @NonNull
        @Override
        public String toString() {
            return "[" + name() + "]";
        }

        /**
         * @noinspection unused
         */
        @NonNull
        public String detailString() {
            List<String> names = Collections.emptyList();
            if (successors != null) {
                names = new ArrayList<>(successors.size());
                for (Node node : successors) {
                    names.add(node.name());
                }
            }
            return "[" + name() + "] p=" + priority +
                    "(" + hashCode() + ")" + "  successors= " + names;
        }

        @Override
        public void clean() {
            task = null;
            priority = 0;
            dependentSize = 0;
            if (successors != null) {
                successors.clear();
                successors = null;
            }
            super.clean();
        }
    }
}
