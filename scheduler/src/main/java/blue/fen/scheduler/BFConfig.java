package blue.fen.scheduler;

import androidx.annotation.IntDef;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import blue.fen.scheduler.priority.PriorityProvider;

/**
 * <p>创建时间：2023/12/31 （星期日｝</p>
 * <p>作者： blue_fen</p>
 * <p>描述：配置信息</p>
 *
 * @noinspection unused
 */
public final class BFConfig {
    /**
     * DEBUG模式开关掩码
     */
    public static final int DEBUG_MASK = 0x1;

    /**
     * 日志打印模式掩码
     */
    public static final int LOG_MODE_MASK = 0x6;

    /**
     * 环检测模式掩码（优先级低于项目配置）
     */
    public static final int CHECK_CYCLE_MODE_MASK = 0x38;

    /**
     * 项目额外数据的控制掩码 <br/>
     * 控制项：{@linkplain BFConfig#FLAG_EXTRA_DATA_ENABLE}、
     * {@linkplain BFConfig#FLAG_EXTRA_DATA_AUTO_CLEAR}
     */
    public static final int EXTRA_DATA_MASK = 0xC0;

    /**
     * 控制多个flag的所有掩码的集合掩码
     */
    private static final int MULTI_FLAG_MASK = EXTRA_DATA_MASK;

    public static boolean isMulti(int mask) {
        return (mask & MULTI_FLAG_MASK) != 0;
    }

    public static final int PRIORITY_DEFAULT = Integer.MAX_VALUE - 2;
    public static final int PRIORITY_MAX = Integer.MAX_VALUE;
    public static final int PRIORITY_MIN = Integer.MIN_VALUE;

    public static final int DEFAULT_PRIORITY_MAX = PriorityProvider.Default.MAX_PRIORITY;
    public static final int DEFAULT_PRIORITY_MIN = PriorityProvider.Default.MIN_PRIORITY;
    public static final int DEFAULT_PRIORITY_DEFAULT = PriorityProvider.Default.DEFAULT_PRIORITY;

    /**
     * 默认检查模式 <br/>
     * DEBUG模式使用{@linkplain BFConfig#CYCLE_CHECK}，RELEASE模式使用{@linkplain BFConfig#NO_CHECK}
     */
    public static final int DEFAULT_CHECK = 0x8;

    /**
     * 不检查循环依赖（RELEASE模式推荐开启）
     */
    public static final int NO_CHECK = 0x10;

    /**
     * 检查是否存在环（RELEASE模式需要开启检查时， 推荐开启）
     */
    public static final int SIMPLE_CHECK = 0x18;

    /**
     * 检查所有强连通分量
     */
    public static final int SCC_CHECK = 0x20;

    /**
     * 检查所有简单环（DEBUG模式推荐开启）
     */
    public static final int CYCLE_CHECK = 0x28;

    /**
     * 环检查模式
     */
    @IntDef({DEFAULT_CHECK, NO_CHECK, SIMPLE_CHECK, SCC_CHECK, CYCLE_CHECK})
    @Retention(RetentionPolicy.SOURCE)
    public @interface CheckCycleMode {
    }

    /**
     * 无限等待
     */
    public static final int INFINITE_AWAIT = -1;

    /**
     * 不等待
     */
    public static final int NO_AWAIT = 0;

    /**
     * 默认等待10秒
     */
    public static final int DEFAULT_AWAIT_OUT_TIME = 10 * 1000;

    /**
     * 是否开启DEBUG模式
     */
    public static final int FLAG_DEBUG = 0x1;


    /**
     * 开启详细日志（任务执行流程、耗时统计、任务详情）<br/>
     * <strong>注意：{@linkplain blue.fen.scheduler.listener.project.ProjectLifecycleObservable}
     * 初始化后，该flag的设置会被忽略</strong>
     */
    public static final int FLAG_DETAIL_LOG_MODE = 0x2;

    /**
     * 开启简单日志（打印任务执行流程）<br/>
     * <strong>注意：{@linkplain blue.fen.scheduler.listener.project.ProjectLifecycleObservable}
     * 初始化后，该flag的设置会被忽略</strong>
     */
    public static final int FLAG_SIMPLE_LOG_MODE = 0x4;

    /**
     * 开启项目额外数据 <br/>
     * <strong>注意：{@linkplain blue.fen.scheduler.listener.project.ProjectLifecycleObservable}
     * 初始化后，该flag的设置会被忽略</strong>
     */
    public static final int FLAG_EXTRA_DATA_ENABLE = 0x40;


    /**
     * 是否开启项目额外数据的自动清理
     */
    public static final int FLAG_EXTRA_DATA_AUTO_CLEAR = 0x80;

    /**
     * 默认配置信息
     */
    public static int DEFAULT_CONFIG_FLAGS =
            FLAG_DEBUG
                    | FLAG_DETAIL_LOG_MODE
                    | DEFAULT_CHECK
                    | EXTRA_DATA_MASK;

    /**
     * 配置信息
     */
    private static int configFlags = DEFAULT_CONFIG_FLAGS;

    /**
     * 替换所有标识
     */
    public static void setFlags(int flags) {
        configFlags = flags;
    }

    /**
     * 获取所有标识
     */
    public static int getFlags() {
        return configFlags;
    }

    /**
     * 设置目标mask的值为目标flag
     */
    public static void setFlag(int mask, int flag) {
        configFlags = (configFlags & ~mask) | (flag & mask);
    }

    /**
     * 清空目标mask的所有内容
     */
    public static void clearFlag(int mask) {
        setFlag(mask, 0);
    }

    /**
     * 获取目标mask的值
     */
    public static int getFlag(int mask) {
        return configFlags & mask;
    }

    /**
     * 判断目标mask是否与目标flag匹配
     */
    public static boolean isMatch(int mask, int flag) {
        return (configFlags & mask) == flag;
    }

    /**
     * 判断目标flag是否开启
     */
    public static boolean isMatchFlag(int flag) {
        assert flag != 0 && (MULTI_FLAG_MASK & flag) == flag;
        return isMatch(flag, flag);
    }

    /**
     * 是否为debug模式
     */
    public static boolean isDebug() {
        return isMatch(DEBUG_MASK, FLAG_DEBUG);
    }

    /**
     * 日志打印模式
     */
    public static int logMode() {
        return isDebug() ? getFlag(LOG_MODE_MASK) : 0;
    }

    /**
     * 环检测模式
     */
    public static int checkCycleMode() {
        return getFlag(CHECK_CYCLE_MODE_MASK);
    }
}
