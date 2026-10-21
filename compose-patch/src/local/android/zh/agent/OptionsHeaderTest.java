package local.android.zh.agent;
import com.android.tools.profilers.cpu.config.*;
import com.android.tools.adtui.model.options.OptionsProvider;
import javax.swing.*;
import java.awt.*;
import java.util.*;

/** 实例化变换后的真实 OptionsPanel，检查任务标题和旧版可编辑名称的隔离。 */
public final class OptionsHeaderTest {
 static void collect(Component component, java.util.List<String> labels, java.util.List<String> inputs) {
  if(component instanceof JLabel label)labels.add(label.getText());
  if(component instanceof javax.swing.text.JTextComponent input)inputs.add(input.getText());
  if(component instanceof Container container)for(Component child:container.getComponents())collect(child,labels,inputs);
 }
 public static void main(String[] args)throws Exception {
  var agent=new ComposeTranslationAgent();String prefix="com.android.tools.adtui.ui.options.OptionsPanel";
  ClassLoader loader=new ClassLoader(OptionsHeaderTest.class.getClassLoader()) {
   @Override protected Class<?> loadClass(String name,boolean resolve)throws ClassNotFoundException {
    if(!name.startsWith("com.android.tools.adtui.ui.options."))return super.loadClass(name,resolve);
    Class<?> result=findLoadedClass(name);
    if(result==null)try(var stream=getParent().getResourceAsStream(name.replace('.','/')+".class")) {
     byte[] original=stream.readAllBytes(),patched=agent.transform(getParent(),name.replace('.','/'),null,null,original);
     byte[] data=patched==null?original:patched;result=defineClass(name,data,0,data.length);
    }catch(Exception e){throw new ClassNotFoundException(name,e);}
    if(resolve)resolveClass(result);return result;
   }
  };
  Class<?> type=loader.loadClass(prefix);var set=type.getMethod("setOption",OptionsProvider.class,boolean.class,boolean.class);
  SwingUtilities.invokeAndWait(()->{
   try {
    String[][] pairs={{"Callstack Sample","调用栈采样"},{"Java/Kotlin Method Trace","Java/Kotlin 方法跟踪"},{"Native Allocations","原生内存分配"},{"自定义任务","自定义任务"}};
    for(String[] pair:pairs)for(boolean task:new boolean[]{true,false}) {
     var config=new SimpleperfConfiguration(pair[0]);int interval=config.getProfilingSamplingIntervalUs();
     Component panel=(Component)type.getConstructor().newInstance();set.invoke(panel,config,false,task);
     var labels=new ArrayList<String>();var inputs=new ArrayList<String>();collect(panel,labels,inputs);
     if(task)assert labels.contains(pair[1]):labels;
     else assert inputs.contains(pair[0]):inputs;
     assert config.getName().equals(pair[0]) && config.getProfilingSamplingIntervalUs()==interval;
    }
   }catch(ReflectiveOperationException e){throw new RuntimeException(e);}
  });
  System.out.println("通过：四种名称/两种模式的真实详细面板标题，旧版名称输入框、模型及采样参数保持不变。");
 }
}
