package local.android.zh.agent;

import com.android.tools.profilers.tasks.ProfilerTaskType;
import com.android.tools.profilers.taskbased.home.StartTaskSelectionError.StartTaskSelectionErrorCode;
import java.lang.classfile.*;
import java.lang.classfile.instruction.InvokeInstruction;
import java.util.*;

/** 使用真实任务与错误枚举执行显示方法，另核对图标补译没有改变 Compose 测试标识。 */
public final class ProfilerDisplayTest {
    public static void main(String[] args) throws Exception {
        var agent = new ComposeTranslationAgent();
        String name = "com.android.tools.profilers.taskbased.common.constants.strings.TaskBasedUxStrings";
        ClassLoader loader = new ClassLoader(ProfilerDisplayTest.class.getClassLoader()) {
            @Override protected Class<?> loadClass(String requested, boolean resolve) throws ClassNotFoundException {
                if (!requested.equals(name) && !requested.startsWith(name + "$")) return super.loadClass(requested, resolve);
                Class<?> type = findLoadedClass(requested);
                if (type == null) {
                    try (var input = getParent().getResourceAsStream(requested.replace('.', '/') + ".class")) {
                        byte[] original = input.readAllBytes();
                        byte[] patched = agent.transform(getParent(), requested.replace('.', '/'), null, null, original);
                        byte[] bytes = patched == null ? original : patched;
                        type = defineClass(requested, bytes, 0, bytes.length);
                    } catch (Exception e) { throw new ClassNotFoundException(requested, e); }
                }
                if (resolve) resolveClass(type);
                return type;
            }
        };
        Class<?> type = loader.loadClass(name);
        Object instance = type.getField("INSTANCE").get(null);
        var shortName = type.getMethod("getTaskShortName", ProfilerTaskType.class, boolean.class);
        String[] expected = {"系统跟踪", "实时性能数据", "方法跟踪", "调用栈采样", "堆转储", "LeakCanary", "Java/Kotlin 内存分配", "原生内存分配", ""};
        for (ProfilerTaskType task : ProfilerTaskType.values()) {
            String description = task.getDescription();
            int rank = task.getRank(); boolean profileable = task.getPrefersProfileable();
            assert shortName.invoke(instance, task, true).equals(expected[task.ordinal()]) : task;
            assert ProfilerTaskType.valueOf(task.name()) == task;
            assert task.getDescription().equals(description) && rank == task.getRank() && profileable == task.getPrefersProfileable();
        }
        var errors = type.getMethod("getStartTaskErrorMessage", StartTaskSelectionErrorCode.class);
        for (StartTaskSelectionErrorCode error : StartTaskSelectionErrorCode.values()) {
            String text = (String) errors.invoke(instance, error);
            assert text.codePoints().anyMatch(c -> c >= 0x4e00 && c <= 0x9fff) : error + ": " + text;
        }
        assert agent.transform(null, "com/android/tools/profilers/tasks/ProfilerTaskType", null, null, new byte[0]) == null;
        for (String simple : List.of("TaskGridItemKt", "TaskGridItemV2Kt")) {
            String internal = "com/android/tools/profilers/taskbased/tabs/taskgridandbars/taskgrid/" + simple;
            byte[] original;
            try (var input = loader.getResourceAsStream(internal + ".class")) { original = input.readAllBytes(); }
            byte[] patched = agent.transform(loader, internal, null, null, original);
            int descriptions = 0, tags = 0, displays = 0;
            for (var method : ClassFile.of().parse(patched).methods()) {
                if (method.code().isEmpty()) continue;
                var instructions = method.code().get().elementStream().filter(e -> e instanceof Instruction).toList();
                for (int i = 0; i < instructions.size() - 1; i++) {
                    if (instructions.get(i) instanceof InvokeInstruction call && call.owner().asInternalName().equals("com/android/tools/profilers/tasks/ProfilerTaskType") && call.name().equalsString("getDescription")) {
                        descriptions++;
                        var next = instructions.get(i + 1);
                        if (next instanceof InvokeInstruction invoke && invoke.owner().asInternalName().equals("androidx/compose/ui/platform/TestTagKt") && invoke.name().equalsString("testTag")) tags++;
                        else { assert ((Instruction) next).opcode() == Opcode.DUP; displays++; }
                    }
                }
            }
            assert descriptions == 2 && tags == 1 && displays == 1 : simple;
        }
        System.out.println("通过：9 种真实任务显示、全部启动错误中文、原任务枚举/描述/排序/采集偏好不变，两个任务卡片 testTag 保持原值。");
    }
}
