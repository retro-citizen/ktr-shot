# KTR Shot

KTR2 掌机的全局实体键截图应用，当前版本 **0.3.1**。通过 Android 无障碍服务识别 **MENU + SELECT** 并保存 PNG，成功后播放一次系统快门音，不弹出截图提示、确认框或预览。

**仅用于 KTR2 掌机。请勿在其他型号或普通 Android 手机上安装使用。** 当前实测系统为 KTR2 / Android 14；Android 最低 API 要求不代表对其他设备的支持。本应用未实现机型安装锁定。

不需要 root、Shizuku 或电脑激活，不修改 KOS，不创建虚拟手柄。本项目为独立工具，与设备厂商无隶属关系。

## 安装与使用

正式 APK 通过仓库的 [Releases](https://github.com/retro-citizen/ktr-shot/releases) 发布，选择名称含 `KTR2-release` 的文件，并用同页 `SHA256SUMS` 核验。应用仅用于 KTR2，详见 [发布说明](RELEASE_NOTES.md)。本仓库同时提供源码，构建方式见下文。

正式签名与早期 debug 测试版不同，不能直接覆盖安装。迁移前请记录设置、备份重要截图，再卸载测试版并重新配置无障碍服务；后续更新需保持同一正式签名。

1. 安装 APK，打开 KTR Shot。
2. 点击“打开无障碍设置”，开启 **KTR Shot 截图服务**，保留 KOS 原有服务。
3. 确认“无障碍：已连接”，打开“启用截图快捷键”。
4. 同时短按 **MENU + SELECT**，再松开两个键；先按哪个都可以。
5. 图片保存到 **Pictures/KTRShots**，可在相册或应用内“打开最近截图”查看。

不必保持应用界面打开。系统通常会在重启后恢复已启用的无障碍服务；若厂商后台管理或强制停止导致服务断开，在设备上关闭再开启本应用的无障碍服务即可。日常暂停使用应用内开关。

## 按键与反馈

- 支持单键或组合键学习，学习超时为 12 秒；可恢复默认 MENU + SELECT。
- 两键全部松开后才能再次触发，长按不会连拍，截图请求有约 800 ms 的最短间隔。
- 所有 `onKeyEvent` 路径均返回 `false`，不吞键、不补发、不独占输入设备。KOS 或模拟器原有快捷键动作可能同时发生。
- MENU + L1 在已测试设备上会改变游戏速度，FUNC 有返回或退出副作用，因此不作为默认组合。
- 截图延迟可设为 0–500 ms，新安装默认 80 ms。设为 **0 ms** 可取消截图请求前的主动等待；它不改变其他手柄按键的响应，也不取消防连拍保护。升级保留已有设置。
- 0 ms 不代表零耗时：系统调度、画面获取、PNG 编码和保存仍需要时间。若原快捷键同时打开菜单，截图是否包含菜单取决于实际时序。
- 成功保存后播放 Android `MediaActionSound.SHUTTER_CLICK`；声音可能比按键稍晚。音色、音量、静音行为由固件音频策略决定，应用不强制修改音量。
- 截图失败不响、不弹提示，原因仅显示在应用设置页；音效不可用不影响图片保存。

## 权限与限制

应用仅用无障碍服务接收按键和调用 `takeScreenshot`，不读取窗口文字，没有联网权限，不上传图片，不申请全盘存储权限。图片通过 MediaStore 写入公共图片目录。

仅支持 KTR2 掌机，实测系统为 Android 14，包括 KOS 主界面和 PS2 游戏内截图、正常手柄操作、重启后自动恢复服务，以及系统快门音和无画面提示。构建最低 API 为 Android 11，但其他型号不在支持范围内，其他 KTR2 固件及模拟器组合也未全部验证。安全窗口或 DRM 内容可能无法截图或得到黑图，不绕过系统保护。

无障碍按键回调仍有系统调度开销，不能承诺零新增输入延迟；截图和存储也可能影响游戏帧时间。后台线程处理 PNG 编码与保存，应用不转发摇杆轴事件。

## 构建与测试

需要 **JDK 17** 和 **Android SDK Platform 35 / Build Tools 35.0.0**，无需 NDK。Gradle 8.10.2 Wrapper 已包含在仓库中，首次构建需要网络下载 Gradle 和 Android 构建依赖。

可用 Android Studio 打开仓库根目录，选择 JDK 17 并安装对应 SDK。命令行方式：将 `JAVA_HOME` 指向 JDK 17，将 `ANDROID_HOME` 指向本机 Android SDK（或使用未跟踪的 `local.properties` 配置 `sdk.dir`）。

```sh
git clone https://github.com/retro-citizen/ktr-shot.git
cd ktr-shot
bash build-local.sh
```

此脚本运行核心检查、构建和 Android Lint，输出 `dist/KTR-Shot-0.3.1-debug.apk`。仅运行组合键测试：

```sh
bash test-core.sh
```

Windows 可通过 Android Studio 构建，或使用 JDK 17 运行 `gradlew.bat :app:assembleDebug :app:lintDebug`；上述 Bash 脚本需要 Bash 环境。

仓库 CI 在 Linux 上运行核心测试、构建和 Lint。正式分发可在 Android Studio 中使用 **Generate Signed Bundle / APK** 和自己保管的签名密钥，或使用以下脚本：

```sh
export KTR_KEYSTORE_PATH=/absolute/path/to/release.jks
export KTR_KEYSTORE_PASSWORD_FILE=/absolute/path/to/keystore.password
bash build-release.sh
```

`ANDROID_HOME` 须指向 SDK；默认密钥别名为 `ktr-shot`，可用 `KTR_KEY_ALIAS` 修改。默认密钥密码与 keystore 密码相同；不同时通过 `KTR_KEY_PASSWORD_FILE` 指定独立密码文件。密码文件应限制为仅本人可读，不要把密码放进命令行参数或日志。

输出为 `dist/KTR-Shot-0.3.1-KTR2-release.apk` 和 `dist/SHA256SUMS`。不得提交签名密钥、密码、访问令牌或本机配置。请离线备份签名材料，后续升级必须沿用同一密钥；卸载会丢失应用设置。

## 源码范围

- `app/src/main/java/dev/ktr/shot/`：按键状态机、无障碍服务、截图保存和设置界面。
- `tests/CoreTest.java`：19 项组合键逻辑检查。
- 不包含原厂 APK、反编译代码、游戏素材、设备截图、调试日志、SDK 或旧原生输入过滤实验。

提交问题时请说明 Android 版本、模拟器、按键及复现步骤；不要公开私人截图。默认 `uiautomator dump` 会临时抑制其他无障碍服务，不适合判断本应用服务是否正常在线。

## 许可证

本项目源码采用 **GNU GPL v3.0 only**（`GPL-3.0-only`），见 [LICENSE](LICENSE)。分发修改后的应用时也需要按该许可证提供对应源码。Gradle Wrapper 属于 Gradle 项目，使用其自身的 Apache-2.0 许可证，见 [THIRD_PARTY_NOTICES.md](THIRD_PARTY_NOTICES.md)。