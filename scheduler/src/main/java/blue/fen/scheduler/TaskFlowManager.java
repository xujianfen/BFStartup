package blue.fen.scheduler;

import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import blue.fen.scheduler.distributor.BFTaskExecutor;
import blue.fen.scheduler.distributor.IDispatcher;
import blue.fen.scheduler.distributor.IDistributor;
import blue.fen.scheduler.listener.task.SchedulerListener;
import blue.fen.scheduler.listener.task.TaskListener;
import blue.fen.scheduler.priority.PriorityAdapter;
import blue.fen.scheduler.priority.PriorityNode;
import blue.fen.scheduler.priority.PriorityQueue;
import blue.fen.scheduler.utils.BFLog;

/**
 * <p>创建时间：2023/12/31 （星期日｝</p>
 * <p>作者： blue_fen</p>
 * <p>描述：任务流水线管理者</p>
 */
public class TaskFlowManager implements TaskFlow, TaskListener {
    private final PriorityAdapter priorityAdapter;
    private final IDistributor distributor;
    private final PriorityQueue<?> readyQueue;
    private final AtomicInteger remaining;
    private final SchedulerListener listener;
    private final BFTaskExecutor executor;
    private final Thread thread;

    private final ReentrantReadWriteLock lock;

    @Override
    public String name() {
        return listener.name();
    }

    @Override
    public PriorityAdapter priorityAdapter() {
        return priorityAdapter;
    }

    @Override
    public IDistributor getDistributor() {
        return distributor;
    }

    @Override
    public PriorityQueue<?> readyQueue() {
        return readyQueue;
    }

    @Override
    public Thread thread() {
        return thread;
    }

    @Override
    public BFTaskExecutor executor() {
        return executor;
    }

    @Override
    public int remaining() {
        return remaining.get();
    }

    TaskFlowManager(BFTaskConfig config, BFProject project, SchedulerListener listener) {
        this.priorityAdapter = new PriorityAdapter(config.priorityProvider);
        this.readyQueue = priorityAdapter.createPriorityQueue();
        this.remaining = new AtomicInteger(project.mapping.size());
        this.distributor = config.distributor;
        this.executor = config.executor;
        this.listener = listener;
        this.thread = Thread.currentThread();
        this.lock = new ReentrantReadWriteLock();
    }

    @Override
    public void addReady(BFScheduler.Node node) {
        assert node != null && node.task() != null;
        readyQueue.offer(priorityAdapter.acquireNode(node.priority(), node));
    }

    public boolean hasNext() {
        return readyQueue.peek() != null;
    }

    public BFScheduler.Node next() {
        PriorityNode node;
        BFScheduler.Node task = null;
        if ((node = readyQueue.poll()) != null) {
            task = (BFScheduler.Node) node.get();
            priorityAdapter.recycleNode(node);
        }
        assert task != null : "task不能为null";
        return task;
    }

    @Override
    public void enqueue() {
        lock.readLock().lock();
        try {
            checkEnqueue();
            tryEnqueue();
            while (hasNext()) {
                BFScheduler.Node node = next();
                IDistributor distributor = getDistributor();
                IDispatcher dispatcher = distributor.createDispatcher(this, this, node);
                distributor.dispatch(dispatcher);
            }
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public void execute() {
        lock.readLock().lock();
        try {
            checkExecuted();
            checkThread();
            nextProgress();
            while (hasNext()) {
                BFScheduler.Node node = next();
                IDistributor distributor = getDistributor();
                IDispatcher dispatcher = distributor.createDispatcher(this, this, node);
                if (dispatcher.isAsync()) {
                    nextProgress();
                    distributor.dispatch(dispatcher);
                    distributor.execute(dispatcher.flow());
                    break;
                } else {
                    dispatcher.perform();
                }
            }
        } finally {
            lock.readLock().unlock();
        }
    }

    private void checkThread() {
        Thread current = Thread.currentThread();
        if (thread != current) {
            throw new IllegalStateException("只有创建SchedulerManager的原始线程才能触发任务调度"
                    + " 预期线程: " + thread.getName() + " 实际线程: " + current.getName());
        }
    }

    private void checkExecuted() {
        if (isBusy()) {
            throw new IllegalStateException("任务正在运行中，不能重复执行！");
        }
        checkValid();
    }

    private void checkEnqueue() {
        checkValid();
    }

    private void checkValid() {
        if (isProgress(RELEASE)) {
            throw new IllegalStateException("任务已销毁！");
        } else if (isInvalid()) {
            throw new IllegalStateException("当前任务进度为【" + progressText() + "】，无效进度！");
        }
    }

    @Override
    public boolean active() {
        return remaining() > 0;
    }

    @Override
    public void callComplete(IDispatcher dispatcher) {
        if (isProgress(EXECUTE)) {
            completeInner(dispatcher.taskNode(), false);
        } else if (isProgress(ENQUEUE)) {
            completeInner(dispatcher.taskNode(), true);
        } else {
            BFLog.err("当前任务进度为【" + progressText() + "】，无法提交【" + dispatcher.name() + "】任务！");
        }
    }

    @Override
    public void complete(BFScheduler.Node node) {
        completeInner(node, true);
    }

    private void completeInner(BFScheduler.Node node, boolean isAsync) {
        if (!isBusy()) {
            BFLog.err(name(), "当前任务进度为【" + progressText() + "】，无法完成【" + node.name() + "】任务");
            return;
        }

        remaining.decrementAndGet();
        completeInner(node);
        listener.complete(node);
        node.clean();

        if (active()) {
            if (isAsync) getDistributor().execute(this);
        } else {
            finish();
        }
    }

    private void completeInner(BFScheduler.Node node) {
        Set<BFScheduler.Node> sucSet = node.getSuccessors();
        if (sucSet != null) {
            Iterator<BFScheduler.Node> iterable = sucSet.iterator();
            while (iterable.hasNext()) {
                BFScheduler.Node suc = iterable.next();
                suc.attach(node);
                iterable.remove();
                suc.dependenciesComplete();
                if (suc.isReady()) {
                    addReady(suc);
                }
            }
        }
    }

    @Override
    public void clean() {
        lock.writeLock().lock();
        listener.clean();
        readyQueue.clean();
        remaining.set(0);
        progress.set(RELEASE);
        distributor.remove(this);
        lock.writeLock().unlock();
    }

    @Override
    public void before(IDispatcher distributor) {
        listener.before(distributor);
    }

    @Override
    public void after(IDispatcher distributor) {
        listener.after(distributor);
        callComplete(distributor);
    }

    @Override
    public void throwable(IDispatcher distributor, Throwable throwable) {
        listener.throwable(distributor, throwable);
        callComplete(distributor);
    }

    @Override
    public void finish() {
        nextProgress();
        listener.finish();
    }

    /**
     * 空闲状态
     */
    private static final int IDLE = 0;

    /**
     * 执行状态（同步）
     */
    private static final int EXECUTE = 1;

    /**
     * 执行状态（异步）
     */
    private static final int ENQUEUE = 2;

    /**
     * 释放状态
     */
    private static final int RELEASE = 3;

    private final String[] PROGRESS_TEXTS = new String[]{
            "IDLE",
            "EXECUTE",
            "ENQUEUE",
            "RELEASE",
            "UNKNOWN"
    };

    private final AtomicInteger progress = new AtomicInteger(IDLE);

    private String progressText() {
        int progress = this.progress.get();
        if (progress < 0 || progress >= PROGRESS_TEXTS.length) {
            progress = PROGRESS_TEXTS.length - 1;
        }
        return PROGRESS_TEXTS[progress];
    }

    private boolean isBusy() {
        int progress = this.progress.get();
        return progress > IDLE && progress < RELEASE;
    }

    private boolean isInvalid() {
        int progress = this.progress.get();
        return progress < IDLE || progress > RELEASE;
    }

    private boolean isProgress(int progress) {
        return this.progress.get() == progress;
    }

    private void nextProgress() {
        progress.incrementAndGet();
    }

    /**
     * @noinspection UnusedReturnValue
     */
    private boolean tryEnqueue() {
        return isProgress(IDLE) && progress.weakCompareAndSet(IDLE, ENQUEUE);
    }
}
