# Release 签名约定

> 本目录存放签名相关约定说明。实际的 `.jks` / `.keystore` 密钥文件**不进 git**（见 `android/.gitignore`）。

## 当前状态

- `debug` 构建：使用 Android SDK 默认 debug keystore，无需配置。
- `release` 构建：`app/build.gradle.kts` 中 `release` 复用 debug 签名（`signingConfig = signingConfigs.getByName("debug")`），仅供本地安装自测。
- 正式分发前，需切换到独立 release keystore。

## Release keystore 接入方式

1. 生成 keystore（放在**仓库外**，不被 git 追踪）：

   ```bash
   keytool -genkeypair -v \
     -keystore ~/.gradle/keystore/renovation-release.jks \
     -alias renovation \
     -keyalg RSA -keysize 2048 -validity 10000
   ```

2. 在 `gradle.properties`（不进 git）或本地环境变量中注入密码，例如：

   ```properties
   RENOVATION_STORE_FILE=C:/Users/<you>/.gradle/keystore/renovation-release.jks
   RENOVATION_STORE_PASSWORD=***
   RENOVATION_KEY_ALIAS=renovation
   RENOVATION_KEY_PASSWORD=***
   ```

3. 在 `app/build.gradle.kts` 中读取环境变量配置 `signingConfigs`，并把 release `signingConfig` 指向它。示例字段名：

   | 字段 | 含义 | 来源 |
   |------|------|------|
   | `storeFile` | keystore 文件路径 | 环境变量 `RENOVATION_STORE_FILE` |
   | `storePassword` | keystore 密码 | 环境变量 `RENOVATION_STORE_PASSWORD` |
   | `keyAlias` | 密钥别名 | 环境变量 `RENOVATION_KEY_ALIAS` |
   | `keyPassword` | 密钥密码 | 环境变量 `RENOVATION_KEY_PASSWORD` |

## 安全约定

- 密钥文件与密码**通过密码管理器（如 1Password / Bitwarden / KeePass）同步给团队成员**，绝不写入 git、README、`gradle.properties` 提交、贴纸、聊天记录。
- `gradle.properties` 若需本地引用，使用环境变量引用或保持文件被 `.gitignore` 覆盖（当前 `local.properties` 已忽略）。
- 丢失 keystore 会导致无法覆盖升级已上架的应用，务必多重备份。
- 同一应用后续所有版本必须用同一 keystore 签名，否则无法覆盖安装。