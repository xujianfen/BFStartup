package blue.fen.scheduler.priority;

/**
 * <p>创建时间：2023/12/31 （星期日｝</p>
 * <p>作者： blue_fen</p>
 * <p>描述：优先级队列节点</p>
 */
public interface PriorityNode {
    int getPriority();

    void setPriority(int priority);

    Object get();
}
