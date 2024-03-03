package blue.fen.scheduler_compiler;

import static blue.fen.scheduler_compiler.utils.Consts.USE_REFLECTION;
import static blue.fen.scheduler_compiler.utils.TypeUtils.*;

import com.google.auto.service.AutoService;
import com.squareup.javapoet.AnnotationSpec;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.TypeSpec;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Filer;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.Processor;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedOptions;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;

import blue.fen.scheduler_annotation.AliasTasks;
import blue.fen.scheduler_annotation.SchedulerTask;
import blue.fen.scheduler_annotation.AliasTask;
import blue.fen.scheduler_compiler.utils.ILogger;
import blue.fen.scheduler_compiler.utils.MessagerLogger;

/**
 * <p>创建时间：2024/03/01 （星期五｝</p>
 * <p>作者： blue_fen</p>
 * <p>描述：</p>
 */
@SupportedOptions(USE_REFLECTION)
@AutoService(Processor.class)
public class SchedulerProcessor extends AbstractProcessor {
    private Filer filer;
    private Types types;

    private Elements elements;

    protected ILogger logger;

    private TypeMirror tmISchedulerTask;

    private final Map<String, Element> taskMap = new HashMap<>();
    private final Map<String, Set<String>> projectMap = new HashMap<>();
    private final Map<String, Map<String, List<AliasTask>>> aliasMap = new HashMap<>();

    boolean useReflection;

    @Override
    public synchronized void init(ProcessingEnvironment processingEnv) {
        super.init(processingEnv);
        filer = processingEnv.getFiler();
        types = processingEnv.getTypeUtils();
        elements = processingEnv.getElementUtils();
        logger = new MessagerLogger(processingEnv.getMessager());

        tmISchedulerTask = elements.getTypeElement(I_SCHEDULER_TASK_NAME).asType();

        Map<String, String> options = processingEnv.getOptions();
        if (!options.isEmpty()) {
            useReflection = Boolean.parseBoolean(options.get(USE_REFLECTION));
        }
        logger.info("任务查找器是否使用反射：" + useReflection);
    }

    @Override
    public Set<String> getSupportedAnnotationTypes() {
        Set<String> types = new HashSet<>();
        types.add(SchedulerTask.class.getCanonicalName());
        return types;
    }

    @Override
    public SourceVersion getSupportedSourceVersion() {
        return SourceVersion.latestSupported();
    }

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        taskMap.clear();

        try {
            processTaskAnnotation(roundEnv);
        } catch (Throwable e) {
            e.printStackTrace();
        }

        try {
            if (!taskMap.isEmpty()) {
                generateFinder().writeTo(filer);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return true;
    }

    private void processTaskAnnotation(RoundEnvironment roundEnv) {
        for (Element element : roundEnv.getElementsAnnotatedWith(SchedulerTask.class)) {
            TypeMirror tm = element.asType();
            if (!types.isSubtype(tm, tmISchedulerTask)) {
                logger.error(String.format("Illegal @SchedulerTask annotation for element %s", element));
                continue;
            }

            String task = element.toString();
            if (!taskMap.containsKey(task)) {
                String[] projects = element.getAnnotation(SchedulerTask.class).projects();
                if (projects.length == 0) {
                    logger.error(String.format("Illegal @SchedulerTask projects is Empty %s", element));
                } else {
                    taskMap.put(task, element);
                    processProjectAnnotation(projects, task);
                }
            }

            processAliasTaskAnnotation(element);
        }
    }

    private void processAliasTaskAnnotation(Element element) {
        AliasTasks aliasTasks = element.getAnnotation(AliasTasks.class);
        if (aliasTasks == null) {
            AliasTask aliasTask = element.getAnnotation(AliasTask.class);
            if (aliasTask != null) {
                processAliasTaskAnnotation(element, aliasTask);
            }
            return;
        }

        for (AliasTask aliasTask : aliasTasks.value()) {
            if (processAliasTaskAnnotation(element, aliasTask)) {
                break;
            }
        }
    }


    private boolean processAliasTaskAnnotation(Element element, AliasTask aliasTask) {
        String[] projects = aliasTask.projects();
        int count = 0;
        if (projects != null) {
            for (String project : projects) {
                if (!project.isEmpty()) {
                    processAliasTaskAnnotation(element, project, aliasTask);
                    count++;
                }
            }
        }
        if (count == 0) {
            processAliasTaskAnnotation(element, "*", aliasTask);
            return true;
        } else {
            return false;
        }
    }

    private void processAliasTaskAnnotation(Element element, String project, AliasTask aliasTask) {
        String task = element.toString();
        Map<String, List<AliasTask>> aliasTaskMap = aliasMap.computeIfAbsent(
                project,
                k -> new HashMap<>()
        );
        List<AliasTask> aliasTasks = aliasTaskMap.computeIfAbsent(
                task,
                k -> new ArrayList<>()
        );
        aliasTasks.add(aliasTask);
    }

    private void processProjectAnnotation(String[] projects, String task) {
        for (String project : projects) {
            if (project == null || project.isEmpty()) continue;
            projectMap.computeIfAbsent(project, k -> new HashSet<>()).add(task);
        }
    }

    private JavaFile generateFinder() {
        MethodSpec newInstance = null;
        String newInstanceName = "";
        if (useReflection) {
            newInstance = generateMethodToNewInstance();
            newInstanceName = newInstance.name;
        }

        MethodSpec.Builder findAllTaskNoEmpty = generateMethodToFindAllTaskNoEmpty();
        MethodSpec.Builder findAllTask = generateMethodToFindAllTask();

        TypeSpec.Builder findImplClass = TypeSpec
                .classBuilder("BFTaskFinder")
                .addModifiers(Modifier.PUBLIC)
                .addJavadoc(
                        CodeBlock.builder()
                                .addStatement("任务查找器")
                                .build()
                );

        if (!taskMap.isEmpty()) {
            int index = 0;
            for (Map.Entry<String, Set<String>> entry : projectMap.entrySet()) {
                String project = entry.getKey();
                MethodSpec.Builder sourceMethodBuilder = generateMethodToProjectSource(index++);

                for (String task : entry.getValue()) {
                    Element element = taskMap.get(task);

                    if (processAliasTask(
                            project,
                            element,
                            newInstanceName,
                            sourceMethodBuilder
                    )) continue;

                    if (useReflection) {
                        sourceMethodBuilder.addStatement(
                                "tasks.add($L($S))",
                                newInstanceName,
                                element.toString()
                        );
                    } else {
                        sourceMethodBuilder.addStatement(
                                "tasks.add(new $T())",
                                ClassName.get((TypeElement) element)
                        );
                    }
                }
                sourceMethodBuilder.addStatement("return tasks");

                MethodSpec sourceMethod = sourceMethodBuilder.build();
                findImplClass.addMethod(sourceMethod);
                findAllTaskNoEmpty
                        .beginControlFlow("case $S:", project)
                        .addStatement("return $L()", sourceMethod.name)
                        .endControlFlow();
            }
        }

        if (newInstance != null) {
            findImplClass
                    .addMethod(newInstance);
        }

        findImplClass
                .addMethod(findAllTask.build())
                .addMethod(
                        findAllTaskNoEmpty
                                .beginControlFlow("default:")
                                .addStatement(
                                        "throw new $T($S)",
                                        ClassName.get(IllegalArgumentException.class),
                                        "无效的项目"
                                )
                                .endControlFlow()
                                .endControlFlow()
                                .build()
                );

        return JavaFile.builder(PACKAGE_NAME, findImplClass.build()).build();
    }

    private MethodSpec generateMethodToNewInstance() {
        MethodSpec.Builder builder = MethodSpec
                .methodBuilder("newInstance")
                .addModifiers(Modifier.PRIVATE, Modifier.STATIC)
                .returns(CLASS_NAME_I_SCHEDULER_TASK)
                .addParameter(String.class, "task")
                .addStatement(
                        "return ($T) Class.forName(task).getConstructor().newInstance()",
                        ClassName.get(tmISchedulerTask)
                );
        addException(builder);
        return builder.build();
    }

    private MethodSpec.Builder generateMethodToFindAllTaskNoEmpty() {
        MethodSpec.Builder builder = MethodSpec
                .methodBuilder("findAllTaskNoEmpty")
                .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                .returns(TYPE_NAME_LIST_OF_SCHEDULER_TASK)
                .addParameter(String.class, "project")
                .beginControlFlow("switch (project)");
        addException(builder);
        return builder;
    }

    private MethodSpec.Builder generateMethodToFindAllTask() {
        return MethodSpec.methodBuilder("findAllTask")
                .addAnnotation(
                        AnnotationSpec.builder(SuppressWarnings.class)
                                .addMember("value", "$S", "unchecked")
                                .build()
                )
                .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                .returns(TYPE_NAME_LIST_OF_SCHEDULER_TASK)
                .addParameter(String.class, "project")
                .beginControlFlow("try")
                .addStatement("return findAllTaskNoEmpty(project)")
                .nextControlFlow("catch ($T e)", ClassName.get(Exception.class))
                .addStatement("return $T.EMPTY_LIST", Collections.class)
                .endControlFlow();
    }

    private List<AliasTask> getAliasTasks(String project, String task) {
        List<AliasTask> aliasTasks = null;
        Map<String, List<AliasTask>> aliasTaskMap = aliasMap.get("*");
        if (aliasTaskMap != null) {
            aliasTasks = aliasTaskMap.get(task);
            if (aliasTasks != null) {
                return aliasTasks;
            }
        }
        aliasTaskMap = aliasMap.get(project);
        if (aliasTaskMap != null) {
            aliasTasks = aliasTaskMap.get(task);
        }
        return aliasTasks;
    }

    private boolean processAliasTask(
            String project,
            Element taskElement,
            String newInstance,
            MethodSpec.Builder builder
    ) {
        String task = taskElement.toString();
        List<AliasTask> aliasTasks = getAliasTasks(project, task);
        if (aliasTasks == null) return false;
        for (AliasTask aliasTask : aliasTasks) {
            processAliasTask(taskElement, aliasTask, newInstance, builder);
        }
        return true;
    }

    private void processAliasTask(
            Element taskElement,
            AliasTask aliasTask,
            String newInstance,
            MethodSpec.Builder builder
    ) {
        String task = taskElement.toString();

        StringBuilder stringBuilder = new StringBuilder()
                .append("tasks.add(\n")
                .append("\t$T.builder()\n");

        Object[] args;
        if (useReflection) {
            stringBuilder.append("\t\t.task($L($S))\n");
            args = new Object[]{
                    CLASS_NAME_I_ALIAS_TASK,
                    newInstance,
                    task
            };
        } else {
            stringBuilder.append("\t\t.task(new $T())\n");
            args = new Object[]{
                    CLASS_NAME_I_ALIAS_TASK,
                    ClassName.get((TypeElement) taskElement)
            };
        }

        String alias = aliasTask.value();
        if (alias.length() > 0) {
            if ("$".equals(alias)) alias = "$$";
            stringBuilder
                    .append("\t\t.alias(\"")
                    .append(alias)
                    .append("\")\n");
        }

        String[] dependencies = aliasTask.dependencies();
        if (dependencies.length == 0) {
            if (aliasTask.noDependencies()) {
                stringBuilder.append("\t\t.noDependencies()\n");
            }
        } else {
            for (int i = 0; i < dependencies.length; i++) {
                if ("$".equals(dependencies[i])) {
                    dependencies[i] = "\"$$\"";
                } else {
                    dependencies[i] = "\"" + dependencies[i] + "\"";
                }
            }
            stringBuilder
                    .append("\t\t.dependencies(")
                    .append(String.join(",", dependencies))
                    .append(")\n");
        }

        stringBuilder
                .append("\t\t.build()\n")
                .append(");\n");

        builder.addCode(stringBuilder.toString(), args);
    }

    private MethodSpec.Builder generateMethodToProjectSource(int index) {
        MethodSpec.Builder builder = MethodSpec
                .methodBuilder("projectSource" + index)
                .addModifiers(Modifier.PRIVATE, Modifier.STATIC)
                .returns(TYPE_NAME_LIST_OF_SCHEDULER_TASK)
                .addStatement("$T<$T> tasks = new $T<>()",
                        CLASS_NAME_LIST,
                        CLASS_NAME_I_SCHEDULER_TASK,
                        CLASS_NAME_ARRAY_LIST
                );
        addException(builder);
        return builder;
    }

    private void addException(MethodSpec.Builder methodBuilder) {
        methodBuilder.addException(NoSuchMethodException.class)
                .addException(IllegalAccessException.class)
                .addException(InvocationTargetException.class)
                .addException(InstantiationException.class)
                .addException(ClassNotFoundException.class);
    }
}
