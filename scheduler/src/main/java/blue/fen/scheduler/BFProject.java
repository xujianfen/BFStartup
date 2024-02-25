package blue.fen.scheduler;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import blue.fen.scheduler.scheduler.ISchedulerTask;

/**
 * <p>创建时间：2024/01/01 （星期一｝</p>
 * <p>作者： blue_fen</p>
 * <p>描述：</p>
 */
public class BFProject {
    private final BFScheduler.Build taskBuild;
    final Map<String, BFScheduler.Node> mapping;
    String name;

    public BFProject(BFScheduler.Build taskBuild) {
        this.taskBuild = taskBuild;
        this.mapping = new LinkedHashMap<>();
    }

    public BFProject register(ISchedulerTask task) {
        if (task == null) return this;
        BFScheduler.Node node = taskBuild.createNode(task);
        mapping.put(task.name(), node);
        return this;
    }

    public BFProject register(ISchedulerTask... tasks) {
        for (ISchedulerTask task : tasks) {
            register(task);
        }
        return this;
    }

    public BFProject register(List<ISchedulerTask> tasks) {
        for (ISchedulerTask task : tasks) {
            register(task);
        }
        return this;
    }

    public BFProject unRegister(ISchedulerTask task) {
        if (task == null) return this;
        mapping.remove(task.name());
        return this;
    }

    public BFProject merge(BFProject project) {
        if (project == null || project.mapping.isEmpty()) return this;
        mapping.putAll(project.mapping);
        return this;
    }

    public BFProject name(String name) {
        this.name = name;
        return this;
    }

    public BFScheduler.Build commit() {
        if (mapping.isEmpty()) return null;
        return taskBuild.project(this);
    }
}
