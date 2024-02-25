package blue.fen.scheduler.block;

import android.annotation.SuppressLint;
import android.os.Looper;
import android.os.MessageQueue;
import android.text.TextUtils;

import androidx.annotation.NonNull;

import java.lang.reflect.Field;
import java.util.Stack;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import blue.fen.scheduler.BFConfig;
import blue.fen.scheduler.BFScheduler;
import blue.fen.scheduler.distributor.BFDefaultTaskExecutor;
import blue.fen.scheduler.utils.BFClass;
import blue.fen.scheduler.utils.BFLog;
import blue.fen.scheduler.utils.BFUtil;

/**
 * <p>创建时间：2023/12/31 （星期日｝</p>
 * <p>作者： blue_fen</p>
 * <p>描述：任务阻塞器</p>
 */
public class TaskBlock implements Block {
    private String name;

    private int waitCount;
    private ILatch latch;
    private long timeout;

    public void setName(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    @Override
    public void setTimeout(int timeout) {
        this.timeout = timeout;
    }

    @Override
    public void tryIncrement(BFScheduler.Node node) {
        if (timeout != 0 && node.task().needBlock()) {
            waitCount++;
        }
    }

    @Override
    public void tryGenerateLatch() {
        if (waitCount > 0 && latch == null) {
            latch = new Latch(waitCount);
        }
    }

    @Override
    public boolean waiting() {
        return latch != null && latch.get() > 0;
    }

    @Override
    public void tryDecrement(BFScheduler.Node node) {
        if (latch != null && node.task().needBlock()) {
            latch.countDown();
        }
    }

    @Override
    public boolean await() {
        if (latch == null) {
            return false;
        }

        long startTime = System.currentTimeMillis();
        boolean isOutTime = latch.await(startTime, timeout);
        if (BFConfig.isDebug()) {
            long endTime = System.currentTimeMillis();
            printWaitFinishInfo(startTime, endTime, isOutTime);
        }
        return !isOutTime;
    }

    private String printName() {
        String name = getName();
        return TextUtils.isEmpty(name) ? "" : "[" + name + "]";
    }

    @SuppressLint("DefaultLocale")
    private void printWaitFinishInfo(long startTime, long endTime, boolean isOutTime) {
        StringBuilder builder = new StringBuilder()
                .append(isOutTime ? "等待超时" : "等待结束")
                .append("，当前时间：")
                .append(System.nanoTime())
                .append("，实际等待时间：")
                .append(String.format("%.3f", (endTime - startTime) / 1000.0f))
                .append("秒");

        if (timeout > 0) {
            builder.append(", 最长等待时间：")
                    .append(String.format("%.3f", timeout / 1000.0f))
                    .append("秒。");
        }
        String name = printName();
        BFLog.d(getName(), name + builder);
    }

    @Override
    public void clean() {
        if (latch != null) {
            latch.clean();
            latch = null;
        }
        timeout = 0;
        waitCount = 0;
    }

    private interface ILatch extends BFClass.BFCleanable {
        void countDown();

        int get();

        boolean await(long startTime, long timeout);
    }

    private final class Latch implements ILatch {
        private final ForegroundLatch foregroundLatch;
        private final BackgroundLatch backgroundLatch;

        private Latch(int waitCount) {
            foregroundLatch = new ForegroundLatch(waitCount);
            backgroundLatch = new BackgroundLatch(waitCount);
        }

        private ILatch latch() {
            return BFUtil.isMainThread() ? foregroundLatch : backgroundLatch;
        }

        @Override
        public void clean() {
            BFDefaultTaskExecutor.getInstance().executeOnMainThread(foregroundLatch::clean);
            backgroundLatch.clean();
        }

        @Override
        public void countDown() {
            foregroundLatch.countDown();
            backgroundLatch.countDown();
        }

        @Override
        public int get() {
            return latch().get();
        }

        @Override
        public boolean await(long startTime, long timeout) {
            return latch().await(startTime, timeout);
        }
    }

    //管理loop栈的持有者，确保loop可以归还ActivityThread
    private static Stack<ForegroundLatch> blockStack;

    @NonNull
    private static Stack<ForegroundLatch> getBlockStack() {
        if (blockStack == null) {
            blockStack = new Stack<>();
        }
        return blockStack;
    }

    private static MessageQueue mQueue;

    private static MessageQueue getMessageQueue() {
        Looper looper = Looper.getMainLooper();
        MessageQueue queue;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            queue = looper.getQueue();
        } else if (mQueue != null) {
            queue = mQueue;
        } else {
            try {
                String fieldName = "mQueue";
                @SuppressLint("DiscouragedPrivateApi")
                Field field = Looper.class.getDeclaredField(fieldName);
                field.setAccessible(true);
                queue = mQueue = (MessageQueue) field.get(looper);
            } catch (Exception e) {
                throw new IllegalArgumentException(e);
            }
        }
        return queue;
    }

    private static long setWithGetQueuePtr(long newPtr) {
        try {
            MessageQueue queue = getMessageQueue();
            String fieldName = "mPtr";
            @SuppressLint("DiscouragedPrivateApi")
            Field field = MessageQueue.class.getDeclaredField(fieldName);
            field.setAccessible(true);
            long mPtr = BFUtil.requireNonNullElse((Long) field.get(queue), 0L);
            field.set(queue, newPtr);
            return mPtr;
        } catch (Exception e) {
            BFLog.err(e);
            return 0;
        }
    }

    private final class ForegroundLatch implements ILatch {
        private final AtomicInteger latch;
        private long expireTime;
        private volatile boolean isLoop;
        private long tempQueuePtr;

        private boolean active;

        private ForegroundLatch(int waitCount) {
            latch = new AtomicInteger(waitCount);
            active = true;
        }

        @Override
        public void clean() {
            if (getBlockStack().isEmpty()) return;
            getBlockStack().peek().cleanInner(true);
        }

        private void cleanInner(boolean clean) {
            if (clean) {
                tryStealQueuePtr();
            } else {
                tryRestoreQueuePtr();
            }
            active = false;
            isLoop = false;
            expireTime = 0;
            latch.set(-1);
        }

        @Override
        public void countDown() {
            tryNotify(latch.decrementAndGet() <= 0);
        }

        @Override
        public int get() {
            return latch.get();
        }

        @Override
        public boolean await(long startTime, long timeout) {
            if (!active) {
                return false;
            }
            if (blockStack != null && !blockStack.isEmpty()) {
                BFLog.w(getName(), "多次在主线程调用await方法，" +
                        "会使后调用者阻塞未完成的先调用者，导致先调用者等待时间延长");
            }
            expireTime = timeout > 0 ? startTime + timeout : timeout;
            boolean outTime = false;
            while (!latch.weakCompareAndSet(0, -1)) {
                if (checkOutTime()) { //是否超时
                    outTime = true;
                    break;
                }
                if (latch.get() < 0) { //等待任务是否全部执行完成
                    break;
                }
                tryLoop();
            }
            tryNotifyNext();
            cleanInner(!active && !getBlockStack().isEmpty());
            return outTime;
        }

        private boolean checkOutTime() {
            return expireTime > 0 && expireTime <= System.currentTimeMillis();
        }

        private void tryNotify() {
            tryNotify(get() <= 0);
        }

        private void tryNotify(boolean finish) {
            if (isLoop && (finish || checkOutTime())) {
                tryStealQueuePtr();
            }
        }

        private void tryNotifyNext() {
            Stack<ForegroundLatch> blockStack = getBlockStack();
            if (blockStack.isEmpty()) return;
            ForegroundLatch stackTop = blockStack.peek();
            if (!active) stackTop.cleanInner(true);
            stackTop.tryNotify();
        }

        private void tryLoop() {
            isLoop = true;
            if (latch.get() > 0) {
                Stack<ForegroundLatch> blockStack = getBlockStack();
                blockStack.push(this);
                try {
                    Looper.loop();
                } finally {
                    ForegroundLatch latch = blockStack.pop();
                    assert latch == this;
                }
            }
            tryRestoreQueuePtr();
            isLoop = false;
        }

        private void tryRestoreQueuePtr() {
            if (tempQueuePtr == 0) return;
            setWithGetQueuePtr(tempQueuePtr);
            tempQueuePtr = 0;
        }

        private void tryStealQueuePtr() {
            if (getBlockStack().peek() != this) return;
            tempQueuePtr = setWithGetQueuePtr(0);
        }
    }

    /**
     * @noinspection InnerClassMayBeStatic
     */
    private final class BackgroundLatch implements ILatch {
        private final CountDownLatch latch;

        private BackgroundLatch(int waitCount) {
            latch = new CountDownLatch(waitCount);
        }

        @Override
        public void countDown() {
            latch.countDown();
        }

        @Override
        public int get() {
            return (int) latch.getCount();
        }

        @Override
        public boolean await(long startTime, long timeout) {
            if (get() <= 0) {
                return false;
            }
            try {
                if (timeout > 0) {
                    return latch.await(timeout, TimeUnit.MICROSECONDS);
                } else {
                    latch.await();
                    return false;
                }
            } catch (InterruptedException e) {
                BFLog.err(e);
                return false;
            }
        }

        @Override
        public void clean() {
            long count = latch.getCount();
            while (count-- > 0) {
                latch.countDown();
            }
        }
    }
}
