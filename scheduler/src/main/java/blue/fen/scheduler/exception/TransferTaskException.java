package blue.fen.scheduler.exception;

import androidx.annotation.NonNull;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * <p>创建时间：2024/02/16 （星期五｝</p>
 * <p>作者： blue_fen</p>
 * <p>描述：转移任务异常，若依赖节点发生异常，且当前任务节点未忽略异常，则会得到依赖节点传递下来的异常
 * 并跳过任务的执行流程，继续向下传递异常</p>
 */
public class TransferTaskException extends RuntimeException {
    public TransferTaskException(String name, Throwable cause) {
        super(cause);
        this.name = name;
        merge(name, cause);
    }

    public TransferTaskException() {
    }

    String name;

    Map<String, Throwable> throwableMap = new ConcurrentHashMap<>();

    public String name() {
        return name;
    }

    public int size() {
        return throwableMap.size();
    }

    public Set<String> names() {
        return throwableMap.keySet();
    }

    public void merge(String name, Throwable cause) {
        if (cause instanceof TransferTaskException) {
            merge((TransferTaskException) cause);
        } else {
            pull(name, cause);
        }
    }

    public void merge(TransferTaskException cause) {
        throwableMap.putAll(cause.throwableMap);
    }

    public void pull(String name, Throwable cause) {
        throwableMap.put(name, cause);
    }

    @NonNull
    @Override
    public String getMessage() {
        StringBuilder builder = new StringBuilder()
                .append("\n|\tThrowableSize：")
                .append(throwableMap.size());
        int i = 0;
        for (Map.Entry<String, Throwable> entry : throwableMap.entrySet()) {
            builder.append("\n|\t")
                    .append("(")
                    .append(++i)
                    .append(") Task[")
                    .append(entry.getKey())
                    .append("] Error: ")
                    .append(entry.getValue());
        }
        return builder.toString();
    }
}