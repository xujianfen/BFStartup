package blue.fen.scheduler.scheduler;

/**
 * <p>创建时间：2024/01/01 （星期一｝</p>
 * <p>作者： blue_fen</p>
 * <p>描述：任务提供者</p>
 */
public interface ITaskProvider {
    String name();
    
    ISchedulerTask task();
}
