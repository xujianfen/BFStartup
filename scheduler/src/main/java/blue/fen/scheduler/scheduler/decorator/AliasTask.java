package blue.fen.scheduler.scheduler.decorator;

import android.text.TextUtils;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import blue.fen.scheduler.scheduler.ISchedulerTask;

/**
 * <p>创建时间：2024/03/02 （星期六｝</p>
 * <p>作者： blue_fen</p>
 * <p>描述：别名任务，用于扩展任务的别名</p>
 */
public class AliasTask extends DecoratorTask {
    private final String alias;
    private final List<String> dependencies;

    private AliasTask(Builder builder) {
        super(builder.task);

        if (TextUtils.isEmpty(builder.alias)) {
            alias = super.name() + "@" + hashCode();
        } else if ("$".equals(builder.alias)) {
            alias = super.name();
        } else {
            alias = builder.alias;
        }
        if (builder.dependencies == null) {
            dependencies = super.dependencies();
        } else {
            dependencies = builder.dependencies;
            int index = dependencies.indexOf("$");
            if (index >= 0) {
                dependencies.set(index, super.name());
            }
        }
    }

    @Override
    public String name() {
        return alias;
    }

    @Override
    public List<String> dependencies() {
        return dependencies;
    }

    public static Builder builder() {
        return new Builder();
    }

    /**
     * @noinspection unused, UnusedReturnValue
     */
    public static final class Builder {
        private ISchedulerTask task;
        private String alias;
        private List<String> dependencies;

        private Builder() {
        }

        /**
         * @param task 需要设置别名的任务
         */
        public Builder task(ISchedulerTask task) {
            this.task = task;
            return this;
        }

        /**
         * 设置任务别名，若为空，则使用自动生成的别名
         */
        public Builder alias(String alias) {
            this.alias = alias;
            return this;
        }

        /**
         * 没有依赖项
         */
        public Builder noDependencies() {
            //noinspection unchecked
            this.dependencies = Collections.EMPTY_LIST;
            return this;
        }

        /**
         * 设置任务依赖项，若为null则使用原来的依赖项
         */
        public Builder dependencies(List<String> dependencies) {
            this.dependencies = dependencies;
            return this;
        }

        /**
         * @see AliasTask.Builder#dependencies(List)
         */
        public Builder dependencies(String... dependencies) {
            this.dependencies = Arrays.asList(dependencies);
            return this;
        }

        public AliasTask build() {
            return new AliasTask(this);
        }
    }
}
