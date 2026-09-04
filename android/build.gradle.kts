// 根 build 文件：只声明 plugin 别名，真正的 plugin 应用在 :app 的 build.gradle.kts
// 注意：AGP 9.0 起 Kotlin 支持内建于 AGP（built-in Kotlin），
// 不再使用 org.jetbrains.kotlin.android 插件（应用会报错）。
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.kotlin.serialization) apply false
    alias(libs.plugins.ksp) apply false
}