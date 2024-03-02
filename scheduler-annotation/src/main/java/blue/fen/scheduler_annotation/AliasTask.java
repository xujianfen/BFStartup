package blue.fen.scheduler_annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * <p>创建时间：2024/03/02 （星期六｝</p>
 * <p>作者： blue_fen</p>
 * <p>描述：任务别名</p>
 * <p>考虑到任务需要重复出现在同一项目中的场景，使用原有逻辑可能较为麻烦，所以提供该注解，方便</p>
 * <p>任务在同一项目中重复使用</p>
 * <strong>使用该注解必须标记{@linkplain SchedulerTask}</strong>
 */
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.CLASS)
@Repeatable(AliasTasks.class)
public @interface AliasTask {
    /**
     * 任务别名
     *
     * @return 默认为空，当该值为空时会自动生成别名，当该值为$时保持任务名
     */
    String value() default "";

    /**
     * 需要生成别名的项目集合
     *
     * @return <p>默认为空</p>
     * <p>当集合为空时，说明后续所有项目都需要使用该别名，这时其他的TaskAlias都会失效</p>
     */
    String[] projects() default {};

    /**
     * 设置别名依赖
     *
     * @return <p>默认为空</p>
     * <p>为空时，默认保持原有依赖，若设置{@linkplain AliasTask#noDependencies()}为true，则表示没有依赖</p>
     * <p>不为空时，设置的别名依赖将覆盖任务依赖，其中"$"可以表示依赖项目中保留原有名称的当前类型任务</p>
     */
    String[] dependencies() default {};

    /**
     * 当依赖项为空时，设置任务是无依赖还是保持原有依赖
     *
     * @return <p>默认为false</p>
     * <p>true 表示无依赖项</p>
     * <p>false 表示保持原有赖项</p>
     */
    boolean noDependencies() default false;
}
