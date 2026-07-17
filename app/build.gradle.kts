import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

val localProperties = Properties().apply {
    val file = rootProject.file("local.properties")
    if (file.exists()) {
        file.inputStream().use(::load)
    }
}

fun getConfigValue(name: String, defaultValue: String = ""): String {
    return providers.gradleProperty(name)
        .orElse(providers.environmentVariable(name))
        .orElse(localProperties.getProperty(name, ""))
        .orElse(defaultValue)
        .get()
}

fun String.asBuildConfigString(): String {
    return "\"" + replace("\\", "\\\\").replace("\"", "\\\"") + "\""
}

android {
    namespace = "com.willdeep.android"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.willdeep.android"
        minSdk = 33
        targetSdk = 36
        versionCode = 107
        versionName = "1.23.0-rc1"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        val umengAppKey = getConfigValue("UMENG_APPKEY")
        val umengMessageSecret = getConfigValue("UMENG_MESSAGE_SECRET")
        val umengChannel = getConfigValue("UMENG_CHANNEL", "willdeep")
        val umengPushEnabled = getConfigValue("UMENG_PUSH_ENABLED", "false").equals("true", ignoreCase = true)
        manifestPlaceholders["UMENG_APPKEY"] = umengAppKey
        manifestPlaceholders["UMENG_MESSAGE_SECRET"] = umengMessageSecret
        manifestPlaceholders["UMENG_CHANNEL"] = umengChannel
        buildConfigField("String", "UMENG_APPKEY", umengAppKey.asBuildConfigString())
        buildConfigField("String", "UMENG_MESSAGE_SECRET", umengMessageSecret.asBuildConfigString())
        buildConfigField("String", "UMENG_CHANNEL", umengChannel.asBuildConfigString())
        buildConfigField("boolean", "UMENG_PUSH_ENABLED", umengPushEnabled.toString())
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
}

dependencies {

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.okhttp)
    implementation(libs.androidx.security.crypto)
    implementation(libs.androidx.camera.core)
    implementation(libs.androidx.camera.camera2)
    implementation(libs.androidx.camera.lifecycle)
    implementation(libs.androidx.camera.view)
    implementation(libs.mlkit.barcode.scanning)
    implementation(libs.coil.compose)
    implementation(libs.umeng.common)
    implementation(libs.umeng.asms)
    implementation(libs.umeng.push)
    testImplementation(libs.junit)
    testImplementation(libs.json)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}
