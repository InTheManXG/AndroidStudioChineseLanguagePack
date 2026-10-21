package local.android.zh;

import com.intellij.openapi.options.ConfigurableEP;
import com.intellij.openapi.options.UnnamedConfigurable;

public final class SettingsTranslationTest {
    public static void main(String[] args) {
        ConfigurableEP<UnnamedConfigurable> ep=new ConfigurableEP<>((com.intellij.openapi.components.ComponentManager)java.lang.reflect.Proxy.newProxyInstance(
            SettingsTranslationTest.class.getClassLoader(),new Class<?>[]{com.intellij.openapi.components.ComponentManager.class},
            (proxy,method,parameters) -> { throw new AssertionError("不应创建设置页："+method.getName()); })) {};
        ep.id="experimental";
        ep.providerClass="com.android.tools.idea.flags.StudioExperimentalConfigurableProvider";
        ep.displayName="Experimental";
        SettingsTranslation.translate(ep);
        assert ep.displayName.equals("实验性功能");
        assert ep.id.equals("experimental");
        SettingsTranslation.translate(ep);
        assert ep.displayName.equals("实验性功能");
        ep.providerClass="unrelated.Provider"; ep.displayName="Experimental";
        SettingsTranslation.translate(ep);
        assert ep.displayName.equals("Experimental") : "误改第三方设置";
        System.out.println("通过：设置标题身份匹配、内部 ID 保护、重复调用幂等。");
    }
}
