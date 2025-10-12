import java.io.FileInputStream
import java.util.Properties

plugins {
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.jetbrainsKotlinAndroid)
    alias(libs.plugins.ktfmt)
    alias(libs.plugins.sonar)
    id("com.google.gms.google-services")
    id("jacoco")
}

val localProps = Properties()
val localPropsFile = rootProject.file("local.properties")
if(localPropsFile.exists()){
    localProps.load(FileInputStream(localPropsFile))
    localProps.forEach{key, value ->
        project.extensions.extraProperties[key.toString()] = value
    }
}

val keystorePropertiesFile = rootProject.file("keystore.properties")
val keystoreProperties = Properties()
if (keystorePropertiesFile.exists()) {
    keystorePropertiesFile.inputStream().use {
        keystoreProperties.load(it)
    }
} else {
    println("Keystore file not found: ${keystorePropertiesFile.path}")
}

android {
    namespace = "com.github.warnastrophy"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.github.warnastrophy"
        minSdk = 28
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }

        manifestPlaceholders["GOOGLE_MAPS_API_KEY"] = project.findProperty("GOOGLE_MAPS_API_KEY") ?: ""
    }

    signingConfigs {
        create("release") {
            storeFile = project.file(
                System.getenv("SIGNING_STORE_FILE")
                    ?: keystoreProperties["storeFile"] as? String
                    ?: "unknown.jks"
            )

            storePassword = System.getenv("SIGNING_STORE_PASSWORD")
                ?: keystoreProperties["storePassword"] as? String

            keyAlias = System.getenv("SIGNING_KEY_ALIAS")
                ?: keystoreProperties["keyAlias"] as? String

            keyPassword = System.getenv("SIGNING_KEY_PASSWORD")
                ?: keystoreProperties["keyPassword"] as? String
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            val releaseConfig = signingConfigs.getByName("release")
            if (releaseConfig.storeFile?.exists() == true) {
                signingConfig = releaseConfig
            } else {
                println("Release signing config unavailable")
            }
        }

        debug {
            enableUnitTestCoverage = true
            enableAndroidTestCoverage = true
        }
    }

    testCoverage {
        jacocoVersion = "0.8.8"
    }

    buildFeatures {
        compose = true
    }

    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.14"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }

    kotlinOptions {
        jvmTarget = "1.8"
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }

    testOptions {
        unitTests {
            isIncludeAndroidResources = true
            isReturnDefaultValues = true
        }
    }

    // Robolectric needs to be run only in debug. But its tests are placed in the shared source set (test)
    // The next lines transfers the src/test/* from shared to the testDebug one
    //
    // This prevent errors from occurring during unit tests
    sourceSets.getByName("testDebug") {
        val test = sourceSets.getByName("test")

        java.setSrcDirs(test.java.srcDirs)
        res.setSrcDirs(test.res.srcDirs)
        resources.setSrcDirs(test.resources.srcDirs)
    }

    sourceSets.getByName("test") {
        java.setSrcDirs(emptyList<File>())
        res.setSrcDirs(emptyList<File>())
        resources.setSrcDirs(emptyList<File>())
    }
}

sonar {
    properties {
        property("sonar.projectKey", "SWENTapp_warnastrophy")
        property("sonar.projectName", "warnastrophy")
        property("sonar.organization", "swentapp")
        property("sonar.host.url", "https://sonarcloud.io")
        property("sonar.login", System.getenv("SONAR_TOKEN"))
        // Comma-separated paths to the various directories containing the *.xml JUnit report files. Each path may be absolute or relative to the project base directory.
        property("sonar.junit.reportPaths", "${project.layout.buildDirectory.get()}/test-results/testDebugunitTest/")
        // Paths to xml files with Android Lint issues. If the main flavor is changed, this file will have to be changed too.
        property("sonar.androidLint.reportPaths", "${project.layout.buildDirectory.get()}/reports/lint-results-debug.xml")
        // Paths to JaCoCo XML coverage report files.
        property("sonar.coverage.jacoco.xmlReportPaths", "${project.layout.buildDirectory.get()}/reports/jacoco/jacocoTestReport/jacocoTestReport.xml")
    }
}

// When a library is used both by robolectric and connected tests, use this function
fun DependencyHandlerScope.globalTestImplementation(dep: Any) {
    androidTestImplementation(dep)
    testImplementation(dep)
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(platform(libs.compose.bom))
    testImplementation(libs.junit)
    globalTestImplementation(libs.androidx.junit)
    globalTestImplementation(libs.androidx.espresso.core)

    // ------------- Jetpack Compose ------------------
    val composeBom = platform(libs.compose.bom)
    implementation(composeBom)
    globalTestImplementation(composeBom)

    implementation(libs.compose.ui)
    implementation(libs.compose.ui.graphics)
    // Material Design 3
    implementation(libs.compose.material3)
    // Integration with activities
    implementation(libs.compose.activity)
    // Integration with ViewModels
    implementation(libs.compose.viewmodel)
    // Android Studio Preview support
    implementation(libs.compose.preview)
    debugImplementation(libs.compose.tooling)
    // UI Tests
    globalTestImplementation(libs.compose.test.junit)
    debugImplementation(libs.compose.test.manifest)

    // Navigation for Compose
    implementation(libs.androidx.navigation.compose)

    // --------- Kaspresso test framework ----------
    globalTestImplementation(libs.kaspresso)
    globalTestImplementation(libs.kaspresso.compose)

    // ----------       Robolectric     ------------
    testImplementation(libs.robolectric)

    // --------------- firebase -------------------
    // Import the Firebase BoM
    implementation(platform("com.google.firebase:firebase-bom:32.8.1"))

    // When using the BoM, you don't specify versions in Firebase library dependencies

    // TODO: Add the dependencies for Firebase products you want to use
    // See https://firebase.google.com/docs/android/setup#available-libraries
    // For example, add the dependencies for Firebase Authentication and Cloud Firestore
    implementation("com.google.firebase:firebase-auth")
    implementation("com.google.firebase:firebase-firestore")

    // --------------- Google Maps -------------------
    implementation("com.google.android.gms:play-services-maps:19.0.0")
    implementation("com.google.maps.android:maps-compose:4.4.1")

    //test mock
    testImplementation("org.mockito:mockito-core:5.2.0")
    testImplementation("org.mockito:mockito-core:5.2.0")
}

tasks.withType<Test> {
    // Configure Jacoco for each tests
    configure<JacocoTaskExtension> {
        isIncludeNoLocationClasses = true
        excludes = listOf("jdk.internal.*")
    }
}

tasks.register("jacocoTestReport", JacocoReport::class) {
    mustRunAfter("testDebugUnitTest", "connectedDebugAndroidTest")

    reports {
        xml.required = true
        html.required = true
    }

    val fileFilter = listOf(
        "**/R.class",
        "**/R$*.class",
        "**/BuildConfig.*",
        "**/Manifest*.*",
        "**/*Test*.*",
        "android/**/*.*",
    )

    val debugTree = fileTree("${project.layout.buildDirectory.get()}/tmp/kotlin-classes/debug") {
        exclude(fileFilter)
    }

    val mainSrc = "${project.layout.projectDirectory}/src/main/java"
    sourceDirectories.setFrom(files(mainSrc))
    classDirectories.setFrom(files(debugTree))
    executionData.setFrom(fileTree(project.layout.buildDirectory.get()) {
        include("outputs/unit_test_code_coverage/debugUnitTest/testDebugUnitTest.exec")
        include("outputs/code_coverage/debugAndroidTest/connected/*/coverage.ec")
    })
}