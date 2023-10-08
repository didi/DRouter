buildscript {

}
// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {

    val agpVersion = "8.1.2"
    val kotlinVersion = "1.9.10"

    id("com.android.application") version agpVersion apply false
    id("com.android.library") version agpVersion apply false
    id("org.jetbrains.kotlin.android") version kotlinVersion apply false
    id("org.jetbrains.kotlin.jvm") version kotlinVersion apply false
}