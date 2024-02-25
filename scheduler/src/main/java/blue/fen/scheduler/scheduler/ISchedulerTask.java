package blue.fen.scheduler.scheduler;

import android.os.Process;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.List;

import blue.fen.scheduler.BFConfig;
import blue.fen.scheduler.BFScheduler;
import blue.fen.scheduler.distributor.IDispatcher;
import blue.fen.scheduler.listener.task.ITaskLifecycleObserver;

/**
 * <p>创建时间：2023/12/31 （星期日｝</p>
 * <p>作者： blue_fen</p>
 * <p>描述：任务接口</p>
 */
public interface ISchedulerTask {
    /**
     * 任务名
     */
    default String name() {
        return getClass().getName();
    }

    /**
     * 执行任务
     *
     * @param projectName 任务所属的项目名
     * @param param       参数提供者
     * @param notifier    分发器（{@link  blue.fen.scheduler.distributor.IDistributor}）启动前
     *                    或者{@link ISchedulerTask#manualDispatch()}={@code false}时，{@code event=null}
     */
    void perform(String projectName,
                 @NonNull ParamProvider param,
                 @Nullable TaskNotifier notifier
    ) throws Exception;

    /**
     * 是否为后台任务
     *
     * @return {@code true} - 使用线程池执行任务<br/>
     * {@code false} - 使用main线程执行任务<br/>
     * 默认值为{@code true}
     */
    default boolean isBackground() {
        return true;
    }

    /**
     * 当前任务依赖的任务集合，默认无依赖任务
     */
    default List<String> dependencies() {
        return null;
    }

    /**
     * <p>默认优先级</p>
     *
     * <strong>
     * 注意：
     * 该方法是执行任务的优先级，而不是任务线程的优先级，请不要与{@link ISchedulerTask#threadPriority()}混淆
     * </strong>
     *
     * @return 后台任务默认为 {@linkplain BFConfig#DEFAULT_PRIORITY_DEFAULT}；<br/>
     * 前台任务默认为 {@linkplain BFConfig#DEFAULT_PRIORITY_DEFAULT}{@code + 1}；
     */
    default int priority() {
        return BFConfig.PRIORITY_DEFAULT + (isBackground() ? 0 : 1);
    }

    /**
     * 是否开启线程优先级设置；若未开启或者任务线程为主线程，则{@link ISchedulerTask#threadPriority()}方法无效
     *
     * @return 默认值为false
     */
    default boolean enableThreadPriority() {
        return false;
    }

    /**
     * 设置执行线程的优先级，{@link ISchedulerTask#enableThreadPriority()}={@code false}时，该方法无效
     *
     * @return 当 {@link ISchedulerTask#isBackground()}={@code true}，
     * 默认值为{@link Process#THREAD_PRIORITY_BACKGROUND}；
     * 否则默认值为{@link Process#THREAD_PRIORITY_FOREGROUND}
     */
    default int threadPriority() {
        return isBackground() ? Process.THREAD_PRIORITY_BACKGROUND : Process.THREAD_PRIORITY_FOREGROUND;
    }

    /**
     * 是否需要阻塞主线程，在超时前，若所有needWait方法执行结束，将唤醒{@link BFScheduler#await}方法
     */
    default boolean needBlock() {
        return false;
    }

    /**
     * <p>
     * 是否异步执行，若异步执行，则启动分发器[{@link  blue.fen.scheduler.distributor.IDistributor}]。
     * 分发器启动后，该值不再影响任务的执行。
     * </p>
     * <strong>
     * 注意：分发器启动前，若{@code isAsync}={@code true}时，发现任务为前台任务
     * ({@link ISchedulerTask#isBackground}={@code  false})，且启动任务和执行任务的线程都为主线程，
     * 那么将不会启动分发器，相当于{@code isAsync}={@code false}<br/>
     * 若不满足默认逻辑，可自定义{@link IDispatcher}
     * </strong>
     *
     * @return {@code true}表示启动分发器<br/>
     * {@code false}表示不启动分发器<br/>
     * 默认值与{@link ISchedulerTask#isBackground()}相同
     */
    default boolean isAsync() {
        return isBackground();
    }

    /**
     * 是否启动手动调度，{@link ISchedulerTask#isAsync()}={@code false}时，不建议开启，
     * 因为只有任务执行到首个{@link ISchedulerTask#isAsync()}={@code true}时，该值才会生效
     */
    default boolean manualDispatch() {
        return false;
    }

    /**
     * <p>是否忽略依赖任务的异常</p>
     *
     * @return {@code true} - 依赖任务出现异常，不会影响当前任务的正常执行<br/>
     * {@code false} - 若依赖任务出现异常，当前任务会把依赖任务的异常包装到
     * {@linkplain blue.fen.scheduler.exception.TransferTaskException}
     * 然后抛出该异常，不会执行任务逻辑<br/>
     * 默认值为{@code false}<br/>
     */
    default boolean ignoreThrow() {
        return false;
    }
}
