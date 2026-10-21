package local.android.zh.agent;

import java.util.*;

/** 运行真实列枚举，确保只改表头显示，枚举身份和列元数据不变。 */
public final class NetworkColumnDisplayTest {
    public static void main(String[] args) throws Exception {
        String prefix="com.android.tools.idea.appinspection.inspectors.network.view.connectionsview.ConnectionColumn";
        var agent=new ComposeTranslationAgent();
        ClassLoader loader=new ClassLoader(NetworkColumnDisplayTest.class.getClassLoader()) {
            @Override protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
                if (!name.startsWith(prefix)) return super.loadClass(name,resolve);
                Class<?> type=findLoadedClass(name);
                if(type==null) try(var stream=getParent().getResourceAsStream(name.replace('.','/')+".class")) {
                    byte[] original=stream.readAllBytes();
                    byte[] patched=agent.transform(getParent(),name.replace('.','/'),null,null,original);
                    byte[] data=patched==null?original:patched;
                    type=defineClass(name,data,0,data.length);
                } catch(Exception e) { throw new ClassNotFoundException(name,e); }
                if(resolve)resolveClass(type);
                return type;
            }
        };
        Class<?> original=Class.forName(prefix), patched=loader.loadClass(prefix);
        Object[] before=original.getEnumConstants(), after=patched.getEnumConstants();
        String[] expected={"URL","名称","路径","主机","请求方法","协议方案","传输协议","请求大小","大小","请求标头","响应标头","类型","状态","时间","请求时间","响应时间","时间线"};
        assert before.length==expected.length && after.length==before.length;
        for(int i=0;i<before.length;i++) {
            Enum<?> a=(Enum<?>)before[i],b=(Enum<?>)after[i];
            assert a.name().equals(b.name()) && a.ordinal()==b.ordinal();
            assert patched.getMethod("valueOf",String.class).invoke(null,a.name())==b;
            assert patched.getMethod("getDisplayString").invoke(b).equals(expected[i]);
            for(String getter:List.of("getWidthRatio","getType","getVisible"))
                assert Objects.equals(original.getMethod(getter).invoke(a),patched.getMethod(getter).invoke(b)):getter;
        }
        System.out.println("通过：17个真实网络列的中文显示、枚举名/顺序/valueOf、宽度、类型及可见性保持。");
    }
}
