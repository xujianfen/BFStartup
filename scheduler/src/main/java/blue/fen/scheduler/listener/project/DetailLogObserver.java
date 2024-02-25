package blue.fen.scheduler.listener.project;

import android.annotation.SuppressLint;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import blue.fen.scheduler.BFConfig;
import blue.fen.scheduler.exception.TransferTaskException;
import blue.fen.scheduler.scheduler.ISchedulerTask;
import blue.fen.scheduler.utils.BFClass;
import blue.fen.scheduler.utils.BFLog;
import blue.fen.scheduler.listener.project.ExtraDataRecords.*;
import blue.fen.scheduler.utils.BFSingleton;

/**
 * <p>创建时间：2024/02/16 （星期五｝</p>
 * <p>作者： blue_fen</p>
 * <p>描述：详细日志</p>
 */
public class DetailLogObserver implements IProjectLifecycleObserver {
    private static final BFSingleton<DetailLogObserver> sInstance =
            BFClass.singleton(DetailLogObserver::new);

    private DetailLogObserver() {
    }

    public static DetailLogObserver getInstance() {
        return sInstance.get();
    }

    @Override
    public void prepare(String project) {
        print(Method.PREPARE, project, null);
    }

    @Override
    public void finish(String project) {
        print(Method.FINISH, project, null);
    }

    @Override
    public void before(String project, ISchedulerTask task) {
        print(Method.BEFORE, project, task);
    }

    @Override
    public void after(String project, ISchedulerTask task) {
        print(Method.AFTER, project, task);
    }

    @Override
    public void error(String project, ISchedulerTask task, Throwable throwable) {
        print(Method.ERROR, project, task);
    }

    @Override
    public void handleError(String project, ISchedulerTask task, Exception e) {
        print(Method.HANDLE_ERROR, project, task);
    }

    private void printLine(StringBuilder builder, String key, Object value) {
        builder.append("|\t")
                .append(key)
                .append(": [")
                .append(value)
                .append("]")
                .append("\n");
    }

    @SuppressLint("DefaultLocale")
    private String printCosTime(long startTime, long endTime) {
        float accuracy = 1000 * 1000 * 1000f;
        return String.format("%.3fs", (endTime - startTime) / accuracy);
    }

    private void print(Method method, String project, ISchedulerTask task) {
        if (!BFConfig.isDebug()) return;

        String taskName = task == null ? null : task.name();
        ResidualRecord record = ExtraDataRecords.getInstance().getRecord(project, taskName);
        //record为空时退化为简单日志
        if (record == null) {
            String msg = "DetailLog[" + project + "] " + method.name() +
                    (taskName != null ? "(" + taskName + ")" : "");
            if (method.isError()) {
                BFLog.err(msg);
            } else {
                BFLog.d(msg);
            }
            return;
        }

        StringBuilder builder = new StringBuilder()
                .append("|================================ ")
                .append(record.getClass().getSimpleName())
                .append("::")
                .append(method.name())
                .append(" ================================")
                .append("\n");

        printLine(builder, "name", record.name);
        if (record.startTime > 0) {
            printLine(builder, "startTime", record.startTime);

            if (record.endTime > 0) {
                printLine(builder, "endTime", record.endTime);
                printLine(builder, "cosTime", printCosTime(record.startTime, record.endTime));
            }
        }

        if (record instanceof TaskRecord) {
            if (record.throwable != null) {
                printLine(builder, "throwable", record.throwable);
            }

            ProjectRecord pr = ((TaskRecord) record).project;
            builder.append("|------- project info -------\n");
            printLine(builder, "name", pr.name);
            printLine(builder, "startTime", pr.startTime);
            if (record.endTime > 0) {
                printLine(builder, "cosTime", printCosTime(pr.startTime, record.endTime));
            }
            printLine(builder, "count", pr.size());
        } else if (record instanceof ProjectRecord) {
            ProjectRecord pr = (ProjectRecord) record;
            printLine(builder, "size", pr.size());

            Set<String> totalTask = null;
            if (pr.size() > 0) {
                totalTask = pr.taskNameSet();
                printLine(builder, "totalTask", totalTask);
            }

            if (totalTask != null) {
                //noinspection unchecked
                Set<String> throwTask = Collections.EMPTY_SET;
                Set<String> successTask = new HashSet<>(totalTask);

                if (pr.throwable instanceof TransferTaskException) {
                    TransferTaskException tte = (TransferTaskException) record.throwable;
                    throwTask = tte.names();
                    successTask.removeAll(throwTask);
                }

                printLine(builder, "successSize", pr.size() - throwTask.size());
                printLine(builder, "successTask", successTask);
                printLine(builder, "throwSize", throwTask.size());
                printLine(builder, "throwTask", throwTask);
            }
        }

        if (task != null) {
            builder.append("|------- task info -----\n");
            printLine(builder, "name", task.name());
            printLine(builder, "priority", task.priority());
            if (task.enableThreadPriority()) {
                printLine(builder, "threadPriority", task.threadPriority());
            }

            List<?> list = task.dependencies();
            printLine(builder, "dependencies", list);
            printLine(builder, "dependenciesSize", list == null ? 0 : list.size());

            printLine(builder, "isAsync", task.isAsync());
            printLine(builder, "isBackground", task.isBackground());
            printLine(builder, "manualDispatch", task.manualDispatch());
            printLine(builder, "needBlock", task.needBlock());
            printLine(builder, "ignoreThrow", task.ignoreThrow());
        }

        builder.append("|============================ end ================================\n");

        if (method.isError()) {
            BFLog.err(builder.toString(), record.throwable);
        } else {
            BFLog.d(builder);
        }
    }
}
