import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.plugin.compose")
    id("org.jetbrains.kotlin.plugin.serialization")
}

// 统一签名：keystore 随仓库分发（个人项目，密钥无需保密）
// 作用：每次 CI 构建产物签名一致，新版本可直接覆盖安装，无需卸载重装
val releaseKeystore = rootProject.file("wencui-release.keystore")

android {
    namespace = "com.wencui.quiz"
    compileSdk = 37

    signingConfigs {
        create("release") {
            storeFile = releaseKeystore
            storePassword = "wencui2026"
            keyAlias = "wencui"
            keyPassword = "wencui2026"
        }
    }

    defaultConfig {
        applicationId = "com.wencui.quiz"
        minSdk = 26
        targetSdk = 36
        versionCode = 7
        versionName = "1.6.0"
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            signingConfig = signingConfigs.getByName("release")
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
        // debug 也统一到同一签名，避免任何场景的签名冲突
        debug {
            signingConfig = signingConfigs.getByName("release")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_17)
    }
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2024.10.00")
    implementation(composeBom)

    implementation("androidx.activity:activity-compose:1.13.0")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    debugImplementation("androidx.compose.ui:ui-tooling")

    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.7")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.7")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")

    implementation(project(":kyant-backdrop"))
    implementation("io.github.kyant0:shapes:1.2.0")

    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.8.1")
}