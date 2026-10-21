package local.android.zh.agent;

import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.regex.Pattern;

/** 通知使用 printf 而非 MessageFormat；验证占位符及成功/警告两种组合。 */
public final class RunFeedbackTest {
    public static void main(String[] args) throws Exception {
        Map<String,String> text=new HashMap<>();
        Pattern formats=Pattern.compile("%(?:[0-9]+\\$)?[sd]");
        int checked=0;
        for(String line:Files.readAllLines(Path.of(args[0]))) {
            String[] f=line.split("\t",-1);
            String source=new String(Base64.getDecoder().decode(f[2]),StandardCharsets.UTF_8);
            String target=new String(Base64.getDecoder().decode(f[3]),StandardCharsets.UTF_8);
            text.put(source,target);
            if (!f[0].contains("GradleTasksExecutorImpl") && !f[0].endsWith("AbstractDeployTask")) continue;
            var before=formats.matcher(source).results().map(m->m.group()).toList();
            var after=formats.matcher(target).results().map(m->m.group()).toList();
            assert before.equals(after):source;
            if(!before.isEmpty()) {
                Object[] values=before.stream().map(v->v.endsWith("d")?Integer.valueOf(7):"用户值").toArray();
                String formatted=String.format(Locale.ROOT,target,values);
                assert !formatted.contains("%s") && !formatted.contains("%d");
                checked++;
            }
        }
        assert checked==3:checked;
        assert String.format(text.get("%s successfully finished in %s%s"),text.get("Install"),"1 s 681 ms","")
            .equals("安装已成功完成，耗时 1 s 681 ms");
        assert String.format(text.get("%s successfully finished in %s%s"),text.get("Apply Changes"),"2 s",text.get(" with no build tasks before launch"))
            .equals("应用更改已成功完成，耗时 2 s（启动前未执行构建任务）");
        assert String.format(text.get(" with %d error(s)"),7).equals("，共 7 个错误");
        System.out.println("通过：3 个格式模板、安装成功/跳过构建提示、动态耗时与错误数量保持正确。");
    }
}
