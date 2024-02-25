package blue.fen.scheduler.scheduler;

/**
 * <p>创建时间：2023/12/31 （星期日｝</p>
 * <p>作者： blue_fen</p>
 * <p>描述：任务通知者</p>
 */
public interface TaskNotifier {
    boolean proceed();

    boolean error(Exception e);
}
