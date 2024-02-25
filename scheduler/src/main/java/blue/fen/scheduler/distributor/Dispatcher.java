package blue.fen.scheduler.distributor;

import blue.fen.scheduler.BFConfig;
import blue.fen.scheduler.BFScheduler;
import blue.fen.scheduler.BFScheduler.Node.State;
import blue.fen.scheduler.TaskFlow;
import blue.fen.scheduler.listener.project.ExtraDataRecords;
import blue.fen.scheduler.listener.task.TaskListener;
import blue.fen.scheduler.scheduler.ISchedulerTask;
import blue.fen.scheduler.scheduler.TaskNotifier;
import blue.fen.scheduler.scheduler.ParamProvider;
import blue.fen.scheduler.utils.BFLog;
import blue.fen.scheduler.utils.BFUtil;

/**
 * <p>创建时间：2023/12/31 （星期日｝</p>
 * <p>作者： blue_fen</p>
 * <p>描述：任务调度器</p>
 */
public class Dispatcher implements IDispatcher, Runnable, ParamProvider, TaskNotifier {
    private final BFScheduler.Node node;
    private final TaskFlow flow;
    private final TaskListener listener;

    Dispatcher(TaskFlow flow, TaskListener listener, BFScheduler.Node node) {
        this.flow = flow;
        this.node = node;
        this.listener = listener;
    }

    private boolean isDebug() {
        return BFConfig.isDebug();
    }

    @Override
    public TaskFlow flow() {
        return flow;
    }

    @Override
    public BFScheduler.Node taskNode() {
        return node;
    }

    private State getState() {
        return node.getState();
    }

    private void setState(State state) {
        node.setState(state);
    }

    private void prepare() {
        setState(State.PREPARE);
        listener.before(this);
    }

    private void throwable(Throwable e) {
        setState(State.THROWABLE);
        if (e != null) {
            taskNode().setThrowable(e);
        } else {
            e = taskNode().getThrowable();
        }
        listener.throwable(this, e);
    }

    private void complete() {
        setState(State.COMPLETE);
        listener.after(this);
    }

    @Override
    public boolean proceed() {
        boolean unComplete = getState().unComplete();
        if (unComplete) {
            complete();
        } else if (isDebug()) {
            BFLog.w(name(), "【" + task().name() + "】任务已完成，不能重复提交");
        }
        return unComplete;
    }

    @Override
    public boolean error(Exception e) {
        boolean unComplete = getState().unComplete();
        if (unComplete) {
            throwable(e);
        } else if (isDebug()) {
            BFLog.w(name(), "【" + task().name() + "】任务已完成，不能重复提交");
        }
        return unComplete;
    }

    @Override
    public boolean isAsync() {
        ISchedulerTask task = task();
        boolean async = task.isAsync();
        if (!async && task.isBackground() && BFUtil.isMainThread()
                && BFUtil.isCurrentThread(flow.thread())) {
            async = true;
        }
        return async;
    }

    @Override
    public void perform() {
        performInternal(task(), false);
    }

    @Override
    public void dispatch() {
        BFTaskExecutor executor = flow.executor();
        if (isDebug()) {
            ISchedulerTask task = task();
            BFLog.d(name(), "使用" + (task.isBackground() ? "后台" : "前台")
                    + "线程执行【" + task.name() + "】");
        }
        if (task().isBackground()) {
            executor.executeOnDiskIO(this, this);
        } else {
            executor.executeOnMainThread(this, this);
        }
    }

    @Override
    public void run() {
        ISchedulerTask task = task();
        if (!flow().active()) {
            BFLog.err(name(), "任务【" + task.name() + "】被中断！！！");
            return;
        }
        if (!BFUtil.isMainThread() && task.enableThreadPriority()) {
            Thread currentThread = Thread.currentThread();
            currentThread.setPriority(task.threadPriority());
        }
        performInternal(task, task.manualDispatch());
    }

    private void performInternal(ISchedulerTask task, boolean manual) {
        if (!task.ignoreThrow() && getState().isThrow()) {
            throwable(null);
            return;
        }

        try {
            if (isDebug() && manual && !isAsync()) {
                BFLog.w(name(), "【" + task.name() + "】任务为非异步请求，手动调度可能失效");
            }
            prepare();
            task.perform(name(), this, manual ? this : null);
            if (!manual) complete();
        } catch (Exception e) {
            throwable(e);
        }
    }

    @Override
    public boolean setParam(Object args) {
        return ExtraDataRecords.getInstance().setParam(name(), task().name(), args);
    }

    @Override
    public boolean clearParam(String task) {
        return ExtraDataRecords.getInstance().setParam(name(), task, null);
    }

    @Override
    public Object getParam(String task) {
        return ExtraDataRecords.getInstance().getParam(name(), task);
    }
}
