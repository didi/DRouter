pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
        maven(url = "${rootProject.path}/.aar/maven/libs-release")
        maven(url = "https://s01.oss.sonatype.org/content/repositories/releases/")
//        maven(url = "https://plugins.gradle.org/m2/")
    }
    includeBuild("plugins/commonLocalModule-plugin")
    includeBuild("plugins/drouter-plugin")
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        maven(url = "${rootProject.path}/.aar/maven")
        maven(url = "https://s01.oss.sonatype.org/content/repositories/releases/")
    }
}
rootProject.name = "DRouter"


include(":demo")
include(":drouter-api")
include(":drouter-api-page")
include(":drouter-api-process")
include(":drouter-api-stub")
include(":drouter-plugin-proxy")
include(":demo-base")
include(":demo-process")
