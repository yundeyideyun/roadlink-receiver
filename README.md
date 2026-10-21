# 途连 RoadLink Receiver

面向可安装 APK 的 Android 10+ 车机的 CarPlay 接收端开发项目。USB 与无线协议、视频、触控、音频和会话恢复代码基于 DiPlay/xcertplay；本分支新增通用首次配置和运行时认证材料导入。

**状态：开发预览版。代码可构建不等于已通过车机或 iPhone 验证。本分支尚无实机测试记录，不保证所有原厂车机兼容。**

## 认证要求

公开源码与普通 CI APK 不包含可供直接连接 iPhone 的认证身份。选择以下方式之一：

1. 在应用中导入自己有权使用的配套 `identity.pk8` 与 `certificate.p7b`；或
2. 使用 CH341 桥接的兼容认证协处理器，通过接收端 USB/I2C 路径认证。

只有普通 CH341 转接器不够。应用不会签发 Apple 凭据。本地匹配校验也不能证明 iPhone 会接受这些材料。没有认证来源时可以构建、模拟测试和查看界面，真实投射仍有此依赖。[认证配置](docs/ROADLINK_AUTHENTICATION.md)

## 功能与证据

| 功能 | 实现来源与状态 | 本分支实机证据 |
| --- | --- | --- |
| USB/iAP2 与无线连接 | 已集成上游代码 | 无 |
| 视频、分辨率、触控 | 已集成上游代码 | 无 |
| 音乐、导航语音、通话与麦克风 | 已集成上游代码，依赖权限和硬件 | 无 |
| 会话恢复与后台生命周期 | 已集成上游代码 | 无 |
| 运行时认证导入、校验、恢复 | 本分支新增 | 自动测试，不证明 iPhone 信任 |
| 通用首次配置 | 本分支新增，默认关闭车型专用输出与自动连接 | 自动测试 |
| HUD、仪表、方向盘及车型扩展 | 保留上游实现，依赖固件，默认不开启 | 无 |

上游自己的实测见 [DiPlay 兼容性文档](https://github.com/shihabal3amri/DiPlay/blob/main/docs/COMPATIBILITY.md)，不计作本分支实测。[本分支变更](ROADLINK_CHANGES.md)

## 使用

1. 在允许的安装入口安装开发 APK，打开“途连 RoadLink”。封闭系统或禁止第三方 APK 的屏幕不在本软件能直接覆盖的范围内。
2. 选择认证来源。导入失败不替换已有有效文件；会话运行时禁止修改认证。
3. 进入接收端，选择 USB 或无线方式，按系统提示授权。无线需要兼容的 Wi-Fi 和蓝牙。
4. 分别验证会话、首帧、触控、音乐、导航和通话。设备枚举或热点建立不等于 CarPlay 连接成功。

安装和调试应在驻车状态完成。没有原车时可用合适的安卓测试设备进行开发，但不将其结果写成原车验证。

## 构建

JDK 25、Gradle Wrapper 9.5.0、AGP 9.3.0、SDK Platform 37.0、Build Tools 36.0.0、NDK 28.2.13676358。

```sh
./gradlew :shared:testDebugUnitTest :common:testDebugUnitTest :home:testDebugUnitTest \
  :mobile:lintDebug :home:lintDebug :maphost:lintDebug \
  :mobile:assembleDebug :home:assembleDebug :maphost:assembleDebug
```

Windows 使用 `gradlew.bat`。主 APK 是 `mobile/build/outputs/apk/debug/mobile-debug.apk`，不是辅助的 home/maphost 样例。包名 `org.roadlink.receiver`，调试版带 `.dev`；本分支主应用最低 API 29。

普通开发构建不设置 `DIPLAY_AUTH_ASSETS_DIR`。身份由使用者运行时导入或外部硬件提供。调试包不是正式稳定签名发布；不同构建环境的调试签名可能不同，覆盖升级不保证成功。

提交前运行 `python3 scripts/check_public_tree.py`；普通 APK 用 `python3 scripts/check_roadlink_apk.py path/to/mobile-debug.apk` 检查不含凭据。CI 只提供“需要认证来源”的开发 APK，不宣传成免配置独立版。

## 来源、许可与维护范围

这是明确标注来源的衍生项目，不是从零独立实现的协议栈。

- [DiPlay](https://github.com/shihabal3amri/DiPlay)，起点提交 `143f4674b60b564a16baae5110b3b90fb7277631`。
- [xcertplay](https://github.com/shilapi/xcertplay)，接收与协议基础，GPL-3.0。
- [DiAuto](https://github.com/shihabal3amri/DiAuto)，部分界面，AGPL-3.0。
- 原始署名、SPDX、[第三方声明](docs/THIRD_PARTY_NOTICES.md)和 `docs/licenses` 中许可文件均保留。原上游说明另存于 `docs/upstream`。

本分支维护重点是通用设备的认证配置、恢复与可复现验证。上游 Star、下载、用户和实测不属于本项目的采用数据。

新增界面包含英文、简体中文及部分阿拉伯文，其余语言有明确英文回退；繁体中文目录目前使用简体文案，待社区翻译。原接收端多语言资源保留。

欢迎提交可复现问题、实测报告、翻译与修复，详见 [贡献说明](CONTRIBUTING.md)。任何兼容性条目都需记录准确的设备与固件组合；当前没有本分支已验证的车型。

Apple、CarPlay 及车厂品牌属于各自权利人，本项目无厂商关联、认证或背书。
