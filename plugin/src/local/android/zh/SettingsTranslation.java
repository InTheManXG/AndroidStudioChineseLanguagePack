package local.android.zh;

import com.intellij.openapi.options.Configurable;
import com.intellij.openapi.options.ConfigurableEP;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectManager;
import com.intellij.openapi.diagnostic.Logger;

/** XML 直写标题不经过资源包，只修改已核对身份的设置项显示字段。 */
public final class SettingsTranslation {
    private static final String[][] TITLES = {
        {"memory.settings", "com.android.tools.idea.memorysettings.MemorySettingsConfigurableProvider", "Memory Settings", "内存设置"},
        {"com.google.gct.login2.settings.GoogleLoginApplicationSettingsConfigurableProvider", "com.google.gct.login2.settings.GoogleLoginApplicationSettingsConfigurableProvider", "Google Accounts", "Google 账号"},
        {"DeclarativeFocus", "com.android.declarative.intellij.focus.services.DeclarativeFocusConfigurable", "Gradle Declarative Focus", "Gradle 声明式专注模式"},
        {"com.android.tools.idea.marketing.configurable.MarketingConfigurableProvider", "com.android.tools.idea.marketing.configurable.MarketingConfigurableProvider", "Communications", "通信"},
        {"experimental", "com.android.tools.idea.flags.StudioExperimentalConfigurableProvider", "Experimental", "实验性功能"},
        {"experimentalPlugin", "com.android.tools.idea.flags.PluginExperimentalConfigurableProvider", "Android (Experimental)", "Android（实验性功能）"},
        {"studioLabs", "com.android.tools.studio.labs.StudioLabsConfigurableProvider", "Studio Labs", "Studio 实验室"},
        {"test.recorder", "com.google.gct.testrecorder.settings.TestRecorderSettingsConfigurable", "Espresso Test Recorder", "Espresso 测试录制器"}
    };

    static void install() {
        for (var ep : Configurable.APPLICATION_CONFIGURABLE.getExtensionList()) translate(ep);
        var manager=ProjectManager.getInstance();
        translateProject(manager.getDefaultProject());
        for (Project project : manager.getOpenProjects()) translateProject(project);
    }

    static void translateProject(Project project) {
        for (var ep : Configurable.PROJECT_CONFIGURABLE.getExtensions(project)) translate(ep);
    }

    static void translate(ConfigurableEP<?> ep) {
        for (String[] title : TITLES) {
            // ID、提供类、原文同时匹配，避免误改第三方同名项；不创建设置页或修改配置值。
            if (title[0].equals(ep.id) && (title[1].equals(ep.providerClass) || title[1].equals(ep.instanceClass))
                    && title[2].equals(ep.displayName)) {
                ep.displayName=title[3];
                Logger.getInstance(SettingsTranslation.class).info("设置标题补译："+ep.id);
            }
        }
    }
}
