package local.android.zh.agent;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.text.MessageFormat;
import java.util.*;
import java.util.jar.JarFile;

/** 核验实际语言包的嵌套调试提示，以及按钮禁用原因的 printf 参数。 */
public final class DebugFeedbackTest {
    public static void main(String[] args) throws Exception {
        Properties bundle=new Properties();
        try (var jar=new JarFile(args[0]); var input=jar.getInputStream(jar.getJarEntry("messages/JavaDebuggerBundle.properties"))) {
            bundle.load(new InputStreamReader(input,StandardCharsets.UTF_8));
        }
        String connection=MessageFormat.format(bundle.getProperty("string.connection"),"localhost:62033","套接字");
        assert connection.equals("localhost:62033（传输方式：套接字）");
        assert MessageFormat.format(bundle.getProperty("status.connected"),connection).equals("已连接到目标虚拟机：localhost:62033（传输方式：套接字）");
        assert MessageFormat.format(bundle.getProperty("status.disconnected"),connection).equals("已断开与目标虚拟机的连接：localhost:62033（传输方式：套接字）");
        for (int count:new int[]{0,1,2,100}) {
            String result=MessageFormat.format(bundle.getProperty("status.classes.reloaded"),count);
            assert result.equals("已重新加载 "+count+" 个类"):result;
        }
        assert MessageFormat.format(bundle.getProperty("status.method.entry.breakpoint.reached"),"foo","Main","Main.kt",42)
            .equals("在 Main(Main.kt:42) 进入方法“foo”");
        Map<String,String> text=new HashMap<>();
        for (String line:Files.readAllLines(Path.of(args[1]))) {
            String[] f=line.split("\t",-1);
            if(!f[0].endsWith("/BaseAction"))continue;
            text.put(new String(Base64.getDecoder().decode(f[2]),StandardCharsets.UTF_8),new String(Base64.getDecoder().decode(f[3]),StandardCharsets.UTF_8));
        }
        assert text.size()==24:text.size();
        assert String.format(text.get("%s (disabled: %s)"),"应用代码更改",text.get("app not detected"))
            .equals("应用代码更改（不可用：未检测到应用）");
        assert String.format(text.get("%s is disabled for this device because %s."),"应用更改",text.get("the selected device is not authorized"))
            .equals("此设备无法使用“应用更改”，原因：所选设备尚未授权。");
        System.out.println("通过：实际 JAR 中连接/断开提示嵌套格式、类数量分支、方法位置、24 条禁用原因及 printf 参数。");
    }
}
