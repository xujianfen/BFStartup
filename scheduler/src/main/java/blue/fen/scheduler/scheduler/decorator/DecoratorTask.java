package blue.fen.scheduler.scheduler.decorator;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.List;

import blue.fen.scheduler.scheduler.ISchedulerTask;
import blue.fen.scheduler.scheduler.ParamProvider;
import blue.fen.scheduler.scheduler.TaskNotifier;

/**
 * <p>创建时间：2024/03/02 （星期六｝</p>
 * <p>作者： blue_fen</p>
 * <p>描述：装饰任务，用于扩展任务的功能</p>
 */
public abstract class DecoratorTask implements ISchedulerTask {
    private final ISchedulerTask task;

    public DecoratorTask(ISchedulerTask task) {
        this.task = task;
    }

    public ISchedulerTask task() {
        return task;
    }

    @Override
    public String name() {
        return task.name();
    }

    @Override
    public void perform(
            String projectName,
            @NonNull ParamProvider param,
            @Nullable TaskNotifier notifier
    ) throws Exception {
        task.perform(projectName, param, notifier);
    }

    @Override
    public boolean isBackground() {
        return task.isBackground();
    }

    @Override
    public List<String> dependencies() {
        return task.dependencies();
    }

    @Override
    public int priority() {
        return task.priority();
    }

    @Override
    public boolean enableThreadPriority() {
        return task.enableThreadPriority();
    }

    @Override
    public int threadPriority() {
        return task.threadPriority();
    }

    @Override
    public boolean needBlock() {
        return task.needBlock();
    }

    @Override
    public boolean isAsync() {
        return task.isAsync();
    }

    @Override
    public boolean manualDispatch() {
        return task.manualDispatch();
    }

    @Override
    public boolean ignoreThrow() {
        return task.ignoreThrow();
    }
}
