package blue.fen.scheduler.priority;

import blue.fen.scheduler.utils.BFClass;

/**
 * <p>创建时间：2023/12/31 （星期日｝</p>
 * <p>作者： blue_fen</p>
 * <p>描述：优先级队列接口</p>
 *
 * @noinspection unused
 */
public interface PriorityQueue<Node extends PriorityNode> extends BFClass.BFCleanable {
    /**
     * 获取当前优先级队列中的最高优先级
     */
    int highestPriority();

    /**
     * 查看并删除首个最高优先级节点
     */
    Node poll();

    /**
     * 查看首个最高优先级节点
     */
    Node peek();

    void offer(Node node);

    void remove(Node node);

    Iterator<Node> iterator();

    interface Iterator<Node extends PriorityNode> {
        boolean hasNext();

        Node next();
    }
}
