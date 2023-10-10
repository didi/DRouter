import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    `java-gradle-plugin`
    `kotlin-dsl`
    `maven-publish`
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

gradlePlugin {
    plugins {
        create("localModuleCommonPlugin") {
            //添加插件
            id = "localModuleCommonPlugin"
            //在根目录创建类 VersionPlugin 继承 Plugin<Project>
            implementationClass = "LocalModuleCommonPlugin"
        }
        create("dRouterPlugin") {
            //添加插件
            id = "com.didi.drouter"
            //在根目录创建类 VersionPlugin 继承 Plugin<Project>
            implementationClass = "com.didi.drouter.DRouterPlugin"
        }
    }
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            groupId = "com.didi.drouter"
            artifactId = "drouter-plugin"
            version = "1.0.0"
            from(components["kotlin"])
        }
    }
    repositories {
        mavenLocal()
        maven {
//            url = uri("file://${rootProject.projectDir}/.aar/maven")
        }
    }
}