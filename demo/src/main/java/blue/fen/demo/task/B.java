package blue.fen.demo.task;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import blue.fen.scheduler.scheduler.ISchedulerTask;
import blue.fen.scheduler.scheduler.ParamProvider;
import blue.fen.scheduler.scheduler.TaskNotifier;
import blue.fen.scheduler.utils.BFLog;
import blue.fen.scheduler_annotation.SchedulerTask;

/**
 * <p>创建时间：2024/02/18 （星期日｝</p>
 * <p>作者： blue_fen</p>
 * <p>描述：</p>
 */
@SchedulerTask(projects = {"test", "test2"})
public class B implements ISchedulerTask {
    @Override
    public void perform(
            String projectName,
            @NonNull ParamProvider param,
            @Nullable TaskNotifier notifier
    ) {
        BFLog.d("执行了B");
    }
}
