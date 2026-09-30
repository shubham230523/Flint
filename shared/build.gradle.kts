import java.io.File
import java.util.Properties
import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidMultiplatformLibrary)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    id("org.jetbrains.kotlin.plugin.serialization")
}

val isMac = System.getProperty("os.name").lowercase().contains("mac")
val isDesktopOnly = providers.gradleProperty("flint.desktopOnly").orNull?.toBoolean() ?: false
val isWebOnly = providers.gradleProperty("flint.webOnly").orNull?.toBoolean() ?: false

val localProperties = Properties()
val localPropertiesFile = rootProject.file("local.properties")
if (localPropertiesFile.exists()) {
    localPropertiesFile.inputStream().use { stream ->
        localProperties.load(stream)
    }
}

fun getSecret(key: String, envKey: String, defaultValue: String = ""): String {
    val localProp = localProperties.getProperty(key)
        ?: localProperties.getProperty(key.replace("flint.", ""))
        ?: localProperties.getProperty(key.replace(".", "_"))
    if (!localProp.isNullOrEmpty()) return localProp.trim()

    val envVal = System.getenv(envKey)
    if (!envVal.isNullOrEmpty()) return envVal.trim()

    val gradleProp = providers.gradleProperty(key).orNull
    if (!gradleProp.isNullOrEmpty()) return gradleProp.trim()

    return defaultValue
}

abstract class GenerateFlintBuildConfigTask : DefaultTask() {
    @get:Input
    abstract val openRouterApiKey: Property<String>

    @get:Input
    abstract val openRouterModelName: Property<String>

    @get:Input
    abstract val geminiApiKey: Property<String>

    @get:Input
    abstract val geminiModelName: Property<String>

    @get:Input
    abstract val testEmail: Property<String>

    @get:Input
    abstract val testPassword: Property<String>

    @get:OutputDirectory
    abstract val outputDir: DirectoryProperty

    @TaskAction
    fun generate() {
        val dir = outputDir.get().asFile
        dir.mkdirs()
        val buildConfigFile = File(dir, "FlintBuildConfig.kt")
        buildConfigFile.writeText("""
            package com.shubhamthorat.flint.core

            /**
             * Auto-generated Flint Build Configuration.
             * Values are loaded from environment variables (GitHub Actions secrets) or local.properties (git-ignored).
             */
            object FlintBuildConfig {
                const val OPENROUTER_API_KEY: String = "${openRouterApiKey.get()}"
                const val OPENROUTER_MODEL_NAME: String = "${openRouterModelName.get()}"
                const val GEMINI_API_KEY: String = "${geminiApiKey.get()}"
                const val GEMINI_MODEL_NAME: String = "${geminiModelName.get()}"
                const val TEST_EMAIL: String = "${testEmail.get()}"
                const val TEST_PASSWORD: String = "${testPassword.get()}"
            }
        """.trimIndent())
    }
}

val generateFlintBuildConfig = tasks.register("generateFlintBuildConfig", GenerateFlintBuildConfigTask::class.java) {
    openRouterApiKey.set(getSecret("openrouter.api.key", "OPENROUTER_API_KEY", ""))
    openRouterModelName.set(getSecret("openrouter.model.name", "OPENROUTER_MODEL_NAME", "nvidia/nemotron-3.5-lightning:free"))
    geminiApiKey.set(getSecret("gemini.api.key", "GEMINI_API_KEY", ""))
    geminiModelName.set(getSecret("gemini.model.name", "GEMINI_MODEL_NAME", "gemini-2.0-flash"))
    testEmail.set(getSecret("flint.test.email", "FLINT_TEST_EMAIL", "shubhamthorat186@gmail.com"))
    testPassword.set(getSecret("flint.test.password", "FLINT_TEST_PASSWORD", "Dream123#"))
    outputDir.set(layout.buildDirectory.dir("generated/source/buildConfig/commonMain/kotlin/com/shubhamthorat/flint/core"))
}

kotlin {
    jvm()

    android {
        namespace = "com.shubhamthorat.flint.shared"
        compileSdk = libs.versions.android.compileSdk.get().toInt()
        minSdk = libs.versions.android.minSdk.get().toInt()

        compilerOptions {
            jvmTarget = JvmTarget.JVM_17
        }
        androidResources {
            enable = true
        }
        withHostTest {
            isIncludeAndroidResources = true
        }
        withDeviceTestBuilder {
            sourceSetTreeName = "test"
        }.configure {
            instrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        }
    }

    js {
        browser {
            commonWebpackConfig {
                outputFileName = "webApp.js"
            }
        }
        binaries.executable()
    }

    if (isMac && !isDesktopOnly && !isWebOnly) {
        listOf(
            iosArm64(),
            iosSimulatorArm64()
        ).forEach { iosTarget ->
            iosTarget.binaries.framework {
                baseName = "Shared"
                isStatic = true
            }
        }
    }

    sourceSets {
        androidMain.dependencies {
            implementation(libs.androidx.core.ktx)
            implementation(libs.compose.uiToolingPreview)
            implementation(libs.compose.uiTooling)
            implementation("io.ktor:ktor-client-cio:3.1.1")
            implementation(project.dependencies.platform("com.google.firebase:firebase-bom:33.9.0"))
        }
        jvmMain.dependencies {
            implementation("io.ktor:ktor-client-cio:3.1.1")
        }
        commonMain {
            kotlin.srcDir(generateFlintBuildConfig)
            dependencies {
                implementation(libs.kotlinx.coroutinesCore)
                implementation(libs.compose.runtime)
                implementation(libs.compose.foundation)
                implementation(libs.compose.material3)
                implementation(libs.compose.ui)
                implementation(libs.compose.components.resources)
                implementation(libs.compose.uiToolingPreview)
                implementation(libs.androidx.lifecycle.viewmodelCompose)
                implementation(libs.androidx.lifecycle.runtimeCompose)
                implementation("io.ktor:ktor-client-core:3.1.1")
                implementation("io.ktor:ktor-client-content-negotiation:3.1.1")
                implementation("io.ktor:ktor-serialization-kotlinx-json:3.1.1")
                implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.8.0")
                implementation("dev.gitlive:firebase-auth:2.1.0")
                implementation("dev.gitlive:firebase-firestore:2.1.0")
                implementation("dev.gitlive:firebase-common:2.1.0")
            }
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
            implementation(libs.kotlinx.coroutinesTest)
        }

        findByName("jsMain")?.dependencies {
            implementation(libs.wrappers.browser)
        }
    }
}

dependencies {
    androidRuntimeClasspath(libs.compose.uiTooling)
}
