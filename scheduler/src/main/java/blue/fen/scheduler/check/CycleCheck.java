package blue.fen.scheduler.check;

import androidx.collection.SparseArrayCompat;

import java.util.*;

import blue.fen.scheduler.BFScheduler.Node;
import blue.fen.scheduler.utils.BFUtil;

/**
 * <p>创建时间：2023/12/31 （星期日｝</p>
 * <p>作者： blue_fen</p>
 * <p>描述：环检测工具，检测任务执行图中出现的所有简单环</p>
 * <p>算法参考：<a href="https://www.youtube.com/watch?v=johyrWospv0">Johnson's Algorithm - All simple cycles in directed graph</a></p>
 */
public class CycleCheck {
    public static void checkDependencies(Map<String, Node> source) {
        Node[] mapping = new Node[source.size()];
        List<List<Integer>> sccAll = new ArrayList<>();
        List<List<Integer>> cycles = new ArrayList<>();
        int[] colors = new int[mapping.length];

        Graph graph = generateGraph(source, mapping);
        findScc(graph, colors, sccAll, mapping.length);
        findCycles(graph, colors, sccAll, cycles);
        tryThrowCircularDependencies(cycles, mapping);
    }

    private static Graph generateGraph(Map<String, Node> source, Node[] mapping) {
        final Map<String, Integer> indexMapping = new HashMap<>();
        int i = 0;
        int initialCapacity = 0;
        for (Node node : source.values()) {
            indexMapping.put(node.name(), i);
            mapping[i++] = node;
            initialCapacity += node.dependentSize();
        }
        final Graph graph = new Graph(initialCapacity);
        for (int u = 0; u < i; u++) {
            Node node = mapping[u];
            Set<Node> successorSet = node.getSuccessors();
            if (successorSet == null) continue;
            Object[] successorArray = successorSet.toArray();
            for (int j = successorArray.length - 1; j >= 0; j--) {
                Node successor = (Node) successorArray[j];
                int v = Objects.requireNonNull(indexMapping.get(successor.name()));
                graph.addEdge(u, v);
            }
        }
        return graph;
    }

    private static void findScc(
            Graph graph,
            int[] colors,
            List<List<Integer>> sccAll,
            int vLen
    ) {
        int[] lows = new int[vLen];
        int[] dTimes = new int[vLen];

        int dTime = 1;
        Stack<Integer> stack = new Stack<>();
        for (int u = 0; u < vLen; u++) {
            if (dTimes[u] == 0) {
                dTime = tarjan(u, dTime, graph, stack, colors, sccAll, lows, dTimes) + 1;
            }
        }
    }

    private static int tarjan(
            int u,
            int dTime,
            Graph graph,
            Stack<Integer> stack,
            int[] colors,
            List<List<Integer>> sccAll,
            int[] lows,
            int[] dTimes
    ) {
        dTimes[u] = lows[u] = dTime;
        stack.push(u);
        colors[u] = -1;
        Graph.Iterator iterator = graph.iterator(u);
        boolean hasSelf = false;
        while (iterator.hasNext()) {
            int v = iterator.next();
            if (dTimes[v] == 0) {
                dTime = tarjan(v, dTime + 1, graph, stack, colors, sccAll, lows, dTimes);
                lows[u] = Math.min(lows[u], lows[v]);
            } else if (u == v) {
                hasSelf = true;
            } else if (colors[v] == -1) {
                lows[u] = Math.min(lows[u], dTimes[v]);
            }
        }
        if (lows[u] == dTimes[u]) {
            int v = stack.pop();
            if (v == u) {
                colors[v] = 0;
                if(hasSelf && stack.isEmpty()) { //处理scc孤点
                    List<Integer> scc = new ArrayList<>();
                    sccAll.add(scc);
                    scc.add(u);
                }
                return dTime;
            }
            List<Integer> scc = new ArrayList<>();
            sccAll.add(scc);
            scc.add(u);
            int cycle = sccAll.size();
            colors[u] = cycle;
            do {
                scc.add(v);
                colors[v] = cycle;
                v = stack.pop();
            } while (v != u);
        }
        return dTime;
    }

    private static void findCycles(
            Graph graph,
            int[] colors,
            List<List<Integer>> sccAll,
            List<List<Integer>> cycles
    ) {
        Set<Integer> blockedSet = new HashSet<>();
        Map<Integer, Set<Integer>> blockedMap = new HashMap<>();
        Stack<Integer> stack = new Stack<>();
        for (List<Integer> scc : sccAll) {
            for (Integer u : scc) {
                findCyclesInScc(
                        graph,
                        colors,
                        stack,
                        cycles,
                        blockedSet,
                        blockedMap,
                        u,
                        u,
                        colors[u]
                );
                colors[u] = 0;
            }
        }
    }

    private static boolean findCyclesInScc(
            Graph graph,
            int[] colors,
            Stack<Integer> stack,
            List<List<Integer>> cycles,
            Set<Integer> blockedSet,
            Map<Integer, Set<Integer>> blockedMap,
            int start,
            int current,
            int cycleIndex
    ) {
        Graph.Iterator iterator = graph.iterator(current);
        blockedSet.add(current);
        stack.push(current);

        boolean foundCycle = false;
        while (iterator.hasNext()) {
            int v = iterator.next();
            if (colors[v] != cycleIndex) {
                if (colors[v] < cycleIndex) {
                    iterator.remove();
                }
                continue;
            }
            if (v == start) {
                List<Integer> cycle = new ArrayList<>(stack);
                cycles.add(cycle);
                foundCycle = true;
            } else if (!blockedSet.contains(v)) {
                boolean gotCycle = findCyclesInScc(
                        graph,
                        colors,
                        stack,
                        cycles,
                        blockedSet,
                        blockedMap,
                        start,
                        v,
                        cycleIndex
                );
                foundCycle |= gotCycle;
            }
        }

        if (foundCycle) {
            unblock(current, blockedSet, blockedMap);
        } else {
            iterator.reset();
            while (iterator.hasNext()) {
                int v = iterator.next();
                if (colors[v] != cycleIndex) continue;
                Set<Integer> set = BFUtil.computeIfAbsent(blockedMap, v, (key) -> new HashSet<>());
                set.add(current);
            }
        }
        stack.pop();
        return foundCycle;
    }

    private static void unblock(
            int u,
            Set<Integer> blockedSet,
            Map<Integer, Set<Integer>> blockedMap
    ) {
        blockedSet.remove(u);
        Set<Integer> blocked = blockedMap.get(u);
        if (blocked != null) {
            for (Integer v : blocked) {
                if(u == v) continue; //处理自循环，防止死循环
                unblock(v, blockedSet, blockedMap);
                blocked.clear();
            }
        }
    }

    private static void tryThrowCircularDependencies(List<List<Integer>> sccAll, Node[] v) {
        if (!sccAll.isEmpty()) {
            throwCircularDependencies(sccAll, v);
        }
    }

    private static void throwCircularDependencies(List<List<Integer>> sccAll, Node[] v) {
        StringBuilder builder = new StringBuilder();
        builder.append("检测到").append(sccAll.size()).append("条循环依赖：\n");
        for (int i = 0; i < sccAll.size(); ) {
            List<Integer> scc = sccAll.get(i);
            builder.append("[").append(++i).append("] ");
            for (Integer j : scc) {
                builder.append(v[j].name()).append("->");
            }
            builder.append(v[scc.get(0)].name()).append("\n");
        }
        throw new IllegalArgumentException(builder.toString());
    }
}


class Graph {
    private final List<Integer> to;
    private final List<Integer> next;
    private final SparseArrayCompat<Integer> head;

    Graph(int initialCapacity) {
        to = new ArrayList<>(initialCapacity);
        next = new ArrayList<>(initialCapacity);
        head = new SparseArrayCompat<>();
    }

    public void addEdge(int u, int v) {
        to.add(v);
        next.add(head(u));
        head.append(u, size() - 1);
    }

    public int size() {
        return to.size();
    }

    public int head(int u) {
        Integer oldHead = head.get(u);
        return oldHead == null ? -1 : oldHead;
    }

    public Iterator iterator(int u) {
        return new Iterator(u);
    }

    public class Iterator {
        private int pre;
        private int index;
        private final int u;

        public Iterator(int u) {
            pre = -2;
            index = head(u);
            this.u = u;
        }

        public boolean hasNext() {
            return index != -1;
        }

        public int next() {
            int _pre = pre == -2 ? -1 : pre == -1 ? head(u) : next.get(pre);
            if (_pre != index) pre = _pre; //防止remove后，pre连跳两次而与index同值，导致删除失败的问题
            int v = to.get(index);
            index = next.get(index);
            return v;
        }

        public void remove() {
            if (pre < 0) {
                head.append(u, index);
            } else {
                next.set(pre, index);
            }
        }

        public void reset() {
            index = head(u);
            pre = -2;
        }
    }
}
