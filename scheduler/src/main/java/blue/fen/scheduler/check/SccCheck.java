package blue.fen.scheduler.check;

import androidx.collection.SparseArrayCompat;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;

import blue.fen.scheduler.BFScheduler.Node;

/**
 * <p>创建时间：2023/12/31 （星期日｝</p>
 * <p>作者： blue_fen</p>
 * <p>描述：强连通分量检测工具，使用{@code kosaraju}算法检测任务执行图中出现的所有强连通分量</p>
 */
public class SccCheck {
    public static void checkDependencies(Map<String, Node> mapping) {
        Node[] v = new Node[mapping.size()];
        SparseArrayCompat<List<Integer>> digraph = new SparseArrayCompat<>();
        SparseArrayCompat<List<Integer>> digraphT = new SparseArrayCompat<>();
        List<List<Integer>> sccAll = new ArrayList<>();

        generateDigraph(mapping, digraph, digraphT, v);
        kosaraju(digraph, digraphT, sccAll, v.length);
        tryThrowCircularDependencies(sccAll, v);
    }

    private static void generateDigraph(Map<String, Node> input,
                                        SparseArrayCompat<List<Integer>> digraph,
                                        SparseArrayCompat<List<Integer>> digraphT,
                                        Node[] v
    ) {
        Map<String, Integer> indexMapping = new HashMap<>();
        int index = 0;
        for (Node node : input.values()) {
            indexMapping.put(node.name(), index);
            v[index++] = node;
        }
        for (int i = 0; i < index; i++) {
            Node node = v[i];
            Set<Node> successorSet = node.getSuccessors();
            if (successorSet == null) continue;
            List<Integer> lineT, line = ensureLine(i, digraph);
            for (Node successor : successorSet) {
                Integer j = indexMapping.get(successor.name());
                line.add(j);
                lineT = ensureLine(j, digraphT);
                lineT.add(i);
            }
        }
    }

    private static List<Integer> ensureLine(
            Integer index,
            SparseArrayCompat<List<Integer>> digraph
    ) {
        List<Integer> line = digraph.get(index);
        if (line == null) {
            line = new ArrayList<>();
            digraph.append(index, line);
        }
        return line;
    }

    private static void kosaraju(
            SparseArrayCompat<List<Integer>> digraph,
            SparseArrayCompat<List<Integer>> digraphT,
            List<List<Integer>> sccAll,
            int vNum
    ) {
        boolean[] vls = new boolean[vNum];
        Stack<Integer> route = new Stack<>();
        int u;
        for (u = 0; u < vNum; u++) dfsT(u, digraphT, vls, route);
        for (u = 0; u < vNum; u++) vls[u] = false;

        List<Integer> scc = new ArrayList<>();
        while (!route.empty()) {
            u = route.pop();
            if (!vls[u]) {
                if (dfs(u, digraph, vls, scc) || scc.size() > 1) {
                    sccAll.add(scc);
                    scc = new ArrayList<>();
                } else {
                    scc.clear();
                }
            }
        }
    }

    private static void dfsT(
            int u,
            SparseArrayCompat<List<Integer>> digraph,
            boolean[] vls,
            Stack<Integer> route
    ) {
        if (vls[u]) return;
        vls[u] = true;
        List<Integer> vs = digraph.get(u);
        int size = vs == null ? 0 : vs.size();
        for (int i = 0; i < size; i++) {
            int v = vs.get(i);
            dfsT(v, digraph, vls, route);
        }
        route.push(u);
    }

    private static boolean dfs(
            int u,
            SparseArrayCompat<List<Integer>> digraph,
            boolean[] vls,
            List<Integer> scc
    ) {
        vls[u] = true;
        scc.add(u);
        List<Integer> vs = digraph.get(u);
        int size = vs == null ? 0 : vs.size();
        boolean hasSelf = false;
        for (int i = 0; i < size; i++) {
            int v = vs.get(i);
            if (!vls[v]) dfs(v, digraph, vls, scc);
            else if (u == v) hasSelf = true;
        }
        return hasSelf && scc.size() == 1; //scc为孤点时返回true
    }

    private static void tryThrowCircularDependencies(
            List<List<Integer>> sccAll,
            Node[] v
    ) {
        if (!sccAll.isEmpty()) {
            throwCircularDependencies(sccAll, v);
        }
    }

    private static void throwCircularDependencies(
            List<List<Integer>> sccAll,
            Node[] v
    ) {
        StringBuilder builder = new StringBuilder();
        builder.append("检测到").append(sccAll.size()).append("个强连通分量：\n");
        for (int i = 0; i < sccAll.size(); ) {
            List<Integer> scc = sccAll.get(i);
            builder.append(" ").append(++i).append("：\n");
            for (Integer j : scc) {
                Node u = v[j];
                builder.append("    ").append(u.name()).append(" (");
                for (Node s : u.getSuccessors()) {
                    builder.append(s.name()).append("，");
                }
                builder.append(")\n");
            }
        }
        throw new IllegalArgumentException(builder.toString());
    }
}
