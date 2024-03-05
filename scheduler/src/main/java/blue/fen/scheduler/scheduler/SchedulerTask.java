package blue.fen.scheduler.scheduler;

import android.text.TextUtils;

import blue.fen.scheduler.utils.BFUtil;

/**
 * <p>创建时间：2023/12/31 （星期日｝</p>
 * <p>作者： blue_fen</p>
 * <p>描述：调度的任务</p>
 *
 * @noinspection unused
 */
public class SchedulerTask extends TaskVariableMapping {
    private final boolean relativePriority;

    private SchedulerTask(Builder builder) {
        super(builder);
        relativePriority = builder.relativePriority;

        isAsync = BFUtil.requireNonNullElse(builder.isAsync, isBackground);
        priority = BFUtil.requireNonNullElse(
                builder.priority,
                priorityDefault()
        );
        threadPriority = BFUtil.requireNonNullElse(
                builder.threadPriority,
                threadPriorityDefault()
        );
    }

    public boolean relativePriority() {
        return relativePriority;
    }

    public static Builder newBuilder() {
        return new Builder();
    }

    public static final class Builder extends TaskVariableMapping.Builder {
        private boolean relativePriority;

        public Builder() {
            initializeWrappedFields();
            isAsync = null;
            this.isBackground = true;
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

        @Override
        public SchedulerTask build() {
            if (TextUtils.isEmpty(name)) {
                throw new IllegalArgumentException("SchedulerTask名称不能为空");
            }

            return new SchedulerTask(this);
        }
    }
}