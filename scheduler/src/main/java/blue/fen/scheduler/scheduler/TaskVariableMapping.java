package blue.fen.scheduler.scheduler;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * <p>创建时间：2024/03/06 （Wednesday｝</p>
 * <p>作者： blue_fen</p>
 * <p>描述：任务变量映射器，将{@linkplain ISchedulerTask}的方法映射为变量</p>
 *
 * @noinspection unused
 */
public abstract class TaskVariableMapping implements ISchedulerTask {
    protected String name;
    protected List<String> dependencies;
    protected Integer priority;
    protected Task task;
    protected Boolean isBackground;
    protected Boolean enableThreadPriority;
    protected Integer threadPriority;
    protected Boolean needBlock;
    protected Boolean isAsync;
    protected Boolean manualDispatch;
    protected Boolean ignoreThrow;

    public TaskVariableMapping(Builder builder) {
        name = builder.name;
        dependencies = builder.dependencies;
        task = builder.task;
        isBackground = builder.isBackground;
        enableThreadPriority = builder.enableThreadPriority;
        needBlock = builder.needBlock;
        manualDispatch = builder.manualDispatch;
        ignoreThrow = builder.ignoreThrow;
        isAsync = builder.isAsync;
        priority = builder.priority;
        threadPriority = builder.threadPriority;
    }

    @Override
    public void perform(String projectName,
                        @NonNull ParamProvider param,
                        @Nullable TaskNotifier notifier
    ) throws Exception {
        if (task != null) {
            task.execute(projectName, name, param, notifier);
        }
    }

    public interface Task {
        void execute(String projectName,
                     String taskName,
                     @NonNull ParamProvider param,
                     @Nullable TaskNotifier notifier
        ) throws Exception;
    }

    @Override
    public String name() {
        return name;
    }

    @Override
    public List<String> dependencies() {
        return dependencies;
    }

    @Override
    public int priority() {
        return priority;
    }

    @Override
    public boolean isBackground() {
        return isBackground;
    }

    @Override
    public boolean enableThreadPriority() {
        return enableThreadPriority;
    }

    @Override
    public int threadPriority() {
        return threadPriority;
    }

    @Override
    public boolean needBlock() {
        return needBlock;
    }

    @Override
    public boolean isAsync() {
        return isAsync;
    }

    @Override
    public boolean manualDispatch() {
        return manualDispatch;
    }

    @Override
    public boolean ignoreThrow() {
        return ignoreThrow;
    }

    protected final String nameDefault() {
        return ISchedulerTask.super.name();
    }

    protected final boolean isBackgroundDefault() {
        return ISchedulerTask.super.isBackground();
    }

    protected final List<String> dependenciesDefault() {
        return ISchedulerTask.super.dependencies();
    }

    protected final int priorityDefault() {
        return ISchedulerTask.super.priority();
    }

    protected final boolean enableThreadPriorityDefault() {
        return ISchedulerTask.super.enableThreadPriority();
    }

    protected final int threadPriorityDefault() {
        return ISchedulerTask.super.threadPriority();
    }

    protected final boolean needBlockDefault() {
        return ISchedulerTask.super.needBlock();
    }

    protected final boolean isAsyncDefault() {
        return ISchedulerTask.super.isAsync();
    }

    protected final boolean manualDispatchDefault() {
        return ISchedulerTask.super.manualDispatch();
    }

    protected final boolean ignoreThrowDefault() {
        return ISchedulerTask.super.ignoreThrow();
    }

    /**
     * @noinspection unchecked
     */
    public static abstract class Builder {
        protected String name;
        protected List<String> dependencies;
        protected Integer priority;
        protected Task task;
        protected Boolean isBackground;
        protected Boolean enableThreadPriority;
        protected Integer threadPriority;
        protected Boolean needBlock;
        protected Boolean isAsync;
        protected Boolean manualDispatch;
        protected Boolean ignoreThrow;

        abstract public TaskVariableMapping build();

        /**
         * 初始化所有封装类型变量
         */
        protected void initializeWrappedFields() {
            priority = 0;
            isBackground = false;
            enableThreadPriority = false;
            threadPriority = 0;
            needBlock = false;
            isAsync = false;
            manualDispatch = false;
            ignoreThrow = false;
        }

        /**
         * @see ISchedulerTask#name()
         */
        public <T extends Builder> T name(String name) {
            this.name = name;
            return (T) this;
        }

        /**
         * 添加依赖项
         */
        public <T extends Builder> T dependencies(List<String> dependencies) {
            this.dependencies = dependencies;
            return (T) this;
        }

        /**
         * 添加依赖项
         */
        public <T extends Builder> T dependencies(String... dependencies) {
            this.dependencies = new ArrayList<>(Arrays.asList(dependencies));
            return (T) this;
        }

        /**
         * 清空依赖项
         */
        public <T extends Builder> T clearDependencies() {
            if (dependencies != null) {
                dependencies.clear();
            }
            return (T) this;
        }

        /**
         * 绝对优先级
         *
         * @see ISchedulerTask#priority()
         */
        public <T extends Builder> T priority(int priority) {
            this.priority = priority;
            return (T) this;
        }

        /**
         * @see ISchedulerTask#perform(String, ParamProvider, TaskNotifier)
         */
        public <T extends Builder> T task(Task task) {
            this.task = task;
            return (T) this;
        }

        /**
         * @see ISchedulerTask#isBackground()
         */
        public <T extends Builder> T isBackground(boolean isBackground) {
            this.isBackground = isBackground;
            return (T) this;
        }

        /**
         * @see ISchedulerTask#enableThreadPriority()
         */
        public <T extends Builder> T enableThreadPriority(boolean enableThreadPriority) {
            this.enableThreadPriority = enableThreadPriority;
            return (T) this;
        }

        /**
         * @see ISchedulerTask#threadPriority()
         */
        public <T extends Builder> T threadPriority(int threadPriority) {
            this.threadPriority = threadPriority;
            return (T) this;
        }

        /**
         * @see ISchedulerTask#needBlock()
         */
        public <T extends Builder> T needBlock(boolean needBlock) {
            this.needBlock = needBlock;
            return (T) this;
        }

        /**
         * @see ISchedulerTask#isAsync()
         */
        public <T extends Builder> T isAsync(boolean isAsync) {
            this.isAsync = isAsync;
            return (T) this;
        }

        /**
         * @see ISchedulerTask#manualDispatch()
         */
        public <T extends Builder> T manualDispatch(boolean manualDispatch) {
            this.manualDispatch = manualDispatch;
            return (T) this;
        }

        /**
         * @see ISchedulerTask#ignoreThrow()
         */
        public <T extends Builder> T ignoreThrow(boolean ignoreThrow) {
            this.ignoreThrow = ignoreThrow;
            return (T) this;
        }
    }
}
