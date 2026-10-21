package local.android.zh;

import java.io.*;
import java.lang.ref.Reference;
import java.nio.charset.StandardCharsets;
import java.util.*;

/** 在真实发行类上检查资源缓存结构、原键完整性和类型表头的上下文译文。 */
public final class MessageBundleTest {
    public static void main(String[] args) throws Exception {
        var install = LocalizationSupplement.class.getDeclaredMethod("loadMessageBundles");
        install.setAccessible(true);
        install.invoke(null);
        Properties targets = LocalizationSupplement.load("message-bundle-targets.properties");
        int keys = 0;
        for (String name : targets.stringPropertyNames()) {
            Class<?> owner = Class.forName(name);
            var field = owner.getDeclaredField("bundleRef"); field.setAccessible(true);
            Object holder = field.get(null);
            var cache = holder.getClass().getDeclaredField("bundleRef"); cache.setAccessible(true);
            ResourceBundle actual = (ResourceBundle)((Reference<?>)cache.get(holder)).get();
            assert actual != null : name;
            String path = targets.getProperty(name);
            try (var original = owner.getClassLoader().getResourceAsStream(path);
                 var translated = LocalizationSupplement.class.getResourceAsStream("/legacy/" + path)) {
                assert original != null && translated != null : path;
                var source = new PropertyResourceBundle(new InputStreamReader(original, StandardCharsets.UTF_8));
                var expected = new PropertyResourceBundle(new InputStreamReader(translated, StandardCharsets.UTF_8));
                // 缓存替换必须包含所有原键，否则罕见界面可能抛 MissingResourceException。
                assert source.keySet().equals(actual.keySet()) : path;
                for (String key : source.keySet()) assert actual.getString(key).equals(expected.getString(key)) : key;
                keys += source.keySet().size();
            }
            if (path.equals("messages/LogcatBundle.properties")) verifyLogcatHtml();
            if (path.equals("messages/DeviceManagerBundle.properties")) {
                assert actual.getString("column.title.name").equals("名称");
                assert actual.getString("column.title.handletype").equals("类型");
            }
        }
        System.out.println("通过：" + targets.size() + " 个真实资源缓存，" + keys + " 个键完整且译文一致。");
    }
    private static void verifyLogcatHtml() throws Exception {
        // 必须经过真实Bundle.message：它会处理properties中的转义，不能直接删反斜线。
        String appId="com.example.application";
        String items=com.android.tools.idea.logcat.LogcatBundle.message("logcat.filter.completion.hint.package.mine.items",appId);
        String delete=com.android.tools.idea.logcat.LogcatBundle.message("logcat.device.combo.delete.tooltip","aabbcc");
        assert items.endsWith("</html>") && items.contains("&nbsp;&nbsp;"+appId);
        assert delete.contains("color:#aabbcc") && delete.contains("&nbsp;");
        for(String html:new String[]{items,delete}) {
            var kit=new javax.swing.text.html.HTMLEditorKit();
            var doc=kit.createDefaultDocument();kit.read(new StringReader(html),doc,0);
            String text=doc.getText(0,doc.getLength());
            assert !text.contains("\\") && !text.contains("&nbsp;"):text;
            assert text.contains("\u00a0"):"不换行空格丢失";
            assert html.equals(items)?text.contains(appId)&&text.contains("当前项目应用"):text.contains("删除条目")&&text.contains("删除");
        }
        System.out.println("通过：Logcat真实资源方法及Swing HTML解析，闭合标签、空格、颜色和应用ID正确。");
    }

}
