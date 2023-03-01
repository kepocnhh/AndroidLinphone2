repositories {
    google()
    maven("https://linphone.org/maven_repository")
    mavenCentral()
}

plugins {
    id("com.android.application")
    id("kotlin-android")
}

android {
    val applicationId = "test.android.linphone2"
    namespace = applicationId
    compileSdk = Version.Android.compileSdk

    defaultConfig {
        this.applicationId = applicationId
        minSdk = Version.Android.minSdk
        targetSdk = Version.Android.targetSdk
        versionName = Version.Application.name
        versionCode = Version.Application.code
        manifestPlaceholders["appName"] = "@string/app_name"
    }

    sourceSets["main"].java.srcDir("src/main/kotlin")

    buildTypes {
        getByName("debug") {
            applicationIdSuffix = ".$name"
            versionNameSuffix = "-$name"
            isMinifyEnabled = false
            isShrinkResources = false
            manifestPlaceholders["buildType"] = name
        }
    }
}

androidComponents.onVariants { variant ->
    val output = variant.outputs.single()
    check(output is com.android.build.api.variant.impl.VariantOutputImpl)
    
output.outputFileName.set("${rootProject.name}-${variant.name}-${Version.Application.name}-${Version.Application.code}.apk")
    afterEvaluate {
        tasks.getByName<JavaCompile>("compile${variant.name.capitalize()}JavaWithJavac") {
            targetCompatibility = Version.jvmTarget
        }
        tasks.getByName<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>("compile${variant.name.capitalize()}Kotlin") {
            kotlinOptions.jvmTarget = Version.jvmTarget
        }
    }
}

dependencies {
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.5.1")
    implementation("org.linphone:linphone-sdk-android:5.2.28")
}
