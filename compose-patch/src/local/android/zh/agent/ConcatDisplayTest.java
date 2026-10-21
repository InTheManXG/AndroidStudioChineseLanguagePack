package local.android.zh.agent;

import java.lang.classfile.*;
import java.lang.classfile.instruction.InvokeDynamicInstruction;
import java.lang.constant.*;
import java.lang.reflect.AccessFlag;
import java.util.jar.JarFile;

/** 执行真实补译后的字符串拼接配方，验证动态参数和用户输入不被破坏。 */
public final class ConcatDisplayTest {
    public static void main(String[] args) throws Exception {
        verify(args[0], "com/android/tools/idea/npw/project/FormFactorSdkControls", "约 <b>\u0001</b>", "99.2%", "约 <b>99.2%</b>");
        String prefix="com/android/tools/idea/appinspection/inspectors/network/view/";
        // 包含变量语法、HTML 和引号的输入也必须原样进入提示，不能被当成译文处理。
        String input="${RULE_NAME}<b>English</b>'中文";
        verify(args[0], prefix+"details/RuleDetailsView", "名为“\u0001”的规则已存在", input, "名为“"+input+"”的规则已存在");
        verify(args[0], prefix+"rules/RuleVariablesDialog", "变量名称重复：\u0001", input, "变量名称重复："+input);
        verify(args[0], "com/android/tools/idea/sqlite/ui/exportToFile/ExportToFileDialogViewImpl", "导出\u0001", "user_table_'${value}'", "导出user_table_'${value}'");
        verify(args[0], "com/android/tools/idea/layoutinspector/ui/toolbar/actions/TargetSelectionActionFactory", "\u0001（不支持 API 29 以下版本）", input, input+"（不支持 API 29 以下版本）");
        verifyMany(args[0], "com/android/tools/idea/layoutinspector/ui/toolbar/actions/ToggleOverlayAction", "无法读取图像“\u0001”。错误：\u0001",
            "无法读取图像“/用户/English.png”。错误：raw 'error' ${value}", "/用户/English.png", "raw 'error' ${value}");
        for(int index:new int[]{1,10,2147483647})verifyMany(args[0],
            "com/android/tools/idea/appinspection/inspectors/backgroundtask/view/EntryDetailsView", "第 \u0001 次实际触发时间", "第 "+index+" 次实际触发时间",index);
        // 快捷键是运行时生成的显示值；数字边界仍按真实 int 配方执行。
        for(String shortcut:new String[]{"⌘↩", "Ctrl+Enter", "${shortcut}'", ""}) {
            verify(args[0], "com/android/tools/idea/sqlite/ui/sqliteEvaluator/SqliteEvaluatorViewImpl", "执行 SQLite 表达式（\u0001）", shortcut, "执行 SQLite 表达式（"+shortcut+"）");
            verify(args[0], "com/android/tools/idea/sqlite/ui/parametersBinding/ParametersBindingDialogViewImpl$InputComponent", "将值设为 NULL（\u0001）", shortcut, "将值设为 NULL（"+shortcut+"）");
        }
        for(int[] count:new int[][]{{0,0},{0,10},{3,10},{10,10},{2147483647,2147483647}})
            verifyMany(args[0], "com/android/tools/idea/sqlite/ui/mainView/DatabaseInspectorViewImpl", "已下载数据库（\u0001/\u0001）…", "已下载数据库（"+count[0]+"/"+count[1]+"）…",count[0],count[1]);
        // 原始路径/类名与已转义错误包含引号和变量语法，必须逐字保留。
        String dexInput="/用户/My File$Inner &lt;error&gt; '${value}'";
        verify(args[0], "com/android/tools/idea/apk/viewer/dex/DexFileViewer", "APK 分析器加载以下文件时遇到问题：\u0001", dexInput, "APK 分析器加载以下文件时遇到问题：" + dexInput + "");
        verify(args[0], "com/android/tools/idea/apk/viewer/dex/DexFileViewer", "APK 分析器已成功从以下位置加载映射：\u0001", dexInput, "APK 分析器已成功从以下位置加载映射：" + dexInput + "");
        verifyMany(args[0], "com/android/tools/idea/apk/viewer/dex/DexFileViewer", "APK 分析器已成功从以下位置加载映射：\u0001\n加载以下文件时遇到问题：\u0001", "APK 分析器已成功从以下位置加载映射：" + dexInput + "\n加载以下文件时遇到问题：" + "second_failed_file.txt" + "", dexInput, "second_failed_file.txt");
        verify(args[0], "com/android/tools/idea/apk/viewer/dex/ShowDisassemblyAction$1", "构建 DEX 文件时出错：\u0001", dexInput, "构建 DEX 文件时出错：" + dexInput + "");
        verify(args[0], "com/android/tools/idea/apk/viewer/dex/ShowDisassemblyAction$1", "无法获取字节码：\u0001", dexInput, "无法获取字节码：" + dexInput + "");
        verify(args[0], "com/android/tools/idea/apk/viewer/dex/ShowReferencesAction", "对 \u0001 的引用", dexInput, "对 " + dexInput + " 的引用");
        verify(args[0], "com/android/tools/idea/apk/viewer/dex/GenerateProguardKeepRuleAction", "\u0001 的 ProGuard 保留规则", dexInput, "" + dexInput + " 的 ProGuard 保留规则");
        System.out.println("通过：真实百分比与规则校验拼接配方执行，动态数字、HTML 和用户输入保持正确。");
    }

    private static void verify(String jarPath, String name, String expectedRecipe, String input, String expected) throws Exception {
        verifyMany(jarPath,name,expectedRecipe,expected,input);
    }

    private static void verifyMany(String jarPath, String name, String expectedRecipe, String expected, Object... inputs) throws Exception {
        byte[] original;
        try(var jar=new JarFile(jarPath)) { original=jar.getInputStream(jar.getJarEntry(name+".class")).readAllBytes(); }
        byte[] patched=new ComposeTranslationAgent().transform(ConcatDisplayTest.class.getClassLoader(),name,null,null,original);
        assert patched!=null;
        DynamicCallSiteDesc recipe=null;
        for(var method:ClassFile.of().parse(patched).methods()) if(method.code().isPresent()) {
            for(var element:method.code().get()) if(element instanceof InvokeDynamicInstruction call
                && call.bootstrapArgs().contains(expectedRecipe)) recipe=call.invokedynamic().asSymbol();
        }
        assert recipe!=null:"未替换拼接配方";
        final var site=recipe;
        var parameterTypes=site.invocationType().resolveConstantDesc(java.lang.invoke.MethodHandles.lookup()).parameterArray();
        assert parameterTypes.length==inputs.length;
        for(Class<?> type:parameterTypes)assert type==int.class || type==String.class:"测试未支持的配方参数";
        byte[] fixtureBytes=ClassFile.of().build(ClassDesc.of("ConcatDisplayFixture"), c ->
            c.withMethodBody("display",site.invocationType(),
                AccessFlag.PUBLIC.mask()|AccessFlag.STATIC.mask(),b->{
                    // 真实配方包括整数序号和多个字符串，按原参数类型与顺序装载。
                    for(int i=0;i<parameterTypes.length;i++)if(parameterTypes[i]==int.class)b.iload(i);else b.aload(i);
                    b.invokedynamic(site).areturn();
                }));
        Class<?> fixture=new ClassLoader(ConcatDisplayTest.class.getClassLoader()) {
            Class<?> define(){return defineClass(null,fixtureBytes,0,fixtureBytes.length);}
        }.define();
        assert fixture.getMethod("display",parameterTypes).invoke(null,inputs).equals(expected):name;
    }
}
