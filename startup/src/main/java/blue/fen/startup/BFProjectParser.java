package blue.fen.startup;

import android.content.ComponentName;
import android.content.Context;
import android.content.pm.PackageManager;
import android.content.pm.ProviderInfo;
import android.content.res.XmlResourceParser;
import android.os.Bundle;
import android.text.TextUtils;

import androidx.annotation.NonNull;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import blue.fen.scheduler.BFConfig;
import blue.fen.scheduler.BFScheduler;
import blue.fen.scheduler.BFTaskConfig;
import blue.fen.scheduler.TaskFlow;
import blue.fen.scheduler.block.Block;
import blue.fen.scheduler.distributor.BFTaskExecutor;
import blue.fen.scheduler.distributor.IDistributor;
import blue.fen.scheduler.priority.PriorityProvider;
import blue.fen.scheduler.scheduler.ISchedulerTask;
import blue.fen.scheduler.scheduler.decorator.AliasTask;
import blue.fen.scheduler.utils.BFLog;

/**
 * <p>创建时间：2024/02/19 （星期一｝</p>
 * <p>作者： blue_fen</p>
 * <p>描述：</p>
 *
 * @noinspection SameParameterValue, unused
 */
public class BFProjectParser {
    private static final String NAMESPACE = "http://schemas.android.com/apk/res-auto";

    /*******************************启动流程配置文件的TAG关键字****************************************/
    private static final String TAG_PROJECT = "project";
    private static final String TAG_CONFIG = "config";
    private static final String TAG_TASK = "task";


    /*******************************启动流程配置文件的属性关键字***************************************/
    private static final String ATTRIBUTE_PROJECT_NAME = "bfp_name";
    private static final String ATTRIBUTE_PROJECT_AUTO_CHECK_DEPENDENCIES = "bfp_autoCheckDependencies";
    private static final String ATTRIBUTE_PROJECT_EXCLUDE_SELF_CIRCULAR = "bfp_excludeSelfCircular";
    private static final String ATTRIBUTE_PROJECT_CHECK_CYCLE_MODE = "bfp_checkCycleMode";
    private static final String ATTRIBUTE_PROJECT_AWAIT_OUT_TIME = "bfp_awaitOutTime";
    private static final String ATTRIBUTE_PROJECT_PRIORITY_PROVIDER = "bfp_priorityProvider";
    private static final String ATTRIBUTE_PROJECT_BLOCK = "bfp_block";
    private static final String ATTRIBUTE_PROJECT_DISTRIBUTOR = "bfp_distributor";
    private static final String ATTRIBUTE_PROJECT_EXECUTOR = "bfp_executor";
    private static final String ATTRIBUTE_PROJECT_FLOW_PROVIDER = "bfp_flowProvider";
    private static final String ATTRIBUTE_TASK_ALIAS = "bft_alias";
    private static final String ATTRIBUTE_TASK_DEPENDENCIES = "bft_dependencies";
    private static final String ATTRIBUTE_CONFIG_DEBUG = "bfc_debug";
    private static final String ATTRIBUTE_CONFIG_LOG_MODE = "bfc_logMode";
    private static final String ATTRIBUTE_CONFIG_CHECK_CYCLE_MODE = "bfc_checkCycleMode";
    private static final String ATTRIBUTE_CONFIG_EXTRA_DATA_ENABLE = "bfc_extraDataEnable";
    private static final String ATTRIBUTE_CONFIG_EXTRA_DATA_AUTO_CLEAR = "bfc_extraDataAutoClear";

    /**
     * 获取启动任务的xml资源
     */
    private static int getProjectXmlResourceId(Context context) throws
            PackageManager.NameNotFoundException {
        ComponentName provider = new ComponentName(context, BFStartupProvider.class);
        PackageManager pm = context.getPackageManager();
        ProviderInfo providerInfo = pm.getProviderInfo(provider, PackageManager.GET_META_DATA);
        Bundle metaData = providerInfo.metaData;
        String startup = context.getString(R.string.bf_startup);
        if (metaData != null) {
            Set<String> keys = metaData.keySet();
            for (String key : keys) {
                if (startup.equals(key)) {
                    return metaData.getInt(key, -1);
                }
            }
        }
        return -1;
    }

    /**
     * 解析并运行初始化任务
     */
    @NonNull
    public static BFScheduler parserMetaData(Context context) throws Exception {
        int resourceId = getProjectXmlResourceId(context);
        if (resourceId == -1) {
            throw new BFStartupException("没有检查到启动任务的xml资源，请根据文档进行配置");
        }
        return parserXmlForSchedulerNoNull(context, resourceId).submit();
    }

    @NonNull
    public static BFScheduler.Build parserXmlForSchedulerNoNull(Context context, int resourceId) throws Exception {
        ProjectInfo projectInfo = parserXmlForProjectInfo(context, resourceId);
        return createScheduler(projectInfo);
    }

    public static BFScheduler.Build parserXmlForScheduler(Context context, int resourceId) {
        try {
            return parserXmlForSchedulerNoNull(context, resourceId);
        } catch (Exception e) {
            BFLog.err(e);
            return null;
        }
    }

    public static ProjectInfo parserXmlForProjectInfo(Context context, int resourceId) throws
            Exception {
        try (XmlResourceParser parser = context.getResources().getXml(resourceId)) {
            int eventType = parser.getEventType(); //获取事件类型
            while (eventType != XmlPullParser.END_DOCUMENT) {
                if (eventType == XmlPullParser.START_TAG) {
                    String tagName = parser.getName(); //获取当前结点名称
                    if (TAG_PROJECT.equals(tagName)) {
                        return readProject(context, parser);
                    } else if (TAG_CONFIG.equals(tagName)) {
                        int configFlags = readConfig(parser);
                        BFConfig.setFlags(configFlags);
                    }
                }
                eventType = parser.next();
            }

            String resourceName = context.getResources().getResourceEntryName(resourceId);
            throw new IllegalArgumentException("不规范的xml格式，请检查@xml/" + resourceName);
        }
    }

    /**
     * 根据{@linkplain BFProjectParser.ProjectInfo}创建{@linkplain BFScheduler}
     */
    public static BFScheduler.Build createScheduler(ProjectInfo projectInfo) {
        return BFScheduler.build()
                .newConfig()
                .copy(projectInfo.taskConfig())
                .commit()
                .newProject()
                .name(projectInfo.name())
                .register(projectInfo.taskList())
                .commit();
    }

    private static int readConfig(XmlResourceParser parser) throws
            IOException, XmlPullParserException {
        parser.require(XmlPullParser.START_TAG, null, TAG_CONFIG);
        int flag = 0;
        flag |= readConfigDebug(parser);
        flag |= readConfigLogMode(parser);
        flag |= readConfigCheckCycleMode(parser);
        flag |= readConfigExtraData(parser);
        //noinspection StatementWithEmptyBody
        while (parser.next() != XmlPullParser.END_TAG) ;
        return flag;
    }

    private static int readConfigDebug(XmlResourceParser parser) {
        boolean debug = parser.getAttributeBooleanValue(
                NAMESPACE, ATTRIBUTE_CONFIG_DEBUG, true);
        return debug ? BFConfig.FLAG_DEBUG : 0;
    }

    private static int readConfigCheckCycleMode(XmlResourceParser parser) {
        int checkCycleMode = parser.getAttributeIntValue(
                NAMESPACE, ATTRIBUTE_CONFIG_CHECK_CYCLE_MODE, 0);
        return readCheckCycleMode(checkCycleMode);
    }

    private static int readCheckCycleMode(int logMode) {
        switch (logMode) {
            case 0:
                return BFConfig.DEFAULT_CHECK;
            case 1:
                return BFConfig.NO_CHECK;
            case 2:
                return BFConfig.SIMPLE_CHECK;
            case 3:
                return BFConfig.SCC_CHECK;
            case 4:
                return BFConfig.CYCLE_CHECK;
            default:
                throw new IllegalArgumentException(logMode + "不是合法的LogMode");
        }
    }

    private static int readConfigLogMode(XmlResourceParser parser) {
        int logMode = parser.getAttributeIntValue(
                NAMESPACE, ATTRIBUTE_CONFIG_LOG_MODE, 0);
        switch (logMode) {
            case 0:
                return BFConfig.FLAG_DETAIL_LOG_MODE;
            case 1:
                return BFConfig.FLAG_SIMPLE_LOG_MODE;
            default:
                throw new IllegalArgumentException(logMode + "不是合法的CheckCycleMode");
        }
    }

    private static int readConfigExtraData(XmlResourceParser parser) {
        int flag = 0;

        boolean enableExtraData = parser.getAttributeBooleanValue(
                NAMESPACE, ATTRIBUTE_CONFIG_EXTRA_DATA_ENABLE, true);
        if (enableExtraData) {
            flag |= BFConfig.FLAG_EXTRA_DATA_ENABLE;
        }

        boolean autoClearExtraData = parser.getAttributeBooleanValue(
                NAMESPACE, ATTRIBUTE_CONFIG_EXTRA_DATA_AUTO_CLEAR, true);
        if (autoClearExtraData) {
            flag |= BFConfig.FLAG_EXTRA_DATA_AUTO_CLEAR;
        }

        return flag;
    }

    public static ProjectInfo readProject(Context context, XmlResourceParser parser) throws
            Exception {
        parser.require(XmlPullParser.START_TAG, null, TAG_PROJECT);

        List<ISchedulerTask> taskList = new ArrayList<>();
        String projectName = readName(parser, context);
        BFTaskConfig taskConfig = readTaskConfig(context, parser);

        while (parser.next() != XmlPullParser.END_TAG) {
            if (parser.getEventType() != XmlPullParser.START_TAG) {
                continue;
            }

            String tag = parser.getName();

            if (TAG_TASK.equals(tag)) {
                taskList.add(readTask(context, parser));
            } else {
                skip(parser);
            }
        }

        return new ProjectInfo(projectName, taskConfig, taskList);
    }

    private static void skip(XmlPullParser parser) throws XmlPullParserException, IOException {
        if (parser.getEventType() != XmlPullParser.START_TAG) {
            throw new IllegalStateException();
        }
        int depth = 1;
        while (depth != 0) {
            switch (parser.next()) {
                case XmlPullParser.END_TAG:
                    depth--;
                    break;
                case XmlPullParser.START_TAG:
                    depth++;
                    break;
            }
        }
    }

    public static class ProjectInfo {
        private final String name;

        private final BFTaskConfig taskConfig;

        private final List<ISchedulerTask> taskList;

        public ProjectInfo(String name,
                           BFTaskConfig taskConfig,
                           List<ISchedulerTask> taskList
        ) {
            this.name = name;
            this.taskConfig = taskConfig;
            this.taskList = taskList;
        }

        public String name() {
            return name;
        }

        public BFTaskConfig taskConfig() {
            return taskConfig;
        }

        public List<ISchedulerTask> taskList() {
            return taskList;
        }
    }

    private static String readName(XmlPullParser parser, Context context) {
        String defaultName = context.getString(R.string.bf_startup_default_name);
        return readName(parser, defaultName);
    }

    private static String readName(XmlPullParser parser, String defaultName) {
        String name = parser.getAttributeValue(NAMESPACE, ATTRIBUTE_PROJECT_NAME);
        return TextUtils.isEmpty(name) ? defaultName : name;
    }

    private static BFTaskConfig readTaskConfig(Context context, XmlResourceParser parser) throws Exception {
        return new BFTaskConfig()
                .autoCheckDependencies(readProjectBoolean(parser, ATTRIBUTE_PROJECT_AUTO_CHECK_DEPENDENCIES, true))
                .excludeSelfCircular(readProjectBoolean(parser, ATTRIBUTE_PROJECT_EXCLUDE_SELF_CIRCULAR, true))
                .checkCycleMode(readProjectCheckCycleMode(parser))
                .awaitOutTime(readProjectInt(context, parser, ATTRIBUTE_PROJECT_AWAIT_OUT_TIME, 0))
                .priorityProvider(readProjectClass(parser, ATTRIBUTE_PROJECT_PRIORITY_PROVIDER, PriorityProvider.class))
                .block(readProjectClass(parser, ATTRIBUTE_PROJECT_BLOCK, Block.class))
                .distributor(readProjectClass(parser, ATTRIBUTE_PROJECT_DISTRIBUTOR, IDistributor.class))
                .executor(readProjectClass(parser, ATTRIBUTE_PROJECT_EXECUTOR, BFTaskExecutor.class))
                .flowProvider(readProjectClass(parser, ATTRIBUTE_PROJECT_FLOW_PROVIDER, TaskFlow.Provider.class));
    }

    private static boolean readProjectBoolean(XmlResourceParser parser, String attribute, boolean defaultValue) {
        return parser.getAttributeBooleanValue(NAMESPACE, attribute, defaultValue);
    }

    private static int readProjectInt(XmlResourceParser parser, String attribute, int defaultValue) {
        return parser.getAttributeIntValue(NAMESPACE, attribute, defaultValue);
    }

    private static int readProjectInt(Context context, XmlResourceParser parser, String attribute, int defaultValue) {
        int resourceId = parser.getAttributeResourceValue(NAMESPACE, attribute, 0);
        if (resourceId == 0) {
            return readProjectInt(parser, attribute, defaultValue);
        } else {
            return context.getResources().getInteger(resourceId);
        }
    }

    private static String readProjectString(XmlResourceParser parser, String attribute) {
        return parser.getAttributeValue(NAMESPACE, attribute);
    }

    private static String readProjectString(Context context, XmlResourceParser parser, String attribute) {
        int resourceId = parser.getAttributeResourceValue(NAMESPACE, attribute, 0);
        if (resourceId == 0) {
            return readProjectString(parser, attribute);
        } else {
            return context.getResources().getString(resourceId);
        }
    }

    private static <T> T readProjectClass(XmlResourceParser parser, String attribute, Class<T> tClass) throws Exception {
        String className = parser.getAttributeValue(NAMESPACE, attribute);
        return className == null ? null : newInstance(className, tClass);
    }

    private static int readProjectCheckCycleMode(XmlResourceParser parser) {
        int checkCycleMode = parser.getAttributeIntValue(
                NAMESPACE, ATTRIBUTE_PROJECT_CHECK_CYCLE_MODE, 0);
        return readCheckCycleMode(checkCycleMode);
    }

    private static ISchedulerTask readTask(Context context, XmlResourceParser parser) throws Exception {
        parser.require(XmlPullParser.START_TAG, null, TAG_TASK);
        Map<String, Object> attribute = readAttribute(context, parser);
        String className = parser.nextText();
        className = className.trim();
        assert !TextUtils.isEmpty(className);
        ISchedulerTask task = newInstance(className, ISchedulerTask.class);
        return attribute != null ? parserAttribute(attribute, task) : task;
    }

    private static Map<String, Object> readAttribute(Context context, XmlResourceParser parser) {
        String alias = readProjectString(context, parser, ATTRIBUTE_TASK_ALIAS);
        if (TextUtils.isEmpty(alias)) return null;
        String dependencies = readProjectString(context, parser, ATTRIBUTE_TASK_DEPENDENCIES);
        Map<String, Object> attribute = new HashMap<>();
        attribute.put(ATTRIBUTE_TASK_ALIAS, alias);
        attribute.put(ATTRIBUTE_TASK_DEPENDENCIES, dependencies);
        return attribute;
    }

    private static ISchedulerTask parserAttribute(Map<String, Object> attribute, ISchedulerTask task) {
        return readAliasTask(attribute, task);
    }

    private static ISchedulerTask readAliasTask(Map<String, Object> attribute, ISchedulerTask task) {
        String alias = (String) attribute.get(ATTRIBUTE_TASK_ALIAS);
        if (TextUtils.isEmpty(alias)) {
            return null;
        }
        AliasTask.Builder aliasTask = AliasTask.builder().task(task);
        if (!"$$".equals(alias)) { //不自动生成别名
            aliasTask.alias(alias);
        }
        String dependencies = (String) attribute.get(ATTRIBUTE_TASK_DEPENDENCIES);
        if (!TextUtils.isEmpty(dependencies)) { //不保留原有依赖
            if ("$$".equals(dependencies)) {
                aliasTask.noDependencies(); //没有依赖项
            } else {
                //noinspection DataFlowIssue
                aliasTask.dependencies(dependencies.split(","));
            }
        }
        return aliasTask.build();
    }

    private static <T> T newInstance(String className, Class<T> tClass) throws Exception {
        Class<?> clazz = Class.forName(className);
        if (tClass.isAssignableFrom(clazz)) {
            //noinspection unchecked
            return (T) clazz.getDeclaredConstructor().newInstance();
        }
        throw new IllegalArgumentException("类型不匹配" + className + "没有实现" + tClass);
    }
}
