package local.android.zh.agent;

import javax.swing.*;
import java.awt.*;
import com.android.tools.profilers.cpu.config.*;

/** 实际运行发行版渲染器，确认只改变显示且保留可编辑配置、API 限制和采样参数。 */
public final class TaskListDisplayTest {
    public static void main(String[] args) throws Exception {
        String family = "com.android.tools.idea.profilers.profilingconfig.CpuProfilingConfigurationsDialog";
        var agent = new ComposeTranslationAgent();
        ClassLoader loader = new ClassLoader(TaskListDisplayTest.class.getClassLoader()) {
            @Override protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
                if (!name.startsWith(family)) return super.loadClass(name, resolve);
                Class<?> type = findLoadedClass(name);
                if (type == null) try (var input = getParent().getResourceAsStream(name.replace('.', '/') + ".class")) {
                    byte[] original = input.readAllBytes();
                    byte[] patched = agent.transform(getParent(), name.replace('.', '/'), null, null, original);
                    byte[] bytes = patched == null ? original : patched;
                    type = defineClass(name, bytes, 0, bytes.length);
                } catch (Exception e) { throw new ClassNotFoundException(name, e); }
                if (resolve) resolveClass(type);
                return type;
            }
        };
        Class<?> parent = loader.loadClass(family + "$ProfilingConfigurable");
        // 测试不启动 IDE 服务；仅构造渲染器实际读取的模式及设备级别字段。
        var unsafeField = sun.misc.Unsafe.class.getDeclaredField("theUnsafe"); unsafeField.setAccessible(true);
        Object owner = ((sun.misc.Unsafe) unsafeField.get(null)).allocateInstance(parent);
        var mode = parent.getDeclaredField("isTaskBasedUxEnabled"); mode.setAccessible(true);
        var level = parent.getDeclaredField("myDeviceLevel"); level.setAccessible(true); level.setInt(owner, 1);
        Class<?> renderer = loader.loadClass(parent.getName() + "$ProfilingConfigurationsListCellRenderer");
        var constructor = renderer.getDeclaredConstructor(parent); constructor.setAccessible(true);
        var method = renderer.getDeclaredMethod("getListCellRendererComponent", JList.class, ProfilingConfiguration.class, int.class, boolean.class, boolean.class);
        method.setAccessible(true);
        String[][] pairs = {{"Callstack Sample", "调用栈采样"}, {"Java/Kotlin Method Trace", "Java/Kotlin 方法跟踪"}, {"Native Allocations", "原生内存分配"}, {"用户自定义配置", "用户自定义配置"}};
        for (boolean taskMode : new boolean[]{true, false}) {
            mode.setBoolean(owner, taskMode);
            for (String[] pair : pairs) {
                var config = new SimpleperfConfiguration(pair[0]); int interval = config.getProfilingSamplingIntervalUs();
                JPanel panel = (JPanel) method.invoke(constructor.newInstance(owner), new JList<>(), config, 0, false, false);
                String text = ((JLabel) panel.getComponent(0)).getText();
                assert text.equals((taskMode ? pair[1] : pair[0]) + "（API 级别 26+）") : text;
                assert config.getName().equals(pair[0]);
                assert config.getProfilingSamplingIntervalUs() == interval;
                assert config.getRequiredDeviceLevel() == 26 && !config.isDeviceLevelSupported(1);
            }
        }
        System.out.println("通过：真实任务列表渲染器，两种模式/四种名称，原配置名、采样间隔、API 限制保持不变。");
    }
}
