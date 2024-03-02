package blue.fen.scheduler_compiler;

import static blue.fen.scheduler_compiler.utils.TypeUtils.*;

import com.google.auto.service.AutoService;
import com.squareup.javapoet.AnnotationSpec;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.TypeSpec;

import java.lang.reflect.InvocationTargetException;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Filer;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.Processor;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;

import blue.fen.scheduler_annotation.SchedulerTask;
import blue.fen.scheduler_compiler.utils.ILogger;
import blue.fen.scheduler_compiler.utils.MessagerLogger;

/**
 * <p>创建时间：2024/03/01 （星期五｝</p>
 * <p>作者： blue_fen</p>
 * <p>描述：</p>
 */
@AutoService(Processor.class)
public class SchedulerProcessor extends AbstractProcessor {
    private Filer filer;
    private Types types;

    private Elements elements;

    protected ILogger logger;

    private TypeMirror tmISchedulerTask;

    private final Map<String, Element> taskMap = new HashMap<>();
    private final Map<String, Set<String>> projectMap = new HashMap<>();

    @Override
    public synchronized void init(ProcessingEnvironment processingEnv) {
        super.init(processingEnv);
        filer = processingEnv.getFiler();
        types = processingEnv.getTypeUtils();
        elements = processingEnv.getElementUtils();
        logger = new MessagerLogger(processingEnv.getMessager());

        tmISchedulerTask = elements.getTypeElement(I_SCHEDULER_TASK_NAME).asType();
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
        }
    }

    private void processProjectAnnotation(String[] projects, String task) {
        for (String project : projects) {
            if (project == null || project.isEmpty()) continue;
            projectMap.computeIfAbsent(project, k -> new HashSet<>()).add(task);
        }
    }

    private JavaFile generateFinder() {
        MethodSpec newInstance = generateMethodToNewInstance();

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
            for (Map.Entry<String, Set<String>> entry : projectMap.entrySet()) {
                String project = entry.getKey();
                MethodSpec sourceMethod = generateMethodToProjectSource(
                        project,
                        newInstance.name,
                        entry.getValue()
                ).build();
                findImplClass.addMethod(sourceMethod);
                findAllTaskNoEmpty
                        .beginControlFlow("case $S:", project)
                        .addStatement("return $L()", sourceMethod.name)
                        .endControlFlow();
            }
        }
        findImplClass
                .addMethod(newInstance)
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

    private MethodSpec.Builder generateMethodToProjectSource(
            String project,
            String newInstance,
            Set<String> taskSet
    ) {
        MethodSpec.Builder builder = MethodSpec
                .methodBuilder(project + "ProjectSource")
                .addModifiers(Modifier.PRIVATE, Modifier.STATIC)
                .returns(TYPE_NAME_LIST_OF_SCHEDULER_TASK)
                .addStatement("$T<$T> tasks = new $T<>()",
                        CLASS_NAME_LIST,
                        CLASS_NAME_I_SCHEDULER_TASK,
                        CLASS_NAME_ARRAY_LIST
                );
        for (String task : taskSet) {
            Element element = taskMap.get(task);
            builder.addStatement(
                    "tasks.add($L($S))",
                    newInstance,
                    element.toString()
            );
        }
        builder.addStatement("return tasks");
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
