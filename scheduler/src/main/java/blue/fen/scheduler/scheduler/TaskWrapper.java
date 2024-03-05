package blue.fen.scheduler.scheduler;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.Collections;
import java.util.List;

/**
 * <p>创建时间：2024/03/06 （Wednesday｝</p>
 * <p>作者： blue_fen</p>
 * <p>描述：任务包装器，用它包装{@linkplain ISchedulerTask}可以修改任意方法的返回值</p>
 *
 * @noinspection unused
 */
public class TaskWrapper extends TaskVariableMapping {
    @NonNull
    private final ISchedulerTask defaultTask;

    public TaskWrapper(Builder builder, @NonNull ISchedulerTask defaultTask) {
        super(builder);
        this.defaultTask = defaultTask;
    }

    @Override
    public void perform(String projectName,
                        @NonNull ParamProvider param,
                        @Nullable TaskNotifier notifier
    ) throws Exception {
        if (task == null) {
            defaultTask.perform(projectName, param, notifier);
        } else {
            super.perform(projectName, param, notifier);
        }
    }

    @Override
    public String name() {
        if (name == null) {
            return defaultTask.name();
        } else {
            return super.name();
        }
    }

    @Override
    public List<String> dependencies() {
        if (dependencies == null) {
            return defaultTask.dependencies();
        } else {
            return super.dependencies();
        }
    }

    @Override
    public int priority() {
        if (priority == null) {
            return defaultTask.priority();
        } else {
            return super.priority();
        }
    }

    @Override
    public boolean isBackground() {
        if (isBackground == null) {
            return defaultTask.isBackground();
        } else {
            return super.isBackground();
        }
    }

    @Override
    public boolean enableThreadPriority() {
        if (enableThreadPriority == null) {
            return defaultTask.enableThreadPriority();
        } else {
            return super.enableThreadPriority();
        }
    }

    @Override
    public int threadPriority() {
        if (threadPriority == null) {
            return defaultTask.threadPriority();
        } else {
            return super.threadPriority();
        }
    }

    @Override
    public boolean needBlock() {
        if (needBlock == null) {
            return defaultTask.needBlock();
        } else {
            return super.needBlock();
        }
    }

    @Override
    public boolean isAsync() {
        if (isAsync == null) {
            return defaultTask.isAsync();
        } else {
            return super.isAsync();
        }
    }

    @Override
    public boolean manualDispatch() {
        if (manualDispatch == null) {
            return defaultTask.manualDispatch();
        } else {
            return super.manualDispatch();
        }
    }

    @Override
    public boolean ignoreThrow() {
        if (ignoreThrow == null) {
            return defaultTask.ignoreThrow();
        } else {
            return super.ignoreThrow();
        }
    }

    public static Builder newBuilder(ISchedulerTask task) {
        return new Builder(task);
    }

    public static final class Builder extends TaskVariableMapping.Builder {
        @NonNull
        private final ISchedulerTask defaultTask;

        public Builder(@NonNull ISchedulerTask defaultTask) {
            this.defaultTask = defaultTask;
        }

        @Override
        public TaskWrapper build() {
            return new TaskWrapper(this, defaultTask);
        }

        /**
         * @noinspection unchecked
         */
        @Override
        public Builder clearDependencies() {
            if (dependencies != null) {
                dependencies.clear();
            } else {
                dependencies = Collections.EMPTY_LIST;
            }
            return this;
        }
    }
}
