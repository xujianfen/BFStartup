package blue.fen.scheduler.block;

import blue.fen.scheduler.BFScheduler;
import blue.fen.scheduler.utils.BFClass;

/**
 * <p>创建时间：2023/12/31 （星期日｝</p>
 * <p>作者： blue_fen</p>
 * <p>描述：任务阻塞接口，调用{@linkplain BFScheduler#await()} ()}时，阻塞调用栈的处理逻辑</p>
 */
public interface Block extends BFClass.BFCleanable {
    void setName(String name);

    String getName();

    void setTimeout(int timeout);

    void tryIncrement(BFScheduler.Node node);

    void tryGenerateLatch();

    boolean waiting();

    void tryDecrement(BFScheduler.Node node);

    /**
     * @noinspection UnusedReturnValue
     */
    boolean await();

    BFClass.Default<Block> DEFAULT = BFClass.defaultImpl(TaskBlock::new);
}
