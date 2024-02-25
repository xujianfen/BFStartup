package blue.fen.scheduler.priority;

/**
 * <p>创建时间：2023/12/31 （星期日｝</p>
 * <p>作者： blue_fen</p>
 * <p>描述：优先级队列提供者适配器</p>
 *
 * @noinspection unused
 */
public class PriorityAdapter implements PriorityProvider {

    private final PriorityProvider provider;

    public PriorityAdapter(PriorityProvider provider) {
        this.provider = provider;
    }

    @Override
    public int maxPriority() {
        return provider.maxPriority();
    }

    @Override
    public int minPriority() {
        return provider.minPriority();
    }

    @Override
    public int defaultPriority() {
        return provider.defaultPriority();
    }

    public int clampPriority(int priority) {
        int clamp = maxPriority();
        if (priority > clamp) {
            return clamp;
        } else if (priority < (clamp = minPriority())) {
            return clamp;
        }
        return priority;
    }

    @Override
    public PriorityQueue<? extends PriorityNode> createPriorityQueue() {
        return provider.createPriorityQueue();
    }

    public <T extends PriorityNode> T acquireNode(Object value) {
        return acquireNode(defaultPriority(), value);
    }

    public <T extends PriorityNode> T acquireNode(int priority) {
        return acquireNode(priority, null);
    }

    @Override
    public <T extends PriorityNode> T acquireNode(int priority, Object value) {
        return provider.acquireNode(priority, value);
    }

    @Override
    public void recycleNode(PriorityNode node) {
        provider.recycleNode(node);
    }
}
