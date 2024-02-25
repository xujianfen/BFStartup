package blue.fen.scheduler;

import android.database.Observable;

import androidx.annotation.NonNull;

import java.util.List;
import java.util.Set;

import blue.fen.scheduler.exception.TransferTaskException;
import blue.fen.scheduler.listener.task.ITaskLifecycleObserver;
import blue.fen.scheduler.scheduler.ISchedulerTask;
import blue.fen.scheduler.scheduler.ITaskProvider;
import blue.fen.scheduler.utils.BFClass;
import blue.fen.scheduler.utils.BFLog;

/**
 * <p>创建时间：2023/12/31 （星期日｝</p>
 * <p>作者： blue_fen</p>
 * <p>描述：</p>
 */
public interface BFScheduler extends BFClass.BFDestroyable {
    static Build build() {
        return new Build();
    }

    String name();

    Observable<ITaskLifecycleObserver> getLifecycle();

    /**
     * <p>尝试执行{@linkplain BFScheduler#execute()}</p>
     *
     * @return <p>返回true，表示执行成功</p>
     * <p>返回false，表示执行失败</p>
     */
    default boolean tryExecute() {
        try {
            execute();
            return true;
        } catch (Exception e) {
            BFLog.err(e);
            return false;
        }
    }

    /**
     * <p>执行工作（优先考虑前台线程）</p>
     * <p><strong>注意：</strong>重复执行工作或者工作被关闭将抛出异常</p>
     * <p>若不希望抛出异常可以使用{@linkplain BFScheduler#tryExecute()}</p>
     */
    void execute();

    /**
     * <p>尝试执行{@linkplain BFScheduler#enqueue()}</p>
     *
     * @return <p>返回true，表示执行成功</p>
     * <p>返回false，表示执行失败</p>
     */
    default boolean tryEnqueue() {
        try {
            enqueue();
            return true;
        } catch (Exception e) {
            BFLog.err(e);
            return false;
        }
    }

    /**
     * <p>执行工作（优先考虑后台线程）</p>
     * <p><strong>注意：</strong>重复执行工作或者工作被关闭将抛出异常</p>
     * <p>若不希望抛出异常可以使用{@linkplain BFScheduler#tryEnqueue()}</p>
     */
    void enqueue();

    /**
     * 阻塞直到所有带阻塞的方法完成或者运行超时
     * <strong>注意：该方法虽然不会阻塞主线程和任务线程，但多次调用会导致后调用者阻塞未完成的先调用者，
     * 导致先调用者的等待事件变长（等待超时的设置可能失效）。<br/>
     * {@code await()}建议只用于阻塞IO线程。<br/>
     * 当然，非必要的话，更推荐使用监听。</strong>
     */
    void await();

    /**
     * @noinspection unused
     */
    class Build {
        private BFTaskConfig config;
        private BFProject project;

        private Factory factory;

        /**
         * 必须在{@link Build#newProject()}和{@link Build#submit()}之前设置，且不能重复
         */
        public Build factory(Factory factory) {
            assert this.factory == null : "BFSchedulerFactory不能重复设置";
            this.factory = factory;
            return this;
        }

        Build config(BFTaskConfig config) {
            this.config = config;
            return this;
        }

        Build project(BFProject project) {
            this.project = project;
            return this;
        }

        public BFTaskConfig newConfig() {
            return new BFTaskConfig.BFTaskConfigBridge(this);
        }

        public BFProject newProject() {
            return new BFProject(this);
        }

        public BFTaskConfig copyWithConfig() {
            return newConfig().copy(config);
        }

        public BFProject copyWithProject() {
            return newProject().merge(project);
        }

        public BFTaskConfig getConfig() {
            return config;
        }

        public BFProject getProject() {
            return project;
        }

        public Factory getFactory() {
            return factory;
        }

        private void ensureFactory() {
            factory = DEFAULT.factory(factory);
        }

        Node createNode(ISchedulerTask task) {
            ensureFactory();
            return factory.createNode(task);
        }

        public BFScheduler submit() {
            if (project == null) {
                return null;
            }
            if (config == null) {
                newConfig().commit();
            }
            ensureFactory();
            return factory.factory(config, project);
        }
    }

    BFClass.Default<Factory> DEFAULT = BFClass.defaultImpl(SchedulerManager.Factory::new);

    interface Factory {
        SchedulerManager factory(BFTaskConfig config, BFProject project);

        Node createNode(ISchedulerTask task);
    }

    interface Node extends BFClass.BFCleanable, ITaskProvider {
        String name();

        @Override
        ISchedulerTask task();

        List<String> dependencies();

        int dependentSize();

        Set<Node> getSuccessors();

        void dependenciesComplete();

        boolean isReady();

        int priority();

        void attach(Node parent);

        void setState(State state);

        State getState();

        Throwable getThrowable();

        void setThrowable(Throwable throwable);

        abstract class Impl implements Node {
            @NonNull
            private State state = State.IDLE;

            Throwable throwable;

            @Override
            public void attach(Node parent) {
                setState(getState().next(parent.getState()));
                trySetThrowable(parent);
            }

            @NonNull
            @Override
            public State getState() {
                return state;
            }

            @Override
            public void setState(@NonNull State state) {
                this.state = state;
            }

            private void trySetThrowable(Node parent) {
                if (task().ignoreThrow()) return;

                Throwable parentThrow = parent.getThrowable();
                if (parentThrow == null) return;

                if (throwable == null) {
                    setThrowable(new TransferTaskException());
                } else if (!(throwable instanceof TransferTaskException)) {
                    TransferTaskException newThrow = new TransferTaskException();
                    newThrow.merge(name(), throwable);
                    setThrowable(newThrow);
                }

                TransferTaskException tte = (TransferTaskException) throwable;
                tte.merge(parent.name(), parentThrow);
            }

            public Throwable getThrowable() {
                return throwable;
            }

            public void setThrowable(Throwable throwable) {
                this.throwable = throwable;
            }

            @Override
            public void clean() {
                state = State.IDLE;
                throwable = null;
            }
        }

        enum State {
            IDLE, PREPARE, THROWABLE, COMPLETE;

            public boolean unComplete() {
                return !isComplete();
            }

            public boolean isComplete() {
                return this == THROWABLE || this == COMPLETE;
            }

            private void assertComplete() {
                assert isComplete() : "任务尚未完成";
            }

            public boolean isThrow() {
                return this == THROWABLE;
            }

            public State next(State parentState) {
                if (isThrow()) return THROWABLE;
                if (parentState == null) parentState = COMPLETE;
                parentState.assertComplete();
                return parentState.isThrow() ? THROWABLE : IDLE;
            }
        }
    }
}
