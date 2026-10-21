package local.android.zh.agent;

import java.lang.classfile.*;
import java.lang.constant.*;
import java.lang.reflect.AccessFlag;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;

/** 验证模板显示映射完全匹配，同时保持生成器读取的原始模板元数据。 */
public final class TemplateDisplayTest {
    public static void main(String[] args) throws Exception {
        var agent=new ComposeTranslationAgent();
        byte[] bytes=ClassFile.of().build(ClassDesc.of("TemplateDisplayFixture"), c ->
            c.withMethodBody("display", MethodTypeDesc.of(ConstantDescs.CD_String, ConstantDescs.CD_String),
                AccessFlag.PUBLIC.mask()|AccessFlag.STATIC.mask(), b -> {
                    b.aload(0);agent.translateTemplateText(b);b.areturn();
                }));
        Class<?> fixture=new ClassLoader(TemplateDisplayTest.class.getClassLoader()) {
            Class<?> define() { return defineClass(null,bytes,0,bytes.length); }
        }.define();
        var display=fixture.getMethod("display",String.class);
        Map<String,String> values=new HashMap<>();
        for (String line:Files.readAllLines(Path.of(args[0]))) {
            String[] fields=line.split("\t");
            String source=new String(Base64.getDecoder().decode(fields[0]),StandardCharsets.UTF_8);
            String target=new String(Base64.getDecoder().decode(fields[1]),StandardCharsets.UTF_8);
            values.put(source,target);
            assert display.invoke(null,source).equals(target):source;
        }
        assert display.invoke(null,"Custom Empty Activity").equals("Custom Empty Activity");
        assert display.invoke(null,"").equals("");
        int checked=0;
        for (var template:new com.android.tools.idea.wizard.template.impl.WizardTemplateProviderImpl().getTemplates()) {
            String name=template.getName(), description=template.getDescription();
            Object recipe=template.getRecipe();
            assert values.containsKey(name) && values.containsKey(description):name;
            assert display.invoke(null,name).equals(values.get(name));
            assert display.invoke(null,description).equals(values.get(description));
            assert template.getName().equals(name) && template.getDescription().equals(description);
            assert template.getRecipe()==recipe;
            checked++;
        }
        System.out.println("通过："+values.size()+" 条显示映射；"+checked+" 个模板的原名称、说明及生成器保持不变。");
    }
}
