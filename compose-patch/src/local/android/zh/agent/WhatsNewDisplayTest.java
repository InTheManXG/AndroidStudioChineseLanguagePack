package local.android.zh.agent;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.zip.ZipInputStream;
import com.android.repository.Revision;
import com.android.tools.idea.whatsnew.assistant.v2.model.WhatsNewMarkdownParser;

/** 实际执行转换后的文档流，并交给发行版 Markdown 解析器验证。 */
public final class WhatsNewDisplayTest {
    static final class Loader extends ClassLoader {
        Loader() { super(WhatsNewDisplayTest.class.getClassLoader()); }
        Class<?> define(String name, byte[] bytes) { return defineClass(name, bytes, 0, bytes.length); }
    }
    public static void main(String[] args) throws Exception {
        String name="com/android/tools/idea/whatsnew/assistant/v2/model/WhatsNewDocumentLoaderImpl";
        Loader loader=new Loader();
        byte[] original;
        try (var stream=loader.getResourceAsStream(name+".class")) { original=stream.readAllBytes(); }
        byte[] patched=new ComposeTranslationAgent().transform(loader,name,null,null,original);
        assert patched!=null;
        var type=loader.define(name.replace('/','.'),patched);
        var method=type.getDeclaredMethod("_init_$lambda$1");
        method.setAccessible(true);
        byte[] actual;
        try (var stream=(InputStream)method.invoke(null)) { actual=stream.readAllBytes(); }
        try (var expected=loader.getResourceAsStream("whats-new-zh.zip")) {
            assert Arrays.equals(actual,expected.readAllBytes()):"运行时中文 ZIP 与产物不一致";
        }
        int count=0;
        try (var zip=new ZipInputStream(new java.io.ByteArrayInputStream(actual))) {
            for (var entry=zip.getNextEntry();entry!=null;entry=zip.getNextEntry()) {
                if (!entry.getName().endsWith(".md")) continue;
                String markdown=new String(zip.readAllBytes(),StandardCharsets.UTF_8);
                String version=entry.getName().replace("md/", "").replace(".md", "");
                var document=WhatsNewMarkdownParser.INSTANCE.parseMarkdown(Revision.parseRevision(version),markdown);
                assert document!=null;
                assert markdown.lines().findFirst().orElseThrow().contains("新增功能");
                count++;
            }
        }
        assert count==9:count;
        System.out.println("通过：运行时文档流与中文 ZIP 一致，9 个真实版本说明可由原解析器读取。");
    }
}
