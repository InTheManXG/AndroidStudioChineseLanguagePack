# Android Studio 中文补译（社区预览版）

**本项目基于 [sollyu/AndroidStudioChineseLanguagePack](https://github.com/sollyu/AndroidStudioChineseLanguagePack) 修改和扩展。** 感谢 sollyu 的 Android Studio 兼容工作，以及 JetBrains 提供的原始中文语言包。上游说明保留在 [UPSTREAM_README.md](UPSTREAM_README.md)，许可证见 [LICENSE](LICENSE)，署名见 [NOTICE](NOTICE)。

> 当前是未完成的预览版，不是官方版本，不承诺全部界面中文化。部分译文由 AI/机器翻译生成并校订，仍可能存在漏译或语义问题。

## 适用版本

仅针对 Android Studio Rabbit 1 / 2026.2.1、build `262.9437.185.2621.16467767` 开发；实测环境为 macOS ARM64、JBR 25。不要根据上游的通用兼容声明推断本补丁兼容其他版本。Java agent 校验目标类哈希，不匹配时跳过。

## 修改内容

- 扩充 Android 专属资源、菜单、设置和工具窗口译文。
- 补译插件修复部分资源缓存绕过中文语言包的问题。
- Java agent 对已审核的界面常量与动态显示边界补译，保留项目内容、代码、路径和内部标识。
- 已安装的预览包含 242 个目标类、771 条固定或拼接文案；这不是全软件覆盖率。
- 附带源码、资源字典和回归测试，不修改 Android Studio 发行包内的 JAR。

## 下载与安装

从本仓库 [Releases](https://github.com/InTheManXG/AndroidStudioChineseLanguagePack/releases) 下载预览版三个 JAR 和 `SHA256SUMS`，核对 SHA-256 后操作。

1. 完全退出 Android Studio，备份现有中文插件、自定义 VM options 和补译文件。
2. 通过“设置 → 插件 → 从磁盘安装插件”安装 `zh-262.10315.125.jar` 与 `android-studio-zh-supplement.jar`。
3. 将 `android-studio-compose-zh.jar` 放在一个长期固定的目录。
4. 通过“帮助 → 编辑自定义 VM 选项”，加入一行（替换为你自己的绝对路径）：

   ```text
   -javaagent:/absolute/path/android-studio-compose-zh.jar
   ```

5. 将 IDE 的 Language and Region / Language 设为 Chinese，重启。

不要重复添加同一 agent。SDK、模拟器、项目目录无需搬迁。安装位置由用户选择，本仓库不自动修改这些配置。

### 回退

退出 IDE，移除新增的 `-javaagent:` 行，卸载补译插件，并恢复备份的原语言包与 VM options，再启动 IDE。保留项目和 SDK 数据。

## 已验证与已知限制

- 在独立远端 macOS 环境通过 23 项编译/回归检查，覆盖字节码变换、显示数据隔离、格式参数和部分真实 Swing renderer。
- 部分欢迎页、设备设置、新建项目页已做界面验证；这不等于全模块验收。
- 仍有构建菜单、首次统计提示、Gradle 提示、APK/DEX 表头等漏译；部分动态界面与服务端内容未覆盖。
- **源码含最新 APK 六列表头修复，23 项检查通过，但尚未安装和界面复验。Release 的补译插件仍是此前已安装的四列识别版本。** 因此当前源码与该附件在这一处有已知差异。
- 测试工程出现 Gradle 依赖下载失败，尚未完成模拟器、完整工程运行和全部分析工具验收。
- IDE 或语言包升级可能改变内部结构。遇到启动或界面异常请先回退，不应直接跨版本使用。

## 源码与构建

`plugin/` 为补译插件；`compose-patch/` 为 agent、字典及对应测试。构建需要目标版本的 Android Studio 及其 JBR 25，不需要重编译 IDE。运行 `python3 build.py /path/to/Android\ Studio.app`，产物在 `dist/`。

字典更新工具通过 `ANDROID_STUDIO_HOME` 指向目标应用；生成时会读取发行类并计算哈希。全量常量导出和本机审计日志不随仓库公开，以免把代码字符串误当成翻译清单，亦避免包含个人环境信息。

公开构建入口已在独立 macOS ARM64 远端编译打包通过；未对其他平台执行构建。以源码测试、版本限制和上述实际验证范围为准。

## 许可和来源

保留上游 Apache-2.0 许可证。新增补译代码按同一许可证提供；JetBrains 原始语言包及其他第三方内容的权利和适用条款仍归原权利人，不能仅凭仓库许可证将其重新授权。原插件：https://plugins.jetbrains.com/plugin/index?xmlId=com.intellij.zh 。
