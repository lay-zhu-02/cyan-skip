import com.android.build.api.variant.impl.VariantOutputImpl
import app.cyanskip.gradle.registerSourcePathsTask
import app.cyanskip.gradle.GenerateSourcePathsTask
import app.cyanskip.gradle.buildProperty
import app.cyanskip.gradle.gitInfo
import app.cyanskip.gradle.releaseBuildKey

val gitInfo = project.gitInfo

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.codeorigin)
}

android {
    namespace = "app.cyanskip.android"
    defaultConfig {
        applicationId = "app.cyanskip.android"
        versionCode = rootProject.extra["cyanskipVersionCode"] as Int
        versionName = rootProject.extra["cyanskipVersionName"] as String
        resValue("bool", "is_accessibility_tool", "true")

        androidResources {
            localeFilters += listOf("zh-rCN", "en")
        }
        ndk {
            abiFilters += listOf("arm64-v8a", "x86_64")
        }

        manifestPlaceholders["buildKey"] = ""
        manifestPlaceholders["channel"] = "cyanskip"
        manifestPlaceholders["commitId"] = gitInfo.commitId
        manifestPlaceholders["commitTime"] = gitInfo.commitTime
        manifestPlaceholders["tagName"] = gitInfo.tagName.orEmpty()
    }

    buildFeatures {
        resValues = true
    }

    val releaseStoreFile = buildProperty("CYANSKIP_STORE_FILE").orNull
    val releaseSigningConfig = if (releaseStoreFile != null) {
        signingConfigs.create("release") {
            storeFile = file(releaseStoreFile)
            storePassword = buildProperty("CYANSKIP_STORE_PASSWORD").orNull
            keyAlias = buildProperty("CYANSKIP_KEY_ALIAS").orNull
            keyPassword = buildProperty("CYANSKIP_KEY_PASSWORD").orNull
        }
    } else {
        signingConfigs.getByName("debug")
    }

    buildTypes {
        all {
            vcsInfo.include = false
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            signingConfig = releaseSigningConfig
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
        }
        debug {
            signingConfig = releaseSigningConfig
            applicationIdSuffix = ".debug"
        }
    }
    // https://github.com/LSPosed/AndroidHiddenApiBypass
    dependenciesInfo {
        includeInApk = false
        includeInBundle = false
    }
    packaging.resources.excludes += setOf(
        "META-INF/**",
        "DebugProbesKt.bin",
    )
}

val generateSourcePaths = registerSourcePathsTask(gitInfo.commitId)
androidComponents.onVariants { variant ->
    variant.sources.assets?.addGeneratedSourceDirectory(generateSourcePaths, GenerateSourcePathsTask::outputDirectory)
}

androidComponents.onVariants(
    androidComponents.selector().withBuildType("release"),
) { variant ->
    variant.manifestPlaceholders.put(
        "buildKey",
        project.releaseBuildKey("cyanskip", gitInfo.commitId),
    )
}

if (buildProperty("CYANSKIP_RENAME_PACKAGE_FLAG").isPresent) {
    androidComponents.onVariants { variant ->
        variant.outputs.onEach { output ->
            output as VariantOutputImpl
            output.outputFileName = "cyan-skip-v${output.versionName.get()}.apk"
        }
    }
}

dependencies {
    implementation(project(":cyanskip-app"))
    // R8 needs hidden framework declarations to preserve system callback implementations.
    compileOnly(project(":cyanskip-hidden-api"))
}
