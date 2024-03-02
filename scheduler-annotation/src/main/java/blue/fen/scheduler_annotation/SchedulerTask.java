package blue.fen.scheduler_annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * <p>创建时间：2024/03/01 （星期五｝</p>
 * <p>作者： blue_fen</p>
 * <p>描述：需要被注解处理器处理的任务必须标记该注解</p>
 */
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.CLASS)
public @interface SchedulerTask {
    String[] projects(); //所属项目名
}
