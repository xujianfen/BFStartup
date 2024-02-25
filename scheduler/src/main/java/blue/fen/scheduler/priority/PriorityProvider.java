package blue.fen.scheduler.priority;

import blue.fen.scheduler.utils.BFClass;

/**
 * <p>创建时间：2023/12/31 （星期日｝</p>
 * <p>作者： blue_fen</p>
 * <p>描述：优先级队列提供者</p>
 */
public interface PriorityProvider {
    int maxPriority();

    int minPriority();

    int defaultPriority();

    PriorityQueue<? extends PriorityNode> createPriorityQueue();

    <T extends PriorityNode> T acquireNode(int priority, Object value);

    void recycleNode(PriorityNode node);

    Default DEFAULT = new Default();

    class Default extends BFClass.Default<PriorityProvider> {
        public static final int MAX_PRIORITY = DefaultPriorityQueue.MAX_PRIORITY;
        public static final int MIN_PRIORITY = DefaultPriorityQueue.MIN_PRIORITY;
        public static final int DEFAULT_PRIORITY = DefaultPriorityQueue.DEFAULT_PRIORITY;

        public PriorityProvider impl() {
            return new DefaultPriorityProvider();
        }
    }
}
