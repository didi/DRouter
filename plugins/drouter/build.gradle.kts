import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    `kotlin-dsl`
    id("com.gradle.plugin-publish") version "1.0.0"
}

repositories {
    google()
    mavenCentral()
    gradlePluginPortal()
    maven(url = "https://s01.oss.sonatype.org/content/repositories/releases/")
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

tasks.withType<KotlinCompile> {
    kotlinOptions {
        jvmTarget = "17"
    }
}

dependencies {
    //添加Gradle相关的API，否则无法自定义Plugin和Task
    implementation(gradleApi())
    implementation("org.jetbrains.kotlin:kotlin-gradle-plugin:1.9.10")
    implementation("org.javassist:javassist:3.29.2-GA")
    implementation("commons-io:commons-io:2.14.0")
    compileOnly("com.android.tools.build:gradle:8.0.2")
}

// 使用 publishPluginMavenPublicationToxxx 发布
version = "1.4.1"
group = "io.github.drouter"

gradlePlugin {
    plugins {
        create("dRouter") {
            // 仅影响本地模块依赖时的名字以及使用 java-gradle-plugin 插件发布时有影响
            // 具体参考 https://docs.gradle.org/current/userguide/plugins.html#sec:plugin_markers
            id = "com.didi.drouter"
            implementationClass = "com.didi.drouter.DRouterPlugin"
        }
    }
}

publishing {
    repositories {
        mavenLocal()
        // TODO 指定上传仓库
//        mavenCentral()
    }
}