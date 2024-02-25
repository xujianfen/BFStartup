package blue.fen.scheduler.distributor;

import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;

import androidx.annotation.NonNull;

import blue.fen.scheduler.BFScheduler;
import blue.fen.scheduler.TaskFlow;
import blue.fen.scheduler.listener.task.TaskListener;
import blue.fen.scheduler.utils.BFClass;
import blue.fen.scheduler.utils.BFLog;
import blue.fen.scheduler.utils.BFSingleton;

/**
 * <p>创建时间：2023/12/31 （星期日｝</p>
 * <p>作者： blue_fen</p>
 * <p>描述：使用{@link HandlerThread}实现的任务分发器</p>
 */
public class DistributorHandler extends Handler implements IDistributor, BFClass.BFCleanable {
    private static volatile HandlerThread sThread;

    private static final BFSingleton<DistributorHandler> sInstance = BFClass.singleton(() -> {
        sThread = new HandlerThread("BFTaskDistributorThread");
        sThread.start();
        return new DistributorHandler(sThread.getLooper());
    });

    private DistributorHandler(Looper looper) {
        super(looper);
    }

    public static DistributorHandler getInstance() {
        return sInstance.get();
    }

    private final static int EXECUTE = 0;
    private final static int DISPATCH = 1;
    private final static int COMPLETE = 2;

    @Override
    public IDispatcher createDispatcher(TaskFlow flow, TaskListener listener, BFScheduler.Node node) {
        return new Dispatcher(flow, listener, node);
    }

    @Override
    public void execute(TaskFlow flow) {
        sendMessage(EXECUTE, flow, flow);
    }

    @Override
    public void dispatch(IDispatcher dispatcher) {
        sendMessage(DISPATCH, dispatcher, dispatcher.flow());
    }

    @Override
    public void complete(IDispatcher dispatcher) {
        sendMessage(COMPLETE, dispatcher, dispatcher.flow());
    }

    private void sendMessage(int arg1, Object obj, TaskFlow flow) {
        if (!flow.active()) return;

        Message m = Message.obtain();
        m.arg1 = arg1;
        m.obj = obj;
        m.what = flow.hashCode();
        sendMessage(m);
    }

    @Override
    public void remove(TaskFlow flow) {
        removeMessages(flow.hashCode());
    }

    @Override
    public void clean() {
        removeCallbacksAndMessages(null);
        sThread.interrupt();
    }

    @Override
    public void handleMessage(@NonNull Message msg) {
        super.handleMessage(msg);
        try {
            handleMessageInner(msg);
        } catch (Exception e) {
            TaskFlow flow = (TaskFlow) msg.obj;
            String name = flow.name();
            BFLog.err("项目(" + name + ")执行失败：", e.getMessage(), e);
            flow.clean();
        }
    }

    private void handleMessageInner(@NonNull Message msg) {
        switch (msg.arg1) {
            case EXECUTE: {
                TaskFlow f = (TaskFlow) msg.obj;
                f.enqueue();
                break;
            }
            case DISPATCH: {
                IDispatcher d = (IDispatcher) msg.obj;
                d.dispatch();
                break;
            }
            case COMPLETE: {
                IDispatcher d = (IDispatcher) msg.obj;
                d.flow().complete(d.taskNode());
                break;
            }
        }
    }
}
