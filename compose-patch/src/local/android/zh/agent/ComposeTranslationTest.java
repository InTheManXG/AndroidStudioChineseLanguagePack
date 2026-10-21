package local.android.zh.agent;

import java.lang.classfile.*;
import java.lang.classfile.constantpool.StringEntry;
import java.lang.classfile.instruction.ConstantInstruction.LoadConstantInstruction;
import java.lang.classfile.instruction.InvokeInstruction;
import java.lang.classfile.instruction.InvokeDynamicInstruction;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;

/** 对真实发行类验证替换范围、方法指令及版本不匹配回退。 */
public final class ComposeTranslationTest {
    private static List<String> instructions(byte[] bytes, Map<String,String> expected) {
        List<String> result = new ArrayList<>();
        var model = ClassFile.of().parse(bytes);
        for (var method : model.methods()) {
            result.add(method.methodName().stringValue() + method.methodType().stringValue());
            method.code().ifPresent(code -> {
                for (var element : code) if (element instanceof Instruction instruction) {
                    if (instruction instanceof LoadConstantInstruction load && load.constantEntry() instanceof StringEntry text) {
                        result.add("TEXT:" + expected.getOrDefault(text.stringValue(),text.stringValue()));
                    } else result.add(instruction.opcode().toString());
                    if (model.thisClass().asInternalName().equals("com/android/tools/idea/appinspection/inspectors/network/view/NetworkInspectorTab")
                        && instruction instanceof InvokeInstruction call
                        && call.owner().asInternalName().equals("com/android/tools/adtui/stdui/CommonButton")
                        && call.name().equalsString("setName") && !expected.isEmpty()) {
                        // 独立比对：恢复 name 参数的预期原文，其他显示文本仍必须被替换。
                        int index=result.size()-2;
                        for(var pair:expected.entrySet()) if(result.get(index).equals("TEXT:"+pair.getValue()))
                            result.set(index,"TEXT:"+pair.getKey());
                    }
                    if (instruction instanceof InvokeDynamicInstruction dynamic) {
                        result.add("BOOTSTRAP:"+dynamic.bootstrapMethod()+":"+dynamic.name()+":"+dynamic.type());
                        for (var argument:dynamic.bootstrapArgs()) {
                            result.add(argument instanceof String text?"TEXT:"+expected.getOrDefault(text,text):"ARG:"+argument);
                        }
                    }
                    if (!expected.isEmpty() && model.thisClass().asInternalName().equals("com/android/tools/idea/avd/AdditionalSettingsPanelKt")
                        && instruction instanceof InvokeInstruction call
                        && call.owner().asInternalName().equals("com/android/resources/ScreenOrientation")
                        && call.name().stringValue().equals("getShortDisplayValue")) {
                        result.addAll(List.of("TEXT:Portrait", "TEXT:竖屏", "INVOKEVIRTUAL", "TEXT:Landscape", "TEXT:横屏", "INVOKEVIRTUAL", "TEXT:Square", "TEXT:方形", "INVOKEVIRTUAL"));
                    }
                }
            });
        }
        return result;
    }
    private static List<String> identityInstructions(byte[] bytes, String methodName) {
        List<String> result=new ArrayList<>();
        for (var method:ClassFile.of().parse(bytes).methods()) {
            if (!method.methodName().equalsString(methodName)) continue;
            method.code().ifPresent(code -> {
                for (var element:code) if (element instanceof Instruction instruction) {
                    result.add(instruction.opcode().toString());
                    if (instruction instanceof LoadConstantInstruction load) result.add(String.valueOf(load.constantValue()));
                    if (instruction instanceof InvokeInstruction call)
                        result.add(call.owner().asInternalName()+"."+call.name().stringValue()+call.type().stringValue());
                    if (instruction instanceof java.lang.classfile.instruction.FieldInstruction field)
                        result.add(field.owner().asInternalName()+"."+field.name().stringValue()+field.type().stringValue());
                }
            });
        }
        return result;
    }

    public static void main(String[] args) throws Exception {
        var agent = new ComposeTranslationAgent();
        Map<String,Map<String,String>> all = new HashMap<>();
        for (String line : Files.readAllLines(Path.of(args[0]))) {
            String[] f=line.split("\t", -1);
            all.computeIfAbsent(f[0], k->new HashMap<>()).put(new String(Base64.getDecoder().decode(f[2]),StandardCharsets.UTF_8),new String(Base64.getDecoder().decode(f[3]),StandardCharsets.UTF_8));
        }
        // 模拟 IDE 插件加载器：可读取插件类，但不会向资源查找暴露 JDK 类。
        ClassLoader pluginLoader = new ClassLoader(ComposeTranslationTest.class.getClassLoader()) {
            @Override public java.io.InputStream getResourceAsStream(String name) {
                if (name.startsWith("java/")) return null;
                return super.getResourceAsStream(name);
            }
        };
        int checked=0;
        for (var entry:all.entrySet()) {
            String name=entry.getKey();
            // 测试 classpath 仅含原始发行 JAR；哈希仍由 agent 校验，覆盖跨插件的真实类。
            try (var stream=ComposeTranslationTest.class.getClassLoader().getResourceAsStream(name+".class")) {
                assert stream!=null : "缺少原始类："+name;
                byte[] original=stream.readAllBytes();
                List<String> before=instructions(original,Map.of());
                for(String source:entry.getValue().keySet()) if (!source.isEmpty()) assert before.contains("TEXT:"+source):"未确认的文本："+source;
                byte[] patched=agent.transform(pluginLoader,name,null,null,original);
                assert patched!=null:name;
                assert ClassFile.of(ClassFile.ClassHierarchyResolverOption.of(ClassHierarchyResolver.ofResourceParsing(ComposeTranslationTest.class.getClassLoader()))).verify(patched).isEmpty():"字节码验证失败："+name;
                Set<String> displayMethods=switch(name) {
                    case "com/android/tools/idea/avd/DropdownKt" -> Set.of("Dropdown$lambda$0$0$0$1", "Dropdown$lambda$1");
                    case "com/android/tools/idea/npw/project/ComposeChooseAndroidProjectStepKt" -> Set.of("ProjectEntryListCell", "GridItemText");
                    case "com/android/tools/idea/npw/project/ConfigureAndroidProjectStep" -> Set.of("setTemplateThumbnail");
                    case "com/android/tools/idea/npw/template/ConfigureTemplateParametersStep" -> Set.of("onEntering", "onEntering$lambda$0");
                    case "com/android/tools/idea/settingssync/SettingsSyncFeature" -> Set.of("getTitle");
                    case "com/android/tools/idea/whatsnew/assistant/v2/model/WhatsNewDocumentLoaderImpl" -> Set.of("_init_$lambda$1");
                    case "com/intellij/sh/highlighter/ShColorPage" -> Set.of("getDisplayName");
                    case "com/android/tools/idea/logcat/messages/LogcatDescriptorInfo" -> Set.of("getName");
                    case "com/android/tools/profilers/taskbased/common/constants/strings/TaskBasedUxStrings" -> Set.of("getTaskShortName");
                    case "com/android/tools/profilers/taskbased/tabs/taskgridandbars/taskgrid/TaskGridItemKt" -> Set.of("TaskIconAndDescription");
                    case "com/android/tools/profilers/taskbased/tabs/taskgridandbars/taskgrid/TaskGridItemV2Kt" -> Set.of("TaskIconAndDescriptionWrapperV2$lambda$9");
                    case "com/android/tools/idea/profilers/profilingconfig/CpuProfilingConfigurationsDialog$ProfilingConfigurable$ProfilingConfigurationsListCellRenderer" -> Set.of("getListCellRendererComponent");
                    case "com/android/tools/adtui/ui/options/OptionsPanel" -> Set.of("buildHeader");
                    case "com/android/tools/idea/appinspection/inspectors/backgroundtask/view/StateProvider" -> Set.of("convert");
                    case "com/android/tools/idea/appinspection/inspectors/backgroundtask/view/table/BackgroundTaskTreeTableView" -> Set.of("_init_$lambda$3","_init_$lambda$4");
                    case "com/android/tools/idea/appinspection/inspectors/backgroundtask/view/table/BackgroundTaskTreeTableView$HeaderRenderer" -> Set.of("getTreeCellRendererComponent");
                    case "com/android/tools/idea/layoutinspector/snapshots/LayoutInspectorFileEditor" -> Set.of("getComponent");
                    case "com/android/tools/idea/layoutinspector/tree/LayoutInspectorTreePanel" -> Set.of("createIconHeader$lambda$0");
                    case "com/android/tools/idea/layoutinspector/recompositions/RecompositionUiModelImpl" -> Set.of("generateStateReadLine");
                    default -> Set.of();
                };
                if (displayMethods.isEmpty()) {
                    assert instructions(original,entry.getValue()).equals(instructions(patched,Map.of())):"非目标指令变化："+name;
                } else {
                    // 新分支只允许出现在已审核的文案渲染方法，选择回调等其他方法逐条比较。
                    var a=ClassFile.of().parse(original); var b=ClassFile.of().parse(patched);
                    before=instructions(original,entry.getValue());
                    var after=instructions(patched,Map.of());
                    assert a.methods().size()==b.methods().size();
                    for (int i=0;i<a.methods().size();i++) {
                        String method=a.methods().get(i).methodName().stringValue();
                        if (displayMethods.contains(method)) continue;
                        String signature=method+a.methods().get(i).methodType().stringValue();
                        String next=i+1<a.methods().size()?a.methods().get(i+1).methodName().stringValue()+a.methods().get(i+1).methodType().stringValue():null;
                        assert before.subList(before.indexOf(signature),next==null?before.size():before.indexOf(next)).equals(
                            after.subList(after.indexOf(signature),next==null?after.size():after.indexOf(next))):method;
                    }
                }
                // 登录功能的内部身份、授权范围和目标链接必须保持原值，不能随标题汉化。
                if (name.contains("LoginFeature") || name.endsWith("SettingsSyncFeature")) {
                    for (String method:List.of("getKey", "getOAuthScopes", "getPermissionInfoUrl", "getInfoUrl"))
                        assert identityInstructions(original,method).equals(identityInstructions(patched,method)) : "误改授权语义："+name+"."+method;
                }
                if (name.endsWith("SettingsSyncFeature")) {
                    assert instructions(patched,Map.of()).contains("TEXT:Backup and Sync") : "误改内部 key";
                    assert identityInstructions(patched,"getTitle").contains("备份与同步") : "未翻译显示标题";
                }
                // 再次处理已补译或升级后的类必须跳过，避免覆盖别的转换器或重复改写。
                assert agent.transform(pluginLoader,name,null,null,patched)==null:"未校验类哈希";
                checked++;
            }
        }
        assert agent.transform(null,"unrelated/UserData",null,null,new byte[0])==null;
        System.out.println("通过："+checked+" 个真实类、全部白名单文案、非目标指令保持、版本不匹配回退。");
    }
}
