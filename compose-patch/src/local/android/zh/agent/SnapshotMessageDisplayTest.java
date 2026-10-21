package local.android.zh.agent;

import java.lang.classfile.*;
import java.lang.classfile.instruction.*;
import java.lang.classfile.instruction.ConstantInstruction.LoadConstantInstruction;
import java.lang.classfile.constantpool.StringEntry;
import java.lang.constant.*;
import java.lang.reflect.AccessFlag;
import java.util.*;

/** 验证快照错误的显示翻译与原始异常、日志隔离。 */
public final class SnapshotMessageDisplayTest {
 public static void main(String[] args)throws Exception {
  String name="com/android/tools/idea/layoutinspector/snapshots/LayoutInspectorFileEditor";
  byte[] original;
  try(var in=SnapshotMessageDisplayTest.class.getClassLoader().getResourceAsStream(name+".class")){original=in.readAllBytes();}
  byte[] patched=new ComposeTranslationAgent().transform(SnapshotMessageDisplayTest.class.getClassLoader(),name,null,null,original);
  assert patched!=null;
  assert diagnostics(original).equals(diagnostics(patched)):"异常或日志原文变化";
  assert diagnostics(patched).size()==3:"未覆盖快照两种异常与日志";
  int translatedTitles=0;
  for(var method:ClassFile.of().parse(patched).methods())if(method.methodName().equalsString("getComponent"))
   for(var element:method.code().orElseThrow())if(element instanceof LoadConstantInstruction load
       && load.constantEntry() instanceof StringEntry s && s.stringValue().equals("加载快照时出错"))translatedTitles++;
  assert translatedTitles==2:"两处StatusText显示出口必须都覆盖";
  byte[] fixture=ClassFile.of().build(ClassDesc.of("SnapshotMessageFixture"),c->c.withMethodBody("display",
   MethodTypeDesc.of(ConstantDescs.CD_String,ConstantDescs.CD_String),AccessFlag.PUBLIC.mask()|AccessFlag.STATIC.mask(),b->{
    b.aload(0);ComposeTranslationAgent.translateSnapshotMessage(b);b.areturn();
   }));
  Class<?> type=new ClassLoader(SnapshotMessageDisplayTest.class.getClassLoader()){
   Class<?> define(){return defineClass(null,fixture,0,fixture.length);}
  }.define();
  var display=type.getMethod("display",String.class);
  String[][] cases={{"Error loading snapshot","加载快照时出错"},
   {"SKP image type is no longer supported starting with Android Studio Panda 2","从 Android Studio Panda 2 起，不再支持 SKP 图像格式"},
   {"Unknown picture type","未知图像类型"},{"/user/Unknown picture type.li","/user/Unknown picture type.li"},{"user error","user error"},{null,null}};
  for(String[] pair:cases)assert Objects.equals(display.invoke(null,(Object)pair[0]),pair[1]);
  System.out.println("通过：快照三种错误显示、未知错误/路径/空值保持，发行类原始异常和日志不变。");
 }
 private static List<String> diagnostics(byte[] bytes) {
  List<String> result=new ArrayList<>();
  for(var method:ClassFile.of().parse(bytes).methods())if(method.methodName().equalsString("getComponent")) {
   String latest=null;
   for(var element:method.code().orElseThrow()) {
    if(element instanceof LoadConstantInstruction load && load.constantEntry() instanceof StringEntry s)latest=s.stringValue();
    if(element instanceof InvokeInstruction call &&
      ((call.owner().asInternalName().equals("com/intellij/openapi/diagnostic/Logger") && call.name().equalsString("warn"))
      || (call.owner().asInternalName().equals("java/lang/IllegalStateException") && call.name().equalsString("<init>"))))result.add(latest);
   }
  }
  return result;
 }
}
