package local.android.zh.agent;

import java.lang.classfile.*;
import java.lang.classfile.instruction.*;
import java.lang.classfile.instruction.ConstantInstruction.LoadConstantInstruction;
import java.lang.constant.*;
import java.lang.reflect.AccessFlag;
import java.util.*;

/** 标题转换必须在扩展之后，表达式、失效标记和调用栈不变。 */
public final class StateReadTitleDisplayTest {
 public static void main(String[] args)throws Exception {
  String name="com/android/tools/idea/layoutinspector/recompositions/RecompositionUiModelImpl";
  byte[] raw;
  try(var in=StateReadTitleDisplayTest.class.getClassLoader().getResourceAsStream(name+".class")){raw=in.readAllBytes();}
  byte[] patched=new ComposeTranslationAgent().transform(StateReadTitleDisplayTest.class.getClassLoader(),name,null,null,raw);
  assert patched!=null;
  int appends=0,rewrites=0,titles=0;boolean converted=false;
  var method=ClassFile.of().parse(patched).methods().stream().filter(m->m.methodName().equalsString("generateStateReadLine")).findFirst().orElseThrow();
  for(var element:method.code().orElseThrow()) {
   if(element instanceof LoadConstantInstruction load && "读取的状态值：".equals(load.constantValue())) {
    assert appends==2 && rewrites==1:"必须在扩展之后、最终追加之前";converted=true;titles++;
   }
   if(element instanceof InvokeInstruction call) {
    if(call.name().equalsString("rewriteStateRead")){assert !converted;rewrites++;}
    if(call.owner().asInternalName().equals("java/lang/StringBuilder") && call.name().equalsString("append")
       && call.type().equalsString("(Ljava/lang/String;)Ljava/lang/StringBuilder;")) {
     appends++;assert converted==(appends==3):"错误的字符串追加位置";
    }
   }
  }
  assert appends==3 && titles==1;
  byte[] fixture=ClassFile.of().build(ClassDesc.of("StateReadTitleFixture"),c->c.withMethodBody("display",
   MethodTypeDesc.of(ConstantDescs.CD_String,ConstantDescs.CD_String),AccessFlag.PUBLIC.mask()|AccessFlag.STATIC.mask(),b->{
    b.aload(0);ComposeTranslationAgent.translateStateReadTitle(b);b.areturn();
   }));
  Class<?> type=new ClassLoader(StateReadTitleDisplayTest.class.getClassLoader()) {
   Class<?> define(){return defineClass(null,fixture,0,fixture.length);}
  }.define();
  var display=type.getMethod("display",String.class);
  for(String value:new String[]{"", "user.State(value='State read value: ')", "${value}<invalidated>", "中文\nvalue: details\n at User.method(Unknown Source:1)"})
   assert display.invoke(null,"State read value: "+value).equals("读取的状态值："+value);
  for(String value:new String[]{null,"custom rewrite", "value: details", " at User.method(Unknown Source:1)","prefix State read value: user"})
   assert Objects.equals(display.invoke(null,(Object)value),value);
  System.out.println("通过：重写扩展之后才转换状态读取标题，表达式/标记/折叠前缀/调用栈/未知输出/null保持。");
 }
}
