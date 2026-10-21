package local.android.zh.agent;

import com.android.tools.idea.logcat.message.LogLevel;
import com.intellij.openapi.editor.colors.TextAttributesKey;
import java.util.*;

/** 实例化发行版配色描述类，验证中文只影响显示名称，不改变配色键、标签或日志枚举。 */
public final class ColorDisplayTest {
    public static void main(String[] args) throws Exception {
        String name="com/android/tools/idea/logcat/messages/LogcatDescriptorInfo";
        var agent=new ComposeTranslationAgent();byte[] original;
        try(var input=ColorDisplayTest.class.getClassLoader().getResourceAsStream(name+".class")){original=input.readAllBytes();}
        byte[] patched=agent.transform(ColorDisplayTest.class.getClassLoader(),name,null,null,original);assert patched!=null;
        Class<?> type=new ClassLoader(ColorDisplayTest.class.getClassLoader()) {
            Class<?> define(){return defineClass(name.replace('/','.'),patched,0,patched.length);}
        }.define();
        var constructor=type.getDeclaredConstructor(LogLevel.class,TextAttributesKey.class,String.class,TextAttributesKey.class,String.class);
        constructor.setAccessible(true);
        var names=Map.of("VERBOSE","详细（Verbose）","DEBUG","调试（Debug）","INFO","信息（Info）","WARN","警告（Warn）","ERROR","错误（Error）","ASSERT","断言（Assert）");
        for(LogLevel level:LogLevel.values()) {
            var levelKey=TextAttributesKey.createTextAttributesKey("AUDIT_LEVEL_"+level.name());
            var messageKey=TextAttributesKey.createTextAttributesKey("AUDIT_MESSAGE_"+level.name());
            Object descriptor=constructor.newInstance(level,levelKey,"tag-original",messageKey,"message-original");
            Map<String,Object> expected=Map.of("getName",names.get(level.name()),"getLevelKey",levelKey,
                "getMessageKey",messageKey,"getLevelTag","tag-original","getMessageTag","message-original");
            for(var item:expected.entrySet()) {
                var method=type.getDeclaredMethod(item.getKey());method.setAccessible(true);
                assert Objects.equals(method.invoke(descriptor),item.getValue()):item.getKey();
            }
            assert LogLevel.valueOf(level.name())==level;
        }
        assert LogLevel.values().length==6;
        // 语言与文件类型的身份类不在补译名单中，不能因设置标题而改名。
        for(String identity:List.of("com/intellij/sh/ShLanguage","com/intellij/sh/ShFileType",
                "com/android/tools/idea/gradle/dcl/lang/psi/DeclarativeFileType","com/android/tools/idea/logcat/filters/parser/LogcatFilterFileType"))
            assert agent.transform(null,identity,null,null,new byte[0])==null:identity;
        System.out.println("通过：6 个真实日志等级描述的中文名、配色键/标签保护；Shell/DCL/Logcat 文件类型身份不改。");
    }
}
