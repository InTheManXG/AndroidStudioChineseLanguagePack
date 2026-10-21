"""用用户指定 IDE 的 JBR 编译独立补丁，不修改发行应用。"""
from pathlib import Path
import os
import subprocess
import sys
import zipfile

root = Path(__file__).resolve().parent
app = Path(sys.argv[1]).resolve()
contents = app / 'Contents'
javac = contents / 'jbr/Contents/Home/bin/javac'
if not javac.is_file():
    raise SystemExit('需要 macOS Android Studio 应用目录及其 JBR 25')
classpath = os.pathsep.join(str(p) for p in contents.rglob('*.jar') if not p.name.startswith('._'))
for folder, artifact in [('plugin', 'android-studio-zh-supplement.jar'), ('compose-patch', 'android-studio-compose-zh.jar')]:
    # 使用独立临时编译目录，避免旧 class 混入新包。
    import tempfile
    with tempfile.TemporaryDirectory() as temp:
        sources = [str(p) for p in (root / folder / 'src').rglob('*.java') if not p.name.endswith('Test.java')]
        subprocess.run([str(javac), '-cp', classpath, '-d', temp, *sources], check=True)
        (root / 'dist').mkdir(exist_ok=True)
        with zipfile.ZipFile(root / 'dist' / artifact, 'w', zipfile.ZIP_DEFLATED) as jar:
            for directory in [Path(temp), root / folder / 'resources']:
                for p in sorted(directory.rglob('*')):
                    if p.is_file() and not p.name.startswith('._'):
                        jar.write(p, p.relative_to(directory).as_posix())
            if folder == 'compose-patch':
                jar.write(root / folder / 'MANIFEST.MF', 'META-INF/MANIFEST.MF')
print('产物位于 dist/；尚需运行回归测试和目标 IDE 界面验收。')
