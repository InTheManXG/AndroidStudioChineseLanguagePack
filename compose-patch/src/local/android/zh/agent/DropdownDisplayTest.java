package local.android.zh.agent;

import java.lang.classfile.*;
import java.lang.constant.*;
import java.lang.reflect.AccessFlag;

/** 验证真实选项对象的局部显示映射，以及配置参数和同名用户字符串不变。 */
public final class DropdownDisplayTest {
    public static void main(String[] args) throws Exception {
        byte[] bytes = ClassFile.of().build(ClassDesc.of("DropdownDisplayFixture"), c ->
            c.withMethodBody("display", MethodTypeDesc.of(ClassDesc.of("java.lang.String"), ClassDesc.of("java.lang.Object")),
                AccessFlag.PUBLIC.mask() | AccessFlag.STATIC.mask(), b -> {
                    b.aload(0);
                    ComposeTranslationAgent.translateDropdown(b, 0);
                    b.areturn();
                }));
        Class<?> fixture = new ClassLoader(DropdownDisplayTest.class.getClassLoader()) {
            Class<?> define() { return defineClass(null, bytes, 0, bytes.length); }
        }.define();
        var display = fixture.getMethod("display", Object.class);
        String[][] cases = {
            {"AvdCamera", "NONE", "无", "none"}, {"AvdCamera", "EMULATED", "模拟摄像头", "emulated"},
            {"AvdCamera", "VIRTUAL_SCENE", "虚拟场景", "virtualscene"}, {"AvdCamera", "WEBCAM", "主机摄像头 0", "webcam0"},
            {"AvdNetworkSpeed", "FULL", "全速", "full"}, {"AvdNetworkSpeed", "LTE", "LTE", "lte"},
            {"AvdNetworkLatency", "NONE", "无延迟", "none"}
        };
        for (String[] row : cases) {
            Class<?> type = Class.forName("com.android.sdklib.internal.avd." + row[0]);
            Object option = type.getField(row[1]).get(null);
            assert display.invoke(null, option).equals(row[2]) : row[0] + row[1];
            assert type.getMethod("getAsParameter").invoke(option).equals(row[3]);
            assert type.getMethod("fromName", String.class).invoke(null, option.toString()) == option;
        }
        Object skin = Class.forName("com.android.tools.idea.avdmanager.skincombobox.NoSkin").getField("INSTANCE").get(null);
        assert display.invoke(null, skin).equals("[无]");
        assert skin.toString().equals("[None]");
        for (String value : new String[] {"Full", "None", "Emulated", "[None]", "VirtualScene"}) {
            assert display.invoke(null, value).equals(value) : "误译用户字符串";
        }
        assert display.invoke(null, (Object)null).equals("null");
        System.out.println("通过：摄像头、网络、外观局部显示映射；原配置、解析和同名用户文字保持不变。");
    }
}
