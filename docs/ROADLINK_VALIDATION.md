# RoadLink Receiver 0.2.0-alpha01：本地验证

日期：2026-10-21（北京时间）。代码基于 DiPlay 起点提交 `143f4674b60b564a16baae5110b3b90fb7277631`。

## 构建与测试

AGENTS.md 指定的完整 9 项 Gradle 任务已报告 BUILD SUCCESSFUL：三个模块测试、三个应用静态检查、三个应用调试构建。没有跳过 lint、建立 baseline 或屏蔽新增错误。

共 3,239 项测试，0 失败、0 错误、0 跳过。其中本分支新增 13 项，其余继承自上游；不把上游测试归为本分支原创。

| 模块 | 测试数 | 失败 | 跳过 |
| --- | ---: | ---: | ---: |
| shared | 1651 | 0 | 0 |
| common | 1582 | 0 | 0 |
| samples/home | 6 | 0 | 0 |

新增测试覆盖认证导入、无效或不匹配材料保留旧配置、读取和大小限制、会话竞争、目录提交恢复、移除保护、首次配置默认值、文件选择、界面导航，以及 1.5 倍字体下的 320dp、960dp 和阿拉伯文 RTL 布局。

| 应用静态检查 | 错误 | 提示 |
| --- | ---: | ---: |
| mobile | 0 | 19 |
| samples/home | 0 | 7 |
| samples/maphost | 0 | 1 |

保留的提示包含继承代码的 API/资源建议和仅用于专用 USB Lockdown 通道的 TLS 实现提示；后者不是普通互联网 TLS 校验器。本记录不宣称已完成独立安全审计。

## 成品

- 包名：`org.roadlink.receiver.dev`；版本：`0.2.0-alpha01-dev`。
- 主启动页：`com.shilapi.xcertplay.RoadLinkSetupActivity`。
- 最低 Android API 29；target/compile API 37。
- ARM64、ARMv7、x86、x86_64 的接收内核本地库均包含在 APK 中。
- 使用 Build Tools 36.0.0 apksigner 验证签名通过，APK v2，调试签名。
- 公开源码检查通过；普通 APK 凭据检查通过，未嵌入认证身份文件。

APK SHA-256：`ee843068ae071a0202c29129d38d97bbca51d79520c4eac39ab92d5365b5b5c7`

## 验证边界

这是软件构建、JVM/Robolectric 与本地图形渲染验证，不是原车或真实 iPhone 验证。没有本分支的完整 iPhone 握手、首帧、触控、声学、通话、Siri 或车机生命周期实测数据。认证文件本地匹配不等于 iPhone 信任，外部认证硬件也尚未实测。

公开开发包需要使用者提供认证来源，不是免认证的即装即连成品。真实设备支持与用户采用量当前未建立，不能把上游的结果算入本分支。
