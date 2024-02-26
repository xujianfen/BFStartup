package blue.fen.startup;

/**
 * <p>创建时间：2024/02/18 （星期日｝</p>
 * <p>作者： blue_fen</p>
 * <p>描述：</p>
 */
public class BFStartupException extends RuntimeException {
    public BFStartupException(Throwable cause) {
        super(cause);
    }

    public BFStartupException(String message) {
        super(message);
    }
}
