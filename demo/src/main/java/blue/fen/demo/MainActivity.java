package blue.fen.demo;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.TextView;

import blue.fen.demo.executoasr.MyExecutoasr;
import blue.fen.demo.task.A;
import blue.fen.demo.task.B;
import blue.fen.scheduler.scheduler.decorator.AliasTask;
import blue.fen.scheduler.BFConfig;
import blue.fen.scheduler.BFScheduler;
import blue.fen.scheduler.BFTaskFinder;
import blue.fen.scheduler.listener.project.AProjectLifecycleObserver;
import blue.fen.scheduler.listener.project.ExtraDataRecords;
import blue.fen.scheduler.listener.project.ProjectLifecycleObservable;
import blue.fen.scheduler.listener.task.ATaskLifecycleObserver;
import blue.fen.scheduler.scheduler.ISchedulerTask;
import blue.fen.scheduler.scheduler.SchedulerTask;
import blue.fen.scheduler.utils.BFLog;
import blue.fen.startup.BFProjectParser;

public class MainActivity extends AppCompatActivity {
    private int count;
    private int finishCount;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        //不使用清单文件注册，可以用这种方式执行启动项目的任务，当然也可以是其他项目
        BFScheduler.Build scheduler = BFProjectParser.parserXmlForScheduler(this, R.xml.startup);
        if (scheduler != null) {
            scheduler.copyWithProject()
                    .name("customStartup")
                    .commit()
                    .submit()
                    .execute();
        }

        BFScheduler.Build build = BFScheduler.build()
                .newConfig()
                .executor(new MyExecutoasr())
                .commit();
        build.newProject()
                .name("test")
                .register(BFTaskFinder.findAllTask("test"))
                .register(AliasTask.builder().task(new A()).dependencies("B2").build())
                .register(AliasTask.builder().alias("B2").task(new B()).build())
                .commit()
                .submit()
                .execute();
        build.newProject()
                .name("test2")
                .register(BFTaskFinder.findAllTask("test2"))
                .commit()
                .submit()
                .execute();

        TextView textView = findViewById(R.id.count);
        Handler mH = new Handler(Looper.getMainLooper());
//        BFConfig.clearFlag(BFConfig.FLAG_EXTRA_DATA_AUTO_CLEAR);
//        BFConfig.setFlags(FLAG_DEBUG | FLAG_DETAIL_LOG_MODE | SIMPLE_CHECK);
//        boolean debug = BFConfig.isDebug();
//        int logMode = BFConfig.logMode();
//        int check = BFConfig.checkCycleMode();
//        BFConfig.setFlag(BFConfig.LOG_MODE_MASK, BFConfig.FLAG_SIMPLE_LOG_MODE);
//        boolean debug2 = BFConfig.isDebug();
//        int logMode2 = BFConfig.logMode();
//        int check2 = BFConfig.checkCycleMode();
////        BFConfig.clearFlag(BFConfig.DEBUG_MAKE);
////        boolean debug3 = BFConfig.isDebug();
////        int logMode3 = BFConfig.logMode();
////        int check3 = BFConfig.checkCycleMode();
//        BFConfig.setFlag(BFConfig.CHECK_CYCLE_MODE_MASK, BFConfig.SCC_CHECK);
//        boolean debug4 = BFConfig.isDebug();
//        int logMode4 = BFConfig.logMode();
//        int check4 = BFConfig.checkCycleMode();
        ProjectLifecycleObservable.getInstance().registerObserver(new AProjectLifecycleObserver() {
            @Override
            public void prepare(String project) {
                ExtraDataRecords.ResidualRecord record = ExtraDataRecords.getInstance().getProject(project);
                if (record == null) {
                    BFLog.w("project prepare record=null");
                    return;
                }
                long s = record.startTime();
                long e = record.endTime();
                Throwable t = record.throwable();
                BFLog.d("project prepare s=" + s + ",e=" + e + ", t=" + t);
            }

            @Override
            public void finish(String project) {
                ExtraDataRecords.ResidualRecord record = ExtraDataRecords.getInstance().getProject(project);
                if (record == null) {
                    BFLog.w("project finish record=null");
                    return;
                }
                long s = record.startTime();
                long e = record.endTime();
                Throwable t = record.throwable();
                BFLog.d("project finish s=" + s + ", e=" + e);
            }

            @Override
            public void before(String project, ISchedulerTask task) {
                ExtraDataRecords.ResidualRecord record = ExtraDataRecords.getInstance().getRecord(project, task.name());
                if (record == null) {
                    BFLog.w("project before record=null");
                    return;
                }
                long s = record.startTime();
                long e = record.endTime();
                Throwable t = record.throwable();
                BFLog.d("project before s=" + s + ", e=" + e + ", t=" + t);
            }

            @Override
            public void handleError(String project, ISchedulerTask task, Exception e) {
                super.handleError(project, task, e);
            }
        });
        mH.postDelayed(new Runnable() {
            @Override
            public void run() {
                textView.setText((count++) + " :: " + finishCount);
                mH.postDelayed(this, 1000);
            }
        }, 1000);
    }

    public void execute(View view) {
        test(false);
    }

    public void enqueue(View view) {
        test(true);
    }

    public void test(boolean async) {
        long start = System.nanoTime();
        SchedulerTask.Builder builder = SchedulerTask.newBuilder()
                .task((projectName, taskName, param, notifier) -> {
                    Thread.sleep(2000);
                    if ("2".equals(taskName)) {
                        param.setParam("hhhhhhh");
                    }
                    long end = System.nanoTime();
                    BFLog.d("执行了[" + taskName + "] 耗时：" + (end - start) / 1000000000);
                    if ("5".equals(taskName) || "7".equals(taskName)) {
                        Object args = param.getParam("2");
                        BFLog.d(taskName + "获得了2的参数：" + args);
//                        ExtraDataRecords.getInstance().clear();
                        param.clearParam("2");
                    }
                    if ("2".equals(taskName) || "4".equals(taskName)) {
                        throw new Exception(taskName + " Error!!!");
                    }
                });
        BFScheduler scheduler = BFScheduler.build()
                .newConfig()
                .awaitOutTime(BFConfig.INFINITE_AWAIT)
                .commit()
                .newProject()
                .register(
                        builder.name("1").isAsync(true).isBackground(true).needBlock(true).dependencies("2").build(),
                        builder.name("2").dependencies("3").build(),
                        builder.name("3").build(),
                        builder.name("4").dependencies("3").ignoreThrow(true).build(),
                        builder.name("5").dependencies("2", "4").ignoreThrow(false).build(),
                        builder.name("6").needBlock(false).dependencies("2").ignoreThrow(true).build(),
                        builder.name("7").dependencies("5").build()
                )
                .commit()
                .submit();
        BFLog.d(scheduler.name() + "::click");

        scheduler.getLifecycle().registerObserver(new ATaskLifecycleObserver() {
            @Override
            public void after(ISchedulerTask task) {
                super.after(task);
                ExtraDataRecords.ResidualRecord record = ExtraDataRecords.getInstance().getRecord(scheduler.name(), task.name());
                if (record == null) {
                    BFLog.w("after record=null");
                    return;
                }
                long s = record.startTime();
                long e = record.endTime();
                Throwable t = record.throwable();
                BFLog.d("after s=" + s + ", e=" + e + ", t=" + t);
            }

            @Override
            public void error(ISchedulerTask task, Throwable throwable) {
                ExtraDataRecords.ResidualRecord record = ExtraDataRecords.getInstance().getRecord(scheduler.name(), task.name());
                if (record == null) {
                    BFLog.w("error record=null");
                    return;
                }
                long s = record.startTime();
                long e = record.endTime();
                Throwable t = record.throwable();
                BFLog.err("error s=" + s + ", e=" + e + ", t=" + t);
            }

            @Override
            public void finish() {
                super.finish();
                ExtraDataRecords.ResidualRecord record = ExtraDataRecords.getInstance().getProject(scheduler.name());
                if (record == null) {
                    BFLog.w("finish record=null");
                    return;
                }
                long s = record.startTime();
                long e = record.endTime();
                Throwable t = record.throwable();
                BFLog.d("finish s=" + s + ", e=" + e + ", t=" + t);
            }
        });
        if (async) {
            scheduler.enqueue();
        } else {
            scheduler.execute();
        }
        new Thread(() -> {
            scheduler.await();
            BFLog.err(scheduler.name() + "::ChildThread等待结束");
        }).start();
        try {
            scheduler.await();
            BFLog.err(scheduler.name() + "::MainThread等待结束");
            finishCount++;
        } catch (Exception e) {
            BFLog.err("异常结束");
            BFLog.err(e);
        }
    }
}