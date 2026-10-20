plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.hilt)
    alias(libs.plugins.ksp)
}

android {
    namespace = "ir.alidevcore.autocare"
    compileSdk = 34

    defaultConfig {
        applicationId = "ir.alidevcore.autocare"
        minSdk = 26
        targetSdk = 34
        val runNumber = System.getenv("GITHUB_RUN_NUMBER")?.toIntOrNull() ?: 1
        versionCode = runNumber
        versionName = "1.0.$runNumber"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }

        // Room schema export
        ksp {
            arg("room.schemaLocation", "$projectDir/schemas")
            arg("room.incremental", "true")
        }
    }

    signingConfigs {
        create("release") {
            val keystorePath = System.getenv("KEYSTORE_PATH") ?: "release.keystore"
            val keystoreFile = file(keystorePath)
            val keystorePassword = System.getenv("KEYSTORE_PASSWORD")
            val keyAliasEnv = System.getenv("KEY_ALIAS")
            val keyPasswordEnv = System.getenv("KEY_PASSWORD")

            if (keystoreFile.exists() && !keystorePassword.isNullOrBlank()) {
                storeFile = keystoreFile
                storePassword = keystorePassword
                keyAlias = keyAliasEnv ?: "autocare"
                keyPassword = keyPasswordEnv ?: keystorePassword
            }
            // CRITICAL SECURITY: Debug signing fallback is strictly removed.
            // If the keystore or passwords are missing, building Release will halt.
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfig = signingConfigs.getByName("release")
        }
        debug {
            isDebuggable = true
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-debug"
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
            // Apache POI packaging exclusions
            excludes += "/META-INF/DEPENDENCIES"
            excludes += "/META-INF/LICENSE"
            excludes += "/META-INF/LICENSE.txt"
            excludes += "/META-INF/license.txt"
            excludes += "/META-INF/NOTICE"
            excludes += "/META-INF/NOTICE.txt"
            excludes += "/META-INF/notice.txt"
            excludes += "/META-INF/ASL2.0"
            excludes += "/META-INF/*.kotlin_module"
            excludes += "META-INF/versions/9/OSGI-INF/MANIFEST.MF"
        }
    }
}

dependencies {
    // AndroidX Core
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.activity.compose)

    // Compose BOM
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.material.icons.extended)

    // Navigation
    implementation(libs.androidx.navigation.compose)

    // Hilt
    implementation(libs.hilt.android)
    ksp(libs.hilt.android.compiler)
    implementation(libs.hilt.navigation.compose)

    // Room
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)

    // WorkManager + Hilt integration
    implementation(libs.androidx.work.runtime.ktx)
    implementation(libs.androidx.hilt.work)
    ksp(libs.androidx.hilt.compiler)

    // DataStore
    implementation(libs.androidx.datastore.preferences)

    // Apache POI (Excel export)
    implementation(libs.apache.poi)
    implementation(libs.apache.poi.ooxml)

    // Debug
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)

    // Testing
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
}

// CRITICAL SECURITY ENFORCEMENT:
// Release APKs MUST have a valid dedicated signing key. Fallback to debug key is strictly prohibited.
gradle.taskGraph.whenReady {
    val isReleaseBuild = allTasks.any { task ->
        task.name.contains("Release", ignoreCase = true) &&
        (task.name.startsWith("assemble") || task.name.startsWith("bundle") || task.name.startsWith("package"))
    }
    if (isReleaseBuild) {
        val keystorePath = System.getenv("KEYSTORE_PATH") ?: "release.keystore"
        val keystoreFile = file(keystorePath)
        val keystorePassword = System.getenv("KEYSTORE_PASSWORD")
        val keyAliasEnv = System.getenv("KEY_ALIAS")
        if (!keystoreFile.exists() || keystorePassword.isNullOrBlank() || keyAliasEnv.isNullOrBlank()) {
            throw GradleException(
                "❌ خطای بحرانی ساخت نسخه Release:\n" +
                "فایل Keystore اختصاصی یا متغیرهای محیطی KEYSTORE_PASSWORD و KEY_ALIAS یافت نشدند.\n" +
                "طبق الزامات امنیتی، نسخه Release هرگز نباید بدون امضای اختصاصی یا با کلید Debug ساخته شود.\n" +
                "مسیر بررسی شده: ${keystoreFile.absolutePath}\n" +
                "برای ساخت نسخه تستی شخصی از دستور './gradlew assembleDebug' استفاده کنید."
            )
        }
    }
}
