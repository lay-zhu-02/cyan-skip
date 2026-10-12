# GitHub 构建与发布

`.github/workflows/release.yml` 在推送 `main`、推送 `v*` 标签或手动运行时，使用 JDK 21、Android SDK 37 和项目的 Gradle Wrapper 执行共享业务测试及签名 Release 构建，再发布 APK 与 SHA-256 校验文件。应用 ID 为 `app.cyanskip.android`，不迁移旧版 `app.qingtiao.android` 的数据。

## 首次配置

发布仓库为 `lay-zhu-02/cyan-skip`。GitHub 认证、签名 Secret 配置、推送和 Actions 操作统一在 layz.top 服务器 `/opt/cyan-skip` 进行，本地负责开发与验证。发布前确认 `origin` 指向自己的仓库，不能将项目发布流程推送到上游 GKD。

在仓库 Settings → Secrets and variables → Actions 配置以下 Repository secrets：

| Secret | 内容 |
| --- | --- |
| `CYANSKIP_STORE_FILE_BASE64` | 固定签名 keystore 的 Base64 内容 |
| `CYANSKIP_STORE_PASSWORD` | keystore 密码 |
| `CYANSKIP_KEY_ALIAS` | 签名密钥 alias |
| `CYANSKIP_KEY_PASSWORD` | 签名密钥密码 |

使用已有签名密钥，后续保持不变。不要上传 keystore 到源码仓库，也不要将密码写入工作流。未配置这些 secrets 时，工作流明确失败，避免每次生成不同的调试签名。可用 `apksigner verify --print-certs <APK>` 比较证书 SHA-256，核对与已分发的同应用 ID 安装包一致。

首次推送后，到 Actions → Build and publish cyan-skip 查看构建；也可以使用 Run workflow 手动触发。工作流的 `contents: write` 权限用于创建 Release、标签及上传附件；仓库须允许该工作流执行。

## 后续更新

- 推送 `main` 会发布 `build-<7 位提交号>` Release，APK 为 `cyan-skip-v<版本>-<7 位提交号>.apk`。
- 正式版本先在 `build.gradle.kts` 更新 `cyanskipVersionName` 和 `cyanskipVersionCode`，再推送对应的 `v<版本>` 标签；工作流核对标签与 APK 版本一致。
- 同一 Release 重新运行时替换 APK 与校验文件，不创建重复 Release。
- GitHub Release 的说明记录源码提交、Android 要求和旧版数据限制。

## 网站下载入口

用户每次点击本站 APK 下载时，layz.top 服务器检查最新稳定 Release（无定时同步），下载 APK 与 `.sha256` 并校验大小和 SHA-256；成功后原子切换当前下载，失败时保留最近一次成功版本。用户通过 `https://layz.top/downloads/cyan-skip.apk` 直接下载，不需要访问 GitHub；配套校验文件位于 `https://layz.top/downloads/cyan-skip.apk.sha256`。网站 `/jump` 显示实际同步版本，同时保留 GitHub Release 备用链接。发布新 Release 无需重新部署网站或手工上传安装包。同步服务源码和运维命令位于网站仓库 `ops/RELEASE-MIRROR.md`。

首次 Release 成功发布并验证 APK 可下载后，再部署网站入口。若最终使用其他仓库，同步修改网站链接与本页地址。
