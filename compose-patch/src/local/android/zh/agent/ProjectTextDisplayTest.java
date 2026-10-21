package local.android.zh.agent;

import java.lang.classfile.*;
import java.lang.classfile.instruction.InvokeDynamicInstruction;
import java.lang.constant.*;
import java.lang.reflect.AccessFlag;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.regex.Pattern;

/** 运行真实拼接配方，验证动态设备图标、颜色、链接与用户配置名。 */
public final class ProjectTextDisplayTest {
    public static void main(String[] args) throws Exception {
        Map<String,Map<String,String>> translations=new HashMap<>();
        for (String line:Files.readAllLines(Path.of(args[0]))) {
            String[] f=line.split("\t",-1);
            String source=new String(Base64.getDecoder().decode(f[2]),StandardCharsets.UTF_8);
            String target=new String(Base64.getDecoder().decode(f[3]),StandardCharsets.UTF_8);
            translations.computeIfAbsent(f[0],k->new HashMap<>()).put(source,target);
        }
        int checked=0;
        for (String name:List.of("com/android/tools/idea/streaming/core/EmptyStatePanel",
                "com/android/tools/idea/gradle/actions/AbstractBuildRunConfigurationAction",
                "com/android/tools/idea/gradle/notification/ProjectSyncStatusNotificationProvider$SyncProblemNotificationPanel",
                "com/android/tools/idea/run/configuration/execution/ExecutionUtils",
                "com/android/tools/idea/gradle/project/build/invoker/GradleTasksExecutorImpl$TaskImpl",
                "com/android/tools/idea/run/tasks/DeployTask",
                "com/android/tools/idea/run/tasks/ApplyChangesTask",
                "com/android/tools/idea/run/tasks/ApplyCodeChangesTask")) {
            byte[] original;
            try(var stream=ProjectTextDisplayTest.class.getClassLoader().getResourceAsStream(name+".class")) { original=stream.readAllBytes(); }
            byte[] patched=new ComposeTranslationAgent().transform(ProjectTextDisplayTest.class.getClassLoader(),name,null,null,original);
            assert patched!=null;
            for (var entry:translations.get(name).entrySet()) {
                // href 是路由身份，翻译链接的可见文字时必须保持其目标与顺序。
                var pattern=Pattern.compile("href=['\"]([^'\"]+)['\"]");
                assert pattern.matcher(entry.getKey()).results().map(m->m.group(1)).toList().equals(
                    pattern.matcher(entry.getValue()).results().map(m->m.group(1)).toList());
            }
            for(var method:ClassFile.of().parse(patched).methods()) if(method.code().isPresent()) {
                for(var element:method.code().get()) if(element instanceof InvokeDynamicInstruction call
                        && call.bootstrapMethod().owner().equals(ClassDesc.of("java.lang.invoke.StringConcatFactory"))) {
                    String translated=call.bootstrapArgs().stream().filter(x->x instanceof String)
                        .map(Object::toString).filter(translations.get(name)::containsValue).findFirst().orElse(null);
                    if (translated==null) continue;
                    var site=call.invokedynamic().asSymbol();
                    int n=site.invocationType().parameterCount();
                    assert Arrays.stream(site.invocationType().parameterArray()).allMatch(ConstantDescs.CD_String::equals);
                    byte[] fixture=ClassFile.of().build(ClassDesc.of("ProjectTextFixture"),c->c.withMethodBody("display",site.invocationType(),
                        AccessFlag.PUBLIC.mask()|AccessFlag.STATIC.mask(),b->{for(int i=0;i<n;i++)b.aload(i);b.invokedynamic(site).areturn();}));
                    Class<?> type=new ClassLoader(ProjectTextDisplayTest.class.getClassLoader()) {
                        Class<?> define(){return defineClass(null,fixture,0,fixture.length);}
                    }.define();
                    Class<?>[] signature=new Class<?>[n];Arrays.fill(signature,String.class);
                    Object[] values=new Object[n];String expected=translated;
                    for(int i=0;i<n;i++) {
                        values[i]="用户值"+i;
                        int at=expected.indexOf('\u0001');assert at>=0;
                        expected=expected.substring(0,at)+values[i]+expected.substring(at+1);
                    }
                    assert type.getMethod("display",signature).invoke(null,values).equals(expected);
                    checked++;
                }
            }
        }
        assert checked==16:checked;
        System.out.println("通过：16 条真实动态拼接配方执行，HTML 链接、颜色参数、设备图标、应用标识与运行配置名保持。");
    }
}
