package local.android.zh.agent;

import java.lang.classfile.*;
import java.lang.classfile.constantpool.StringEntry;
import java.lang.classfile.instruction.ConstantInstruction.LoadConstantInstruction;
import java.lang.classfile.instruction.InvokeInstruction;
import java.lang.classfile.instruction.InvokeDynamicInstruction;
import java.lang.constant.ClassDesc;
import java.lang.constant.MethodTypeDesc;
import java.lang.instrument.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.security.*;
import java.util.*;

/** 只在已审核且哈希完全相同的类中替换显示字符串，不修改磁盘上的应用。 */
public final class ComposeTranslationAgent implements ClassFileTransformer {
    record Entry(String hash, Map<String, String> text) {}
    private final Map<String, Entry> entries = new HashMap<>();
    private final Map<String, String> templateText = new LinkedHashMap<>();

    private final String whatsNewZip;

    public ComposeTranslationAgent() throws Exception {
        try (var stream = getClass().getResourceAsStream("/whats-new-zh.zip")) {
            if (stream == null) throw new IllegalStateException("缺少新增功能中文文档");
            whatsNewZip = Base64.getEncoder().encodeToString(stream.readAllBytes());
            // 常量池 UTF-8 长度有上限，扩大文档包时应改用分块资源读取。
            if (whatsNewZip.length() >= 60000) throw new IllegalStateException("中文文档超出常量长度限制");
        }
        try (var stream = getClass().getResourceAsStream("/translations.tsv")) {
            if (stream == null) throw new IllegalStateException("缺少补译字典");
            for (String line : new String(stream.readAllBytes(), StandardCharsets.UTF_8).split("\n")) {
                String[] fields = line.split("\t", -1);
                if (fields.length != 4) throw new IllegalArgumentException("补译字典格式错误");
                Entry entry = entries.computeIfAbsent(fields[0], k -> new Entry(fields[1], new HashMap<>()));
                if (!entry.hash.equals(fields[1])) throw new IllegalArgumentException("同类哈希冲突");
                String source = decode(fields[2]), target = decode(fields[3]);
                // 空文本行只注册类哈希，供显示调用转换使用，不匹配任何文字。
                if (source.isEmpty() && target.isEmpty()) continue;
                String old = entry.text.putIfAbsent(source, target);
                if (old != null && !old.equals(target)) throw new IllegalArgumentException("同文案翻译冲突");
            }
        }
        try (var stream = getClass().getResourceAsStream("/template-display.tsv")) {
            if (stream == null) throw new IllegalStateException("缺少模板显示译文");
            for (String line : new String(stream.readAllBytes(), StandardCharsets.UTF_8).split("\n")) {
                String[] fields=line.split("\t", -1);
                if (fields.length!=2) throw new IllegalArgumentException("模板显示译文格式错误");
                templateText.put(decode(fields[0]),decode(fields[1]));
            }
        }
    }

    private static String decode(String value) {
        return new String(Base64.getDecoder().decode(value), StandardCharsets.UTF_8);
    }

    public static void premain(String config, Instrumentation instrumentation) {
        try {
            // 配置不明或切换成其他语言时停止补译，避免对非中文会话强制覆盖。
            String settings = Files.readString(Path.of(config, "options", "ide.general.xml"));
            if (!settings.contains("name=\"selectedLocale\" value=\"zh-CN\"")) return;
            instrumentation.addTransformer(new ComposeTranslationAgent());
            System.err.println("[Android中文补译] Compose/设备菜单补译已启用");
        } catch (Exception e) {
            System.err.println("[Android中文补译] 未启用补译：" + e);
        }
    }

    @Override
    public byte[] transform(ClassLoader loader, String name, Class<?> redefining,
                            ProtectionDomain domain, byte[] bytes) {
        Entry entry = entries.get(name);
        if (entry == null) return null;
        try {
            String hash = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
            // shortcut: 仅支持本次审核的类版本；IDE 更新后必须重新审核，不猜测新字节码。
            if (!hash.equals(entry.hash)) {
                System.err.println("[Android中文补译] 类版本变化，跳过：" + name);
                return null;
            }
            int[] changed = {0};
            // IDE 插件资源加载器不暴露 JDK 类，必须回退到平台加载器；不初始化业务类。
            var hierarchy = ClassHierarchyResolver.ofResourceParsing(loader)
                .orElse(ClassHierarchyResolver.ofResourceParsing(ClassLoader.getPlatformClassLoader()));
            ClassFile classFile = ClassFile.of(ClassFile.ClassHierarchyResolverOption.of(hierarchy));
            // 当前哈希版本中，这六个文案首次出现均传给 setName，后续才用于菜单显示。
            Set<String> identityLabels = name.equals("com/android/tools/idea/appinspection/inspectors/network/view/NetworkInspectorTab")
                ? new HashSet<>(Set.of("Export connections", "Clear data", "Zoom out", "Zoom in", "Reset zoom", "Zoom to selection"))
                : new HashSet<>();
            byte[] result = classFile.transformClass(classFile.parse(bytes),
                ClassTransform.transformingMethodBodies((builder, element) -> {
                    if (element instanceof InvokeDynamicInstruction dynamic
                        && dynamic.bootstrapMethod().owner().equals(ClassDesc.of("java.lang.invoke.StringConcatFactory"))) {
                        var site=dynamic.invokedynamic().asSymbol();
                        var arguments=site.bootstrapArgs();
                        boolean replaced=false;
                        for (int i=0;i<arguments.length;i++) {
                            if (arguments[i] instanceof String text && entry.text.containsKey(text)) {
                                // 拼接配方中的动态参数标记必须保留，否则百分比等运行时数据会错位。
                                String target=entry.text.get(text);
                                if (!Arrays.equals(text.chars().filter(c->c==1||c==2).toArray(),target.chars().filter(c->c==1||c==2).toArray()))
                                    throw new IllegalArgumentException("拼接参数标记不一致");
                                arguments[i]=target; replaced=true; changed[0]++;
                            }
                        }
                        if (replaced) {
                            builder.invokedynamic(java.lang.constant.DynamicCallSiteDesc.of(dynamic.bootstrapMethod(),site.invocationName(),site.invocationType(),arguments));
                            return;
                        }
                    }
                    if (name.equals("com/android/tools/idea/avd/DropdownKt")
                        && element instanceof InvokeInstruction call
                        && call.owner().asInternalName().equals("java/lang/String")
                        && call.name().stringValue().equals("valueOf")
                        && call.type().stringValue().equals("(Ljava/lang/Object;)Ljava/lang/String;")) {
                        translateDropdown(builder, 0);
                        changed[0]++;
                        return;
                    }
                    if (element instanceof LoadConstantInstruction load && load.constantEntry() instanceof StringEntry string) {
                        String value = entry.text.get(string.stringValue());
                        if (value != null && !identityLabels.remove(string.stringValue())) { builder.ldc(value); changed[0]++; return; }
                    }
                    builder.with(element);
                    // 只在后台任务表格和详情的格式化显示调用后转换，模型状态仍用于排序与图标判断。
                    if ((name.equals("com/android/tools/idea/appinspection/inspectors/backgroundtask/view/StateProvider")
                        || name.equals("com/android/tools/idea/appinspection/inspectors/backgroundtask/view/table/BackgroundTaskTreeTableView"))
                        && element instanceof InvokeInstruction call
                        && call.owner().asInternalName().equals("com/android/tools/idea/appinspection/inspectors/backgroundtask/view/EntryFormatUtilKt")
                        && call.name().equalsString("capitalizedName") && call.type().equalsString("(Ljava/lang/String;)Ljava/lang/String;")) {
                        for (String[] pair : new String[][]{{"Unspecified","未指定"},{"Enqueued","已入队"},{"Running","运行中"},
                            {"Succeeded","已成功"},{"Failed","已失败"},{"Blocked","等待前置任务"},{"Cancelled","已取消"},
                            {"Unrecognized","无法识别"},{"Scheduled","已调度"},{"Started","已开始"},{"Stopped","已停止"},
                            {"Finished","已完成"},{"Set","已设置"},{"Fired","已触发"},{"Acquired","已获取"},{"Released","已释放"}}) {
                            builder.dup().ldc(pair[0]).invokevirtual(ClassDesc.of("java.lang.String"), "equals",
                                MethodTypeDesc.of(java.lang.constant.ConstantDescs.CD_boolean, ClassDesc.of("java.lang.Object")))
                                .ifThen(match -> match.pop().ldc(pair[1]));
                        }
                        changed[0]++;
                    }
                    if (name.equals("com/android/tools/idea/npw/project/ComposeChooseAndroidProjectStepKt")
                        && element instanceof InvokeInstruction call
                        && call.owner().asInternalName().equals("com/android/tools/idea/npw/project/GridItem")
                        && call.name().stringValue().equals("getTitle")
                        && call.type().stringValue().equals("()Ljava/lang/String;")) {
                        translateTemplateText(builder);
                        changed[0]++;
                    }
                    if ((name.equals("com/android/tools/idea/npw/project/ConfigureAndroidProjectStep")
                        || name.equals("com/android/tools/idea/npw/template/ConfigureTemplateParametersStep"))
                        && element instanceof InvokeInstruction call
                        && call.owner().asInternalName().equals("com/android/tools/idea/wizard/template/Template")
                        && (call.name().equalsString("getName") || call.name().equalsString("getDescription"))
                        && call.type().equalsString("()Ljava/lang/String;")) {
                        translateTemplateText(builder);
                        changed[0]++;
                    }
                    // 方向枚举还供配置解析使用，仅翻译此页面显示调用的返回值。
                    if (name.equals("com/android/tools/idea/avd/AdditionalSettingsPanelKt")
                        && element instanceof InvokeInstruction call
                        && call.owner().asInternalName().equals("com/android/resources/ScreenOrientation")
                        && call.name().stringValue().equals("getShortDisplayValue")
                        && call.type().stringValue().equals("()Ljava/lang/String;")) {
                        for (String[] pair : new String[][] {{"Portrait", "竖屏"}, {"Landscape", "横屏"}, {"Square", "方形"}}) {
                            builder.ldc(pair[0]).ldc(pair[1]).invokevirtual(ClassDesc.of("java.lang.String"), "replace",
                                MethodTypeDesc.of(ClassDesc.of("java.lang.String"), ClassDesc.of("java.lang.CharSequence"), ClassDesc.of("java.lang.CharSequence")));
                        }
                        changed[0]++;
                    }
                }));
            if (name.equals("com/android/tools/idea/npw/project/ComposeChooseAndroidProjectStepKt")) {
                // 分类文字只在渲染函数的局部参数内转换，不改模板名或用于生成工程的模型。
                result = classFile.transformClass(classFile.parse(result), ClassTransform.transformingMethodBodies(
                    method -> method.methodName().equalsString("ProjectEntryListCell"), new CodeTransform() {
                        @Override public void atStart(CodeBuilder builder) {
                            builder.aload(0);
                            translateTemplateText(builder);
                            builder.astore(0);
                            changed[0]++;
                        }
                        @Override public void accept(CodeBuilder builder, CodeElement element) { builder.with(element); }
                    }));
            }
            if (name.equals("com/android/tools/idea/settingssync/SettingsSyncFeature")) {
                // 同一英文也用于授权功能 key，只改标题 getter，保留原字段与身份匹配。
                result = classFile.transformClass(classFile.parse(result), ClassTransform.transformingMethodBodies(
                    method -> method.methodName().equalsString("getTitle"), (builder, element) -> {
                        if (element instanceof java.lang.classfile.instruction.ReturnInstruction) {
                            builder.pop().ldc("备份与同步").areturn();
                            changed[0]++;
                        } else builder.with(element);
                    }));
            }
            if (name.equals("com/android/tools/idea/whatsnew/assistant/v2/model/WhatsNewDocumentLoaderImpl")) {
                // 只替换默认文档流；注入的流、版本筛选、链接和加载流程保持原样。
                result = classFile.transformClass(classFile.parse(result), ClassTransform.transformingMethodBodies(
                    method -> method.methodName().equalsString("_init_$lambda$1"), (builder, element) -> {
                        if (element instanceof InvokeInstruction call
                            && call.owner().asInternalName().equals("java/lang/Class")
                            && call.name().equalsString("getResourceAsStream")
                            && call.type().equalsString("(Ljava/lang/String;)Ljava/io/InputStream;")) {
                            var input = ClassDesc.of("java.io.ByteArrayInputStream");
                            var decoder = ClassDesc.of("java.util.Base64$Decoder");
                            builder.pop2().new_(input).dup()
                                .invokestatic(ClassDesc.of("java.util.Base64"), "getDecoder", MethodTypeDesc.of(decoder))
                                .ldc(whatsNewZip)
                                .invokevirtual(decoder, "decode", MethodTypeDesc.of(ClassDesc.ofDescriptor("[B"), ClassDesc.of("java.lang.String")))
                                .invokespecial(input, "<init>", MethodTypeDesc.of(ClassDesc.ofDescriptor("V"), ClassDesc.ofDescriptor("[B")));
                            changed[0]++;
                        } else builder.with(element);
                    }));
            }
            if (name.equals("com/intellij/sh/highlighter/ShColorPage")) {
                // 此页面原先直接显示语言 ID；仅改页面标题，保留解析器与文件关联所需的 ID。
                result = classFile.transformClass(classFile.parse(result), ClassTransform.transformingMethodBodies(
                    method -> method.methodName().equalsString("getDisplayName"), (builder, element) -> {
                        if (element instanceof java.lang.classfile.instruction.ReturnInstruction) {
                            builder.pop().ldc("Shell 脚本").areturn(); changed[0]++;
                        } else builder.with(element);
                    }));
            }
            if (name.equals("com/android/tools/idea/logcat/messages/LogcatDescriptorInfo")) {
                // 日志等级仍用于筛选和解析，仅在配色描述 getter 的返回值处补充中文。
                result = classFile.transformClass(classFile.parse(result), ClassTransform.transformingMethodBodies(
                    method -> method.methodName().equalsString("getName"), (builder, element) -> {
                        if (element instanceof java.lang.classfile.instruction.ReturnInstruction) {
                            for (String[] pair : new String[][]{{"Verbose","详细（Verbose）"},{"Debug","调试（Debug）"},
                                    {"Info","信息（Info）"},{"Warn","警告（Warn）"},{"Error","错误（Error）"},{"Assert","断言（Assert）"}}) {
                                builder.dup().ldc(pair[0]).invokevirtual(ClassDesc.of("java.lang.String"), "equals",
                                    MethodTypeDesc.of(java.lang.constant.ConstantDescs.CD_boolean, ClassDesc.of("java.lang.Object")))
                                    .ifThen(match -> match.pop().ldc(pair[1]));
                            }
                            changed[0]++;
                        }
                        builder.with(element);
                    }));
            }
            if (name.equals("com/android/tools/idea/profilers/profilingconfig/CpuProfilingConfigurationsDialog$ProfilingConfigurable$ProfilingConfigurationsListCellRenderer")) {
                // 仅任务模式显示固定名称；旧版可编辑配置名及保存模型必须保持原样。
                var parent = ClassDesc.of("com.android.tools.idea.profilers.profilingconfig.CpuProfilingConfigurationsDialog$ProfilingConfigurable");
                result = classFile.transformClass(classFile.parse(result), ClassTransform.transformingMethodBodies(
                    method -> method.methodName().equalsString("getListCellRendererComponent"), (builder, element) -> {
                        builder.with(element);
                        if (element instanceof InvokeInstruction call
                            && call.owner().asInternalName().equals("com/android/tools/profilers/cpu/config/ProfilingConfiguration")
                            && call.name().equalsString("getName")) {
                            builder.aload(0).getfield(ClassDesc.of(name.replace('/', '.')), "this$0", parent)
                                .getfield(parent, "isTaskBasedUxEnabled", java.lang.constant.ConstantDescs.CD_boolean)
                                .ifThen(display -> {
                                    translateProfilerTitle(display);
                                    display.dup().ldc("Java/Kotlin Method Trace").invokevirtual(ClassDesc.of("java.lang.String"), "equals",
                                        MethodTypeDesc.of(java.lang.constant.ConstantDescs.CD_boolean, ClassDesc.of("java.lang.Object")))
                                        .ifThen(match -> match.pop().ldc("Java/Kotlin 方法跟踪"));
                                });
                            changed[0]++;
                        }
                    }));
            }
            if (name.equals("com/android/tools/adtui/ui/options/OptionsPanel")) {
                // buildHeader 仅在任务模式构造标题；转换局部显示值，不改属性模型和名称输入框。
                result = classFile.transformClass(classFile.parse(result), ClassTransform.transformingMethodBodies(
                    method -> method.methodName().equalsString("buildHeader"), (builder, element) -> {
                        builder.with(element);
                        if (element instanceof InvokeInstruction call
                            && call.owner().asInternalName().equals("java/lang/String")
                            && call.name().equalsString("valueOf")
                            && call.type().equalsString("(Ljava/lang/Object;)Ljava/lang/String;")) {
                            translateProfilerTitle(builder);
                            builder.dup().ldc("Java/Kotlin Method Trace").invokevirtual(ClassDesc.of("java.lang.String"), "equals",
                                MethodTypeDesc.of(java.lang.constant.ConstantDescs.CD_boolean, ClassDesc.of("java.lang.Object")))
                                .ifThen(match -> match.pop().ldc("Java/Kotlin 方法跟踪"));
                            changed[0]++;
                        }
                    }));
            }
            String profilerMethod = switch (name) {
                case "com/android/tools/profilers/taskbased/common/constants/strings/TaskBasedUxStrings" -> "getTaskShortName";
                case "com/android/tools/profilers/taskbased/tabs/taskgridandbars/taskgrid/TaskGridItemKt" -> "TaskIconAndDescription";
                case "com/android/tools/profilers/taskbased/tabs/taskgridandbars/taskgrid/TaskGridItemV2Kt" -> "TaskIconAndDescriptionWrapperV2$lambda$9";
                default -> null;
            };
            if (profilerMethod != null) {
                // 任务描述也用作 Compose testTag；保留模型及首个标签调用，只转换显示出口。
                result = classFile.transformClass(classFile.parse(result), ClassTransform.transformingMethodBodies(
                    method -> method.methodName().equalsString(profilerMethod), new CodeTransform() {
                        int descriptionCalls;
                        @Override public void accept(CodeBuilder builder, CodeElement element) {
                            if (profilerMethod.equals("getTaskShortName") && element instanceof java.lang.classfile.instruction.ReturnInstruction)
                                translateProfilerTitle(builder);
                            builder.with(element);
                            if (!profilerMethod.equals("getTaskShortName") && element instanceof InvokeInstruction call
                                && call.owner().asInternalName().equals("com/android/tools/profilers/tasks/ProfilerTaskType")
                                && call.name().equalsString("getDescription") && ++descriptionCalls == 2)
                                translateProfilerTitle(builder);
                        }
                    }));
                changed[0]++;
            }
            if(name.equals("com/android/tools/idea/layoutinspector/snapshots/LayoutInspectorFileEditor")) {
                result=classFile.transformClass(classFile.parse(result),ClassTransform.transformingMethodBodies(
                    method->method.methodName().equalsString("getComponent"),(builder,element)->{
                        // 异常仍以原文记录日志，只在错误页 StatusText 接收显示文字时翻译。
                        if(element instanceof InvokeInstruction call
                            && call.owner().asInternalName().equals("com/android/tools/idea/layoutinspector/snapshots/LayoutInspectorFileEditor$getComponent$status$1")
                            && call.name().equalsString("appendLine") && call.type().equalsString("(Ljava/lang/String;)Lcom/intellij/util/ui/StatusText;")) {
                            translateSnapshotMessage(builder);changed[0]++;
                        }
                        builder.with(element);
                    }));
            }
            if(name.equals("com/android/tools/idea/layoutinspector/tree/LayoutInspectorTreePanel")) {
                result=classFile.transformClass(classFile.parse(result),ClassTransform.transformingMethodBodies(
                    method->method.methodName().equalsString("createIconHeader$lambda$0"),(builder,element)->{
                        // 图标表头忽略列名；使用已译tooltip提供读屏名称，不改列模型或标识。
                        if(element instanceof java.lang.classfile.instruction.ReturnInstruction) {
                            builder.aload(0).invokevirtual(ClassDesc.of("javax.swing.JLabel"),"getAccessibleContext",
                                MethodTypeDesc.of(ClassDesc.of("javax.accessibility.AccessibleContext")))
                                .aload(0).invokevirtual(ClassDesc.of("javax.swing.JComponent"),"getToolTipText",
                                    MethodTypeDesc.of(ClassDesc.of("java.lang.String")))
                                .invokevirtual(ClassDesc.of("javax.accessibility.AccessibleContext"),"setAccessibleName",
                                    MethodTypeDesc.of(java.lang.constant.ConstantDescs.CD_void,ClassDesc.of("java.lang.String")));
                            changed[0]++;
                        }
                        builder.with(element);
                    }));
            }
            if(name.equals("com/android/tools/idea/layoutinspector/recompositions/RecompositionUiModelImpl")) {
                result=classFile.transformClass(classFile.parse(result),ClassTransform.transformingMethodBodies(
                    method->method.methodName().equalsString("generateStateReadLine"),new CodeTransform() {
                        int stringAppends;
                        @Override public void accept(CodeBuilder builder,CodeElement element) {
                            // 第三次字符串追加在所有重写扩展之后；前两次构造扩展输入必须保持英文。
                            if(element instanceof InvokeInstruction call && call.owner().asInternalName().equals("java/lang/StringBuilder")
                                && call.name().equalsString("append") && call.type().equalsString("(Ljava/lang/String;)Ljava/lang/StringBuilder;")
                                && ++stringAppends==3) { translateStateReadTitle(builder);changed[0]++; }
                            builder.with(element);
                        }
                    }));
            }
            boolean backgroundHeader=name.equals("com/android/tools/idea/appinspection/inspectors/backgroundtask/view/table/BackgroundTaskTreeTableView$HeaderRenderer");
            if (backgroundHeader || name.equals("com/android/tools/idea/appinspection/inspectors/backgroundtask/view/table/BackgroundTaskTreeTableView")) {
                String methodName=backgroundHeader?"getTreeCellRendererComponent":"_init_$lambda$3";
                result=classFile.transformClass(classFile.parse(result),ClassTransform.transformingMethodBodies(
                    method->method.methodName().equalsString(methodName),new CodeTransform() {
                        int appends;
                        @Override public void accept(CodeBuilder builder,CodeElement element) {
                            // 当前哈希版本中第二次 append 只显示空态字符串，第一次显示用户类名。
                            if(element instanceof InvokeInstruction call &&
                                ((backgroundHeader && call.owner().asInternalName().equals("javax/swing/JLabel") && call.name().equalsString("setText"))
                                || (!backgroundHeader && call.owner().asInternalName().equals("com/intellij/ui/SimpleColoredComponent") && call.name().equalsString("append") && ++appends==2))) {
                                String[][] pairs=backgroundHeader?new String[][]{{"Workers","工作任务"},{"Jobs","调度任务"},{"Alarms","定时任务"},{"WakeLocks","唤醒锁"}}:
                                    new String[][]{{"No workers have been detected.","尚未检测到工作任务。"},{"No jobs have been detected.","尚未检测到调度任务。"},{"No alarms have been detected.","尚未检测到定时任务。"},{"No wake locks have been detected.","尚未检测到唤醒锁。"}};
                                for(String[] pair:pairs)builder.dup().ldc(pair[0]).invokevirtual(ClassDesc.of("java.lang.String"),"equals",
                                    MethodTypeDesc.of(java.lang.constant.ConstantDescs.CD_boolean,ClassDesc.of("java.lang.Object"))).ifThen(match->match.pop().ldc(pair[1]));
                                changed[0]++;
                            }
                            builder.with(element);
                        }
                    }));
            }
            if (changed[0] == 0) return null;
            System.err.println("[Android中文补译] " + name + "：" + changed[0] + "处");
            return result;
        } catch (Exception e) {
            // 补译失败时继续加载原类，不能为了显示语言阻断 IDE 功能。
            System.err.println("[Android中文补译] 跳过 " + name + "：" + e);
            return null;
        }
    }

    static void translateStateReadTitle(CodeBuilder builder) {
        var string=ClassDesc.of("java.lang.String");
        // 仅转换显示标题前缀，扩展输出的其他内容与null保持；用户表达式中的同名文字不替换。
        builder.dup().instanceOf(string).ifThen(nonNull->nonNull.dup().ldc("State read value: ")
            .invokevirtual(string,"startsWith",MethodTypeDesc.of(java.lang.constant.ConstantDescs.CD_boolean,string))
            .ifThen(match->match.ldc(18).invokevirtual(string,"substring",MethodTypeDesc.of(string,java.lang.constant.ConstantDescs.CD_int))
                .ldc("读取的状态值：").swap().invokevirtual(string,"concat",MethodTypeDesc.of(string,string))));
    }

    static void translateSnapshotMessage(CodeBuilder builder) {
        for(String[] pair:new String[][]{{"Error loading snapshot","加载快照时出错"},
            {"SKP image type is no longer supported starting with Android Studio Panda 2","从 Android Studio Panda 2 起，不再支持 SKP 图像格式"},
            {"Unknown picture type","未知图像类型"}}) {
            // 常量作为 equals 接收者，未知消息和空值均保持原样。
            builder.dup().ldc(pair[0]).swap().invokevirtual(ClassDesc.of("java.lang.String"),"equals",
                MethodTypeDesc.of(java.lang.constant.ConstantDescs.CD_boolean,ClassDesc.of("java.lang.Object")))
                .ifThen(match->match.pop().ldc(pair[1]));
        }
    }

    static void translateProfilerTitle(CodeBuilder builder) {
        for (String[] pair : new String[][]{{"System Trace","系统跟踪"},{"Live View","实时视图"},
                {"Java/Kotlin Method Recording","Java/Kotlin 方法录制"},{"Callstack Sample","调用栈采样"},
                {"Heap Dump","堆转储"},{"Java/Kotlin Allocations","Java/Kotlin 内存分配"},
                {"Native Allocations","原生内存分配"},{"Unspecified","未指定"}}) {
            builder.dup().ldc(pair[0]).invokevirtual(ClassDesc.of("java.lang.String"), "equals",
                MethodTypeDesc.of(java.lang.constant.ConstantDescs.CD_boolean, ClassDesc.of("java.lang.Object")))
                .ifThen(match -> match.pop().ldc(pair[1]));
        }
    }

    void translateTemplateText(CodeBuilder builder) {
        for (var pair : templateText.entrySet()) {
            // 完全相等才替换，避免模板名相互包含时产生混合译文。
            builder.dup().ldc(pair.getKey()).invokevirtual(ClassDesc.of("java.lang.String"), "equals",
                MethodTypeDesc.of(java.lang.constant.ConstantDescs.CD_boolean, ClassDesc.of("java.lang.Object")))
                .ifThen(match -> match.pop().ldc(pair.getValue()));
        }
    }

    // 仅处理已知选项类型；相同英文的用户自定义名称和枚举持久化值保持原样。
    static void translateDropdown(CodeBuilder builder, int index) {
        String[] types = {"com.android.sdklib.internal.avd.AvdCamera", "com.android.sdklib.internal.avd.AvdNetworkSpeed",
            "com.android.sdklib.internal.avd.AvdNetworkLatency", "com.android.tools.idea.avdmanager.skincombobox.NoSkin"};
        String[][][] words = {{{"None", "无"}, {"Emulated", "模拟摄像头"}, {"VirtualScene", "虚拟场景"}, {"Webcam0", "主机摄像头 0"}},
            {{"Full", "全速"}}, {{"None", "无延迟"}}, {{"[None]", "[无]"}}};
        if (index == types.length) {
            builder.invokestatic(ClassDesc.of("java.lang.String"), "valueOf",
                MethodTypeDesc.of(ClassDesc.of("java.lang.String"), ClassDesc.of("java.lang.Object")));
            return;
        }
        builder.dup().instanceOf(ClassDesc.of(types[index])).ifThenElse(yes -> {
            yes.invokestatic(ClassDesc.of("java.lang.String"), "valueOf",
                MethodTypeDesc.of(ClassDesc.of("java.lang.String"), ClassDesc.of("java.lang.Object")));
            for (String[] pair : words[index]) {
                yes.ldc(pair[0]).ldc(pair[1]).invokevirtual(ClassDesc.of("java.lang.String"), "replace",
                    MethodTypeDesc.of(ClassDesc.of("java.lang.String"), ClassDesc.of("java.lang.CharSequence"), ClassDesc.of("java.lang.CharSequence")));
            }
        }, no -> translateDropdown(no, index + 1));
    }
}
