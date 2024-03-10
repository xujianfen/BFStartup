# BFStartup
[![Author](https://img.shields.io/badge/Author-xujianfen-blue.svg)](https://github.com/xujianfen)
[![API](https://img.shields.io/badge/API-16%2B-green.svg)](https://android-arsenal.com/api?level=16)
[![Language](https://img.shields.io/badge/language-java-orange.svg)](https://www.oracle.com/java)
[![Platform](https://img.shields.io/badge/platform-android-ff69b4.svg)](https://www.android.com/)
[![License](https://img.shields.io/badge/license-Apache%202-green.svg)](https://www.apache.org/licenses/LICENSE-2.0)
[![Release](https://img.shields.io/github/v/release/xujianfen/BFStartup)](https://github.com/xujianfen/BFStartup/releases)



## 简介

  ```BFStartup```是可灵活调度任务的安卓启动框架。它设计的初衷是用来解决启动过程中多线程加载任务依赖的问题，当然目前来说，它已经不局限于启动过程，适用于更加普遍的任务调度场景。

项目创建之初，参考了很多启动框架并出于学习的目的进行了大胆创新。在任务启动过程中，摈弃了拓扑排序后将任务直接放入线程池的方式，将其划分为任务循环依赖检查、任务流水线调度、同异步任务分发三个过程，引入任务依赖关系检测器，就绪任务优先级队列、任务分发器和调度器，使任务的不同阶段充分解耦。

此外，项目还添加了任务监听器和任务阻塞器。以任务监听器为基础，在核心流程之外，提供了日志打印器和数据记录器。日志打印器使任务处理过程更为可观，数据记录器则方便任务间的数据传递，以支持更为复杂的场景；任务阻塞器，可以在待等待任务执行完成之前，对调用方进行阻塞。由于JAVA对携程的支持较差，为了防止在主线程调用阻塞器时触发ANR问题，项目基于栈的执行方式对主线程阻塞场景做了特殊处理。当然，这存在一定的局限性。

为了保持启动框架的初衷，项目为此添加了```startup```模块，方便启动流程中对依赖任务的处理。同时添加了```scheduler-annotation```和```scheduler-compiler```模块，给开发者提供更多收集任务的选择。



## 参考

以下开源仓库对```BFStartup```有很大的启发，感谢项目的创建者和维护者们。

比起这些已经获得广泛认可的开源项目，```BFStartup```还有很多不成熟的地方。有机会的话，```BFStartup```会继续迭代和优化，为使用者带来更好的体验。

> https://github.com/DSAppTeam/Anchors
>
> https://github.com/Shouheng88/AndroidStartup/
>
> https://github.com/idisfkj/android-startup
>
> https://github.com/alibaba/alpha
>
> https://github.com/mission-peace/interview/



## 添加依赖

添加MavenCenter

```groovy
repositories { mavenCentral() }
```

添加任务调度器

```groovy
implementation 'io.github.xujianfen.pram:scheduler:$latest-version'
```

添加启动依赖

```groovy
implementation 'io.github.xujianfen.pram:startup:$latest-version'
```

若需要使用注解处理器收集任务，可以添加如下依赖

```groovy
implementation 'io.github.xujianfen.pram:scheduler-annotation:$latest-version'
implementation 'io.github.xujianfen.pram:scheduler-compiler:$latest-version'
```



## 使用方法
### 目录

​	[1. 任务调度器](#1)

​		[(1) 快速使用](#1_1)

​		[(2) 任务全局配置](#1_2)

​		[(3) 任务计划配置](#1_3)

​		[(4) 任务监听器](#1_4)

​				[[1] 基本使用](#1_4_1)

​				[[2] 日志打印器](#1_4_2)

​				[[3] 数据记录器](#1_4_3)

​		[(5) 任务扩展](#1_5)

​				[[1] 任务别名](#1_5_1)

​				[[2] 任务包装器](#1_5_2)


​	[2. 任务启动器](#2)

​	[3. 任务注解处理器](#3)



### 1. 任务调度器<a id="1"></a>

#### (1) 快速使用<a id="1_1"></a>

添加依赖

```groovy
implementation 'io.github.xujianfen.pram:scheduler:$latest-version'
```

使用范例

```java
//定义任务
public class A implements ISchedulerTask {
    @Override
    public void perform(
            String projectName,
            @NonNull ParamProvider param,
            @Nullable TaskNotifier notifier
    ) {
        BFLog.d("执行了B");
    }
}

public class B implements ISchedulerTask {
    @Override
    public void perform(
            String projectName,
            @NonNull ParamProvider param,
            @Nullable TaskNotifier notifier
    ) {
        BFLog.d("执行了B");
    }

    @Override
    public List<String> dependencies() {
        return Collections.singletonList("A");
    }
}

//配置开发模式
if (BuildConfig.DEBUG) {
    BFConfig.setFlag(DEBUG_MASK, FLAG_DEBUG);
} else {
    BFConfig.clearFlag(DEBUG_MASK);
}

//执行任务
BFScheduler.build()
    .newProject()
    .register(new A(), new B())
    .commit()
    .submit()
    .execute();
```



#### (2) 任务全局配置<a id="1_2"></a>

```BFConfig```用于全局配置，配置详情见[```BFConfig```](https://github.com/xujianfen/BFStartup/blob/master/scheduler/src/main/java/blue/fen/scheduler/BFConfig.java)。

```java
import static blue.fen.scheduler.BFConfig.*;
import blue.fen.scheduler.BFConfig;

//替换默认配置
BFConfig.setFlags(FLAG_DEBUG | FLAG_DETAIL_LOG_MODE | SIMPLE_CHECK); 

//设置开发模式 (掩码为DEBUG_MASK)
BFConfig.setFlag(DEBUG_MASK, FLAG_DEBUG);//Debug模式
BFConfig.clearFlag(DEBUG_MASK); //Release模式

//日志设置 (掩码为LOG_MODE_MASK)
BFConfig.setFlag(LOG_MODE_MASK, FLAG_SIMPLE_LOG_MODE);//设置简单日志
BFConfig.setFlag(LOG_MODE_MASK, FLAG_DETAIL_LOG_MODE);//设置详细日志

//设置项目额外数据，可用于任务间传递数据 (掩码为EXTRA_DATA_MASK)
BFConfig.setFlag(FLAG_EXTRA_DATA_ENABLE, FLAG_EXTRA_DATA_ENABLE);//开启项目额外数据，可用于任务间的数据传递
BFConfig.setFlag(FLAG_EXTRA_DATA_AUTO_CLEAR, FLAG_EXTRA_DATA_AUTO_CLEAR);//开启自动清理额外数据，会在项目执行完成时自动清理数据

//设置环检测模式
BFConfig.setFlag(CHECK_CYCLE_MODE_MASK, CYCLE_CHECK); //检查所有简单环（DEBUG模式推荐开启）
BFConfig.setFlag(CHECK_CYCLE_MODE_MASK, SCC_CHECK); //检查所有强连通分量
BFConfig.setFlag(CHECK_CYCLE_MODE_MASK, SIMPLE_CHECK); //检查是否存在环（RELEASE模式需要开启检查时， 推荐开启）
BFConfig.setFlag(CHECK_CYCLE_MODE_MASK, NO_CHECK); //不检查循环依赖（RELEASE模式推荐开启）
```

需要注意的是，同样是全局配置，```LOG_MODE_MASK```和```EXTRA_DATA_MASK```必须在任意```Scheduler```创建配置，否则会失效。而其他配置在任意时期都有效，但还是建议都在初始化时配置。

以下是```BFConfig```的默认配置

| 掩码类别              | 控制掩码                   | 默认值                     | 描述                                                         |
| --------------------- | -------------------------- | -------------------------- | ------------------------------------------------------------ |
| DEBUG_MASK            | DEBUG_MASK                 | FLAG_DEBUG                 | 默认为Debug模式                                              |
| LOG_MODE_MASK         | LOG_MODE_MASK              | FLAG_DETAIL_LOG_MODE       | 默认开启详细日志                                             |
| CHECK_CYCLE_MODE_MASK | CHECK_CYCLE_MODE_MASK      | DEFAULT_CHECK              | 默认情况下<br/>Debug模式使用CYCLE_CHECK<br/>Release模式使用NO_CHECK<br/>**注意：CHECK_CYCLE_MODE_MASK<br/>属于低优先级配置，会被Scheduler<br/>的配置覆盖** |
| EXTRA_DATA_MASK       | FLAG_EXTRA_DATA_ENABLE     | FLAG_EXTRA_DATA_ENABLE     | 默认开启项目额外数据                                         |
| EXTRA_DATA_MASK       | FLAG_EXTRA_DATA_AUTO_CLEAR | FLAG_EXTRA_DATA_AUTO_CLEAR | 默认自动清理额外数据                                         |

以下是```BFConfig```中和配置flag相关的基础方法

| 方法        | 描述                                       |
| ----------- | ------------------------------------------ |
| setFlags    | 替换所有标识                               |
| getFlags    | 获取所有标识                               |
| setFlag     | 设置目标mask或者flag的值为目标flag         |
| clearFlag   | 清空目标mask或者flag的所有内容             |
| getFlag     | 获取目标mask或者flag的值                   |
| isMatch     | 判断目标mask是否与目标flag匹配             |
| isMatchFlag | 判断目标flag是否开启                       |
| isMulti     | 判断传入的mask是否为控制多个flag的集合掩码 |

为了方便使用，```BFConfig```提供了一些可以直接调用的方法

| 方法           | 描述             |
| -------------- | ---------------- |
| isDebug        | 是否为debug模式  |
| logMode        | 获取日志打印模式 |
| checkCycleMode | 获取环检测模式   |



#### (3) 任务计划配置<a id="1_3"></a>


```java
       ISchedulerTask task1 = SchedulerTask.newBuilder()
                .relativePriority(true) //使用相对优先级
                .priority(1) //任务优先级（若开启相对优先级，该值为默认优先级+设置优先级）
                .enableThreadPriority(true)//开启线程优先级设置，默认不开启
                .threadPriority(3)//任务执行线程的优先级，主线程任务(前台任务)或者enableThreadPriority为false时为失效
                .name("t1") //任务名，默认使用类名
                .task((projectName, taskName, param, notifier) -> {
                    BFLog.d("项目名：" + projectName
                            + "任务名：" + taskName
                            + "任务参数提供者：" + param  //需要开启FLAG_EXTRA_DATA_ENABLE
                            + "任务通知者：" + notifier); //用于通知任务完成事件，manualDispatch为false时为null
                })
                .isBackground(true) //true表示后台任务，false表示前台任务，默认为true
                .isAsync(true) //是否异步执行，默认值等于isBackground
                .needBlock(true) //是否需要阻塞，默认为false
                .dependencies("2") //依赖任务名
                .manualDispatch(true) //是否启动手动调度，默认为false
                .ignoreThrow(true) //忽略依赖任务的异常，默认为false
                .build();
        ISchedulerTask task2 = SchedulerTask.newBuilder().name("2").build();

        BFScheduler scheduler = BFScheduler.build()
                .newConfig()//创建项目配置
                .copy(null) //用于复制其他项目的配置
                .awaitOutTime(BFConfig.INFINITE_AWAIT) //任务等待时间，调用BFScheduler.await()时生效，默认值为DEFAULT_AWAIT_OUT_TIME
                .checkCycleMode(CYCLE_CHECK) //设置环检测模式，默认使用BFConfig的CHECK_CYCLE_MODE_MASK配置
                .excludeSelfCircular(true) //是否排除自循环，默认为true
                .autoCheckDependencies(true) //是否自动检测依赖，若为false需要手动检测，默认为true
                .executor(new MyExecutoasr()) //自定义线程池，可根据项目需要设置
                .priorityProvider(null) //自定义优先级队列，作为项目扩展用，不建议使用
                .distributor(null) //自定义任务分发器，作为项目扩展用，不建议使用
                .block(null) //自定义任务阻塞器，作为项目扩展用，不建议使用
                .flowProvider(null) //自定义任务流水线，作为项目扩展用，不建议使用
                .commit() //提交项目配置
                .newProject() //创建项目
                .name("testProject") //项目名，默认为Scheduler对象的hashCode值
                .merge(null) //用于合并其他项目的任务
                .register(task1, task2) //注册任务
                .commit() //提交项目
                .factory(null) //自定义Scheduler，作为项目扩展用，不建议使用
                .submit() ;//提交计划

        //执行任务有四种方式执行，注意它只能执行一次
        //execute和enqueue方法在任务状态异常或者重复执行任务时会抛出异常
        scheduler.execute(); //① 执行任务，在遇到异步任务前使用当前线程执行，遇到后使用任务分发器执行
        scheduler.enqueue(); //② 执行任务，直接使用任务分发器执行任务
		boolean success;
        success = scheduler.tryExecute(); //③ 尝试执行execute()，不会抛出异常，但会返回是否成功
        success = scheduler.tryEnqueue(); //④ 尝试执行enqueue()，不会抛出异常，但会返回是否成功

        MyTaskLifecycleObserver observer = new MyTaskLifecycleObserver();
        scheduler.getLifecycle().registerObserver(observer); //注册任务监听
        scheduler.getLifecycle().unregisterObserver(observer); //注销任务监听
        scheduler.getLifecycle().unregisterAll(); //注销所有任务监听

		scheduler.name(); //获取项目名称
        scheduler.await(); //等待needBlock==true的任务全部执行完成
        scheduler.destroy();//销毁所有任务，并唤醒所有等待
```



#### (4) 任务监听器<a id="1_4"></a>

##### [1] 基本使用<a id="1_4_1"></a>

使用项目监听器```ProjectLifecycleObservable```，可以监听所有项目的任务。

```java
    ProjectLifecycleObservable.getInstance().registerObserver(new AProjectLifecycleObserver() {
        @Override
        public void finish(String project) {
            //项目完成时做的处理
        }

        @Override
        public void before(String project, ISchedulerTask task) {
            //任务执行前的处理
        }

        @Override
        public void error(String project, ISchedulerTask task, Throwable throwable) {
            //处理任务出现的问题
        }

        @Override
        public void handleError(String project, ISchedulerTask task, Exception e) {
            //处理监听过程中出现的问题
        }
    });
```

使用任务计划监听器，可以监听单个项目的任务

```java
 	scheduler.getLifecycle().registerObserver(new ATaskLifecycleObserver() {
        @Override
        public void finish() {
            //项目完成时做的处理
        }

        @Override
        public void before(ISchedulerTask task) {
            //任务执行前的处理
        }

        @Override
        public void error(ISchedulerTask task, Throwable throwable) {
            //处理任务出现的问题
        }

        @Override
        public void handleError(ISchedulerTask task, Exception e) {
            //处理监听过程中出现的问题
        }
    });
```



##### [2] 日志打印器<a id="1_4_2"></a>

以[Demo](https://github.com/xujianfen/BFStartup/blob/master/demo/src/main/java/blue/fen/demo/MainActivity.java)中点击事件执行的任务为例

```java
BFScheduler scheduler = BFScheduler.build()
                .newConfig()
                .awaitOutTime(BFConfig.INFINITE_AWAIT)
                .commit()
                .newProject()
                .register(
                        builder.name("1").isAsync(true).isBackground(true).needBlock(true).dependencies("2").build(),
                        builder.name("2").dependencies("3").build(),
                        builder.name("3").build(),
                        builder.name("4").dependencies("3").ignoreThrow(true).build(),
                        builder.name("5").dependencies("2", "4").ignoreThrow(false).build(),
                        builder.name("6").needBlock(false).dependencies("2").ignoreThrow(true).build(),
                        builder.name("7").dependencies("5").build()
                )
                .commit()
                .submit();
```

简单日志打印信息

```shell
# 项目准备阶段打印的日志
SimpleLog[59727673] PREPARE
...
# 任务正常执行的日志
SimpleLog[59727673] BEFORE(3)
SimpleLog[59727673] AFTER(3)
...
# 异常任务日志
SimpleLog[59727673] ERROR(2) throw java.lang.Exception: 2 Error!!!
...
# 上游任务（依赖任务）异常，导致的异常日志
SimpleLog[59727673] ERROR(5) throw blue.fen.scheduler.exception.TransferTaskException:
|	ThrowableSize：2
|	(1) Task[2] Error: java.lang.Exception: 2 Error!!!
|	(2) Task[4] Error: java.lang.Exception: 4 Error!!!
...
#项目完成日志
SimpleLog[59727673] FINISH
```

详细日志打印信息

```shell
# 项目准备阶段打印的日志
|================================ ProjectRecord::PREPARE ================================
|	name: [195577314]
|	startTime: [2034936860686819]
|	size: [0]
|============================ end ================================
...
# 任务正常执行的日志
|================================ TaskRecord::BEFORE ================================
|	name: [3]
|	startTime: [2034936876793742]
|------- project info -------
|	name: [195577314]
|	startTime: [2034936860686819]
|	count: [1]
|------- task info -----
|	name: [3]
|	priority: [0]
|	dependencies: [[3]]
|	dependenciesSize: [1]
|	isAsync: [true]
|	isBackground: [true]
|	manualDispatch: [false]
|	needBlock: [true]
|	ignoreThrow: [false]
|============================ end ================================
|================================ TaskRecord::AFTER ================================
|	name: [3]
|	startTime: [2034936876793742]
|	endTime: [2034938882117665]
|	cosTime: [2.005s]
|------- project info -------
|	name: [195577314]
|	startTime: [2034936860686819]
|	cosTime: [2.021s]
|	count: [1]
|------- task info -----
|	name: [3]
|	priority: [0]
|	dependencies: [[3]]
|	dependenciesSize: [1]
|	isAsync: [true]
|	isBackground: [true]
|	manualDispatch: [false]
|	needBlock: [true]
|	ignoreThrow: [false]
|============================ end ================================
...
# 异常任务日志
|================================ TaskRecord::ERROR ================================
|	name: [2]
|	startTime: [2034938896764742]
|	endTime: [2034940900364127]
|	cosTime: [2.004s]
|	throwable: [java.lang.Exception: 2 Error!!!]
|------- project info -------
|	name: [195577314]
|	startTime: [2034936860686819]
|	cosTime: [4.040s]
|	count: [3]
|------- task info -----
|	name: [2]
|	priority: [0]
|	dependencies: [[3]]
|	dependenciesSize: [1]
|	isAsync: [true]
|	isBackground: [true]
|	manualDispatch: [false]
|	needBlock: [true]
|	ignoreThrow: [false]
|============================ end ================================\
...
# 上游任务（依赖任务）异常，导致的异常日志
|================================ TaskRecord::ERROR ================================
|	name: [1]
|	throwable: [blue.fen.scheduler.exception.TransferTaskException:
|	ThrowableSize：1
|	(1) Task[2] Error: java.lang.Exception: 2 Error!!!]
|------- project info -------
|	name: [195577314]
|	startTime: [2034936860686819]
|	cosTime: [4.069s]
|	count: [6]
|------- task info -----
|	name: [1]
|	priority: [0]
|	dependencies: [[2]]
|	dependenciesSize: [1]
|	isAsync: [true]
|	isBackground: [true]
|	manualDispatch: [false]
|	needBlock: [true]
|	ignoreThrow: [false]
|============================ end ================================
...
#项目完成日志
|================================ ProjectRecord::FINISH ================================
|	name: [195577314]
|	startTime: [2034936860686819]
|	endTime: [2034942984483511]
|	cosTime: [6.124s]
|	size: [7]
|	totalTask: [[1, 2, 3, 4, 5, 6, 7]]
|	successSize: [3]
|	successTask: [[3, 6, 7]]
|	throwSize: [4]
|	throwTask: [[1, 2, 4, 5]]
|============================ end ================================
```



##### [3] 数据记录器<a id="1_4_3"></a>

数据记录器用于任务间的数据传递

```java
ExtraDataRecords extraDataRecords = ExtraDataRecords.getInstance();

//获取任意记录，若项目存在且任务存在，则返回任务记录，若项目存在且任务不存在则返回项目记录，若项目不存在，则返回null
ExtraDataRecords.ResidualRecord record = extraDataRecords.getRecord(projectName, taskName);
//获取项目记录，若项目存在，则返回项目记录，否则返回null
ExtraDataRecords.ResidualRecord record = extraDataRecords.getProject(projectName);
//获取任务记录，若项目存在且任务存在，则返回任务记录，否则返回null
ExtraDataRecords.ResidualRecord record = extraDataRecords.getTask(projectName, taskName);
                                                                  
if (record != null) {
	record.startTime(); //启动时间
	record.endTime(); //结束时间
	record.throwable(); //异常记录
}

ResidualRecord record = extraDataRecords.removeProject(projectName); //删除项目记录，成功则返回被删除的记录
ResidualRecord record = extraDataRecords.removeTask(projectName, taskName); //删除任务记录，成功则返回被删除的记录
ResidualRecord record = extraDataRecords.clear(); //清空全局的任务记录

extraDataRecords.canParam(record); //判断record是否可以设置参数，任务记录可以设置参数，项目记录不可以设置参数

boolean success = extraDataRecords.setParam(name(), task().name(), "args"); //设置任务参数，返回是否设置成功,设置失败表示找不到任务。
Object param = extraDataRecords.getParam(projectName, taskName); //获取任务参数，找不到任务会返回null
Object param = extraDataRecords.getParam(projectName, taskName); //获取任务参数，找不到任务会返回null
```

```ParamProvider```基于数据记录器完成，删除任务记录可能使```ParamProvider```失效。以下是对```ParamProvider```的使用

```java
public class A implements ISchedulerTask {
    @Override
    public void perform(String projectName,
                        @NonNull ParamProvider param,
                        @Nullable TaskNotifier notifier
   ) throws InterruptedException {
        param.setParam("args"); //设置当前的任务参数
        Object args = param.getParam(taskName); //获取指定任务的参数
        param.clearParam(taskName); //清空指定任务的参数
    }
}
```



#### (5) 任务扩展<a id="1_5"></a>

在复用有少许区别的任务或者同一个项目中使用同一个任务时，虽然有很多种方式可以实现，但相对麻烦。所以为任务支持了相关的扩展。

##### [1] 任务别名<a id="1_5_1"></a>

任务别名主要是为了支持同一个项目中复用同一个任务的场景，考虑该场景可能出现频率相对较多，所以在[任务启动器](#2)和[任务注解处理器](#3_alias)中对别名任务做了支持。

由于任务之间以任务名来区分，所以使用任务别名，就可以区分为不同任务。考虑到复用任务后可能导致依赖关系发生变化，所以也支持了任务依赖的修改。

**注意："$"表示任务原名，无论在```alias```中，还是在```dependencies```中，出现的"$"都以任务原名来处理。** 

以下是对别名任务的使用

```java
ISchedulerTask a = new A();
ISchedulerTask source = AliasTask.builder()
    .task(a)
    .alias("$") //保持任务原名
    //若不使用dependencies方法，则默认保持原有依赖
    .build(); //这里得到的source和a任务对项目来说是相同的


ISchedulerTask dependencySource = AliasTask.builder()
    .task(a)
    .alias("B") //任务名改为B
    .dependencies("$", "C") //这里依赖了原任务也就是A任务 
    .build();

ISchedulerTask dependencySource = AliasTask.builder()
    .task(a)
    .alias("C") //任务名改为C
    .noDependencies() // 这里表示任务没有依赖，即根任务
    .build();
```



##### [2] 任务包装器<a id="1_5_2"></a>

任务包装器是用于复用有较少区别的任务的场景，在使用任务包装器之前，请先考虑是否需要定义一个新的任务或者重新设计代码。

由于应尽量避免使用任务包装器，所以在[任务启动器](#2)和[任务注解处理器](#3)中并没有添加相应的支持。

使用方式和任务别名类似，以下是使用范例

```java
ISchedulerTask newTask = TaskWrapper.newBuilder(task)
    .name("newName") //注意这里不会特殊处理"$"
    .priority(newPriority)
    .isAsync(newIsAsync)
    .build();
```




### 2. 任务启动器<a id="2"></a>

添加依赖

```groovy
implementation 'io.github.xujianfen.pram:scheduler:$latest-version'
implementation 'io.github.xujianfen.pram:startup:$latest-version'
```

通过 ```ContentProvider ```启动任务

```xml
<?xml version="1.0" encoding="utf-8"?>
<manifest>
    <application>
        <provider
            android:name="blue.fen.startup.BFStartupProvider"
            android:authorities="${applicationId}.androidx-startup"
            android:exported="false"
            tools:node="merge">
            <meta-data
                android:name="bf.startup"
                android:resource="@xml/startup" />
        </provider>
    </application>
</manifest>
 
```

使用XML配置文件构建任务，配置规则见 [XML属性定义](https://github.com/xujianfen/BFStartup/blob/master/startup/src/main/res/values/attrs.xml)

```xml
<?xml version="1.0" encoding="utf-8"?>
<resources xmlns:app="http://schemas.android.com/apk/res-auto">
    <config
        app:bfc_checkCycleMode="scc_check"
        app:bfc_debug="true"
        app:bfc_extraDataAutoClear="false"
        app:bfc_extraDataEnable="true"
        app:bfc_logMode="simple"
        app:bfp_checkCycleMode="scc_check" />  
    <project
        app:bfp_awaitOutTime="@integer/infinite_await"
        app:bfp_checkCycleMode="no_check"
        app:bfp_executor="blue.fen.demo.executoasr.MyExecutoasr"
        app:bfp_name="startup">
        <task
            app:bft_alias="@string/bf_auto_alias"
            app:bft_dependencies="$,blue.fen.demo.task.B">
            blue.fen.demo.task.A
        </task>
        <task app:bft_alias="$">blue.fen.demo.task.A</task>
        <task>blue.fen.demo.task.B</task>
        <task
            app:bft_alias="B2"
            app:bft_dependencies="@string/bf_no_dependencies">
            blue.fen.demo.task.B
        </task>
    </project>
</resources>
```

若需要自定义启动任务发生的时机，可以去掉在```AndroidManifest```文件配置的 ```ContentProvider ```。然后使用以下方式执行XML配置的项目。

```java
BFProjectParser.parserXmlForScheduler(this, R.xml.startup).execute();
```

这也许不太保险，因为当找不到XML文件或者解析失败时，```parserXmlForScheduler```会返回null，可以自行添加判空逻辑。

当然，```parserXmlForScheduler```其实不仅仅可以作为启动任务来调用，它只是通过XML来创建```BFScheduler.Build```，所以它基本适用与所有```Scheduler```可以用到的时机。可以定义多个类似```@xml/startup```的任务来表示不同的项目。需要注意的是，不是作为启动配置的XML文件，不推荐使用```config```标签。

重新定义XML文件有时候会过于麻烦，所以可以通过以下方式，对XML解析出来的项目配置进行更改

```java
//不使用清单文件注册，可以用这种方式执行启动项目的任务，当然也可以是其他场景的项目
BFScheduler.Build scheduler = BFProjectParser.parserXmlForScheduler(this, R.xml.startup);
if (scheduler != null) {
    scheduler.copyWithProject()
        .name("customStartup")
        .commit()
        .submit()
        .execute();
}
```



### 3. 任务注解处理器<a id="3"></a>

添加依赖

```groovy
implementation 'io.github.xujianfen.pram:scheduler:$latest-version'
implementation 'io.github.xujianfen.pram:scheduler-annotation:$latest-version'
implementation 'io.github.xujianfen.pram:scheduler-compiler:$latest-version'
```

使用范例

```java
//标注SchedulerTask，在projects参数添加任务所属的项目集合，注解处理器会将任务添加到对应的项目里面
@SchedulerTask(projects = "test")
public class A implements ISchedulerTask {
    @Override
    public void perform(String projectName,
                        @NonNull ParamProvider param,
                        @Nullable TaskNotifier notifier
    ) throws InterruptedException {
        BFLog.d("执行了A");
    }
}

//任务查找器，由理器生成
import blue.fen.scheduler.BFTaskFinder;

String projectName = "test";
List<ISchedulerTask> tasks = r.findAllTask(projectName) //查找目标项目中所有收集到的任务

//注册查找到的项目，并执行
BFScheduler.build()
    .newProject()
    .name(projectName)
    .register(tasks)
    .commit()
    .submit()
    .execute();
```

注解处理器可以配置是否使用反射，如果使用，则生成的```BFTaskFinder```将通过反射的方式创建收集的任务，如果不使用则直接使用```new ```来创建任务。

```groovy
android {
    defaultConfig {
        javaCompileOptions {
            annotationProcessorOptions {
                //默认不使用反射
                arguments = [BF_USE_REFLECTION: "true"]
            }
        }
    }
}
```

为了支持[任务别名](#1_5_1)，特别添加了```@AliasTask```注解，用于收集别名任务。<a id="3_alias"></a>

使用范例

```java
@SchedulerTask(projects = {"test", "test2"})
@AliasTask(value = "$", projects = "test2", dependencies = {"A2"})
@AliasTask(value = "A1", projects = "test2", dependencies = {"A2", "$"})
@AliasTask(value = "A2", projects = {"test2", "test"}, noDependencies = true)
public class A implements ISchedulerTask {
}
```

以下是```@AliasTask```注解的参数介绍

| 参数           | 默认值 | 描述                                                         |
| -------------- | ------ | ------------------------------------------------------------ |
| value          | 空字符 | 当该值为空时会自动生成别名，当该值为$时保持任务名            |
| projects       | 空集合 | 默认为空，当集合为空时，说明后续所有项目都需要使用该别名，这时其他的TaskAlias都会失效 |
| dependencies   | 空集合 | 设置别名任务的依赖<br/>为空时，默认保持原有依赖，若设置```noDependencies```为true，则表示没有依赖<br/>不为空时，设置的别名依赖将覆盖任务依赖，其中"$"可以表示依赖项目中保留原有名称的当前类型任务 |
| noDependencies | false  | 当依赖项为空时，设置任务是无依赖还是保持原有依赖<br/>true 表示无依赖项<br/>false 表示保持原有赖项 |



