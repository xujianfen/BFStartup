package blue.fen.scheduler.listener.project;

import static blue.fen.scheduler.BFConfig.FLAG_EXTRA_DATA_AUTO_CLEAR;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import blue.fen.scheduler.BFConfig;
import blue.fen.scheduler.exception.TransferTaskException;
import blue.fen.scheduler.scheduler.ISchedulerTask;
import blue.fen.scheduler.utils.BFClass;
import blue.fen.scheduler.utils.BFSingleton;

/**
 * <p>创建时间：2024/02/16 （星期五｝</p>
 * <p>作者： blue_fen</p>
 * <p>描述：记录项目中的额外数据（耗时统计，任务间的参数传递等）</p>
 *
 * @noinspection unused
 */
public class ExtraDataRecords implements IProjectLifecycleObserver {
    private static final BFSingleton<ExtraDataRecords> sInstance =
            BFClass.singleton(ExtraDataRecords::new);

    private ExtraDataRecords() {
    }

    public static ExtraDataRecords getInstance() {
        return sInstance.get();
    }

    private final Map<String, ProjectRecord> projectRecordMap = new HashMap<>();

    private ProjectRecord getProjectRecordNoNull(String project) {
        ProjectRecord record = projectRecordMap.get(project);
        if (record == null) {
            record = new ProjectRecord(project);
            projectRecordMap.put(project, record);
        }
        return record;
    }

    public boolean canParam(ResidualRecord record) {
        return record instanceof TaskRecord;
    }

    public synchronized boolean setParam(ResidualRecord record, Object args) {
        if (canParam(record)) {
            ((TaskRecord) record).setParam(args);
            return true;
        }
        return false;
    }

    public synchronized Object getParam(ResidualRecord record) {
        return canParam(record) ? ((TaskRecord) record).getParam() : null;
    }

    public synchronized boolean setParam(String project, String task, Object args) {
        TaskRecord record = (TaskRecord) getTask(project, task);
        if (record == null) return false;
        record.setParam(args);
        return true;
    }

    public synchronized Object getParam(String project, String task) {
        TaskRecord record = (TaskRecord) getTask(project, task);
        return record != null ? record.getParam() : null;
    }

    public synchronized ResidualRecord getRecord(@NonNull String project, @Nullable String task) {
        ProjectRecord pr = projectRecordMap.get(project);
        if (pr == null || task == null) return pr;
        return pr.taskRecordMap.get(task);
    }

    public synchronized ResidualRecord getProject(@NonNull String project) {
        return projectRecordMap.get(project);
    }

    public ResidualRecord getTask(@NonNull String project, @Nullable String task) {
        ResidualRecord record = getRecord(project, task);
        return record instanceof TaskRecord ? (TaskRecord) record : null;
    }

    public synchronized ResidualRecord removeTask(@NonNull String project, @NonNull String task) {
        ProjectRecord record = (ProjectRecord) getProject(project);
        return record == null ? null : record.taskRecordMap.remove(task);
    }

    /**
     * @noinspection UnusedReturnValue
     */
    public synchronized ResidualRecord removeProject(@NonNull String project) {
        return projectRecordMap.remove(project);
    }

    public synchronized void clear() {
        projectRecordMap.clear();
    }

    @Override
    public void closeout(String project) {
        if (BFConfig.isMatchFlag(FLAG_EXTRA_DATA_AUTO_CLEAR)) {
            removeProject(project);
        }
    }

    @Override
    public synchronized void prepare(String project) {
        getProjectRecordNoNull(project)
                .before();
    }

    @Override
    public synchronized void finish(String project) {
        getProjectRecordNoNull(project)
                .after();
    }

    @Override
    public synchronized void before(String project, ISchedulerTask task) {
        getProjectRecordNoNull(project)
                .getRecordNoNull(task)
                .before();
    }

    @Override
    public synchronized void after(String project, ISchedulerTask task) {
        getProjectRecordNoNull(project)
                .getRecordNoNull(task)
                .after();
    }

    @Override
    public synchronized void error(String project, ISchedulerTask task, Throwable throwable) {
        getProjectRecordNoNull(project)
                .getRecordNoNull(task)
                .error(throwable);
    }

    @Override
    public synchronized void handleError(String project, ISchedulerTask task, Exception e) {
        getProjectRecordNoNull(project)
                .getRecordNoNull(task)
                .error(e);
    }

    public static abstract class ResidualRecord {
        final String name;
        long startTime;
        long endTime;

        Throwable throwable;

        ResidualRecord(String name) {
            this.name = name;
        }

        void before() {
            startTime = System.nanoTime();
        }

        void after() {
            endTime = System.nanoTime();
        }

        void error(Throwable t) {
            throwable = t;
            endTime = System.nanoTime();
        }

        public long startTime() {
            return startTime;
        }

        public long endTime() {
            return endTime;
        }

        public Throwable throwable() {
            return throwable;
        }
    }

    static class TaskRecord extends ResidualRecord {
        ProjectRecord project;

        TaskRecord(ProjectRecord project, String name) {
            super(name);
            this.project = project;
        }

        void error(Throwable t) {
            super.error(t);
            project.error(new TransferTaskException(name, t));
        }

        Object param;

        public Object getParam() {
            return param;
        }

        void setParam(Object param) {
            this.param = param;
        }
    }

    static class ProjectRecord extends ResidualRecord {
        final Map<String, TaskRecord> taskRecordMap = new HashMap<>();

        ProjectRecord(String name) {
            super(name);
            before();
        }

        public Set<String> taskNameSet() {
            return taskRecordMap.keySet();
        }

        public int size() {
            return taskRecordMap.size();
        }

        ResidualRecord getRecordNoNull(ISchedulerTask task) {
            if (task == null) {
                return this;
            }

            String name = task.name();
            TaskRecord record = taskRecordMap.get(name);
            if (record == null) {
                record = new TaskRecord(this, name);
                taskRecordMap.put(name, record);
            }
            return record;
        }

        @Override
        void error(Throwable t) {
            if (t instanceof TransferTaskException) {
                if (!(throwable instanceof TransferTaskException)) {
                    throwable = new TransferTaskException();
                }
                TransferTaskException tte = (TransferTaskException) t;
                ((TransferTaskException) throwable).pull(tte.name(), tte);
            }
        }
    }
}
