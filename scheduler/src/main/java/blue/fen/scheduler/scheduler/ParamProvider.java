package blue.fen.scheduler.scheduler;

/**
 * <p>创建时间：2024/02/17 （星期六｝</p>
 * <p>作者： blue_fen</p>
 * <p>描述：任务参数提供者</p>
 *
 * @noinspection UnusedReturnValue
 */
public interface ParamProvider {
    /**
     * 设置当前任务的参数
     */
    boolean setParam(Object args);

    /**
     * 清除目标任务的所有参数
     *
     * @param task 目标任务
     * @return 若目标任务存在，即清除成功，返回true，否则返回false
     */
    boolean clearParam(String task);

    /**
     * @param task 目标任务
     * @return 返回目标任务设置的参数，若目标任务不存在或者目标任务未设置参数，则返回null
     */
    Object getParam(String task);
}
