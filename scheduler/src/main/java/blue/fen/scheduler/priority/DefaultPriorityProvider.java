package blue.fen.scheduler.priority;

import blue.fen.scheduler.utils.BFLog;

/**
 * <p>创建时间：2023/12/31 （星期日｝</p>
 * <p>作者： blue_fen</p>
 * <p>描述：默认优先级队列提供者</p>
 */
public class DefaultPriorityProvider implements PriorityProvider {
    @Override
    public int maxPriority() {
        return DefaultPriorityQueue.MAX_PRIORITY;
    }

    @Override
    public int minPriority() {
        return DefaultPriorityQueue.MIN_PRIORITY;
    }

    @Override
    public int defaultPriority() {
        return DefaultPriorityQueue.DEFAULT_PRIORITY;
    }

    @Override
    public PriorityQueue<? extends PriorityNode> createPriorityQueue() {
        return new DefaultPriorityQueue();
    }

    @Override
    public <T extends PriorityNode> T acquireNode(int priority, Object value) {
        //noinspection unchecked
        return (T) DefaultPriorityQueue.Node.acquire(priority, value);
    }

    @Override
    public void recycleNode(PriorityNode node) {
        if (node instanceof DefaultPriorityQueue.Node) {
            ((DefaultPriorityQueue.Node) node).recycle();
        } else {
            BFLog.w("非DefaultPriorityQueue.Node类型，不推荐使用DefaultPriorityProvider");
        }
    }
}
