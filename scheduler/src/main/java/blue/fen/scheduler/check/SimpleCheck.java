package blue.fen.scheduler.check;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import blue.fen.scheduler.BFScheduler.Node;
import blue.fen.scheduler.TaskFlow;
import blue.fen.scheduler.priority.PriorityQueue;

/**
 * <p>创建时间：2023/12/31 （星期日｝</p>
 * <p>作者： blue_fen</p>
 * <p>描述：环检测工具，可以检测任务中是否存在循环依赖</p>
 */
public class SimpleCheck {
    public static void checkDependencies(TaskFlow data) {
        PriorityQueue.Iterator<?> iterator = data.readyQueue().iterator();
        Map<String, Boolean> track = new HashMap<>();
        while (iterator.hasNext()) {
            Node node = (Node) iterator.next().get();
            if (dfs(node, track)) {
                throwCircularDependencies();
            }
        }
        if (track.size() != data.remaining()) { //不相等，说明存在一个含环的连通分量（该连通分量不存在入度为0的点）
            throwCircularDependencies();
        }
    }

    private static boolean dfs(Node node, Map<String, Boolean> track) {
        String name = node.name();
        Boolean existStack = track.get(name); //三个状态：null 未入栈；true 在栈上；false 已出栈；
        if (existStack != null) return existStack;

        Set<Node> successors = node.getSuccessors();
        track.put(name, true);
        if (successors != null) {
            for (Node s : successors) {
                if (dfs(s, track)) {
                    return true;
                }
            }
        }
        track.put(name, false);
        return false;
    }

    private static void throwCircularDependencies() {
        throw new IllegalArgumentException("存在循环依赖!!!");
    }
}
