package local.android.zh.agent;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import com.intellij.ui.ColoredTreeCellRenderer;
import com.intellij.openapi.util.text.StringUtil;
import com.android.tools.idea.sqlite.model.*;
import com.android.tools.idea.sqlite.ui.mainView.ViewDatabase;

/** 执行真实结构树渲染，保护数据库身份、HTML转义与SQL类型显示。 */
public final class DatabaseSchemaDisplayTest {
 public static void main(String[] args)throws Exception {
  verifyQueryTitles();
  String base="com.android.tools.idea.sqlite.ui.mainView.";
  var agent=new ComposeTranslationAgent();
  ClassLoader loader=new ClassLoader(DatabaseSchemaDisplayTest.class.getClassLoader()) {
   @Override protected Class<?> loadClass(String name,boolean resolve)throws ClassNotFoundException {
    if(!name.equals(base+"LeftPanelView$SchemaTreeCellRenderer") && !name.startsWith(base+"LeftPanelViewKt"))return super.loadClass(name,resolve);
    Class<?> type=findLoadedClass(name);
    if(type==null)try(var in=getParent().getResourceAsStream(name.replace('.','/')+".class")) {
     byte[] raw=in.readAllBytes(),patched=agent.transform(getParent(),name.replace('.','/'),null,null,raw);
     byte[] data=patched==null?raw:patched;type=defineClass(name,data,0,data.length);
    }catch(Exception e){throw new ClassNotFoundException(name,e);}
    if(resolve)resolveClass(type);return type;
   }
  };
  var ctor=loader.loadClass(base+"LeftPanelView$SchemaTreeCellRenderer").getDeclaredConstructor();ctor.setAccessible(true);
  SwingUtilities.invokeAndWait(()->{
   try {
    var cell=(ColoredTreeCellRenderer)ctor.newInstance();
    for(String name:new String[]{"closed","non-native","may be read-only","用户${db}"}) {
     for(int state=0;state<4;state++) {
      String path="/data/<user>&'"+name+".db",api="custom.<API>&Driver";
      var id=new SqliteDatabaseId.LiveSqliteDatabaseId(path,name,42,state==1,state==2,api);
      var view=new ViewDatabase(id,state!=0);String snapshot=id.toString();
      cell.clear();cell.customizeCellRenderer(new JTree(),new DefaultMutableTreeNode(view),false,false,false,0,false);
      String suffix=new String[]{"已关闭","非原生连接","可能为只读",""}[state];
      // false 包含灰色附加说明；仅主文本会漏掉状态和SQL类型。
      String text=cell.getCharSequence(false).toString();
      assert text.startsWith(name):"用户数据库名被翻译："+text;
      assert suffix.isEmpty() || text.contains(suffix):text;
      assert cell.getToolTipText().equals("<html>"+StringUtil.escapeXmlEntities(path)+"<br>打开所用 API："+StringUtil.escapeXmlEntities(api)+"</html>"):cell.getToolTipText();
      assert id.toString().equals(snapshot) && view.getDatabaseId()==id && id.getConnectionId()==42;
     }
     // 普通字符串节点和列名可能与原文相同，不能按运行时内容全局替换。
     cell.clear();cell.customizeCellRenderer(new JTree(),new DefaultMutableTreeNode(name),false,false,false,0,false);
     assert cell.getCharSequence(false).toString().equals(name);
     for(SqliteAffinity affinity:SqliteAffinity.values())for(boolean nullable:new boolean[]{false,true}) {
      var column=new SqliteColumn(name,affinity,nullable,false);String snapshot=column.toString();
      cell.clear();cell.customizeCellRenderer(new JTree(),new DefaultMutableTreeNode(column),false,false,false,0,false);
      // false 包含灰色附加说明；仅主文本会漏掉状态和SQL类型。
      String text=cell.getCharSequence(false).toString();
      assert text.startsWith(name) && text.contains(affinity.name()):text;
      assert text.contains("NOT NULL")==!nullable:text;
      assert column.toString().equals(snapshot) && column.getAffinity()==affinity;
     }
    }
   }catch(ReflectiveOperationException e){throw new RuntimeException(e);}
  });
  System.out.println("通过：真实数据库树四种连接显示、同名用户数据、路径/API转义、五种SQL类型及NULL约束保持。");
 }
 private static void verifyQueryTitles()throws Exception {
  String name="com/android/tools/idea/sqlite/controllers/DatabaseInspectorControllerImpl";
  byte[] raw;
  try(var in=DatabaseSchemaDisplayTest.class.getClassLoader().getResourceAsStream(name+".class")){raw=in.readAllBytes();}
  byte[] patched=new ComposeTranslationAgent().transform(DatabaseSchemaDisplayTest.class.getClassLoader(),name,null,null,raw);
  assert patched!=null;
  var method=java.lang.classfile.ClassFile.of().parse(patched).methods().stream()
      .filter(m->m.methodName().equalsString("openNewEvaluatorTab")).findFirst().orElseThrow();
  var strings=new java.util.ArrayList<String>();
  for(var element:method.code().orElseThrow())
   if(element instanceof java.lang.classfile.instruction.ConstantInstruction.LoadConstantInstruction load
      && load.constantValue() instanceof String text)strings.add(text);
  int index=strings.indexOf("新建查询");assert index>=0 && !strings.contains("New Query");
  // 从真实方法提取默认名及四个命名参数，交给原版重名生成器执行。
  String seed=strings.get(index),prefix=strings.get(index+1),suffix=strings.get(index+2),before=strings.get(index+3),after=strings.get(index+4);
  assert prefix.isEmpty() && suffix.isEmpty() && before.equals(" [") && after.equals("]");
  var occupied=new java.util.LinkedHashSet<String>(java.util.List.of("New Query","用户表","新建查询","新建查询 [1]"));
  var original=java.util.Set.copyOf(occupied);
  String title=com.intellij.util.text.UniqueNameGenerator.generateUniqueName(seed,prefix,suffix,before,after,n->!occupied.contains(n));
  assert title.equals("新建查询 [2]"):title;
  assert occupied.equals(original):"改写已有表名或标签";
  occupied.add(title);
  assert com.intellij.util.text.UniqueNameGenerator.generateUniqueName(seed,prefix,suffix,before,after,n->!occupied.contains(n)).equals("新建查询 [3]");
  occupied.remove("新建查询");
  assert com.intellij.util.text.UniqueNameGenerator.generateUniqueName(seed,prefix,suffix,before,after,n->!occupied.contains(n)).equals("新建查询");
  System.out.println("通过：真实默认标题参数及原版重名生成器，中文同名表/连续新建/标题释放保持正确，已有名称不变。");
 }

}
