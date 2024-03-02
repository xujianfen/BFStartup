package blue.fen.scheduler_annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * <p>创建时间：2024/03/03 （星期日｝</p>
 * <p>作者： blue_fen</p>
 * <p>描述：</p>
 */
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.CLASS)
public @interface AliasTasks {
    AliasTask[] value();
}
