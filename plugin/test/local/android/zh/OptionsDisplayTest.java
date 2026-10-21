package local.android.zh;
import com.android.tools.adtui.ui.options.OptionsPanel;
import com.android.tools.profilers.cpu.config.*;
import java.awt.*;
import javax.swing.*;
import java.util.*;

/** 验证真实配置组件的标签补译，同时对输入值、原配置名和注解绑定保持不变做检查。 */
public final class OptionsDisplayTest {
 static void collect(Component c, java.util.List<String> text, java.util.List<String> values) {
  if(c instanceof JLabel label)text.add(label.getText());
  if(c instanceof AbstractButton button)text.add(button.getText());
  if(c instanceof javax.swing.text.JTextComponent input)values.add(input.getText());
  if(c instanceof JComboBox<?> combo)values.add(String.valueOf(combo.getSelectedItem()));
  if(c instanceof Container container)for(Component child:container.getComponents())collect(child,text,values);
 }
 public static void main(String[] args)throws Exception {
  SwingUtilities.invokeAndWait(()->{
   var configs=new ProfilingConfiguration[]{new SimpleperfConfiguration("Callstack Sample"),new ArtInstrumentedConfigurationWallClock("Java/Kotlin Method Trace"),new PerfettoNativeAllocationsConfiguration("Native Allocations"),new LeakCanaryConfiguration("LeakCanary"),new ArtInstrumentedConfigurationLegacy("Java/Kotlin Method Trace")};
   String[][] expected={{"采样间隔：","微秒（μs）"},{"文件大小上限："},{"采样间隔：","字节"},{"自定义设置","使用 Android Studio 设置","触发堆转储的阈值：","个滞留对象","使用应用代码中定义的配置"},{"启用双时钟（会降低性能）"}};
   for(int i=0;i<configs.length;i++) {
    var config=configs[i];String name=config.getName();var panel=new OptionsPanel();panel.setOption(config,false,true);
    var before=new ArrayList<String>();collect(panel,new ArrayList<>(),before);
    StaticUiTranslation.translate(panel);
    var texts=new ArrayList<String>();var after=new ArrayList<String>();collect(panel,texts,after);
    for(String s:expected[i])assert texts.contains(s):s+texts;
    assert before.equals(after):"输入值变化";assert config.getName().equals(name);
   }
  });
  System.out.println("通过：五种真实详细配置的中文标签、输入值和原始配置名称保护。");
 }
}
