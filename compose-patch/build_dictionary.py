"""固定当前版本的显示文案与类哈希；不处理设备名、镜像名或配置值。"""
from pathlib import Path
from zipfile import ZipFile
import base64,csv,hashlib,json,os
root=Path(__file__).resolve().parent
# 远端验证显式指定同版本 IDE，原机路径仅保留为构建来源默认值。
lib=Path(os.environ['ANDROID_STUDIO_HOME'])/'Contents/plugins/android/lib'
translations={
'avd.ConfigureDevicePanelKt':{'Configure virtual device':'配置虚拟设备'},
'avd.Tab':{'Device':'设备','Additional settings':'其他设置'},
'avd.DevicePanelKt':{
'Name':'名称','Select system image':'选择系统镜像','Services':'服务','System Image':'系统镜像',
'Use the filters to help find the system image that you prefer. The combination of device profile and system image is only an approximation of the equivalent physical hardware.':'使用筛选条件查找所需的系统镜像。设备配置与系统镜像的组合只能近似模拟对应的实体硬件。',
'Show system images with SDK extensions':'显示包含 SDK 扩展的系统镜像','Show unsupported system images':'显示不受支持的系统镜像',
'Loading system images...':'正在加载系统镜像…','No system images available matching the current set of filters.':'没有符合当前筛选条件的系统镜像。','Show All':'显示全部',
'Filter by images that include full support for the Google Play Store, only Google APIs and services, or the base version of Android (AOSP) without any Google apps or services':'按镜像类型筛选：完整支持 Google Play 商店、仅含 Google API 和服务，或不含 Google 应用及服务的 Android 基础版本（AOSP）。'},
'avd.ConfigurationPageKt':{'Finish':'完成','Loading system images...':'正在加载系统镜像…','No system images available.':'没有可用的系统镜像。','Emulator (Preview)':'模拟器（预览版）','Creating AVD':'正在创建虚拟设备','Confirm Download':'确认下载','Error Creating AVD':'创建虚拟设备出错','An error occurred while creating the AVD. See idea.log for details.':'创建虚拟设备时发生错误，详情请查看 idea.log。'},
'avd.EditVirtualDeviceDialog$Companion':{'Edit Device':'编辑设备','Loading':'正在加载','The hardware profile for this device is no longer present. Please create a new device.':'此设备的硬件配置已不存在，请创建新设备。'},
'adddevicedialog.DeviceDetailsKt':{'Device':'设备','System':'系统','Download':'下载','OEM':'制造商','API Level':'API 级别','Supported API Levels':'支持的 API 级别','System Image':'系统镜像','Services':'服务','Translated ABI':'转译 ABI','Screen':'屏幕','Resolution':'分辨率','Density':'像素密度','None':'无'},
'adddevicedialog.DeviceTableColumns':{'Name':'名称','Type':'类型','Height':'高度','Width':'宽度','Density':'像素密度','OEM':'制造商','Virtual':'虚拟','Physical':'实体'},
'deviceprovisioner.StudioDefaultDeviceActionPresentation':{'Boot from Snapshot':'从快照启动','Cold Boot':'冷启动','Pair Glasses':'配对眼镜','Unpair Glasses':'取消眼镜配对','Wipe Data':'清除数据'},
'devicemanagerv2.ColdBootAction':{'Cold Boot':'冷启动'},
'devicemanagerv2.PairWearableDeviceAction':{'Pair Wearable':'配对穿戴设备'},
'devicemanagerv2.EditDeviceAction':{'Edit':'编辑','Edit this device':'编辑此设备'},
'devicemanagerv2.DuplicateDeviceAction':{'Duplicate':'创建副本','Duplicate this device':'创建此设备的副本'},
'devicemanagerv2.WipeDataAction':{'Wipe Data':'清除数据','Wipe the user data of this AVD':'清除此虚拟设备的用户数据','Confirm Data Wipe':'确认清除数据',' This will stop the device.':' 此操作会停止设备。'},
'devicemanagerv2.DeleteDeviceAction':{'Delete':'删除','Delete this device':'删除此设备','Confirm Deletion':'确认删除',' This will stop the device.':' 此操作会停止设备。'},
'devicemanagerv2.OpenDeviceExplorerAction':{'Open in Device Explorer':'在设备资源管理器中打开'},
'devicemanagerv2.ViewDetailsAction':{'View Details':'查看详情'},
'devicemanagerv2.ShowOnDiskAction':{'Show':'在访达中显示','Show this device':'显示此设备的文件'},
'devicemanagerv2.DeviceManagerWelcomeScreenFrame':{'Device Manager':'设备管理器'},
'avd.AdditionalSettingsPanelKt':{'Device skin':'设备外观','Camera':'摄像头','Network':'网络','Startup':'启动','Emulated Performance':'模拟性能','Specify a RAM value':'请指定内存大小','RAM value is too large':'内存大小过大','Specify a VM heap size':'请指定虚拟机堆大小','VM heap size is too large':'虚拟机堆大小过大','Optimal':'最佳','Preferred ABI':'首选 ABI','Front':'前置','Rear':'后置','Speed':'速度','Latency':'延迟','Orientation':'屏幕方向','Default boot':'默认启动方式','CPU cores':'CPU 核心数','Graphics acceleration':'图形加速','VM heap size':'虚拟机堆大小',
'None: no camera installed for AVD\nEmulated: use a simulated camera\nWebcam0: use host computer webcam or built-in camera':'None：不安装摄像头\nEmulated：使用模拟摄像头\nWebcam0：使用主机的网络摄像头或内置摄像头',
'None: no camera installed for AVD\nVirtualScene: use a virtual camera in a simulated environment\nEmulated: use a simulated camera\nWebcam0: use host computer webcam or built-in camera':'None：不安装摄像头\nVirtualScene：使用模拟环境中的虚拟摄像头\nEmulated：使用模拟摄像头\nWebcam0：使用主机的网络摄像头或内置摄像头',
'The amount of RAM on the AVD. This RAM is allocated from the host system while the AVD is running. Larger amounts of RAM will allow the AVD to run more applications, but have a greater impact on the host system.':'虚拟设备的内存大小。运行时从主机分配；增大内存可运行更多应用，但也会占用更多主机资源。',
'The amount of RAM available to the Java virtual machine (VM) to allocate to running apps on the AVD. A larger VM heap allows applications to run longer between garbage collection events.':'Java 虚拟机可分配给虚拟设备上应用的内存大小。增大虚拟机堆可延长两次垃圾回收之间的运行时间。'}
}
# 只修改显示值，保留 Boot/GraphicsMode 的枚举名和写入 AVD 的配置键值。
translations.update({
'avd.Boot': {'Cold':'冷启动', 'Quick':'快速启动'},
'avd.GraphicsMode': {'Automatic':'自动', 'Hardware':'硬件加速', 'Software':'软件渲染'},
'avd.ExpandedStorageRadioButton$CUSTOM': {'Custom':'自定义'},
'avd.ExpandedStorageRadioButton$EXISTING_IMAGE': {'Existing image':'现有镜像'},
'avd.ExpandedStorageRadioButton$NONE': {'None':'无'},
'avd.StorageGroupKt': {
'Storage':'存储', 'Internal storage':'内部存储', 'Expanded storage':'扩展存储',
'The amount of non-removable space available to store data on the AVD':'虚拟设备用于存储数据的不可移除存储空间大小',
'Custom: The amount of expanded storage available to store data on the AVD. We recommend at least 100 MB in order to use the camera in the emulator.\nExisting image: Choose a file path to an existing expanded storage image. Using an existing image is useful when sharing data (pictures, media, files, etc.) between AVDs.\nNone: No expanded storage on this AVD':'自定义：虚拟设备用于存储数据的扩展存储空间大小。要使用模拟器摄像头，建议至少设置 100 MB。\n现有镜像：选择已有的扩展存储镜像文件，便于在多个虚拟设备之间共享图片、媒体、文件等数据。\n无：此虚拟设备不使用扩展存储。',
'Warning':'警告', 'Modifying storage size erases existing content':'修改存储大小将清除现有内容',
'Specify an internal storage value':'请指定内部存储大小', 'Internal storage is too large':'内部存储大小过大',
'Specify an SD card size':'请指定 SD 卡大小', 'SD card size is too large':'SD 卡大小过大'
}})
translations['avd.AdditionalSettingsPanelKt']['RAM']='内存（RAM）'
translations['avd.DropdownKt']={'':''}  # 仅登记显示调用转换的类哈希。
translations['avd.AdditionalSettingsPanelKt'].update({
'Sets the initial state of the simulated network transfer rate used by the AVD. The network speed can also be adjusted in the emulator.':'设置虚拟设备的初始模拟网络传输速率，也可在模拟器中调整网络速度。',
'Sets the initial state of the simulated network transfer latency used by the AVD. Latency is the delay in processing data across the network. The network latency can also be adjusted in the emulator.':'设置虚拟设备的初始模拟网络延迟，即数据通过网络传输时的处理延迟，也可在模拟器中调整。'
})
for source in list(translations['avd.AdditionalSettingsPanelKt']):
 if source.startswith('None: no camera'):
  translations['avd.AdditionalSettingsPanelKt'][source]=translations['avd.AdditionalSettingsPanelKt'][source].replace('None：','无：').replace('Emulated：','模拟摄像头：').replace('VirtualScene：','虚拟场景：').replace('Webcam0：','主机摄像头 0：')
entries=[]
translations['avd.ComposableSingletons$DevicePanelKt']={'Recommended':'推荐'}
translations['avd.ComposableSingletons$DevicePanelKt$lambda$-737147449$1']={'Download':'下载'}
translations['avd.FileInputFieldKt']={'select file':'选择文件'}
# 内存选项使用整数模型，翻译仅限标题和渲染器格式，不改变 Xmx 或推荐值计算。
translations['memorysettings.MemorySettingsConfigurable$MyComponent']={
 'IDE Heap Size Settings':'IDE 堆内存设置','IDE max heap size:':'IDE 最大堆内存：','Daemon Heap Size Settings':'守护进程堆内存设置'}
translations['memorysettings.MemorySettingsConfigurable$MyComponent$2']={'%s - current':'%s（当前）'}
translations['memorysettings.MemorySettingsConfigurable$MyComponent$3']={'%s - current':'%s（当前）','%s - default':'%s（默认）','%s - recommended':'%s（推荐）'}
translations['memorysettings.GradleComponent']={'Daemon Heap Size Settings':'守护进程堆内存设置','Gradle daemon max heap size:':'Gradle 守护进程最大堆内存：','Kotlin daemon max heap size:':'Kotlin 守护进程最大堆内存：'}
translations['marketing.configurable.MarketingConfigurable']={
 'Communications':'通信','Fetching':'正在获取','Applying':'正在应用',
 'Receive relevant emails and notifications regarding Android Studio news and updates':'接收有关 Android Studio 新闻和更新的相关电子邮件及通知',
 'This page requires a signed in Google account. ':'请先登录 Google 账号，再使用此页面。',
 'Sign in':'登录','Authorize':'授权',
 'If you are currently opted into sharing usage statistics, that data will be linked to ':'如果您已选择共享使用统计信息，这些数据将关联到',
 'your Google Account and used to personalize communications in accordance with ':'您的 Google 账号，并依据以下政策用于个性化通信：',
 "Google's Privacy Policy":'Google 隐私权政策','Retry':'重试',
 'Preferences for sharing usage statistics is available in ':'您可以在以下位置设置是否共享使用统计信息：',
 'Data Sharing':'数据共享',
 'Android Studio was unable to save your communications preference.':'Android Studio 无法保存您的通信偏好设置。'}
translations['npw.project.ComposeChooseAndroidProjectStepKt']={'Templates':'模板'}
translations['npw.project.ConfigureAndroidProjectStep']={
'Save location':'保存位置', '&Save location':'保存位置(&S)',
'The name that will be shown in the Android launcher for this application':'此应用在 Android 启动器中显示的名称'
}
translations['npw.template.ConfigureTemplateParametersStep']={'':''}
translations['avd.StudioLocalEmulatorProvisionerPlugin$createDeviceAction$1']={'Create Virtual Device':'创建虚拟设备'}
translations['devicemanagerv2.DeviceManagerPanel']={'Add Device':'添加设备','Add a new device':'添加新设备','No devices connected.':'没有已连接的设备。','Add a new device...':'添加新设备…'}
translations['npw.project.FormFactorSdkControls']={'API level percentage':'API 级别占比','(loading SDK Data and Stats)':'（正在加载 SDK 数据和统计信息）','<html>Your app will run on %1$s of devices.</html>':'<html>您的应用可在 %1$s 的设备上运行。</html>','approximately <b>\u0001</b>':'约 <b>\u0001</b>'}
translations['whatsnew.assistant.v2.model.WhatsNewDocumentLoaderImpl']={'':''}
translations['whatsnew.assistant.v2.ui.WhatsNewEditor']={"What's New":"新增功能"}
translations['whatsnew.assistant.v2.ui.WhatsNewEditorAction']={"What's New in Android Studio":"Android Studio 新增功能"}
translations['whatsnew.assistant.v2.ui.WhatsNewVirtualFileImpl']={"What's New":"新增功能"}
# 项目界面只替换固定显示文字与拼接配方，配置名称、链接和回调保持原值。
translations.update({
'avd.AddDeviceWizard': {'Add Device':'添加设备','Show obsolete device profiles':'显示已过时的设备配置','New hardware profile...':'新建硬件配置…','Import hardware profile...':'导入硬件配置…'},
'streaming.core.StreamingToolWindowManager$NewTabAction': {'Add Device':'添加设备','Show a new device':'显示新设备'},
'gradle.actions.SyncProjectAction': {'Sync Project with _Gradle Files':'使用 Gradle 文件同步项目(_G)'},
'gradle.actions.AbstractBuildRunConfigurationAction': {
 'Build Selected Run Configuration':'构建所选运行配置','Build selected Run Configuration':'构建所选运行配置',
 'Assemble Run Configuration (No Configuration Selected)':'构建运行配置（尚未选择配置）',
 "Assemble '\u0001' Run Configuration":"构建运行配置“\u0001”"},
'gradle.notification.ProjectSyncStatusNotificationProvider$NotificationPanel$Type$3': {'Gradle project sync in progress...':'正在同步 Gradle 项目…'},
'gradle.notification.ProjectSyncStatusNotificationProvider$NotificationPanel$Type$4': {'Gradle project sync failed. Basic functionality (e.g. editing, debugging) will not work properly.':'Gradle 项目同步失败，编辑、调试等基本功能将无法正常使用。'},
'gradle.notification.ProjectSyncStatusNotificationProvider$SyncProblemNotificationPanel': {'Try Again':'重试',"Open 'Build' View":"打开“构建”窗口",'Show Log in \u0001':'在\u0001中显示日志'},
'whatsnew.assistant.v2.ui.WhatsNewEditorPanelKt$TableOfContents$1$2': {'Menu':'目录'}
})
translations['streaming.core.EmptyStatePanel']={
 'To mirror a&nbsp;physical device, connect it via USB cable or over WiFi.':'要镜像实体设备，请通过 USB 数据线或 Wi-Fi 连接设备。',
 "To launch a&nbsp;virtual device, click \u0001 and select the device from the list, or use the&nbsp;<font color = \u0001><a href='DeviceManager'>Device&nbsp;Manager</a></font>.":"要启动虚拟设备，请点击 \u0001 并从列表中选择设备，或使用<font color = \u0001><a href='DeviceManager'>设备管理器</a></font>。",
 "To launch a&nbsp;virtual device, click \u0001 and select a virtual device, or select the&nbsp;<b>Launch in the&nbsp;Running&nbsp;Devices tool window</b> option in&nbsp;the&nbsp;<font color = \u0001><a href='EmulatorSettings'>Emulator&nbsp;settings</a></font> and use the&nbsp;<font color = \u0001><a href='DeviceManager'>Device&nbsp;Manager</a></font>.":"要启动虚拟设备，请点击 \u0001 并选择虚拟设备，或在<font color = \u0001><a href='EmulatorSettings'>模拟器设置</a></font>中勾选<b>在“运行中的设备”工具窗口中启动</b>，然后使用<font color = \u0001><a href='DeviceManager'>设备管理器</a></font>。",
 "To mirror a&nbsp;physical device, connect it via USB cable or over WiFi, click \u0001 and select the&nbsp;device from the&nbsp;list. You may also select the&nbsp;<b>Activate mirroring when a&nbsp;new physical device is connected</b> option in&nbsp;the&nbsp;<font color = \u0001><a href='DeviceMirroringSettings'>Device&nbsp;Mirroring&nbsp;settings</a></font>.":"要镜像实体设备，请通过 USB 数据线或 Wi-Fi 连接设备，点击 \u0001 并从列表中选择设备。您也可以在<font color = \u0001><a href='DeviceMirroringSettings'>设备镜像设置</a></font>中勾选<b>连接新的实体设备时启用镜像</b>。"
}
translations['navigator.nodes.android.AndroidBuildScriptsGroupNode']={'Project Gradle Files':'项目 Gradle 文件','Gradle Scripts':'Gradle 脚本'}
translations['actions.license.ShowLicensesUsedAction']={'_Licenses':'许可证(_L)','Collecting licenses':'正在收集许可证'}
# 运行反馈的内部任务 ID、通知组名、命令及应用标识不参与翻译。
translations.update({
'run.configuration.execution.ExecutionUtils': {
 'Launching devices':'正在启动设备','Waiting for all target devices to come online':'正在等待所有目标设备上线',
 'Checking Wear OS Surface API version':'正在检查 Wear OS Surface API 版本',
 "Error while executing: '\u0001'":"执行时出错：“\u0001”",
 'Error while checking version, message: \u0001':'检查版本时出错，错误信息：\u0001',
 'Error while checking version':'检查版本时出错'},
'gradle.project.build.invoker.GradleTasksExecutorImpl$TaskImpl': {
 'Gradle Build Running':'正在执行 Gradle 构建','Gradle sync needed':'需要同步 Gradle 项目','Sync project':'同步项目',
 'Unable to determine project Android Gradle Plugin (AGP) version.':'无法确定项目的 Android Gradle 插件（AGP）版本。',
 'Project was built with Android Gradle Plugin (AGP) %s but it is synced with %s.':'项目使用 Android Gradle 插件（AGP）%s 构建，但同步时使用的是 %s。',
 'Failed to complete Gradle execution. Project may be closing or already closed.':'无法完成 Gradle 执行。项目可能正在关闭或已经关闭。',
 'Build cancelled':'构建已取消','Gradle build \u0001':'Gradle 构建\u0001',' with %d error(s)':'，共 %d 个错误','\u0001 in \u0001':'\u0001，耗时 \u0001'},
'gradle.project.build.invoker.GradleTasksExecutorImpl$TaskImpl$Companion': {'finished':'已完成','failed':'失败','cancelled':'已取消'},
'run.tasks.AbstractDeployTask': {' with no build tasks before launch':'（启动前未执行构建任务）','%s successfully finished in %s%s':'%s已成功完成，耗时 %s%s','Learn more about missing build tasks':'了解启动前未执行构建任务的详情'},
'run.tasks.DeployTask': {'Install':'安装','Installing application: \u0001':'正在安装应用：\u0001','App restart successful without requiring a re-install.':'应用已成功重启，无需重新安装。','App restart successful without re-installing the following APK(s): \u0001':'应用已成功重启，未重新安装以下 APK：\u0001'},
'run.tasks.ApplyChangesTask': {'Apply Changes':'应用更改','Applying changes to application: \u0001':'正在将更改应用到应用：\u0001','Activity restarted. No code or resource changes detected.':'Activity 已重启，未检测到代码或资源更改。','Activity restarted without re-installing the following APK(s): \u0001':'Activity 已重启，未重新安装以下 APK：\u0001'},
'run.tasks.ApplyCodeChangesTask': {'Apply Code Changes':'应用代码更改','No code changes detected.':'未检测到代码更改。','Applying code changes to application: \u0001':'正在将代码更改应用到应用：\u0001','No code changes detected. The ollowing APK(s) are not installed: \u0001':'未检测到代码更改，未安装以下 APK：\u0001'},
'run.AndroidRunConfigurationExecutor$run$1': {'Launching':'正在启动','Launching on devices':'正在设备上启动'},
'run.AndroidRunConfigurationExecutor$debug$1': {'Cannot launch a debug session on more than 1 device.':'无法同时在多台设备上启动调试会话。','Launching':'正在启动','Launching on devices':'正在设备上启动','Restoring app data':'正在恢复应用数据','Connecting debugger':'正在连接调试器'},
'run.AndroidRunConfigurationExecutor$applyChanges$1': {'Applying changes to':'正在应用更改至','Launching on devices':'正在设备上启动'},
'avdmanager.AvdManagerConnection$continueToStartAvd$2$progressJob$1': {'Launching emulator':'正在启动模拟器'}
})
translations.update({
'execution.common.applychanges.ApplyChangesAction': {'Apply Changes and Restart Activity':'应用更改并重启 Activity','&Apply Changes and Restart Activity':'应用更改并重启 Activity(&A)','Attempt to apply resource and code changes and restart activity.':'尝试应用资源和代码更改，并重启 Activity。'},
'execution.common.applychanges.CodeSwapAction': {'Apply Code Changes':'应用代码更改','Apply Cod&e Changes':'应用代码更改(&E)','Attempt to apply only code changes without restarting anything.':'尝试仅应用代码更改，不重启应用或 Activity。'},
'streaming.actions.ZoomLevelIndicator': {'Zoom Level: \u0001':'缩放比例：\u0001'},
'ui.resourcemanager.ResourceExplorerToolFactory': {'Resource Manager':'资源管理器'},
'ui.resourcemanager.ResourceExplorerToolFactoryKt': {'Waiting for successful sync...':'正在等待项目同步成功…','Loading...':'正在加载…'},
'ui.resourcemanager.actions.OpenResourceManagerAction': {'Resource Manager':'资源管理器','Open the Resource Manager':'打开资源管理器'}
})
# 禁用原因只用于按钮标题和说明；原执行条件与 applicationId 检测保持不变。
translations['execution.common.applychanges.BaseAction']={
 '%s (disabled: %s)':'%s（不可用：%s）','%s is disabled for this device because %s.':'此设备无法使用“%s”，原因：%s。',
 'no configuration selected':'未选择运行配置','there is no configuration selected':'尚未选择运行配置',
 'unsupported configuration':'不支持此运行配置','the selected configuration is not supported':'不支持所选的运行配置',
 "can't detect applicationId":'无法识别 applicationId','unsupported execution target':'不支持此运行目标',
 'devices not connected':'设备未连接','the selected devices are not connected':'所选设备未连接',
 'device not authorized':'设备未授权','the selected device is not authorized':'所选设备尚未授权',
 'device not ready':'设备尚未就绪','the selected device is not ready yet':'所选设备尚未就绪',
 'incompatible device API level':'设备 API 级别不兼容','its API level is lower than 26':'设备的 API 级别低于 26',
 'app not detected':'未检测到应用','the app is not yet running or not debuggable':'应用尚未运行或不支持调试',
 'building and/or launching':'正在构建或启动','the selected configuration is currently building and/or launching':'所选配置正在构建或启动',
 'more than one executor is running':'多个执行器正在运行','more than one type of executor is running configuration':'多个类型的执行器正在运行此配置',
 'no runner available':'没有可用的运行器','there are no Program Runners available to run the given configuration (perhaps project needs a sync?)':'没有可用于运行此配置的程序运行器，项目可能需要同步'
}
translations['execution.common.applychanges.ApplyChangesAction'].update({'debug execution':'正在调试','it is currently not allowed during debugging':'当前不允许在调试期间执行此操作'})
translations['execution.common.applychanges.CodeSwapAction'].update({'test project':'测试配置','the selected configuration is a test configuration':'所选配置为测试配置'})
translations['logcat.filters.LogcatFilterColorSettingsPage']={'Logcat Filter':'Logcat 筛选器'}
translations['logcat.messages.LogcatDescriptorInfo']={'':''}  # 仅转换配色页显示名，不修改日志枚举或配色键。
translations['profilers.AndroidProfilerToolWindow']={'Home':'首页','Past Recordings':'历史录制'}
# 这些英文也可能是 XML/线程标识，仅修改已核对的 UI 类中的固定标签。
translations['npw.dynamicapp.ModuleDownloadConditions']={'+ device-feature':'+ 设备特性'}
translations['npw.dynamicapp.ModuleDownloadDeviceFeature']={'device-feature':'设备特性'}
translations['profilers.profilingconfig.CpuProfilingConfigurationsDialog$ProfilingConfigurable']={
 'Task Settings':'任务设置','CPU Recording Configurations':'CPU 录制配置',
 'Empty configuration names are not allowed. Please rename or delete them before continuing.':'配置名称不能为空，请先重命名或删除空名称配置。'}
translations['profilers.profilingconfig.CpuProfilingConfigurationsDialog$ProfilingConfigurable$ProfilingConfigurationsListCellRenderer']={' (API Level %d+)':'（API 级别 %d+）'}
with ZipFile(lib/'android.jar') as jar:
 for suffix,values in translations.items():
  name='com/android/tools/idea/'+suffix.replace('.','/')
  data=jar.read(name+'.class');sha=hashlib.sha256(data).hexdigest()
  for source,target in values.items():entries.append(dict(jar='android.jar',class_name=name,sha256=sha,source=source,target=target))
# 实验室页面的标题、说明和按钮均为显示参数；功能标记与控件测试 ID 保持原值。
with ZipFile(lib/'android.jar') as jar:
 for suffix,values in {
  'StudioLabsSettingsConfigurable': {
   'Studio Labs':'Studio 实验室',
   'Opt in to Studio Labs to get early access to experimental features.':'启用 Studio 实验室功能，抢先体验实验性功能。'},
  'StudioLabsSettingsConfigurable$Companion': {
   'Journeys':'Journeys（AI 自动化测试）',
   'Use the vision and reasoning capabilities of AI to convert natural language instructions into automated tests.\n\nAn IDE restart is required for this change to take effect.':'利用 AI 的视觉与推理能力，将自然语言指令转换为自动化测试。\n\n更改将在重启 IDE 后生效。',
   '(Test only) Prompt Library':'提示词库（仅供测试）',
   'Allows to store frequently used prompts for quick access. Optionally share prompts with other people working on a same project.':'保存常用提示词，方便快速使用；也可以与同一项目的协作者共享。',
   'Prompt Library settings':'提示词库设置'},
  'StudioLabsFeaturePanelUi': {'Enable':'启用','Disable':'停用'}
 }.items():
  name='com/android/tools/studio/labs/'+suffix
  sha=hashlib.sha256(jar.read(name+'.class')).hexdigest()
  for source,target in values.items(): entries.append(dict(jar='android.jar',class_name=name,sha256=sha,source=source,target=target))
# 登录页面只修改提示和功能说明，保留 OAuth scope、内部 key、URL 与授权行为。
for relative,classes in {
 'plugins/url-assistant/lib/url-assistant.jar': {'com/google/urlassistant/ui/AddUrlDialog': {'Activity':'目标 Activity'}},
 'plugins/gradle-declarative/lib/gradle-declarative.jar': {
  'com/android/tools/idea/gradle/dcl/lang/DeclarativeLanguage': {'Gradle Declarative Configuration Language':'Gradle 声明式配置语言'}
 },
 'plugins/sh-plugin/lib/intellij.sh.core.jar': {'com/intellij/sh/highlighter/ShColorPage': {'':''}},
 'plugins/android/lib/android.jar': {
  'com/android/build/output/LongDownloadsNotifier': {'Sync is taking a significant amount of time to download dependencies.':'同步过程中下载依赖项耗时较长。'},
  'com/android/build/output/LongDownloadsNotifier$notifyIfTimeElapsed$1$1': {'Open Sync tool window for details':'打开“同步”工具窗口查看详情'},
  'com/google/gct/login2/ui/GoogleLoginAction': {'Sign in to Google...':'登录 Google…','Initializing...':'正在初始化…'},
  'com/google/gct/login2/ui/StartupSignInDialog': {
   'Skip for now':'暂时跳过',
   'Sign in to \u0001':'登录 \u0001',
   'Sign in to your Google account to use Gemini and other Google services in \u0001.':'登录 Google 账号，即可在 \u0001 中使用 Gemini 和其他 Google 服务。'},
  'com/google/gct/login2/ui/ComposableSingletons$StartupSignInDialogKt': {'Sign in to your Google Account':'登录 Google 账号'},
  'com/google/gct/login2/settings/GoogleLoginApplicationSettingsConfigurableUi': {
   'Add new account':'添加新账号','Available integrations':'可用集成',
   '<html><center>Reauthorization required</center></html>':'<html><center>需要重新授权</center></html>',
   '<html><center>You need to refresh your authorization to see available services.</center></html>':'<html><center>请刷新授权以查看可用服务。</center></html>',
   'Refresh authorization':'刷新授权','More info':'更多信息','Allowed':'已允许','Allow':'允许',
   'No Google accounts added':'尚未添加 Google 账号',
   'Add a Google account to start using features like Gemini, Device':'添加 Google 账号，即可使用 Gemini、设备串流，',
   'Streaming, and App Quality Insights with Crashlytics.':'以及集成 Crashlytics 的应用质量洞察等功能。',
   'Add an account...':'添加账号…'},
  'com/android/tools/idea/vitals/VitalsLoginFeature': {
   'Go to Play Console':'前往 Play 管理中心',
   'See metrics and data about the apps in your Google Play Developer account. Used in Android Vitals integration in App Quality Insights.':'查看 Google Play 开发者账号中应用的指标和数据，用于应用质量洞察中的 Android Vitals 集成。'},
  'com/android/tools/idea/settingssync/SettingsSyncFeature': {
   'Sync your settings to your Google Account to keep them across computers and re-installs.':'将设置同步到 Google 账号，以便在不同电脑间或重装后保留这些设置。'},
  'com/android/tools/idea/settingssync/SettingsSyncFeature$settingsAction$1': {'Configure Backup and Sync':'配置备份与同步'}
 },
 'plugins/firebase/lib/firebase.jar': {
  'com/google/services/firebase/FirebaseLoginFeature': {
   'View and administer all your Firebase data and settings. \u0001':'查看和管理您的全部 Firebase 数据与设置。\u0001',
   'Used in Firebase Assistant, Crashlytics integration in App Quality Insights, and Device Streaming':'用于 Firebase 助手、应用质量洞察中的 Crashlytics 集成以及设备串流。',
   'Used in Firebase Assistant and Crashlytics integration in App Quality Insights':'用于 Firebase 助手和应用质量洞察中的 Crashlytics 集成。',
   'Go to Firebase Console':'前往 Firebase 控制台'}
 },
 'plugins/gemini/lib/aiplugin.jar': {
  'com/android/studio/ml/GeminiLoginFeature': {'Submit data to Google for AI-based developer assistance.':'向 Google 提交数据，以获得基于 AI 的开发辅助。'},
  'com/android/studio/ml/GeminiLoginFeature$settingsAction$1': {'Configure Gemini':'配置 Gemini'}
 }
}.items():
 with ZipFile(lib.parents[2]/relative) as jar:
  for name,values in classes.items():
   sha=hashlib.sha256(jar.read(name+'.class')).hexdigest()
   for source,target in values.items(): entries.append(dict(jar=relative,class_name=name,sha256=sha,source=source,target=target))
with ZipFile(lib/'adt-ui.jar') as jar:
 name='com/android/tools/adtui/ui/options/OptionsPanel'
 entries.append(dict(jar='adt-ui.jar',class_name=name,sha256=hashlib.sha256(jar.read(name+'.class')).hexdigest(),source='',target=''))
with ZipFile(lib/'adt-ui.jar') as jar:
 name='com/android/tools/adtui/compose/ComposableSingletons$ComposeWizardKt';sha=hashlib.sha256(jar.read(name+'.class')).hexdigest()
 for source,target in {'Cancel':'取消','Previous':'上一步'}.items():entries.append(dict(jar='adt-ui.jar',class_name=name,sha256=sha,source=source,target=target))
# 图标的内容描述用于屏幕阅读器；只替换已审核字符串，不改图标键和排序状态。
with ZipFile(lib/'adt-ui.jar') as jar:
 name='com/android/tools/adtui/compose/table/TableKt'
 sha=hashlib.sha256(jar.read(name+'.class')).hexdigest()
 for source,target in {'Sorted ascending':'按升序排列','Sorted descending':'按降序排列'}.items():
  entries.append(dict(jar='adt-ui.jar',class_name=name,sha256=sha,source=source,target=target))
with ZipFile(lib.parents[2]/'lib/intellij.platform.jewel.ui.jar') as jar:
 name='org/jetbrains/jewel/ui/component/DropdownKt'
 sha=hashlib.sha256(jar.read(name+'.class')).hexdigest()
 entries.append(dict(jar='intellij.platform.jewel.ui.jar',class_name=name,sha256=sha,source='Dropdown Chevron',target='展开下拉菜单'))
with ZipFile(lib/'wizard-template.jar') as jar:
 for suffix in ['BuildConfigurationLanguageForNewProject','BuildConfigurationLanguageForNewModule']:
  name='com/android/tools/idea/wizard/template/'+suffix
  sha=hashlib.sha256(jar.read(name+'.class')).hexdigest()
  for source,target in {'Kotlin DSL (build.gradle.kts) [Recommended]':'Kotlin DSL (build.gradle.kts) [推荐]','Declarative DSL (build.gradle.dcl)':'声明式 DSL (build.gradle.dcl)'}.items():
   entries.append(dict(jar='wizard-template.jar',class_name=name,sha256=sha,source=source,target=target))

# 性能分析器的独立 Compose 文案；只登记已审核的显示类，不替换任务枚举或采集配置。
with ZipFile(lib/'android-profilers.jar') as jar:
 name='com/android/tools/profilers/cpu/CpuFrameTooltipView'
 sha=hashlib.sha256(jar.read(name+'.class')).hexdigest()
 entries.append(dict(jar='android-profilers.jar',class_name=name,sha256=sha,source='RenderThread',target='渲染线程（RenderThread）'))
with ZipFile(lib/'android-profilers.jar') as jar:
 for name,values in {'com/android/tools/profilers/taskbased/common/buttons/EnterTaskButtonKt': {'Start anyway': '仍然开始',
                                                                            'Start profiler task': '开始性能分析任务',
                                                                            'Open profiler task': '打开性能分析任务'},
 'com/android/tools/profilers/taskbased/common/constants/strings/TaskBasedUxStrings': {'Analyze Memory Usage': '分析内存使用情况',
                                                                                       'View Live Telemetry': '查看实时性能数据',
                                                                                       'Capture System Activities': '采集系统活动',
                                                                                       'Track Memory Consumption': '跟踪内存消耗',
                                                                                       'Find Memory Leaks with LeakCanary': '使用 '
                                                                                                                            'LeakCanary '
                                                                                                                            '查找内存泄漏',
                                                                                       'Find CPU Hotspots': '查找 CPU '
                                                                                                            '性能热点',
                                                                                       'Live Telemetry': '实时性能数据',
                                                                                       'Method Trace': '方法跟踪',
                                                                                       'Stack Sampling': '调用栈采样',
                                                                                       'Callstack Sample': '调用栈采样',
                                                                                       'No valid device is selected': '未选择有效设备',
                                                                                       'No valid process is selected': '未选择有效进程',
                                                                                       'No valid task is selected': '未选择有效任务',
                                                                                       "Tasks configured to run at process start require your app's process to be selected": '在进程启动时运行任务，需要先选择应用进程。',
                                                                                       "The selected task does not support 'Start profiler task from process start'": '所选任务不支持在进程启动时开始性能分析。',
                                                                                       "The API level is too low to 'Start profiler task from process start'": 'API '
                                                                                                                                                               '级别过低，无法在进程启动时开始性能分析。',
                                                                                       "The API level is too low to 'Start profiler task from now'": 'API '
                                                                                                                                                     '级别过低，无法立即开始性能分析。',
                                                                                       "'Start profiler task from now' requires a running process": '立即开始性能分析需要一个正在运行的进程。',
                                                                                       'The selected device is offline': '所选设备已离线',
                                                                                       "'Start profiler task from now' requires a running debuggable process": '立即开始性能分析需要一个正在运行且可调试的进程。',
                                                                                       'No task starting point selected': '未选择任务起始时机',
                                                                                       "Studio's LeakCanary integration library not detected in the process. Please try 'Start profiler task from process start'.": '未在进程中检测到 '
                                                                                                                                                                                                                    'Studio '
                                                                                                                                                                                                                    '的 '
                                                                                                                                                                                                                    'LeakCanary '
                                                                                                                                                                                                                    '集成库，请尝试在进程启动时开始性能分析。',
                                                                                       "Studio's LeakCanary integration library not detected in the process. Please add the 'studio-leakcanary' dependency to the app's build.": '未在进程中检测到 '
                                                                                                                                                                                                                                 'Studio '
                                                                                                                                                                                                                                 '的 '
                                                                                                                                                                                                                                 'LeakCanary '
                                                                                                                                                                                                                                 '集成库，请在应用的构建配置中添加 '
                                                                                                                                                                                                                                 'studio-leakcanary '
                                                                                                                                                                                                                                 '依赖。',
                                                                                       'Checking LeakCanary presence...': '正在检查 '
                                                                                                                          'LeakCanary…',
                                                                                       'Connection timeout: The app may be running in the background. Bring the app to the foreground and reselect the process to retry.': '连接超时：应用可能正在后台运行。请将应用切换到前台，重新选择进程后重试。',
                                                                                       "Studio's LeakCanary integration library failed to attach. Required internal APIs are missing from the compiled app.": '无法附加 '
                                                                                                                                                                                                              'Studio '
                                                                                                                                                                                                              '的 '
                                                                                                                                                                                                              'LeakCanary '
                                                                                                                                                                                                              '集成库：编译后的应用缺少所需的内部 '
                                                                                                                                                                                                              'API。',
                                                                                       'Cannot start the selected task while a debugger is attached to the process.': '调试器已附加到该进程，无法启动所选任务。',
                                                                                       'LeakCanary profiling is not supported for apps containing Private Compute Core (PCC) components.': '包含 '
                                                                                                                                                                                           'Private '
                                                                                                                                                                                           'Compute '
                                                                                                                                                                                           'Core（PCC）组件的应用不支持 '
                                                                                                                                                                                           'LeakCanary '
                                                                                                                                                                                           '性能分析。',
                                                                                       'This task cannot be run in this configuration': '此任务无法在当前配置下运行',
                                                                                       'Yes': '是',
                                                                                       'No': '否',
                                                                                       'Unknown': '未知',
                                                                                       'Examine memory footprint to investigate issues such as leaks, duplicated bitmaps, and large object retention.': '检查内存占用，排查内存泄漏、重复位图和大对象滞留等问题。',
                                                                                       'Monitor live CPU and memory behavior to identify anomalies or determine if a deeper recording is needed.': '实时监控 '
                                                                                                                                                                                                   'CPU '
                                                                                                                                                                                                   '和内存表现，识别异常并判断是否需要进一步录制分析。',
                                                                                       'Identify performance bottlenecks with low overhead and precise timing. Supports default and custom instrumentation.': '以较低开销和精确计时定位性能瓶颈，支持默认和自定义插桩。',
                                                                                       'Track allocation patterns and memory consumption to optimize footprint and surface potential leaks.': '跟踪内存分配模式和消耗，优化内存占用并发现潜在泄漏。',
                                                                                       'Narrow down the root cause of memory leaks with a leak trace.': '通过泄漏引用链定位内存泄漏的根因。',
                                                                                       'Analyze exact execution paths and per-method timing for Java/Kotlin code. Adds significant overhead and may skew timing measurements.': '分析 '
                                                                                                                                                                                                                                'Java/Kotlin '
                                                                                                                                                                                                                                '代码的精确执行路径及各方法耗时。此操作会产生较大开销，可能影响计时结果。',
                                                                                       'Find hotspots and see where threads spend their time across Java/Kotlin and C++ code.': '查找性能热点，了解线程在 '
                                                                                                                                                                                'Java/Kotlin '
                                                                                                                                                                                '和 '
                                                                                                                                                                                'C++ '
                                                                                                                                                                                '代码中的耗时分布。',
                                                                                       'Monitor native memory usage and identify consumption or leaks in C++ code.': '监控原生内存使用情况，定位 '
                                                                                                                                                                     'C++ '
                                                                                                                                                                     '代码中的内存消耗和泄漏。',
                                                                                       'Dumps the heap showing which objects in your app are using memory at the time of capture': '生成堆转储，查看采集时应用中哪些对象占用了内存。',
                                                                                       'Displays and records a streaming timeline of CPU usage and memory footprint': '显示并录制 '
                                                                                                                                                                      'CPU '
                                                                                                                                                                      '使用率和内存占用的实时时间线。',
                                                                                       'Captures a trace that can help you understand how your app interacts with system resources': '采集跟踪记录，了解应用与系统资源的交互情况。',
                                                                                       'Records Java and Kotlin memory allocations': '录制 '
                                                                                                                                     'Java '
                                                                                                                                     '和 '
                                                                                                                                     'Kotlin '
                                                                                                                                     '内存分配。',
                                                                                       'Pulls memory leaks detected by LeakCanary from an Android device': '从 '
                                                                                                                                                           'Android '
                                                                                                                                                           '设备获取 '
                                                                                                                                                           'LeakCanary '
                                                                                                                                                           '检测到的内存泄漏。',
                                                                                       'Captures the call stacks during your app’s Java/Kotlin code execution': '采集应用执行 '
                                                                                                                                                                'Java/Kotlin '
                                                                                                                                                                '代码时的调用栈。',
                                                                                       "Uses sampling to capture the call stacks of an app's native and Java/Kotlin code": '通过采样获取应用原生代码和 '
                                                                                                                                                                           'Java/Kotlin '
                                                                                                                                                                           '代码的调用栈。',
                                                                                       'Captures native memory allocations': '采集原生内存分配。',
                                                                                       'Captures all live objects on the Java/Kotlin heap at a single point in time.': '采集某一时刻 '
                                                                                                                                                                       'Java/Kotlin '
                                                                                                                                                                       '堆中的全部存活对象。',
                                                                                       'Displays a real-time timeline of CPU and memory usage as you interact with the app.': '在操作应用时，实时显示 '
                                                                                                                                                                              'CPU '
                                                                                                                                                                              '和内存使用情况的时间线。',
                                                                                       'Records a system-wide Perfetto trace of CPU, threads, frame timing, and other system events.': '使用 '
                                                                                                                                                                                       'Perfetto '
                                                                                                                                                                                       '录制系统范围的 '
                                                                                                                                                                                       'CPU、线程、帧耗时等系统事件。',
                                                                                       'Records each Java/Kotlin object allocation along with its allocating stack.': '记录每次 '
                                                                                                                                                                      'Java/Kotlin '
                                                                                                                                                                      '对象分配及其调用栈。',
                                                                                       'Detects and reports memory leaks using LeakCanary in debug builds.': '在调试构建中使用 '
                                                                                                                                                             'LeakCanary '
                                                                                                                                                             '检测并报告内存泄漏。',
                                                                                       'Records every Java/Kotlin method call with full entry and exit detail.': '记录每次 '
                                                                                                                                                                 'Java/Kotlin '
                                                                                                                                                                 '方法调用，包括完整的进入和退出信息。',
                                                                                       'Periodically samples call stacks across Java/Kotlin and C++ code using simpleperf.': '使用 '
                                                                                                                                                                             'simpleperf '
                                                                                                                                                                             '定期采样 '
                                                                                                                                                                             'Java/Kotlin '
                                                                                                                                                                             '和 '
                                                                                                                                                                             'C++ '
                                                                                                                                                                             '代码的调用栈。',
                                                                                       'Captures net native memory allocations over the recording period.': '采集录制期间的原生内存净分配量。'},
 'com/android/tools/profilers/taskbased/common/table/TableRowsKt': {'Preferred process': '首选进程'},
 'com/android/tools/profilers/taskbased/tabs/home/processlist/ProcessTableKt': {'Profileable': '可分析性能',
                                                                                'Debuggable': '可调试',
                                                                                'Not running': '未运行',
                                                                                'Process name': '进程名称',
                                                                                'Manifest Configuration': '清单配置'},
 'com/android/tools/profilers/taskbased/tabs/home/processlist/deviceselection/common/DeviceSelectionContentKt': {'Not running': '未运行',
                                                                                                                 'No Devices': '无设备',
                                                                                                                 'Loading': '正在加载'},
 'com/android/tools/profilers/taskbased/tabs/pastrecordings/recordinglist/ComposableSingletons$RecordingActionGroupKt': {'Import recording': '导入录制',
                                                                                                                         'Export recording': '导出录制',
                                                                                                                         'Delete recording': '删除录制'},
 'com/android/tools/profilers/taskbased/tabs/pastrecordings/recordinglist/RecordingListKt': {'No recordings': '暂无录制',
                                                                                             'To add a new recording, start a new task or import a previously captured one.': '开始新任务或导入已有录制，即可添加录制记录。'},
 'com/android/tools/profilers/taskbased/tabs/taskgridandbars/taskbars/ComposableSingletons$TaskTitleBarKt': {'Task Configurations': '任务配置'},
 'com/android/tools/profilers/taskbased/tabs/taskgridandbars/taskbars/ComposableSingletons$TaskTitleBarKt$lambda$-1049838307$1': {'Task Configurations': '任务配置'},
 'com/android/tools/profilers/taskbased/tabs/taskgridandbars/taskbars/TaskTitleBarKt': {'Tasks': '任务'},
 'com/android/tools/profilers/taskbased/tabs/taskgridandbars/taskbars/notifications/StartTaskErrorKt': {'Task start selection error': '任务启动选项有误',
                                                                                                        "You can restart the process as 'debuggable' by clicking 'Profiler: Run as debuggable (complete data)' in the main toolbar's more actions menu. This will trigger a rebuild.": '在主工具栏的“更多操作”菜单中点击“性能分析器：以可调试模式运行（完整数据）”，可重新构建并以可调试模式重启进程。'},
 'com/android/tools/profilers/taskbased/tabs/taskgridandbars/taskbars/options/TaskStartingPointDropdownKt': {'Start profiler task from': '性能分析起始时机',
                                                                                                             'Now': '现在',
                                                                                                             'attaches to selected process': '附加到所选进程',
                                                                                                             'Process start': '进程启动时',
                                                                                                             'restarts process': '重启进程',
                                                                                                             'starts process': '启动进程',
                                                                                                             'Option unavailable': '选项不可用',
                                                                                                             'Not specified': '未指定'},
 'com/android/tools/profilers/taskbased/tabs/taskgridandbars/taskgrid/TaskGridItemKt': {'': ''},
 'com/android/tools/profilers/taskbased/tabs/taskgridandbars/taskgrid/TaskGridItemV2Kt': {'': ''}}.items():
  sha=hashlib.sha256(jar.read(name+'.class')).hexdigest()
  for source,target in values.items(): entries.append(dict(jar='android-profilers.jar',class_name=name,sha256=sha,source=source,target=target))
# 录制提示条仅改文字与无障碍描述，不改变关闭或不再提示的回调。
with ZipFile(lib/'android-profilers.jar') as jar:
 for name,values in {
  'com/android/tools/profilers/taskbased/tabs/pastrecordings/banner/PastRecordingsBannerKt': {
   'Info':'提示',
   'Profiler recordings are not preserved across Android Studio restarts. If the task supports exporting you can export the recording to preserve it.':'重启 Android Studio 后，性能分析录制记录不会保留。若任务支持导出，请导出录制记录以便保存。',
   "Don't show again":'不再显示'},
  'com/android/tools/profilers/taskbased/tabs/pastrecordings/banner/ComposableSingletons$PastRecordingsBannerKt$lambda$-1429815136$1': {'Close':'关闭'}
 }.items():
  sha=hashlib.sha256(jar.read(name+'.class')).hexdigest()
  for source,target in values.items(): entries.append(dict(jar='android-profilers.jar',class_name=name,sha256=sha,source=source,target=target))
# 调用点已核对：模拟器加载提示和检查器显示名，不触碰 inspectorId 或启动参数。
with ZipFile(lib/'android.jar') as jar:
 for name,values in {
  # 只改此视图的面板标题和 UrlData 文案；树根 Databases 是其他类的模型身份，保留。
  'com/android/tools/idea/sqlite/ui/mainView/DatabaseInspectorViewImpl': {'Databases':'数据库','Learn more':'了解详情'},
  # 四处分组文字仅传给 Separator，不参与设备身份或连接逻辑。
  'com/android/tools/idea/streaming/core/StreamingToolWindowManager': {'Connected Devices':'已连接设备','Reserved Remote Devices':'已预留的远程设备','Remote Devices':'远程设备','Virtual Devices':'虚拟设备'},
  'com/android/tools/idea/streaming/emulator/EmulatorDisplayPanel': {'Connecting to the Emulator':'正在连接模拟器'},
  'com/android/tools/idea/streaming/emulator/EmulatorView': {'Connecting to the Emulator':'正在连接模拟器'},
  'com/android/tools/idea/appinspection/inspectors/network/ide/NetworkInspectorTabProvider': {'Network Inspector':'网络检查器'},
  'com/android/tools/idea/appinspection/inspectors/backgroundtask/ide/BackgroundTaskInspectorTabProvider': {'Background Task Inspector':'后台任务检查器'}
 }.items():
  sha=hashlib.sha256(jar.read(name+'.class')).hexdigest()
  for source,target in values.items(): entries.append(dict(jar='android.jar',class_name=name,sha256=sha,source=source,target=target))
# 网络检查器仅翻译 UI 文案与列 displayString；保留枚举名、卡片路由和请求数据。
with ZipFile(lib/'android.jar') as jar:
 for name,values in {'com/android/tools/idea/appinspection/inspectors/network/view/NetworkInspectorTab': {'Export connections': '导出连接记录', 'Export Connections': '导出连接记录', 'Clear data': '清空数据', 'Zoom out': '缩小', 'Zoom in': '放大', 'Reset zoom': '重置缩放', 'Zoom to selection': '缩放到选区', 'Attach to live': '跟随实时数据', 'Detach live': '停止跟随实时数据'}, 'com/android/tools/idea/appinspection/inspectors/network/view/NetworkInspectorView': {'Connection View': '连接视图', 'Thread View': '线程视图', 'Rules': '规则', 'Network inspector data unavailable': '网络检查器数据不可用', "There is no information for the network traffic you've selected.": '所选网络流量没有可用信息。', 'Learn More': '了解详情', 'Selected range': '所选范围'}, 'com/android/tools/idea/appinspection/inspectors/network/view/details/OverviewTabContent': {'Overview': '概览'}, 'com/android/tools/idea/appinspection/inspectors/network/view/connectionsview/ConnectionColumn$NAME': {'Name': '名称'}, 'com/android/tools/idea/appinspection/inspectors/network/view/connectionsview/ConnectionColumn$PATH': {'Path': '路径'}, 'com/android/tools/idea/appinspection/inspectors/network/view/connectionsview/ConnectionColumn$HOST': {'Host': '主机'}, 'com/android/tools/idea/appinspection/inspectors/network/view/connectionsview/ConnectionColumn$METHOD': {'Method': '请求方法'}, 'com/android/tools/idea/appinspection/inspectors/network/view/connectionsview/ConnectionColumn$SCHEME': {'Scheme': '协议方案'}, 'com/android/tools/idea/appinspection/inspectors/network/view/connectionsview/ConnectionColumn$TRANSPORT': {'Transport': '传输协议'}, 'com/android/tools/idea/appinspection/inspectors/network/view/connectionsview/ConnectionColumn$REQUEST_SIZE': {'Request Size': '请求大小'}, 'com/android/tools/idea/appinspection/inspectors/network/view/connectionsview/ConnectionColumn$SIZE': {'Size': '大小'}, 'com/android/tools/idea/appinspection/inspectors/network/view/connectionsview/ConnectionColumn$REQUEST_HEADERS': {'Request Headers': '请求标头'}, 'com/android/tools/idea/appinspection/inspectors/network/view/connectionsview/ConnectionColumn$RESPONSE_HEADERS': {'Response Headers': '响应标头'}, 'com/android/tools/idea/appinspection/inspectors/network/view/connectionsview/ConnectionColumn$TYPE': {'Type': '类型'}, 'com/android/tools/idea/appinspection/inspectors/network/view/connectionsview/ConnectionColumn$STATUS': {'Status': '状态'}, 'com/android/tools/idea/appinspection/inspectors/network/view/connectionsview/ConnectionColumn$TIME': {'Time': '时间'}, 'com/android/tools/idea/appinspection/inspectors/network/view/connectionsview/ConnectionColumn$REQUEST_TIME': {'Request Time': '请求时间'}, 'com/android/tools/idea/appinspection/inspectors/network/view/connectionsview/ConnectionColumn$RESPONSE_TIME': {'Response Time': '响应时间'}, 'com/android/tools/idea/appinspection/inspectors/network/view/connectionsview/ConnectionColumn$TIMELINE': {'Timeline': '时间线'}}.items():
  sha=hashlib.sha256(jar.read(name+'.class')).hexdigest()
  for source,target in values.items(): entries.append(dict(jar='android.jar',class_name=name,sha256=sha,source=source,target=target))
# 请求详情仅转换标题、标签与操作名称，HTTP/Proto 内容和剪贴板命令保持原值。
with ZipFile(lib/'android.jar') as jar:
 for name,values in {'com/android/tools/idea/appinspection/inspectors/network/view/details/OverviewTabContent$Companion': {'Request': '请求',
                                                                                                       'Method': '请求方法',
                                                                                                       'Status': '状态',
                                                                                                       'Request type': '请求类型',
                                                                                                       'Request size': '请求大小',
                                                                                                       'Response type': '响应类型',
                                                                                                       'Response size': '响应大小',
                                                                                                       'Initiating thread': '发起线程',
                                                                                                       'Other threads': '其他线程',
                                                                                                       'Timing': '时序',
                                                                                                       'Sent': '发送',
                                                                                                       'Received': '接收'},
 'com/android/tools/idea/appinspection/inspectors/network/view/details/RequestTabContent': {'Request': '请求',
                                                                                            'Application Headers': '应用请求标头',
                                                                                            'Body': '正文'},
 'com/android/tools/idea/appinspection/inspectors/network/view/details/CallStackTabContent': {'Call Stack': '调用栈'},
 'com/android/tools/idea/appinspection/inspectors/network/view/details/GrpcDataComponentFactory': {'View Proto Text': '查看 '
                                                                                                                      'Proto '
                                                                                                                      '文本',
                                                                                                   'View Raw': '查看原始数据',
                                                                                                   'View Text': '查看文本',
                                                                                                   'Payload (Proto)': '载荷（Proto）',
                                                                                                   'Payload': '载荷'},
 'com/android/tools/idea/appinspection/inspectors/network/view/details/HttpDataComponentFactory': {'Body': '正文',
                                                                                                   'Not available': '不可用',
                                                                                                   'View Parsed': '查看解析结果',
                                                                                                   'View Source': '查看原文'},
 'com/android/tools/idea/appinspection/inspectors/network/view/details/HttpDataComponentFactory$Companion': {'Form Data': '表单数据'},
 'com/android/tools/idea/appinspection/inspectors/network/view/connectionsview/CopyAsCurlAction': {'Copy as cURL': '复制为 '
                                                                                                                   'cURL '
                                                                                                                   '命令'},
 'com/android/tools/idea/appinspection/inspectors/network/view/connectionsview/CopyUrlAction': {'Copy URL': '复制 '
                                                                                                            'URL'}}.items():
  sha=hashlib.sha256(jar.read(name+'.class')).hexdigest()
  for source,target in values.items(): entries.append(dict(jar='android.jar',class_name=name,sha256=sha,source=source,target=target))
# 规则编辑器仅翻译标题、控件标签和提示；不修改规则数据、用户输入或匹配表达式。
with ZipFile(lib/'android.jar') as jar:
 for name,values in {'com/android/tools/idea/appinspection/inspectors/network/view/details/BodyRuleDialog': {'Body Rule': '正文规则', 'Find by': '查找条件', 'Replace entire body': '替换整个正文', 'Replace with': '替换为'}, 'com/android/tools/idea/appinspection/inspectors/network/view/details/HeaderRuleDialog': {'Add new header': '添加标头', 'Edit existing header': '编辑现有标头', 'Find by': '查找条件', 'Header Rule': '标头规则', 'Header name matching is case insensitive. Regex that selects for case will have the case selection ignored': '标头名称匹配不区分大小写，正则表达式中的大小写匹配设置也会被忽略。', 'Header name:': '标头名称：', 'Header value match is case sensitive': '标头值匹配区分大小写。', 'Header value:': '标头值：', 'New header name:': '新标头名称：', 'Please provide a non-empty header name': '请输入非空的标头名称', 'Please select a header name or value to replace': '请选择要替换的标头名称或值', 'Replace with': '替换为'}, 'com/android/tools/idea/appinspection/inspectors/network/view/details/RuleTableWithWarning': {'Order of rules indicate execution order.': '规则按列表顺序执行。'}, 'com/android/tools/idea/appinspection/inspectors/network/view/rules/RulesTableView$CloneRuleAction': {'Clone': '复制', 'Clone rule': '复制规则'}, 'com/android/tools/idea/appinspection/inspectors/network/view/rules/RulesTableView$OpenRuleVariablesAction': {'Variables': '变量', 'Define variables': '定义变量'}}.items():
  sha=hashlib.sha256(jar.read(name+'.class')).hexdigest()
  for source,target in values.items(): entries.append(dict(jar='android.jar',class_name=name,sha256=sha,source=source,target=target))
# 动态校验提示仅替换显示配方，保留参数占位符和 ${RULE_NAME} 变量语法。
with ZipFile(lib/'android.jar') as jar:
 for name,values in {'com/android/tools/idea/appinspection/inspectors/network/view/details/RuleDetailsView': {'Apply rule for status:': '对以下状态码应用规则：', 'Body rules': '正文规则', 'Enter rule name': '输入规则名称', 'Header rules': '标头规则', 'Host URL:': '主机 URL：', 'Invalid variables: ': '无效变量：', 'Port should be an integer between 0 and 65535': '端口必须是 0 到 65535 之间的整数', 'Replace with status code:': '替换为状态码：', 'Rule Details': '规则详情', 'Rule name cannot be blank': '规则名称不能为空', "Rule named '\x01' already exists": '名为“\x01”的规则已存在', 'Status code should be an integer between 100 and 599': '状态码必须是 100 到 599 之间的整数', 'URL is malformed': 'URL 格式不正确', 'Name:': '名称：', 'Response': '响应', 'Origin': '请求来源', 'Protocol:': '协议：', 'Port:': '端口：', 'Path:': '路径：', 'Query:': '查询参数：', 'Method:': '请求方法：'}, 'com/android/tools/idea/appinspection/inspectors/network/view/rules/RuleVariablesDialog': {'Rule Variables': '规则变量', "Rule variables can be used in rules by referencing them with '${RULE_NAME}' ": "在规则中使用 '${RULE_NAME}' 格式引用规则变量", 'Duplicate variable names: \x01': '变量名称重复：\x01', 'Name': '名称', 'Value': '值'}}.items():
  sha=hashlib.sha256(jar.read(name+'.class')).hexdigest()
  for source,target in values.items(): entries.append(dict(jar='android.jar',class_name=name,sha256=sha,source=source,target=target))
# 后台任务约束在 convert 中仅生成 JBLabel，不修改任务约束或协议数据。
with ZipFile(lib/'android.jar') as jar:
 for name,values in {'com/android/tools/idea/appinspection/inspectors/backgroundtask/view/BackgroundTaskEntriesView$FilterWithTagToggleAction': {'All tags': '所有标签'}, 'com/android/tools/idea/appinspection/inspectors/backgroundtask/view/EnqueuedAtProvider': {'Unavailable': '不可用', 'Enqueue location is only known for workers started after opening the inspector.': '只有在打开检查器后启动的工作任务，才能显示其入队位置。'}, 'com/android/tools/idea/appinspection/inspectors/backgroundtask/view/WorkConstraintProvider': {'Network must be metered': '需要按流量计费的网络', 'Network must be unmetered': '需要不按流量计费的网络', 'Network must not be roaming': '需要非漫游网络', 'Requires battery not low': '需要电量充足', 'Requires charging': '需要正在充电', 'Requires idle device': '需要设备处于空闲状态', 'Requires storage not low': '需要存储空间充足', 'Network must be connected': '需要连接网络', 'Network must be recognized': '需要可识别的网络'}, 'com/android/tools/idea/appinspection/inspectors/backgroundtask/view/JobConstraintProvider': {'Network must be metered': '需要按流量计费的网络', 'Network must be unmetered': '需要不按流量计费的网络', 'Network must not be roaming': '需要非漫游网络', 'Requires battery not low': '需要电量充足', 'Requires charging': '需要正在充电', 'Requires idle device': '需要设备处于空闲状态', 'Requires storage not low': '需要存储空间充足'}}.items():
  sha=hashlib.sha256(jar.read(name+'.class')).hexdigest()
  for source,target in values.items(): entries.append(dict(jar='android.jar',class_name=name,sha256=sha,source=source,target=target))
# 任务详情仅替换字段标签和导航链接，字段值继续来自原始任务模型。
with ZipFile(lib/'android.jar') as jar:
 for name,values in {'com/android/tools/idea/appinspection/inspectors/backgroundtask/view/EntryDetailsView': {'Task Details': '任务详情', 'Interval time': '间隔时间', 'Trigger time': '计划触发时间', 'Window time': '触发时间窗口', 'Listener tag': '监听器标签', 'Time started': '开始时间', 'Time fired': '实际触发时间', 'Time cancelled': '取消时间', 'Elapsed time': '耗时', 'Related Worker': '关联的工作任务', 'Needs reschedule': '需要重新调度', 'Related Job': '关联的调度任务', 'Unique work chain': '唯一工作链', 'Run count': '运行次数', 'Output data': '输出数据', 'Component package': '组件包名', 'Component class': '组件类名', 'Request code': '请求码', 'Enqueued by': '入队调用位置', 'Time completed': '完成时间', 'Show in graph': '在图中显示', 'Show in table': '在表格中显示', 'Callstacks': '调用栈'}}.items():
  sha=hashlib.sha256(jar.read(name+'.class')).hexdigest()
  for source,target in values.items(): entries.append(dict(jar='android.jar',class_name=name,sha256=sha,source=source,target=target))
# 详情中的频率和调用栈标题只用于显示；保留大写状态及 setName 内部标识。
with ZipFile(lib/'android.jar') as jar:
 for name,values in {'com/android/tools/idea/appinspection/inspectors/backgroundtask/view/EntryDetailsView': {'Type': '类型', 'Exact': '精确触发', 'Description': '说明', 'Results': '结果', 'Set': '已设置', 'Cancelled': '已取消', 'Pending Intent:': '待执行意图（PendingIntent）：', 'Intent:': '意图（Intent）：', 'Creator': '创建者', 'Flags': '标志', 'Action': '操作', 'Data': '数据', 'Categories': '类别', 'Identifier': '标识符', 'Package': '包名', 'Tag': '标签', 'Level': '级别', 'Acquired': '已获取', 'Released': '已释放', 'Service': '服务', 'Constraints': '约束条件', 'Frequency': '执行频率', 'Periodic': '周期执行', 'OneTime': '一次性执行', 'State': '状态', 'Execution': '执行情况', 'Reschedule': '重新调度', 'Scheduled': '已调度', 'Finished': '已完成', 'Class': '类', 'Tags': '标签', 'WorkContinuation': '工作任务链', 'Previous': '前置任务', 'Next': '后续任务'}}.items():
  sha=hashlib.sha256(jar.read(name+'.class')).hexdigest()
  for source,target in values.items(): entries.append(dict(jar='android.jar',class_name=name,sha256=sha,source=source,target=target))
# 状态翻译仅登记两个显示调用方，原始模型类不进入变换名单。
with ZipFile(lib/'android.jar') as jar:
 for suffix in ['StateProvider','table/BackgroundTaskTreeTableView','table/BackgroundTaskTreeTableView$HeaderRenderer']:
  name='com/android/tools/idea/appinspection/inspectors/backgroundtask/view/'+suffix
  entries.append(dict(jar='android.jar',class_name=name,sha256=hashlib.sha256(jar.read(name+'.class')).hexdigest(),source='',target=''))
# 布局检查器菜单仅翻译动作标题，不改所选视图及显示/隐藏行为。
with ZipFile(lib/'android.jar') as jar:
 for name,values in {'com/android/tools/idea/layoutinspector/common/BrowseChromeDevToolsAction': {'Open Chrome DevTools': '打开 Chrome 开发者工具'}, 'com/android/tools/idea/layoutinspector/common/ShowAllAction': {'Show All': '显示全部'}, 'com/android/tools/idea/layoutinspector/common/HideSubtreeAction': {'Hide Subtree': '隐藏子树'}, 'com/android/tools/idea/layoutinspector/common/ShowOnlySubtreeAction': {'Show Only Subtree': '仅显示子树'}, 'com/android/tools/idea/layoutinspector/common/ShowOnlyParentsAction': {'Show Only Parents': '仅显示父级视图'}, 'com/android/tools/idea/layoutinspector/common/ShowSubtreeAction': {'Show Subtree': '显示子树'}, 'com/android/tools/idea/layoutinspector/common/ViewContextMenuFactoryKt$showViewContextMenu$group$1': {'Select View': '选择视图'}}.items():
  sha=hashlib.sha256(jar.read(name+'.class')).hexdigest()
  for source,target in values.items(): entries.append(dict(jar='android.jar',class_name=name,sha256=sha,source=source,target=target))
# 通知只翻译显示内容；保留 Layout Inspector 工具窗口ID与 stop 链接命令。
with ZipFile(lib/'android.jar') as jar:
 for name,values in {'com/android/tools/idea/layoutinspector/LayoutInspector': {'Inspector Error': '布局检查器错误'}, 'com/android/tools/idea/layoutinspector/LayoutInspectorToolWindowManagerListener': {'<b>Layout Inspection</b> is running in the background.<br>\nYou can either <a href="stop">stop</a> it, or leave it running and resume your session later.': '<b>布局检查</b>正在后台运行。<br>\n您可以<a href="stop">停止</a>检查，也可以保持运行，稍后继续此会话。'}, 'com/android/tools/idea/layoutinspector/pipeline/InspectorClientLaunchMonitor': {'Continue Waiting': '继续等待', 'Disconnect': '断开连接', 'Resume Debugger': '恢复调试器运行'}}.items():
  sha=hashlib.sha256(jar.read(name+'.class')).hexdigest()
  for source,target in values.items(): entries.append(dict(jar='android.jar',class_name=name,sha256=sha,source=source,target=target))
# 重组观察菜单只翻译动作显示名称，观察状态与节点身份保持不变。
with ZipFile(lib/'android.jar') as jar:
 for name,values in {'com/android/tools/idea/layoutinspector/recompositions/ObserveNodeAction': {'Observe Node': '观察节点', 'Stop Observing Node': '停止观察节点'}, 'com/android/tools/idea/layoutinspector/recompositions/ObserveAllAction': {'Observe All': '观察全部节点'}, 'com/android/tools/idea/layoutinspector/recompositions/ObserveNoneAction': {'Observe None': '停止观察所有节点'}}.items():
  sha=hashlib.sha256(jar.read(name+'.class')).hexdigest()
  for source,target in values.items(): entries.append(dict(jar='android.jar',class_name=name,sha256=sha,source=source,target=target))
# 仅翻译发送到错误通知/气泡的固定说明，异常对象与源码定位逻辑不变。
with ZipFile(lib/'android.jar') as jar:
 for name,values in {'com/android/tools/idea/layoutinspector/pipeline/appinspection/AppInspectionInspectorClient': {'Failed to attach layout inspector agent.': '无法附加布局检查器代理。', 'Layout Inspector crashed on the device.': '设备上的布局检查器已崩溃。', 'An unknown error happened.': '发生未知错误。'}, 'com/android/tools/idea/layoutinspector/pipeline/appinspection/compose/LambdaParameterItem': {'Could not determine source location': '无法确定源代码位置', 'Could not determine exact source location': '无法确定准确的源代码位置'}}.items():
  sha=hashlib.sha256(jar.read(name+'.class')).hexdigest()
  for source,target in values.items(): entries.append(dict(jar='android.jar',class_name=name,sha256=sha,source=source,target=target))
# 工具栏只翻译标题、提示与文件选择器说明；设备/进程身份、文件类型和alpha值不变。
with ZipFile(lib/'android.jar') as jar:
 for name,values in {'com/android/tools/idea/layoutinspector/ui/toolbar/actions/RenderSettingsAction': {'View Options': '视图选项', 'Show Borders': '显示边框', 'Show Layout Bounds': '显示布局边界', 'Show View Label': '显示视图标签'}, 'com/android/tools/idea/layoutinspector/ui/toolbar/actions/HighlightColorAction': {'Recomposition Highlight Color': '重组高亮颜色'}, 'com/android/tools/idea/layoutinspector/ui/toolbar/actions/SelectDeviceAction': {'Select device': '选择设备', 'Select a device to connect to.': '选择要连接的设备。', 'No Device Available': '没有可用设备', 'No Device Selected': '尚未选择设备'}, 'com/android/tools/idea/layoutinspector/ui/toolbar/actions/SelectDeviceAction$DetachPresentation': {'Stop Inspector': '停止检查器', 'Stop running the layout inspector against the current device.': '停止对当前设备运行布局检查器。'}, 'com/android/tools/idea/layoutinspector/ui/toolbar/actions/SingleDeviceSelectProcessAction': {'Select Process': '选择进程', 'Select a process to connect to.': '选择要连接的进程。'}, 'com/android/tools/idea/layoutinspector/ui/toolbar/actions/TargetSelectionActionFactory': {'Stop Inspector': '停止检查器', 'Stop running the layout inspector against the current process': '停止对当前进程运行布局检查器'}, 'com/android/tools/idea/layoutinspector/ui/toolbar/actions/ToggleOverlayAction': {'Clear Overlay': '清除叠加图像', 'Load Overlay': '加载叠加图像', 'Choose Overlay': '选择叠加图像', 'Image files': '图像文件'}, 'com/android/tools/idea/layoutinspector/ui/toolbar/actions/AlphaSliderAction': {'Overlay Alpha:': '叠加图像不透明度：'}}.items():
  sha=hashlib.sha256(jar.read(name+'.class')).hexdigest()
  for source,target in values.items(): entries.append(dict(jar='android.jar',class_name=name,sha256=sha,source=source,target=target))
# 组件树与快照仅翻译界面文字，保留文件类型名称、li扩展名及模型行为。
with ZipFile(lib/'android.jar') as jar:
 for name,values in {'com/android/tools/idea/layoutinspector/tree/GotoDeclarationAction': {'Go To Declaration': '转到声明', 'Go To Declaration (No Source Information Found)': '转到声明（未找到源代码信息）'}, 'com/android/tools/idea/layoutinspector/tree/FilterGroupAction': {'View Options for Component Tree': '组件树视图选项'}, 'com/android/tools/idea/layoutinspector/tree/SystemNodeFilterAction': {'Filter System-Defined Layers': '过滤系统定义的图层'}, 'com/android/tools/idea/layoutinspector/tree/HighlightSemanticsAction': {'Highlight Semantics Layers': '高亮语义图层'}, 'com/android/tools/idea/layoutinspector/tree/CallstackAction': {'Show Compose as Callstack': '以调用栈形式显示 Compose'}, 'com/android/tools/idea/layoutinspector/tree/SupportLines': {'Show Support Lines': '显示辅助线'}, 'com/android/tools/idea/layoutinspector/tree/RecompositionCounts': {'Show Recomposition Counts': '显示重组次数', 'Show Recomposition Counts (No Source Information Found)': '显示重组次数（未找到源代码信息）', 'Show Recomposition Counts (Needs Compose 1.2.1+)': '显示重组次数（需要 Compose 1.2.1 或更高版本）'}, 'com/android/tools/idea/layoutinspector/tree/RootPanel': {'Waiting for Layout Inspector to connect.': '正在等待布局检查器连接。'}, 'com/android/tools/idea/layoutinspector/snapshots/SnapshotAction': {'Snapshot Export/Import': '导出/导入快照'}, 'com/android/tools/idea/layoutinspector/snapshots/ExportSnapshotAction': {'Export Snapshot': '导出快照', 'Export a snapshot of Layout inspector to share, inspect, and use offline.': '导出布局检查器快照，以便分享、检查和离线使用。', 'Save Layout Snapshot': '保存布局快照', 'Save layout inspector snapshot': '保存布局检查器快照', 'Saving snapshot': '正在保存快照'}, 'com/android/tools/idea/layoutinspector/snapshots/ImportSnapshotAction': {'Import Snapshot': '导入快照', 'Import a snapshot, open into an editor.': '导入快照并在编辑器中打开。', 'Load LayoutSnapshot': '加载布局快照', 'Load layout inspector snapshot': '加载布局检查器快照', 'Opening snapshot': '正在打开快照'}, 'com/android/tools/idea/layoutinspector/snapshots/LayoutInspectorFileType': {'Layout Inspector Snapshot': '布局检查器快照'}}.items():
  sha=hashlib.sha256(jar.read(name+'.class')).hexdigest()
  for source,target in values.items(): entries.append(dict(jar='android.jar',class_name=name,sha256=sha,source=source,target=target))
# 计数仅翻译可见标题和图标提示，列名与计数数据保持不变。
with ZipFile(lib/'android.jar') as jar:
 for name,values in {'com/android/tools/idea/layoutinspector/tree/LayoutInspectorTreePanel': {'Recomposition counts': '重组次数', 'Click to reset recomposition counts': '点击重置重组次数', 'Child Recompositions': '子节点重组次数', 'Skipped Recompositions': '跳过重组次数'}}.items():
  sha=hashlib.sha256(jar.read(name+'.class')).hexdigest()
  for source,target in values.items(): entries.append(dict(jar='android.jar',class_name=name,sha256=sha,source=source,target=target))
with ZipFile(lib/'android.jar') as jar:
 name='com/android/tools/idea/layoutinspector/snapshots/LayoutInspectorFileEditor'
 entries.append(dict(jar='android.jar',class_name=name,sha256=hashlib.sha256(jar.read(name+'.class')).hexdigest(),source='',target=''))
# 动态显示配方保留参数顺序，设备名、文件名、底层错误和触发序号均原样传入。
with ZipFile(lib/'android.jar') as jar:
 for name,values in {'com/android/tools/idea/appinspection/inspectors/backgroundtask/view/EntryDetailsView': {'Time fired #\x01': '第 \x01 次实际触发时间'}, 'com/android/tools/idea/layoutinspector/ui/toolbar/actions/TargetSelectionActionFactory': {'\x01 (Unsupported for API < 29)': '\x01（不支持 API 29 以下版本）'}, 'com/android/tools/idea/layoutinspector/ui/toolbar/actions/ToggleOverlayAction': {'Failed to read image from "\x01" Error: \x01': '无法读取图像“\x01”。错误：\x01', 'Error': '错误'}}.items():
  sha=hashlib.sha256(jar.read(name+'.class')).hexdigest()
  for source,target in values.items(): entries.append(dict(jar='android.jar',class_name=name,sha256=sha,source=source,target=target))
# 参数变化标题不参与表达式生成；保留状态读取重写器输入与调用栈格式。
with ZipFile(lib/'android.jar') as jar:
 for name,values in {'com/android/tools/idea/layoutinspector/recompositions/RecompositionUiModelImpl': {'Parameter changes:': '参数变化：'}}.items():
  sha=hashlib.sha256(jar.read(name+'.class')).hexdigest()
  for source,target in values.items(): entries.append(dict(jar='android.jar',class_name=name,sha256=sha,source=source,target=target))
# 侧栏只改stripe标题；进程后缀仅追加到显示名，原ProcessDescriptor不变。
with ZipFile(lib/'android.jar') as jar:
 for name,values in {'com/android/tools/idea/appinspection/ide/ui/AppInspectionToolWindowFactory': {'App Inspection': '应用检查'}, 'com/android/tools/idea/appinspection/ide/ui/SelectProcessActionKt': {' [DETACHED]': ' [已断开]'}}.items():
  sha=hashlib.sha256(jar.read(name+'.class')).hexdigest()
  for source,target in values.items(): entries.append(dict(jar='android.jar',class_name=name,sha256=sha,source=source,target=target))
# 数据库导出只翻译弹窗和校验文案，保留SQL、表名、路径和格式扩展名。
with ZipFile(lib/'android.jar') as jar:
 for name,values in {'com/android/tools/idea/sqlite/annotator/RunSqliteStatementGutterIconAction': {'Choose Database': '选择数据库'}, 'com/android/tools/idea/sqlite/ui/exportToFile/ExportToFileDialogViewImpl': {'Query Results': '查询结果', 'Export': '导出', 'Cancel': '取消', 'Export \x01': '导出\x01', 'File type:': '文件类型：', 'Save As...': '另存为…', 'Path not defined': '未指定路径', 'File name not specified': '未指定文件名', 'Path is invalid': '路径无效', 'Path is an existing directory': '该路径是已存在的目录', 'Parent directory not defined': '未指定父目录', 'Parent directory does not exist': '父目录不存在'}}.items():
  sha=hashlib.sha256(jar.read(name+'.class')).hexdigest()
  for source,target in values.items(): entries.append(dict(jar='android.jar',class_name=name,sha256=sha,source=source,target=target))
# 查询界面仅改标题、按钮、占位提示及工具提示，保留SQL正文、标签模型名和Workbench身份。
with ZipFile(lib/'android.jar') as jar:
 for name,values in {'com/android/tools/idea/sqlite/ui/parametersBinding/ParametersBindingDialogViewImpl': {'Statement': '语句', 'Query Parameters': '查询参数', 'Run': '执行', 'Cancel': '取消'}, 'com/android/tools/idea/sqlite/ui/parametersBinding/ParametersBindingDialogViewImpl$InputComponent$Action$Add': {'Add value': '添加值'}, 'com/android/tools/idea/sqlite/ui/parametersBinding/ParametersBindingDialogViewImpl$InputComponent$Action$Remove': {'Remove value': '移除值'}, 'com/android/tools/idea/sqlite/ui/sqliteEvaluator/QueryHistoryView': {'Press Enter to insert': '按 Enter 键插入'}, 'com/android/tools/idea/sqlite/ui/sqliteEvaluator/SqliteEvaluatorViewImpl': {'Run': '执行', 'Show query history': '显示查询历史', 'Enter query...': '输入查询语句…'}, 'com/android/tools/idea/sqlite/ui/tableView/TableViewImpl': {'Results are read-only': '查询结果为只读', 'Refresh table': '刷新表', 'Go to first page': '转到第一页', 'Go to previous page': '转到上一页', 'Go to next page': '转到下一页', 'Go to last page': '转到最后一页'}, 'com/android/tools/idea/sqlite/ui/tableView/TableViewImpl$IsForcedConnectionBanner': {'Non-native connection. This database has not yet been opened by the app so it may contain an outdated schema.': '非原生连接。应用尚未打开此数据库，因此数据库结构可能已过时。'}, 'com/android/tools/idea/sqlite/fileType/SqliteFileType': {'Android SQLite database': 'Android SQLite 数据库'}, 'com/android/tools/idea/sqlite/DatabaseInspectorTabProvider': {'Database Inspector': '数据库检查器'}, 'com/android/tools/idea/sqlite/ui/mainView/DatabaseInspectorViewImpl': {'Moving to offline mode': '正在切换到脱机模式', 'Offline mode unavailable.': '脱机模式不可用。'}}.items():
  sha=hashlib.sha256(jar.read(name+'.class')).hexdigest()
  for source,target in values.items(): entries.append(dict(jar='android.jar',class_name=name,sha256=sha,source=source,target=target))
# 动态提示仅翻译配方，快捷键文本与下载计数按原顺序传入。
with ZipFile(lib/'android.jar') as jar:
 for name,values in {'com/android/tools/idea/sqlite/ui/sqliteEvaluator/SqliteEvaluatorViewImpl': {'Run SQLite expression (\x01)': '执行 SQLite 表达式（\x01）'}, 'com/android/tools/idea/sqlite/ui/parametersBinding/ParametersBindingDialogViewImpl$InputComponent': {'Set value to null (\x01)': '将值设为 NULL（\x01）'}, 'com/android/tools/idea/sqlite/ui/mainView/DatabaseInspectorViewImpl': {'(\x01/\x01) databases downloaded...': '已下载数据库（\x01/\x01）…'}}.items():
  sha=hashlib.sha256(jar.read(name+'.class')).hexdigest()
  for source,target in values.items(): entries.append(dict(jar='android.jar',class_name=name,sha256=sha,source=source,target=target))
# 结构树只翻译固定说明；SQL类型、NOT NULL、数据库名及路径保持原值。
with ZipFile(lib/'android.jar') as jar:
 for name,values in {'com/android/tools/idea/sqlite/ui/mainView/LeftPanelView$SchemaTreeCellRenderer': {'closed': '已关闭', 'non-native': '非原生连接', 'may be read-only': '可能为只读', '<br>Opened with ': '<br>打开所用 API：'}}.items():
  sha=hashlib.sha256(jar.read(name+'.class')).hexdigest()
  for source,target in values.items(): entries.append(dict(jar='android.jar',class_name=name,sha256=sha,source=source,target=target))
# 默认查询标题参与UI重名检查，实际标签身份为独立TabId；不改表名或SQL。
with ZipFile(lib/'android.jar') as jar:
 for name,values in {'com/android/tools/idea/sqlite/controllers/DatabaseInspectorControllerImpl': {'New Query': '新建查询'}}.items():
  sha=hashlib.sha256(jar.read(name+'.class')).hexdigest()
  for source,target in values.items(): entries.append(dict(jar='android.jar',class_name=name,sha256=sha,source=source,target=target))
# Workbench显示标题与内部TREE名称为不同字段；只改显示标题。
with ZipFile(lib/'android.jar') as jar:
 for name,values in {'com/android/tools/idea/layoutinspector/tree/LayoutInspectorTreePanelDefinition': {'Component Tree': '组件树'}}.items():
  sha=hashlib.sha256(jar.read(name+'.class')).hexdigest()
  for source,target in values.items(): entries.append(dict(jar='android.jar',class_name=name,sha256=sha,source=source,target=target))
# Logcat只改动作显示标题；IgnoreApp实际按应用ID操作，不沿用原构造标题的Tag笔误。
with ZipFile(lib/'android.jar') as jar:
 for name,values in {'com/android/tools/idea/logcat/actions/CreateScratchFileAction': {'Create a Scratch File from JSON/XML': '从 JSON/XML 创建临时文件'}, 'com/android/tools/idea/logcat/actions/IgnoreAppAction': {'Ignore Tag': '忽略应用'}, 'com/android/tools/idea/logcat/actions/IgnoreTagAction': {'Ignore Tag': '忽略标签'}, 'com/android/tools/idea/logcat/actions/ToggleFilterAction': {'Toggle Filter': '切换筛选条件'}}.items():
  sha=hashlib.sha256(jar.read(name+'.class')).hexdigest()
  for source,target in values.items(): entries.append(dict(jar='android.jar',class_name=name,sha256=sha,source=source,target=target))
# 仅翻译设置页内置示例，保留日志格式标记、筛选语法和文件类型内部名称。
with ZipFile(lib/'android.jar') as jar:
 for name,values in {'com/android/tools/idea/logcat/filters/LogcatFilterColorSettingsPage': {'// Note that the selected line does not show background color\n\ntag:foo bar line~:Foo|Bar level:DEBUG': '// 注意：选中的行不显示背景色\n\ntag:foo bar line~:Foo|Bar level:DEBUG'}, 'com/android/tools/idea/logcat/filters/parser/LogcatFilterFileType': {'Logcat filter language file': 'Logcat 筛选语言文件'}, 'com/android/tools/idea/logcat/messages/LogcatColorSettingsPage': {'Logcat:\n\n2022-01-06 14:19:34.697 <v> V </v> <vm>Sample verbose text</vm>\n2022-01-06 14:19:34.697 <d> D </d> <dm>Sample debug text</dm>\n2022-01-06 14:19:34.697 <i> I </i> <im>Sample info text</im>\n2022-01-06 14:19:34.697 <w> W </w> <wm>Sample warn text</wm>\n2022-01-06 14:19:34.697 <e> E </e> <em>Sample error text</em>\n2022-01-06 14:19:34.697 <a> A </a> <am>Sample assert text</am>': 'Logcat:\n\n2022-01-06 14:19:34.697 <v> V </v> <vm>详细日志示例</vm>\n2022-01-06 14:19:34.697 <d> D </d> <dm>调试日志示例</dm>\n2022-01-06 14:19:34.697 <i> I </i> <im>信息日志示例</im>\n2022-01-06 14:19:34.697 <w> W </w> <wm>警告日志示例</wm>\n2022-01-06 14:19:34.697 <e> E </e> <em>错误日志示例</em>\n2022-01-06 14:19:34.697 <a> A </a> <am>断言日志示例</am>'}, 'com/android/tools/idea/logcat/messages/LogcatFormatDialogKt': {'Sample logcat message 1.': 'Logcat 示例消息 1。', 'Sample logcat message 2.': 'Logcat 示例消息 2。', 'Sample logcat message 3.': 'Logcat 示例消息 3。', 'Sample logcat multiline\nmessage.': 'Logcat 多行\n消息示例。'}}.items():
  sha=hashlib.sha256(jar.read(name+'.class')).hexdigest()
  for source,target in values.items(): entries.append(dict(jar='android.jar',class_name=name,sha256=sha,source=source,target=target))
# 仅翻notifyError通知；同分支Logger诊断原文与终止命令不变。
with ZipFile(lib/'android.jar') as jar:
 for name,values in {'com/android/tools/idea/logcat/actions/TerminateAppActions$ForceStopAppAction$actionPerformed$1': {'Force stop application failed': '强制停止应用失败'}, 'com/android/tools/idea/logcat/actions/TerminateAppActions$CrashAppAction$actionPerformed$1': {'Crash application failed': '触发应用崩溃失败'}}.items():
  sha=hashlib.sha256(jar.read(name+'.class')).hexdigest()
  for source,target in values.items(): entries.append(dict(jar='android.jar',class_name=name,sha256=sha,source=source,target=target))
# APK摘要标签和提示不改变版本数据、路径或大小列的内部名称。
with ZipFile(lib/'android.jar') as jar:
 for name,values in {'com/android/tools/idea/apk/viewer/ApkViewPanel': {'Parsing Manifest': '正在解析清单', 'Estimating download size..': '正在估算下载大小…', 'APK size: ': 'APK 大小：', ', Download Size: ': '，下载大小：', 'Zip file size: ': 'ZIP 文件大小：', 'Raw File Size: ': '原始文件大小：', ' (Version Name: ': ' (版本名称：', ', Version Code: ': '，版本代码：', '1. The <b>APK size</b> reflects the actual size of the file, and is the minimum amount of space it will consume on the disk after installation.\n2. The <b>download size</b> is the estimated size of the file for new installations (Google Play serves a highly compressed version of the file).\nFor application updates, Google Play serves patches that are typically much smaller.\nThe installation size may be higher than the APK size depending on various other factors.': '1. <b>APK 大小</b>是文件的实际大小，也是安装后至少占用的磁盘空间。\n2. <b>下载大小</b>是首次安装时的预计下载量（Google Play 提供经过高度压缩的文件）。\n更新应用时，Google Play 通常提供体积小得多的补丁。\n受其他因素影响，实际安装大小可能大于 APK 大小。', 'The <b>zip file size</b> reflects the actual size of the zip file on disk.\n': '<b>ZIP 文件大小</b>表示 ZIP 文件在磁盘上的实际大小。\n'}, 'com/android/tools/idea/apk/viewer/ApkEditor$refreshApk$1': {'Reading APK contents': '正在读取 APK 内容'}, 'com/android/tools/idea/apk/viewer/dex/NavigateToSourceAction': {'Navigate to Source': '跳转到源码'}}.items():
  sha=hashlib.sha256(jar.read(name+'.class')).hexdigest()
  for source,target in values.items(): entries.append(dict(jar='android.jar',class_name=name,sha256=sha,source=source,target=target))
# DEX动作与通知仅改显示文案，保留映射文件名、通知组ID及生成的规则内容。
with ZipFile(lib/'android.jar') as jar:
 for name,values in {'com/android/tools/idea/apk/viewer/dex/GenerateProguardKeepRuleAction': {'Generate Proguard keep rule': '生成 ProGuard 保留规则', 'Generates Proguard keep rule': '生成 ProGuard 保留规则'}, 'com/android/tools/idea/apk/viewer/dex/ShowDisassemblyAction': {'Show Bytecode': '显示字节码'}, 'com/android/tools/idea/apk/viewer/dex/ShowDisassemblyAction$1': {'View Dex Bytecode': '查看 DEX 字节码'}, 'com/android/tools/idea/apk/viewer/dex/DexFileViewer': {"APK Analyzer couldn't find any ProGuard mapping files. The filenames must match one of: mapping.txt, seeds.txt, usage.txt": 'APK 分析器未找到任何 ProGuard 映射文件。文件名必须是 mapping.txt、seeds.txt 或 usage.txt。', 'Error Loading Mappings...': '加载映射文件时出错', 'Loading dex stats': '正在加载 DEX 统计信息'}}.items():
  sha=hashlib.sha256(jar.read(name+'.class')).hexdigest()
  for source,target in values.items(): entries.append(dict(jar='android.jar',class_name=name,sha256=sha,source=source,target=target))
# 动态DEX通知/弹窗只翻译拼接配方，路径、成员名和转义后的错误原样传入。
with ZipFile(lib/'android.jar') as jar:
 for name,values in {'com/android/tools/idea/apk/viewer/dex/DexFileViewer': {'APK Analyzer encountered problems loading: \x01': 'APK 分析器加载以下文件时遇到问题：\x01', 'APK Analyzer successfully loaded maps from: \x01': 'APK 分析器已成功从以下位置加载映射：\x01', 'APK Analyzer successfully loaded maps from: \x01\nThere were problems loading: \x01': 'APK 分析器已成功从以下位置加载映射：\x01\n加载以下文件时遇到问题：\x01'}, 'com/android/tools/idea/apk/viewer/dex/ShowDisassemblyAction$1': {'Error constructing dex file: \x01': '构建 DEX 文件时出错：\x01', 'Unable to get byte code: \x01': '无法获取字节码：\x01'}, 'com/android/tools/idea/apk/viewer/dex/ShowReferencesAction': {'References to \x01': '对 \x01 的引用'}, 'com/android/tools/idea/apk/viewer/dex/GenerateProguardKeepRuleAction': {'Proguard keep rules for \x01': '\x01 的 ProGuard 保留规则'}}.items():
  sha=hashlib.sha256(jar.read(name+'.class')).hexdigest()
  for source,target in values.items(): entries.append(dict(jar='android.jar',class_name=name,sha256=sha,source=source,target=target))
# APK比较仅翻译复选框及说明，列名参与排序持久化，保持原值。
with ZipFile(lib/'android.jar') as jar:
 for name,values in {'com/android/tools/idea/apk/viewer/diff/ApkDiffPanel': {'Show File-By-File patch size (may take a long time)': '显示逐文件补丁大小（可能需要较长时间）', 'This is a size estimation for the update that Play store sends to the device': '这是 Google Play 向设备发送的更新补丁的预计大小。'}}.items():
  sha=hashlib.sha256(jar.read(name+'.class')).hexdigest()
  for source,target in values.items(): entries.append(dict(jar='android.jar',class_name=name,sha256=sha,source=source,target=target))
(root/'translations.json').write_text(json.dumps(entries,ensure_ascii=False,indent=2))
def b64(s):return base64.b64encode(s.encode()).decode()
template_display=json.loads((root/'template-display.json').read_text())
(root/'resources/template-display.tsv').write_text(''.join(b64(source)+'\t'+b64(target)+'\n' for source,target in template_display.items()))
(root/'resources/translations.tsv').write_text(''.join('\t'.join([e['class_name'],e['sha256'],b64(e['source']),b64(e['target'])])+'\n' for e in entries))
with (root/'设备界面补译.csv').open('w',encoding='utf-8-sig',newline='') as f:
 w=csv.writer(f);w.writerow(['来源类','英文','中文']);w.writerows((e['class_name'],e['source'],e['target']) for e in entries)
print(sum(bool(e['source']) for e in entries),'条显示文案，',len(set(e['class_name'] for e in entries)),'个类')
