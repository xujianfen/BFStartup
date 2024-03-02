package blue.fen.demo.task;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import blue.fen.scheduler.scheduler.ISchedulerTask;
import blue.fen.scheduler.scheduler.ParamProvider;
import blue.fen.scheduler.scheduler.TaskNotifier;
import blue.fen.scheduler.utils.BFLog;
import blue.fen.scheduler_annotation.SchedulerTask;

/**
 * 创建时间：2023/12/31 （星期日｝
 * 作者： blue_fen
 * 描述：
 */
@SchedulerTask(projects = "test")
public class A implements ISchedulerTask {
    @Override
    public void perform(String projectName,
                        @NonNull ParamProvider param,
                        @Nullable TaskNotifier notifier
    ) throws InterruptedException {
        BFLog.d("执行了A");
        Thread.sleep(6000);
    }

    @Override
    public boolean needBlock() {
        return true;
    }
}
