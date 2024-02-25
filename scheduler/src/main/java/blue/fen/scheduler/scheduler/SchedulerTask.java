package blue.fen.scheduler.scheduler;

import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import blue.fen.scheduler.BFConfig;
import blue.fen.scheduler.utils.BFUtil;

/**
 * <p>创建时间：2023/12/31 （星期日｝</p>
 * <p>作者： blue_fen</p>
 * <p>描述：调度的任务</p>
 *
 * @noinspection unused
 */
public class SchedulerTask implements ISchedulerTask {
    private final String name;
    private final List<String> dependencies;
    private final int priority;
    private final boolean relativePriority;
    private final Task task;
    private final boolean isBackground;
    private final boolean enableThreadPriority;
    private final int threadPriority;
    private final boolean needBlock;
    private final boolean isAsync;
    private final boolean manualDispatch;
    private final boolean ignoreThrow;

    private SchedulerTask(Builder builder) {
        name = builder.name;
        dependencies = builder.dependencies;
        relativePriority = builder.relativePriority;
        task = builder.task;
        isBackground = builder.isBackground;
        enableThreadPriority = builder.enableThreadPriority;
        needBlock = builder.needBlock;
        manualDispatch = builder.manualDispatch;
        ignoreThrow = builder.ignoreThrow;

        isAsync = BFUtil.requireNonNullElse(builder.isAsync, isBackground);
        priority = BFUtil.requireNonNullElse(
                builder.priority,
                ISchedulerTask.super.priority()
        );
        threadPriority = BFUtil.requireNonNullElse(
                builder.threadPriority,
                ISchedulerTask.super.threadPriority()
        );
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

    public boolean relativePriority() {
        return relativePriority;
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

    public static Builder newBuilder() {
        return new Builder();
    }

    public static final class Builder {
        private String name;
        private List<String> dependencies;
        private Integer priority;
        private boolean relativePriority;
        private Task task;
        private boolean isBackground;
        private boolean enableThreadPriority;
        private Integer threadPriority;
        private boolean needBlock;
        private Boolean isAsync;
        private boolean manualDispatch;
        private boolean ignoreThrow;

        public Builder() {
            this.isBackground = true;
        }

        /**
         * @see ISchedulerTask#name()
         */
        public Builder name(String name) {
            this.name = name;
            return this;
        }

        /**
         * 添加依赖项
         */
        public Builder dependencies(List<String> dependencies) {
            this.dependencies = dependencies;
            return this;
        }

        /**
         * 添加依赖项
         */
        public Builder dependencies(String... dependencies) {
            this.dependencies = new ArrayList<>(Arrays.asList(dependencies));
            return this;
        }

        /**
         * 清空依赖项
         */
        public Builder clearDependencies() {
            if (dependencies != null) {
                dependencies.clear();
            }
            return this;
        }

        /**
         * 绝对优先级
         *
         * @see ISchedulerTask#priority()
         */
        public Builder priority(int priority) {
            this.priority = priority;
            return this;
        }

        /**
         * 相对优先级 <br/>
         * 相对优先级 = 绝对优先级{@linkplain Builder#priority(int)} -
         * 默认优先级{@linkplain blue.fen.scheduler.BFConfig#DEFAULT_PRIORITY_DEFAULT}
         */
        public Builder relativePriority(boolean relativePriority) {
            this.relativePriority = relativePriority;
            return this;
        }

        /**
         * @see ISchedulerTask#perform(String, ParamProvider, TaskNotifier)
         */
        public Builder task(Task task) {
            this.task = task;
            return this;
        }

        /**
         * @see ISchedulerTask#isBackground()
         */
        public Builder isBackground(boolean isBackground) {
            this.isBackground = isBackground;
            return this;
        }

        /**
         * @see ISchedulerTask#enableThreadPriority()
         */
        public Builder enableThreadPriority(boolean enableThreadPriority) {
            this.enableThreadPriority = enableThreadPriority;
            return this;
        }

        /**
         * @see ISchedulerTask#threadPriority()
         */
        public Builder threadPriority(int threadPriority) {
            this.threadPriority = threadPriority;
            return this;
        }

        /**
         * @see ISchedulerTask#needBlock()
         */
        public Builder needBlock(boolean needBlock) {
            this.needBlock = needBlock;
            return this;
        }

        /**
         * @see ISchedulerTask#isAsync()
         */
        public Builder isAsync(boolean isAsync) {
            this.isAsync = isAsync;
            return this;
        }

        /**
         * @see ISchedulerTask#manualDispatch()
         */
        public Builder manualDispatch(boolean manualDispatch) {
            this.manualDispatch = manualDispatch;
            return this;
        }

        /**
         * @see ISchedulerTask#ignoreThrow()
         */
        public Builder ignoreThrow(boolean ignoreThrow) {
            this.ignoreThrow = ignoreThrow;
            return this;
        }

        public SchedulerTask build() {
            if (TextUtils.isEmpty(name)) {
                throw new IllegalArgumentException("SchedulerTask名称不能为空");
            }
            return new SchedulerTask(this);
        }
    }
}
