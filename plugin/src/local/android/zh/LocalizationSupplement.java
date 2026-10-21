package local.android.zh;

import com.intellij.ide.AppLifecycleListener;
import com.intellij.l10n.LocalizationStateService;
import com.intellij.openapi.actionSystem.ActionManager;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import java.io.InputStreamReader;
import java.lang.ref.SoftReference;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.util.*;

/** 为当前 262 平台补充语言资源加载与 Android 专属动作的译文。 */
public final class LocalizationSupplement implements AppLifecycleListener {
    private static final Logger LOG = Logger.getInstance(LocalizationSupplement.class);
    private static final List<ResourceBundle> LEGACY_BUNDLES = new ArrayList<>();

    @Override
    public void appStarted() {
        ApplicationManager.getApplication().invokeLater(() -> {
            String locale = LocalizationStateService.getInstance().getSelectedLocale();
            if (!"zh-CN".equals(locale) && !"zh_CN".equals(locale)) return;
            loadLegacyBundles();
            loadMessageBundles();
            translateActions();
            SettingsTranslation.install();
            StaticUiTranslation.install();
        });
    }

    static Properties load(String name) {
        Properties result = new Properties();
        try (var stream = LocalizationSupplement.class.getResourceAsStream("/" + name)) {
            if (stream == null) throw new IllegalStateException("缺少资源：" + name);
            result.load(new InputStreamReader(stream, StandardCharsets.UTF_8));
        } catch (Exception e) {
            // 补译失败时保留原界面，不能阻断 IDE 或隐藏功能。
            LOG.warn("无法读取补译资源：" + name, e);
        }
        return result;
    }

    private static void loadLegacyBundles() {
        String[][] targets = {
            {"org.jetbrains.android.util.AndroidBundle", "ourBundle", "messages/AndroidBundle.properties"},
            {"com.google.gct.login2.GoogleLoginBundle", "bundleRef", "message/GoogleLoginBundle.properties"},
            {"com.android.tools.idea.gradle.project.upgrade.AgpUpgradeBundle", "ourBundle", "messages/AgpUpgradeBundle.properties"},
            {"com.android.tools.rendering.RenderingBundle", "ourBundle", "messages/RenderingBundle.properties"},
            {"com.google.gct.studio.samples.SamplesBrowserBundle", "ourBundle", "messages/SamplesBrowserBundle.properties"}
        };
        int applied = 0;
        for (String[] target : targets) {
            try (var stream = LocalizationSupplement.class.getResourceAsStream("/legacy/" + target[2])) {
                if (stream == null) throw new IllegalStateException("缺少旧式资源：" + target[2]);
                ResourceBundle bundle = new PropertyResourceBundle(new InputStreamReader(stream, StandardCharsets.UTF_8));
                Class<?> owner = Class.forName(target[0], true, LocalizationSupplement.class.getClassLoader());
                Field cache = owner.getDeclaredField(target[1]);
                if (!java.lang.ref.Reference.class.isAssignableFrom(cache.getType())) throw new IllegalStateException("缓存结构已变化");
                cache.setAccessible(true);
                // 旧 Android 包绕过 IntelliJ 的语言包查找；仅替换其可写资源缓存，不修改类或方法。
                cache.set(null, new SoftReference<>(bundle));
                // 持有强引用，避免软引用回收后又加载原英文资源。
                LEGACY_BUNDLES.add(bundle);
                applied++;
            } catch (ReflectiveOperationException | java.io.IOException | RuntimeException e) {
                LOG.warn("旧式资源补译未生效：" + target[0], e);
            }
        }
        LOG.info("旧式中文资源已加载：" + applied + "/" + targets.length);
    }

    private static void translateActions() {
        Properties translations = load("action-translations.properties");
        int applied = 0;
        for (String key : translations.stringPropertyNames()) {
            int split = key.lastIndexOf('.');
            AnAction action = ActionManager.getInstance().getAction(key.substring(0, split));
            if (action == null) continue; // 可选插件的动作未注册时跳过，保留正常启动。
            if (key.endsWith(".text")) action.getTemplatePresentation().setText(translations.getProperty(key));
            else action.getTemplatePresentation().setDescription(translations.getProperty(key));
            applied++;
        }
        LOG.info("Android 动作补译已应用：" + applied);
    }

    private static void loadMessageBundles() {
        Properties targets = load("message-bundle-targets.properties");
        int applied = 0;
        for (String name : targets.stringPropertyNames()) {
            String resource = targets.getProperty(name);
            try (var stream = LocalizationSupplement.class.getResourceAsStream("/legacy/" + resource)) {
                if (stream == null) throw new IllegalStateException("缺少资源：" + resource);
                ResourceBundle bundle = new PropertyResourceBundle(new InputStreamReader(stream, StandardCharsets.UTF_8));
                Class<?> owner = Class.forName(name, true, LocalizationSupplement.class.getClassLoader());
                Field holderField = owner.getDeclaredField("bundleRef");
                holderField.setAccessible(true);
                Object holder = holderField.get(null);
                if (!holder.getClass().getName().equals("com.android.tools.idea.localization.MessageBundleReference"))
                    throw new IllegalStateException("资源持有者结构已变化");
                Field cache = holder.getClass().getDeclaredField("bundleRef");
                if (cache.getType() != SoftReference.class) throw new IllegalStateException("资源缓存结构已变化");
                cache.setAccessible(true);
                // 此加载器绕过语言包；只替换完整资源缓存，不改资源键或消息格式化调用。
                cache.set(holder, new SoftReference<>(bundle));
                LEGACY_BUNDLES.add(bundle);
                applied++;
            } catch (ReflectiveOperationException | java.io.IOException | RuntimeException e) {
                LOG.warn("Android 资源补译未生效：" + name, e);
            }
        }
        LOG.info("Android 专属中文资源已加载：" + applied + "/" + targets.size());
    }
}
