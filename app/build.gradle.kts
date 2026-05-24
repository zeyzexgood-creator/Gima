import com.android.tools.build.apkzlib.zip.ZFile
import java.io.ByteArrayOutputStream
import java.nio.file.Files
import java.nio.file.Paths

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {

    signingConfigs {
        create("release") {
            keyAlias = "AIDE+"
            keyPassword = "123789456"
            storePassword = "123789456"
            storeFile = file("release.jks")
        }
        create("debug1") {
            keyAlias = "androiddebug"
            keyPassword = "123789456"
            storePassword = "123789456"
            storeFile = file("debug.jks")
        }
    }

    namespace = "io.github.zeroaicy.aide"
    compileSdk = 35

    val gitCommitHash: String by lazy {
        @Suppress("DEPRECATION")
        Runtime.getRuntime()
            .exec("git rev-parse HEAD")
            .inputStream
            .bufferedReader()
            .use { it.readText().trim() }
    }

    val getCommitHash: () -> String = {
        val stdout = ByteArrayOutputStream()
        exec {
            commandLine("git", "rev-parse", "--short", "HEAD")
            standardOutput = stdout
        }
        stdout.toString().trim()
    }

    defaultConfig {
        applicationId = "io.github.zeroaicy.aide"
        minSdk = 24
        targetSdk = 28
        versionCode = 2008210017
        versionName = "2.3.2.9-${getCommitHash()}"

        buildConfigField("String", "GIT_HASH", "\"${gitCommitHash}\"")

        // ✅ AKTIFKAN MultiDex - SOLUSI UNTUK 64K METHOD LIMIT
        multiDexEnabled = true

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    flavorDimensions.add("api")

    productFlavors {
        create("default") {
            dimension = "api"
            versionNameSuffix = ""
            isDefault = true
            signingConfig = signingConfigs.getByName("debug1")
        }

        create("termux") {
            dimension = "api"
            versionNameSuffix = "-termux"
            applicationId = "io.github.zeroaicy.aide2"
            dependencies {
                api(projects.termux.termuxApp)
            }
            signingConfig = signingConfigs.getByName("debug1")
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfig = signingConfigs.getByName("debug1")
        }
        debug {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfig = signingConfigs.getByName("debug1")
        }
    }
    
    compileOptions {
        isCoreLibraryDesugaringEnabled = isRelease
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }

    kotlinOptions {
        jvmTarget = "21"
    }

    packaging {
        resources {
            pickFirsts += "/META-INF/{AL2.0,LGPL2.1}"
            pickFirsts += "/assets/*/*/*/*/*/*/*"
            pickFirsts += "/assets/*/*/*/*/*/*"
            pickFirsts += "/assets/*/*/*/*/*"
            pickFirsts += "/assets/*/*/*/*"
            pickFirsts += "/assets/*/*/*"
            pickFirsts += "/assets/*/*"
            pickFirsts += "/assets/*"
            pickFirsts += "*/*/*/*/*/*/*/*/*/*/*/*/*/*/*"
            pickFirsts += "*/*/*/*/*/*/*/*/*/*/*/*/*/*"
            pickFirsts += "*/*/*/*/*/*/*/*/*/*/*/*/*"
            pickFirsts += "*/*/*/*/*/*/*/*/*/*//*/*"
            pickFirsts += "*/*/*/*/*/*/*/*/*/*/*/*"
            pickFirsts += "*/*/*/*/*/*/*/*/*/*/*"
            pickFirsts += "*/*/*/*/*/*/*/*/*/*"
            pickFirsts += "*/*/*/*/*/*/*/*/*"
            pickFirsts += "*/*/*/*/*/*/*/*"
            pickFirsts += "*/*/*/*/*/*/*"
            pickFirsts += "*/*/*/*/*/*"
            pickFirsts += "*/*/*/*/*"
            pickFirsts += "*/*/*/*"
            pickFirsts += "*/*/*"
            pickFirsts += "*/*"
            pickFirsts += "/*"
            pickFirsts += "about_files/*"
        }
        jniLibs {
            useLegacyPackaging = true
            pickFirsts += "/lib/*/*"
        }
    }
    
    androidResources {
        val publicXmlFile = project.rootProject.file("${project(":appAideBase").projectDir.path}/src/main/res/values/public.xml")
        val publicTxtFile = project.rootProject.file("${layout.buildDirectory.asFile.get().path}/public.txt")

        publicTxtFile.parentFile?.mkdirs()
        publicTxtFile.createNewFile()

        val nodes = javax.xml.parsers.DocumentBuilderFactory.newInstance()
            .newDocumentBuilder()
            .parse(publicXmlFile)
            .documentElement
            .getElementsByTagName("public")

        for (i in 0 until nodes.length) {
            val node = nodes.item(i)
            val type = node.attributes.getNamedItem("type").nodeValue
            val name = node.attributes.getNamedItem("name").nodeValue
            val id = node.attributes.getNamedItem("id").nodeValue
            publicTxtFile.appendText("${android.defaultConfig.applicationId}:$type/$name = $id\n")
        }

        @Suppress("DEPRECATION")
        additionalParameters("--stable-ids", publicTxtFile.path)
    }

    buildFeatures {
        buildConfig = true
        viewBinding = true
        renderScript = false
    }
    
    lint {
        abortOnError = false
        checkReleaseBuilds = false
    }
}

dependencies {
    implementation(fileTree(mapOf("dir" to "libs", "include" to listOf("*.jar", "*.aar"))))

    api(projects.appRewrite)

    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("com.google.android.material:material:1.13.0-alpha09")
    
    // ✅ DEPENDENCY MULTIDEX - WAJIB UNTUK MIN SDK < 21
    // Untuk minSdk 24, sebenarnya otomatis, tapi tetap ditambahkan untuk keamanan
    implementation("androidx.multidex:multidex:2.0.1")

    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.6.1")

    if (isRelease) {
        coreLibraryDesugaring("com.android.tools:desugar_jdk_lib:2.1.3")
    }
}

configurations.all {
    exclude("net.java.dev.jna", "jna")
    exclude("net.java.dev.jna", "jna-platform")
    exclude("org.bouncycastle", "bcprov-jdk15on")
}

val isRelease: Boolean = gradle.startParameter.taskNames.any { taskName ->
    taskName.contains("Release", ignoreCase = true)
}

tasks.withType<Copy> {
    duplicatesStrategy = DuplicatesStrategy.INCLUDE
}

// ✅ TIDAK ADA LAGI TASK MODIFIKASI MERGEDEX YANG BERMASALAH
// Semua kode afterEvaluate yang bermasalah telah dihapus
