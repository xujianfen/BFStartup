package blue.fen.scheduler.listener.task;

import blue.fen.scheduler.BFScheduler;
import blue.fen.scheduler.listener.task.TaskListener;

/**
 * <p>创建时间：2024/01/01 （星期一｝</p>
 * <p>作者： blue_fen</p>
 * <p>描述：</p>
 */
public interface SchedulerListener extends TaskListener {
    String name();

    void complete(BFScheduler.Node node);
}
