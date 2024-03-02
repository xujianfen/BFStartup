package blue.fen.scheduler_compiler.utils;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;

/**
 * Type definitions.
 */
public class TypeUtils {
    public static final String PACKAGE_NAME = "blue.fen.scheduler";

    public static final ClassName CLASS_NAME_I_SCHEDULER_TASK =
            ClassName.get(PACKAGE_NAME + ".scheduler", "ISchedulerTask");

    public static final ClassName CLASS_NAME_LIST =
            ClassName.get("java.util", "List");

    public static final ClassName CLASS_NAME_ARRAY_LIST =
            ClassName.get("java.util", "ArrayList");

    public static final TypeName TYPE_NAME_LIST_OF_SCHEDULER_TASK =
            ParameterizedTypeName.get(CLASS_NAME_LIST, CLASS_NAME_I_SCHEDULER_TASK);

    public final static String I_SCHEDULER_TASK_NAME = PACKAGE_NAME + ".scheduler.ISchedulerTask";
}
