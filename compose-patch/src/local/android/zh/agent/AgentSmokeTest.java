package local.android.zh.agent;

/** 在独立 JVM 中验证 premain 加载与真实枚举显示文字，同时保留枚举持久化名称。 */
public final class AgentSmokeTest {
    public static void main(String[] args) throws Exception {
        Class<?> type = Class.forName("com.android.tools.idea.avd.Tab");
        var field=type.getDeclaredField("ADDITIONAL_SETTINGS");field.setAccessible(true);
        Object value=field.get(null);
        var method=type.getDeclaredMethod("getText");method.setAccessible(true);
        assert ((Enum<?>)value).name().equals("ADDITIONAL_SETTINGS") : "误改枚举标识";
        assert method.invoke(value).equals(args[0]) : method.invoke(value);
        boolean chinese=args[0].equals("其他设置");
        Class<?> boot=Class.forName("com.android.tools.idea.avd.Boot");
        Object quick=boot.getField("QUICK").get(null);
        assert quick.toString().equals(chinese?"快速启动":"Quick");
        var settings=(java.util.Map<?,?>)boot.getMethod("getProperties").invoke(quick);
        assert settings.equals(java.util.Map.of("fastboot.chosenSnapshotFile","","fastboot.forceChosenSnapshotBoot","no","fastboot.forceColdBoot","no","fastboot.forceFastBoot","yes")):settings;
        Class<?> graphics=Class.forName("com.android.tools.idea.avd.GraphicsMode");
        Object automatic=graphics.getField("AUTO").get(null);
        assert ((Enum<?>)automatic).name().equals("AUTO");
        assert automatic.toString().equals(chinese?"自动":"Automatic");
        // 不改共享方向枚举，避免影响配置解析和其他功能按英文显示名查找。
        Class<?> orientation=Class.forName("com.android.resources.ScreenOrientation");
        Object portrait=orientation.getField("PORTRAIT").get(null);
        assert orientation.getMethod("getResourceValue").invoke(portrait).equals("port");
        assert orientation.getMethod("getShortDisplayValue").invoke(portrait).equals("Portrait");
        for (String suffix : new String[]{"NewProject", "NewModule"}) {
            Class<?> build=Class.forName("com.android.tools.idea.wizard.template.BuildConfigurationLanguageFor"+suffix);
            Object kts=build.getField("KTS").get(null);
            assert ((Enum<?>)kts).name().equals("KTS");
            assert kts.toString().equals(chinese?"Kotlin DSL (build.gradle.kts) [推荐]":"Kotlin DSL (build.gradle.kts) [Recommended]");
            if (suffix.equals("NewProject")) {
                assert build.getMethod("getUseKts").invoke(kts).equals(true);
                assert build.getMethod("getUseKts").invoke(build.getField("DCL").get(null)).equals(false);
            }
        }
        System.out.println("通过：启动、图形配置与共享方向枚举保持原值。");
        System.out.println("通过：agent 实际加载，显示文字="+method.invoke(value)+"，枚举标识保持原值。");
    }
}
