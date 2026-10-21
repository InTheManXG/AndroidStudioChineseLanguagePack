package local.android.zh.agent;

import java.lang.reflect.*;
import java.util.*;
import javax.swing.*;
import com.intellij.ui.SimpleColoredComponent;
import androidx.work.inspection.WorkManagerInspectorProtocol.WorkInfo;

/** 用真实四类任务及全部枚举状态检查显示转换，保护模型状态和图标。 */
public final class BackgroundStateDisplayTest {
 public static void main(String[] args)throws Exception {
  String base="com.android.tools.idea.appinspection.inspectors.backgroundtask.";
  var agent=new ComposeTranslationAgent();
  ClassLoader loader=new ClassLoader(BackgroundStateDisplayTest.class.getClassLoader()) {
   @Override protected Class<?> loadClass(String name,boolean resolve)throws ClassNotFoundException {
    if(!name.startsWith(base+"view.table.") && !name.equals(base+"view.StateProvider"))return super.loadClass(name,resolve);
    Class<?> type=findLoadedClass(name);
    if(type==null)try(var in=getParent().getResourceAsStream(name.replace('.','/')+".class")) {
     byte[] raw=in.readAllBytes(),patched=agent.transform(getParent(),name.replace('.','/'),null,null,raw);
     byte[] data=patched==null?raw:patched;type=defineClass(name,data,0,data.length);
    }catch(Exception e){throw new ClassNotFoundException(name,e);}
    if(resolve)resolveClass(type);return type;
   }
  };
  var entryType=Class.forName(base+"model.entries.BackgroundTaskEntry");
  var provider=loader.loadClass(base+"view.StateProvider");
  var convert=provider.getMethod("convert",entryType);Object instance=provider.getField("INSTANCE").get(null);
  var original=Class.forName(base+"view.StateProvider");
  var originalConvert=original.getMethod("convert",entryType);Object originalInstance=original.getField("INSTANCE").get(null);
  var table=loader.loadClass(base+"view.table.BackgroundTaskTreeTableView");
  var render=table.getDeclaredMethod("_init_$lambda$4",SimpleColoredComponent.class,Object.class,boolean.class);render.setAccessible(true);
  Map<String,String> expected=Map.ofEntries(Map.entry("UNSPECIFIED","未指定"),Map.entry("ENQUEUED","已入队"),Map.entry("RUNNING","运行中"),Map.entry("SUCCEEDED","已成功"),Map.entry("FAILED","已失败"),Map.entry("BLOCKED","等待前置任务"),Map.entry("CANCELLED","已取消"),Map.entry("UNRECOGNIZED","无法识别"),Map.entry("SCHEDULED","已调度"),Map.entry("STARTED","已开始"),Map.entry("STOPPED","已停止"),Map.entry("FINISHED","已完成"),Map.entry("SET","已设置"),Map.entry("FIRED","已触发"),Map.entry("ACQUIRED","已获取"),Map.entry("RELEASED","已释放"));
  int[] count={0};
  SwingUtilities.invokeAndWait(()->{
   try {
    verifyCategories(loader,table);
    for(String kind:List.of("WorkEntry","JobEntry","AlarmEntry","WakeLockEntry")) {
     Class<?> type=Class.forName(base+"model.entries."+kind);Object entry=type.getConstructor(String.class).newInstance("user-id");
     var field=type.getDeclaredField(kind.equals("WorkEntry")?"work":"_status");field.setAccessible(true);
     Object[] states=kind.equals("WorkEntry")?WorkInfo.State.values():field.getType().getEnumConstants();
     for(Object state:states) {
      String raw=((Enum<?>)state).name();
      if(kind.equals("WorkEntry"))((WorkInfo.Builder)field.get(entry)).setStateValue(state==WorkInfo.State.UNRECOGNIZED?-1:((WorkInfo.State)state).getNumber());
      else field.set(entry,state);
      JLabel before=(JLabel)originalConvert.invoke(originalInstance,entry);
      JLabel after=(JLabel)convert.invoke(instance,entry);
      assert after.getText().equals(expected.get(raw)):kind+":"+raw+":"+after.getText();
      // 运行中图标每次调用都会新建动画实例；比较其类型和尺寸，其余图标比较对象。
      if(before.getIcon() instanceof com.intellij.ui.AnimatedIcon.Default) {
       assert after.getIcon() instanceof com.intellij.ui.AnimatedIcon.Default;
       assert after.getIcon().getIconWidth()==before.getIcon().getIconWidth();
       assert after.getIcon().getIconHeight()==before.getIcon().getIconHeight();
      } else assert java.util.Objects.equals(after.getIcon(),before.getIcon()):"图标变化 "+kind+":"+raw;
      SimpleColoredComponent cell=new SimpleColoredComponent();render.invoke(null,cell,entry,false);
      assert cell.getCharSequence(true).toString().equals(expected.get(raw)):cell.getCharSequence(true);
      assert type.getMethod("getStatus").invoke(entry).equals(raw):"模型状态变化";
      assert type.getMethod("getId").invoke(entry).equals("user-id");count[0]++;
     }
    }
   }catch(ReflectiveOperationException e){throw new RuntimeException(e);}
  });
  System.out.println("通过：四类真实任务的 "+count[0]+" 个状态在详情及表格中显示中文，原始状态、ID和图标保持。");
 }

 private static void verifyCategories(ClassLoader loader,Class<?> table)throws ReflectiveOperationException {
  String base="com.android.tools.idea.appinspection.inspectors.backgroundtask.";
  var header=loader.loadClass(base+"view.table.BackgroundTaskTreeTableView$HeaderRenderer");
  var ctor=header.getDeclaredConstructor();ctor.setAccessible(true);
  var renderer=(javax.swing.tree.TreeCellRenderer)ctor.newInstance();
  var firstColumn=table.getDeclaredMethod("_init_$lambda$3",SimpleColoredComponent.class,Object.class,boolean.class);firstColumn.setAccessible(true);
  String[][] pairs={{"Workers","工作任务","No workers have been detected.","尚未检测到工作任务。"},
   {"Jobs","调度任务","No jobs have been detected.","尚未检测到调度任务。"},
   {"Alarms","定时任务","No alarms have been detected.","尚未检测到定时任务。"},
   {"WakeLocks","唤醒锁","No wake locks have been detected.","尚未检测到唤醒锁。"},
   {"CustomGroup","CustomGroup","Custom text","Custom text"}};
  var workType=Class.forName(base+"model.entries.WorkEntry");
  var field=workType.getDeclaredField("work");field.setAccessible(true);
  for(String[] pair:pairs) {
   var node=new javax.swing.tree.DefaultMutableTreeNode(pair[0]);
   var label=(JLabel)renderer.getTreeCellRendererComponent(new JTree(),node,false,false,false,0,false);
   assert label.getText().equals(pair[1]) && node.getUserObject().equals(pair[0]);
   var cell=new SimpleColoredComponent();firstColumn.invoke(null,cell,pair[2],false);
   assert cell.getCharSequence(true).toString().equals(pair[3]);
   // 即使任务类名恰好等于空态原文，真实任务分支也不得翻译它。
   Object work=workType.getConstructor(String.class).newInstance("user-id");
   ((WorkInfo.Builder)field.get(work)).setWorkerClassName(pair[2]);
   cell=new SimpleColoredComponent();firstColumn.invoke(null,cell,work,false);
   assert cell.getCharSequence(true).toString().equals(pair[2]):"误改用户类名";
  }
  System.out.println("通过：真实分组/空态渲染器，节点原值、同名任务类名及未知文字保持。");
 }
}
