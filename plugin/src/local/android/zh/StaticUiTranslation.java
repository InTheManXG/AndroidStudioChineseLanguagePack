package local.android.zh;

import java.awt.*;
import java.awt.event.*;
import java.util.*;
import javax.swing.*;
import com.intellij.openapi.diagnostic.Logger;

/** 只翻译 Android 窗口中的静态显示控件；不触碰可编辑文本或表格、列表数据。 */
final class StaticUiTranslation {
    private static final Properties TEXT = LocalizationSupplement.load("static-ui-translations.properties");
    private static final Properties ANCHORS = LocalizationSupplement.load("android-ui-anchors.properties");
    private static final Set<Window> PENDING = Collections.newSetFromMap(new WeakHashMap<>());
    private static int translatedCount;
    private static final Set<String> WINDOW_TITLES = Set.of("Device Manager", "New Project", "Create Virtual Device", "SDK Component Installer");

    static void install() {
        Toolkit.getDefaultToolkit().addAWTEventListener(event -> {
            if (event instanceof WindowEvent windowEvent) {
                if (windowEvent.getID() == WindowEvent.WINDOW_OPENED || windowEvent.getID() == WindowEvent.WINDOW_ACTIVATED) schedule(windowEvent.getWindow());
            } else if (event instanceof ContainerEvent containerEvent && containerEvent.getID() == ContainerEvent.COMPONENT_ADDED) {
                schedule(SwingUtilities.getWindowAncestor(containerEvent.getContainer()));
            }
        }, AWTEvent.WINDOW_EVENT_MASK | AWTEvent.CONTAINER_EVENT_MASK);
        for (Window window : Window.getWindows()) schedule(window);
    }

    private static void schedule(Window window) {
        if (window == null || !PENDING.add(window)) return;
        // 合并同一窗口连续添加组件的事件，避免每个标签都遍历一次整棵控件树。
        SwingUtilities.invokeLater(() -> {
            PENDING.remove(window);
            if (!window.isDisplayable() || !containsAndroidUi(window)) return;
            int before = translatedCount;
            translate(window);
            if (translatedCount > before) {
                window.validate();
                window.repaint();
                Logger.getInstance(StaticUiTranslation.class).info("静态界面新增补译：" + (translatedCount-before));
            }
        });
    }

    private static boolean containsAndroidUi(Component component) {
        // 独立设备管理器使用通用 JFrame，不能只根据组件包名识别。
        if (component instanceof Frame frame && WINDOW_TITLES.contains(frame.getTitle())) return true;
        String name = component.getClass().getName();
        // 部分 Android 表单仅返回普通 JPanel；用源码确认过的长标签识别，不依据用户输入。
        if (component instanceof JLabel label && label.getText()!=null && ANCHORS.containsKey(label.getText())) return true;
        if (name.startsWith("com.android.") || name.startsWith("org.jetbrains.android.") || name.startsWith("com.google.gct.") || name.startsWith("com.google.aiplugin.")) return true;
        if (component instanceof Container container)
            for (Component child : container.getComponents()) if (containsAndroidUi(child)) return true;
        return false;
    }

    private static String translated(String original) {
        return original == null ? null : TEXT.getProperty(original, original);
    }

    static void translate(Component component) {
        // 渲染器会承载用户文件名、数据库值等数据，不能按静态文案处理。
        if (component instanceof CellRendererPane || component instanceof javax.swing.text.JTextComponent) return;
        if (component instanceof JComboBox<?> combo) translateCarPlatform(combo);
        if (component instanceof Frame frame && WINDOW_TITLES.contains(frame.getTitle())) {
            frame.setTitle(translated(frame.getTitle()));
        }
        if (component instanceof Dialog dialog && WINDOW_TITLES.contains(dialog.getTitle())) {
            dialog.setTitle(translated(dialog.getTitle()));
        }
        if (component instanceof com.intellij.ui.HyperlinkLabel link) {
            String old=link.getText(), value=translated(old);
            if (!Objects.equals(old,value)) { link.setHyperlinkText(value); translatedCount++; }
        } else if (component instanceof com.intellij.openapi.wm.impl.SquareStripeButton stripe) {
            // 新工具栏缓存独立的无障碍标题；按真实窗口 ID 同步显示，不能改窗口 ID。
            ProjectUiTranslation.translate(stripe.getToolWindow());
            for (String[] title : ProjectUiTranslation.TITLES) {
                if (title[0].equals(stripe.getToolWindow().getId())
                    && !title[2].equals(stripe.getPresentation().getText())) {
                    stripe.getPresentation().setText(title[2]);
                    translatedCount++;
                }
            }
        } else if (component instanceof com.intellij.openapi.actionSystem.impl.ActionButton actionButton) {
            var presentation=actionButton.getPresentation();
            String old=presentation.getText(), value=translated(old);
            // 附加调试器按钮在语言资源加载前缓存标题，只同步此已审核动作的固定原文。
            if ("org.jetbrains.android.actions.AndroidConnectDebuggerAction".equals(actionButton.getAction().getClass().getName())
                    && "Attach Debugger to Android Process".equals(old))
                value=actionButton.getAction().getTemplatePresentation().getText();
            if (!Objects.equals(old,value)) {
                presentation.setText(value);
                actionButton.getAction().getTemplatePresentation().setText(value);
                translatedCount++;
            }
        } else if (component instanceof JLabel label) {
            String old = label.getText(), value = translated(old);
            if (!Objects.equals(old, value)) { label.setText(value); translatedCount++; }
        } else if (component instanceof AbstractButton button) {
            String old = button.getText(), value = translated(old);
            if (!Objects.equals(old, value)) { button.setText(value); translatedCount++; }
        } else if (component instanceof JTabbedPane tabs) {
            for (int i=0; i<tabs.getTabCount(); i++) {
                String old=tabs.getTitleAt(i), value=translated(old);
                if (!Objects.equals(old,value)) { tabs.setTitleAt(i,value); translatedCount++; }
            }
        }
        if (component instanceof JTable table) {
            var columns=table.getColumnModel();
            Set<String> headers=new HashSet<>();
            for (int i=0;i<columns.getColumnCount();i++) headers.add(String.valueOf(columns.getColumn(i).getHeaderValue()));
            // 后台任务表格还须匹配专用排序器；同名用户表格不属于补译范围。
            boolean background = headers.equals(Set.of("Class", "Status", "Start", "Retries"))
                && columns.getColumnCount()==4 && table.getRowSorter()!=null
                && table.getRowSorter().getClass().getName().equals("com.android.tools.adtui.common.ColumnTreeBuilder$ColumnTreeTableRowSorter");
            // APK 普通视图在当前版本含压缩与对齐列；仍按完整列组识别，避免误改用户表格。
            boolean apk = table.getRowSorter()!=null
                && table.getRowSorter().getClass().getName().equals("com.android.tools.adtui.common.ColumnTreeBuilder$ColumnTreeTableRowSorter")
                && columns.getColumnCount()==headers.size()
                && (headers.equals(Set.of("File","Size","Download Size","% of Download Size","Compressed","Alignment"))
                    || headers.equals(Set.of("File","Size","Download Size","% of Download Size"))
                    || headers.equals(Set.of("File","Old Size","New Size","Diff Size"))
                    || headers.equals(Set.of("File","Old Size","New Size","Patch Size")));
            if (background || apk || (headers.contains("API Level") && headers.contains("Revision") && headers.contains("Status"))) {
                Map<String,String> apkHeaders=Map.of("File","文件","Size","大小","Download Size","下载大小",
                    "% of Download Size","下载大小占比","Old Size","旧大小","New Size","新大小","Diff Size","大小差异","Patch Size","补丁大小","Compressed","已压缩","Alignment","对齐方式");
                Map<String,String> backgroundHeaders=Map.of("Class","类","Status","状态","Start","开始时间","Retries","重试次数");
                for (int i=0;i<columns.getColumnCount();i++) {
                    var column=columns.getColumn(i);
                    String old=String.valueOf(column.getHeaderValue()), value=background?backgroundHeaders.get(old):apk?apkHeaders.get(old):translated(old);
                    if (!Objects.equals(old,value)) {
                        // 保留原列标识，避免依赖 getColumn(原名称) 的逻辑失效。
                        column.setIdentifier(column.getIdentifier());
                        column.setHeaderValue(value);
                        translatedCount++;
                    }
                }
                if (table.getTableHeader()!=null) table.getTableHeader().repaint();
            }
        }
        if (component instanceof JComponent swing) {
            // 动态 tooltip getter 可能启动模态任务；只读 Swing 已设置的静态属性。
            Object tooltip=swing.getClientProperty(JComponent.TOOL_TIP_TEXT_KEY);
            if (tooltip instanceof String old) {
                String value=translated(old);
                if (!Objects.equals(old,value)) swing.setToolTipText(value);
            }
        }
        if (component instanceof Container container)
            for (Component child : container.getComponents()) translate(child);
    }

    private static <T> void translateCarPlatform(JComboBox<T> combo) {
        String first="Android Automotive OS only", second="Android Automotive OS and Android Auto";
        if (combo.getItemCount()!=2 || !first.equals(combo.getItemAt(0)) || !second.equals(combo.getItemAt(1))
            || Boolean.TRUE.equals(combo.getClientProperty("local.android.zh.carRenderer"))) return;
        var renderer=combo.getRenderer();
        if (renderer==null) return;
        // 原选项字符串参与模板条件判断，仅转换渲染结果，不替换模型或选中项。
        combo.setRenderer((list,value,index,selected,focused) -> {
            Component display=renderer.getListCellRendererComponent(list,value,index,selected,focused);
            if (display instanceof JLabel label) {
                if (first.equals(value)) label.setText("仅 Android Automotive OS");
                else if (second.equals(value)) label.setText("Android Automotive OS 与 Android Auto");
            }
            return display;
        });
        combo.putClientProperty("local.android.zh.carRenderer",true);
    }
}
