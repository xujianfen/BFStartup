package blue.fen.scheduler.priority;

import androidx.annotation.NonNull;
import androidx.core.util.Pools;

import blue.fen.scheduler.utils.BFPool;

/**
 * <p>创建时间：2023/12/31 （星期日｝</p>
 * <p>作者： blue_fen</p>
 * <p>描述：默认优先级队列</p>
 */
public class DefaultPriorityQueue implements PriorityQueue<DefaultPriorityQueue.Node> {
    public static final int MAX_PRIORITY = 0xF;
    public static final int MIN_PRIORITY = 0;
    public static final int DEFAULT_PRIORITY = MAX_PRIORITY >> 1;

    private static boolean checkPriority(int priority) {
        return (priority & ~0xF) == 0;
    }

    private final CircularDoubleLinkedList[] prioritySparseArray = new CircularDoubleLinkedList[MAX_PRIORITY + 1];

    /**
     * 优先级目录
     */
    private short directory;

    private int highestPriority;

    private void registerDirectory(int priority) {
        short newDirectory = addPriority(directory, priority);
        if (newDirectory != directory) {
            directory = newDirectory;
            highestPriority = calculateHighestPriority();
        }
    }

    private void unRegisterDirectory(int priority) {
        short newDirectory = removePriority(directory, priority);
        if (newDirectory != directory) {
            directory = newDirectory;
            highestPriority = calculateHighestPriority();
        }
    }

    private int calculateHighestPriority() {
        return calculateHighestPriority(directory);
    }

    private static short addPriority(int directory, int priority) {
        return (short) (directory | 1 << priority);
    }

    private static short removePriority(int directory, int priority) {
        return (short) (directory & ~(1 << priority));
    }

    private static int calculateHighestPriority(int directory) {
        if (directory == 0) {
            return -1;
        }
        int cursor, i, j = 4;
        do {
            i = --j << 2;
            cursor = 0xF << i;
        } while ((directory & cursor) == 0);
        cursor = (directory & cursor) >>> i;
        j = 0;
        while ((cursor & 0x8 >> j) == 0) j++;
        return i + (~j & 3);
    }

    @Override
    public int highestPriority() {
        return highestPriority;
    }

    @Override
    public Node poll() {
        Node node = peek();
        remove(node);
        return node;
    }

    @Override
    public Node peek() {
        if (highestPriority < 0) {
            return null;
        }
        CircularDoubleLinkedList linkedList = prioritySparseArray[highestPriority];
        if (linkedList == null || linkedList.isEmpty()) {
            return null;
        }
        return linkedList.get();
    }

    @Override
    public void offer(Node node) {
        if (node == null) {
            return;
        }
        int priority = node.getPriority();
        CircularDoubleLinkedList linkedList = prioritySparseArray[priority];
        if (linkedList == null) {
            linkedList = CircularDoubleLinkedList.acquire();
            prioritySparseArray[priority] = linkedList;
        }
        if (linkedList.isEmpty()) {
            registerDirectory(priority);
        }
        linkedList.add(node);
    }

    @Override
    public void remove(Node node) {
        if (node == null) {
            return;
        }
        int priority = node.getPriority();
        CircularDoubleLinkedList linkedList = prioritySparseArray[priority];
        if (linkedList != null && linkedList.isNoEmpty()) {
            linkedList.remove(node);
            if (linkedList.isEmpty()) {
                prioritySparseArray[priority] = null;
                linkedList.recycle();
                unRegisterDirectory(priority);
            }
        }
    }

    @Override
    public void clean() {
        for (CircularDoubleLinkedList circularDoubleLinkedList : prioritySparseArray) {
            if (circularDoubleLinkedList != null) circularDoubleLinkedList.recycle();
        }
    }

    static class CircularDoubleLinkedList {
        private static final Pools.Pool<CircularDoubleLinkedList> sPool = new BFPool<>(4);

        public static CircularDoubleLinkedList acquire() {
            CircularDoubleLinkedList linkedList = sPool.acquire();
            if (linkedList == null) {
                linkedList = new CircularDoubleLinkedList();
            }
            return linkedList;
        }

        public void recycle() {
            head = null;
            sPool.release(this);
        }

        Node head;

        public boolean isEmpty() {
            return head == null;
        }

        public boolean isNoEmpty() {
            return head != null;
        }

        public void add(Node node) {
            if (head == null) {
                head = node;
                head.pre = head.next = head;
            } else {
                node.pre = head;
                node.next = head.next;
                node.next.pre = node;
                head.next = node;
            }
        }

        public void remove(Node node) {
            if (head.equals(node)) {
                if (head.equals(node.pre)) {
                    head = null;
                    return;
                } else {
                    head = node.pre;
                }
            }
            node.pre.next = node.next;
            node.next.pre = node.pre;
            node.next = null;
            node.pre = null;
        }

        public Node get() {
            return head;
        }
    }

    @Override
    public Iterator iterator() {
        return new Iterator();
    }

    class Iterator implements blue.fen.scheduler.priority.PriorityQueue.Iterator<Node> {
        private int directory;
        private Node current;
        private Node head;

        public Iterator() {
            this.directory = DefaultPriorityQueue.this.directory;
        }

        public boolean hasNext() {
            int directory = this.directory;
            if (current != null) {
                if (head.equals(current.pre)) {
                    directory = removePriority(directory, calculateHighestPriority(directory));
                } else {
                    return true;
                }
            }
            do {
                int highestPriority = calculateHighestPriority(directory);
                if (highestPriority < 0) {
                    return false;
                }
                CircularDoubleLinkedList linkedList = prioritySparseArray[highestPriority];
                if (linkedList != null && linkedList.get() != null) {
                    return true;
                } else {
                    directory = removePriority(directory, highestPriority);
                }
            } while (true);
        }

        public Node next() {
            if (current != null) {
                if (head.equals(current.pre)) {
                    directory = removePriority(directory, calculateHighestPriority(directory));
                    head = current = null;
                } else {
                    current = current.pre;
                }
            }
            while (current == null) {
                int highestPriority = calculateHighestPriority(directory);
                if (highestPriority < 0) {
                    break;
                }
                CircularDoubleLinkedList linkedList = prioritySparseArray[highestPriority];
                head = current = linkedList == null ? null : linkedList.get();
                if (current == null) {
                    directory = removePriority(directory, highestPriority);
                }
            }
            return current;
        }
    }

    static class Node implements PriorityNode {
        private final static Pools.Pool<DefaultPriorityQueue.Node> sPool = new BFPool<>(4);

        public static Node acquire(int priority, Object value) {
            DefaultPriorityQueue.Node node = sPool.acquire();
            if (node == null) {
                node = new DefaultPriorityQueue.Node(priority, value);
            } else {
                node.reset(priority, value);
            }
            return node;
        }

        public void recycle() {
            pre = null;
            next = null;
            value = null;
            priority = DEFAULT_PRIORITY;
            sPool.release(this);
        }

        public Node pre, next;
        private int priority;

        private Object value;

        public Node(int priority, Object value) {
            reset(priority, value);
        }

        public void reset(int priority, Object value) {
            setPriority(priority);
            this.value = value;
        }

        @Override
        public int getPriority() {
            return priority;
        }

        @Override
        public void setPriority(int priority) {
            if (checkPriority(priority)) {
                this.priority = priority;
            } else {
                throw new IllegalArgumentException("错误的优先级：" + Integer.toBinaryString(priority));
            }
        }

        @Override
        public Object get() {
            return value;
        }

        @NonNull
        @Override
        public String toString() {
            return priority + "";
        }
    }
}
