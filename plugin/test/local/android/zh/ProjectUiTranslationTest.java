package local.android.zh;

import com.intellij.openapi.wm.ToolWindow;
import java.lang.reflect.Proxy;

/** 限制可调用 API，确保补译不触碰窗口身份、用户内容或操作状态。 */
public final class ProjectUiTranslationTest {
    public static void main(String[] args) {
        for (String[] item : ProjectUiTranslation.TITLES) {
            for (boolean dynamic : new boolean[]{false, true}) {
                String[] values = {item[1], dynamic ? "用户内容标题" : item[1], item[1]};
                ToolWindow window = (ToolWindow) Proxy.newProxyInstance(ToolWindow.class.getClassLoader(), new Class[]{ToolWindow.class}, (proxy, method, arguments) -> {
                    return switch (method.getName()) {
                        case "isDisposed" -> false;
                        case "getId" -> item[0];
                        case "getStripeTitle" -> values[0];
                        case "getTitle" -> values[1];
                        case "getStripeShortTitleProvider" -> (java.util.function.Supplier<String>) () -> values[2];
                        case "setStripeShortTitleProvider" -> { values[2]=(String)((java.util.function.Supplier<?>)arguments[0]).get(); yield null; }
                        case "setStripeTitle" -> { values[0]=(String)arguments[0]; yield null; }
                        case "setTitle" -> { values[1]=(String)arguments[0]; yield null; }
                        default -> throw new AssertionError("不允许修改窗口行为："+method);
                    };
                });
                ProjectUiTranslation.translate(window);
                ProjectUiTranslation.translate(window);
                assert values[0].equals(item[2]);
                assert values[2].equals(item[2]);
                assert values[1].equals(dynamic ? "用户内容标题" : item[2]);
                assert window.getId().equals(item[0]);
            }
        }
        ProjectUiTranslation.translate(null);
        System.out.println("通过："+ProjectUiTranslation.TITLES.length+" 个工具窗口标题、动态内容保护、重复处理保持、内部 ID 不变。");
    }
}
