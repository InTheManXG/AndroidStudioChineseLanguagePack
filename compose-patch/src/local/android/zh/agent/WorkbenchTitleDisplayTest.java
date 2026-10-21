package local.android.zh.agent;

import java.util.Objects;
import com.android.tools.adtui.workbench.ToolWindowDefinition;

/** 检查真实Workbench定义的显示标题与持久化身份分离。 */
public final class WorkbenchTitleDisplayTest {
 public static void main(String[] args)throws Exception {
  verifyIconHeaders();
  String name="com.android.tools.idea.layoutinspector.tree.LayoutInspectorTreePanelDefinition";
  byte[] raw;
  try(var in=WorkbenchTitleDisplayTest.class.getClassLoader().getResourceAsStream(name.replace('.','/')+".class")){raw=in.readAllBytes();}
  byte[] patched=new ComposeTranslationAgent().transform(WorkbenchTitleDisplayTest.class.getClassLoader(),name.replace('.','/'),null,null,raw);
  assert patched!=null;
  Class<?> type=new ClassLoader(WorkbenchTitleDisplayTest.class.getClassLoader()) {
   Class<?> define(){return defineClass(name,patched,0,patched.length);}
  }.define();
  var before=(ToolWindowDefinition<?>)Class.forName(name).getConstructor().newInstance();
  var after=(ToolWindowDefinition<?>)type.getConstructor().newInstance();
  assert before.getTitle().equals("Component Tree") && after.getTitle().equals("组件树");
  assert after.getName().equals("TREE") && before.getName().equals(after.getName());
  // 工厂实例来自不同类加载器，其他可观察配置逐项与原版比较。
  for(var method:ToolWindowDefinition.class.getMethods()) {
   if(method.getDeclaringClass()!=ToolWindowDefinition.class || method.getParameterCount()!=0
       || method.getName().equals("getTitle") || method.getName().equals("getFactory"))continue;
   if(method.getName().startsWith("get") || method.getName().startsWith("is"))
    assert Objects.equals(method.invoke(before),method.invoke(after)):method.getName();
  }
  assert before.getFactory()!=null && after.getFactory()!=null;
  System.out.println("通过：真实组件树标题中文，TREE身份、图标、停靠/分割/尺寸和功能配置保持。");
 }
 private static void verifyIconHeaders()throws Exception {
  String name="com.android.tools.idea.layoutinspector.tree.LayoutInspectorTreePanel";
  byte[] raw;
  try(var in=WorkbenchTitleDisplayTest.class.getClassLoader().getResourceAsStream(name.replace('.','/')+".class")){raw=in.readAllBytes();}
  byte[] patched=new ComposeTranslationAgent().transform(WorkbenchTitleDisplayTest.class.getClassLoader(),name.replace('.','/'),null,null,raw);
  assert patched!=null;
  Class<?> type=new ClassLoader(WorkbenchTitleDisplayTest.class.getClassLoader()) {
   Class<?> define(){return defineClass(name,patched,0,patched.length);}
  }.define();
  var render=type.getDeclaredMethod("createIconHeader$lambda$0",com.intellij.ui.components.JBLabel.class,javax.swing.JTable.class,Object.class,boolean.class,boolean.class,int.class,int.class);
  render.setAccessible(true);
  javax.swing.SwingUtilities.invokeAndWait(()->{
   try {
    for(String tooltip:new String[]{"重组次数","子节点重组次数","跳过重组次数",null}) {
     var label=new com.intellij.ui.components.JBLabel();label.setToolTipText(tooltip);
     String expectedName=tooltip==null?label.getAccessibleContext().getAccessibleName():tooltip;
     var table=new javax.swing.JTable(new Object[][]{{7}},new Object[]{"Child Counts"});
     Object identifier=table.getColumnModel().getColumn(0).getIdentifier();
     assert render.invoke(null,label,table,"Child Counts",false,false,0,0)==label;
     assert Objects.equals(label.getAccessibleContext().getAccessibleName(),expectedName):"读屏名称："+label.getAccessibleContext().getAccessibleName();
     assert Objects.equals(label.getToolTipText(),tooltip) && label.getText().isEmpty();
     assert table.getColumnName(0).equals("Child Counts") && table.getValueAt(0,0).equals(7);
     assert table.getColumnModel().getColumn(0).getIdentifier()==identifier;
     render.invoke(null,label,table,"Child Counts",false,false,0,0);
     assert Objects.equals(label.getAccessibleContext().getAccessibleName(),expectedName):"读屏名称："+label.getAccessibleContext().getAccessibleName();
    }
   }catch(ReflectiveOperationException e){throw new RuntimeException(e);}
  });
  System.out.println("通过：真实图标表头回调的中文无障碍名称、无提示及重复渲染，列名/标识/数据保持。");
 }

}
