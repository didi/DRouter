import com.android.build.gradle.BaseExtension
import org.gradle.api.JavaVersion
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.dependencies
import org.gradle.kotlin.dsl.getByType
import org.gradle.kotlin.dsl.withType
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

@Suppress("unused")
class LocalModuleCommonPlugin: Plugin<Project>{
    override fun apply(project: Project) {
        project.extensions.getByType(BaseExtension::class).run {
            val isApp = this is com.android.build.gradle.AppExtension
            compileSdkVersion(34)
            defaultConfig {
                minSdk = 14
                targetSdk = 34
                testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
            }

            buildTypes {
                getByName("release"){
                    isMinifyEnabled = isApp
                    proguardFiles(
                        getDefaultProguardFile("proguard-android-optimize.txt"),
                        "proguard-rules.pro"
                    )

                }
            }
            compileOptions {
                sourceCompatibility = JavaVersion.VERSION_17
                targetCompatibility = JavaVersion.VERSION_17
            }

            project.tasks.withType<KotlinCompile>{
                kotlinOptions {
                    jvmTarget = "17"
                }
            }

            project.dependencies{
                add("testImplementation", "junit:junit:4.13.2")
                add("androidTestImplementation", "androidx.test:runner:1.5.2")
                add("androidTestImplementation", "androidx.test.espresso:espresso-core:3.5.1")
            }
        }
    }
}