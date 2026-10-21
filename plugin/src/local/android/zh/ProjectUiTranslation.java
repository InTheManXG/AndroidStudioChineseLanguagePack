package local.android.zh;

import com.intellij.l10n.LocalizationStateService;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.startup.ProjectActivity;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowManager;
import com.intellij.openapi.wm.ex.ToolWindowManagerListener;
import java.util.List;
import kotlin.Unit;
import kotlin.coroutines.Continuation;

/** 项目级界面在应用启动后才创建，需同时覆盖现有窗口与延迟注册的窗口。 */
public final class ProjectUiTranslation implements ProjectActivity, ToolWindowManagerListener, com.intellij.openapi.project.DumbAware {
    static final String[][] TITLES = {
        {"App Inspection", "App Inspection", "应用检查"},
        {"App Quality Insights", "App Quality Insights", "应用质量洞察"},
        {"Device Explorer", "Device Explorer", "设备资源管理器"},
        {"Device Manager 2", "Device Manager", "设备管理器"},
        {"Running Devices", "Running Devices", "运行中的设备"},
        {"Layout Inspector", "Layout Inspector", "布局检查器"},
        {"Android Profiler", "Profiler", "性能分析器"},
        {"Build Variants", "Build Variants", "构建变体"},
        {"Focus", "Focus", "专注模式"},
        {"Resources Explorer", "Resource Manager", "资源管理器"},
        {"Bookmarks", "Bookmarks", "书签"},
        {"Structure", "Structure", "结构"},
        {"App Links Assistant", "App Links Assistant", "应用链接助手"},
        {"Backup and Sync History", "Backup and Sync History", "备份与同步历史"},
        {"Layout Validation", "Layout Validation", "布局验证"},
        {"Logcat", "Logcat", "日志（Logcat）"},
        {"TODO", "TODO", "待办事项"}
    };

    private static boolean enabled() {
        String locale = LocalizationStateService.getInstance().getSelectedLocale();
        return "zh-CN".equals(locale) || "zh_CN".equals(locale);
    }

    @Override public Object execute(Project project, Continuation<? super Unit> continuation) {
        ApplicationManager.getApplication().invokeLater(() -> {
            if (project.isDisposed() || !enabled()) return;
            com.intellij.openapi.diagnostic.Logger.getInstance(ProjectUiTranslation.class).info("项目界面补译启动");
            SettingsTranslation.translateProject(project);
            ToolWindowManager manager = ToolWindowManager.getInstance(project);
            for (String id : manager.getToolWindowIds()) translate(manager.getToolWindow(id));
        });
        return Unit.INSTANCE;
    }

    @Override public void toolWindowsRegistered(List<String> ids, ToolWindowManager manager) {
        manager.invokeLater(() -> {
            if (!enabled()) return;
            for (String id : ids) translate(manager.getToolWindow(id));
        });
    }

    @Override public void toolWindowShown(ToolWindow window) {
        if (enabled()) translate(window);
    }

    static void translate(ToolWindow window) {
        if (window == null || window.isDisposed()) return;
        for (String[] title : TITLES) {
            if (!title[0].equals(window.getId())) continue;
            var shortTitle = window.getStripeShortTitleProvider();
            if (shortTitle != null && (title[1].equals(shortTitle.get()) || title[0].equals(shortTitle.get())))
                window.setStripeShortTitleProvider(() -> title[2]);
            // 仅改已知显示值，不覆盖动态内容标题，更不改变供其他插件查找的窗口 ID。
            if (title[1].equals(window.getStripeTitle()) || title[0].equals(window.getStripeTitle()))
                window.setStripeTitle(title[2]);
            if (title[1].equals(window.getTitle()) || title[0].equals(window.getTitle()))
                window.setTitle(title[2]);
        }
    }
}
