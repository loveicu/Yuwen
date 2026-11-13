pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
        // 国内镜像作兜底（GitHub Actions 在海外，官方源优先；本地构建时镜像可加速）
        maven { url = uri("https://maven.aliyun.com/repository/google") }
        maven { url = uri("https://maven.aliyun.com/repository/central") }
        maven { url = uri("https://maven.aliyun.com/repository/gradle-plugin") }
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        maven { url = uri("https://maven.aliyun.com/repository/google") }
        maven { url = uri("https://maven.aliyun.com/repository/central") }
    }
}

rootProject.name = "WenCuiQuiz"
include(":app")
include(":kyant-backdrop")
project(":kyant-backdrop").projectDir = file("third-party/kyant-backdrop")