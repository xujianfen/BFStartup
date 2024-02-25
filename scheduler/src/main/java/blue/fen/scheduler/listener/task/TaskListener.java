package blue.fen.scheduler.listener.task;

import blue.fen.scheduler.distributor.IDispatcher;
import blue.fen.scheduler.utils.BFClass;

/**
 * <p>创建时间：2024/01/01 （星期一｝</p>
 * <p>作者： blue_fen</p>
 * <p>描述：</p>
 */
public interface TaskListener extends BFClass.BFCleanable {
    void before(IDispatcher distributor);

    void after(IDispatcher distributor);

    void throwable(IDispatcher distributor, Throwable throwable);

    void finish();
}
