import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.openapi.generator)
}

// the values of secrets.properties (git ignored), like ios/Config/Secrets.xcconfig
val secrets = Properties().apply {
    val file = rootProject.file("secrets.properties")
    if (file.exists()) file.inputStream().use { load(it) }
}

fun secret(key: String, default: String = ""): String = secrets.getProperty(key)?.trim() ?: default

val apiBaseUrl = secret("API_BASE_URL", "http://10.0.2.2:8080/api")

// the client of the backend is generated from the shared contract, a changed DTO breaks the build
val generatedApiDir = layout.buildDirectory.dir("generated/openapi")
val sourceSpec = rootProject.file("../api/openapi.json")
val normalizedSpec = layout.buildDirectory.file("openapi/openapi.json")

// the kotlin generator only understands the openapi 3.0 way of nullability, and merges a oneOf into
// one class with all properties required. Translate both, the contract itself stays api/openapi.json.
val normalizeApiSpec by tasks.registering {
    inputs.file(sourceSpec)
    outputs.file(normalizedSpec)
    doLast {
        @Suppress("UNCHECKED_CAST")
        val spec = groovy.json.JsonSlurper().parse(sourceSpec) as MutableMap<String, Any?>
        OpenApiNormalizer(spec).run()
        val target = normalizedSpec.get().asFile
        target.parentFile.mkdirs()
        target.writeText(groovy.json.JsonOutput.prettyPrint(groovy.json.JsonOutput.toJson(spec)))
    }
}

openApiGenerate {
    generatorName.set("kotlin")
    library.set("jvm-retrofit2")
    inputSpec.set(normalizedSpec.get().asFile.absolutePath)
    outputDir.set(generatedApiDir.get().asFile.absolutePath)
    packageName.set("tech.project101.labs.api")
    configOptions.set(
        mapOf(
            "serializationLibrary" to "kotlinx_serialization",
            "useCoroutines" to "true",
            "dateLibrary" to "java8",
            "enumPropertyNaming" to "UPPERCASE",
            "omitGradleWrapper" to "true",
            "sourceFolder" to "src/main/kotlin",
        ),
    )
    generateApiTests.set(false)
    generateModelTests.set(false)
    generateApiDocumentation.set(false)
    generateModelDocumentation.set(false)
}

android {
    namespace = "tech.project101.labs"
    compileSdk = 36

    defaultConfig {
        applicationId = "tech.project101.labs"
        minSdk = 28
        targetSdk = 36
        versionCode = 1
        versionName = "0.1.0"

        buildConfigField("String", "API_BASE_URL", "\"$apiBaseUrl\"")
        buildConfigField("String", "AUTH_ISSUER", "\"${secret("AUTH_ISSUER")}\"")
        buildConfigField("String", "AUTH_CLIENT_ID", "\"${secret("AUTH_CLIENT_ID")}\"")
        // a backend in the local network usually runs without tls
        manifestPlaceholders["usesCleartextTraffic"] = apiBaseUrl.startsWith("http://").toString()
    }

    signingConfigs {
        val storeFile = secret("RELEASE_STORE_FILE")
        if (storeFile.isNotEmpty()) {
            create("release") {
                this.storeFile = rootProject.file(storeFile)
                storePassword = secret("RELEASE_STORE_PASSWORD")
                keyAlias = secret("RELEASE_KEY_ALIAS")
                keyPassword = secret("RELEASE_KEY_PASSWORD")
            }
        }
    }

    buildTypes {
        release {
            // r8 drops the unused icons, the apk shrinks from about 65 to a few MB
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            // without an own key the release apk is signed with the debug key of this machine,
            // updates then only install over it from the same machine
            signingConfig = signingConfigs.findByName("release") ?: signingConfigs.getByName("debug")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }

    kotlin {
        jvmToolchain(21)
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    sourceSets["main"].java.srcDir(generatedApiDir.map { it.dir("src/main/kotlin") })
}

tasks.named("openApiGenerate") { dependsOn(normalizeApiSpec) }
tasks.named("preBuild") { dependsOn(tasks.named("openApiGenerate")) }

dependencies {
    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.compose.material3)
    implementation(libs.compose.material.icons)
    debugImplementation(libs.compose.ui.tooling)

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.browser)

    implementation(libs.okhttp)
    implementation(libs.okhttp.logging)
    implementation(libs.retrofit)
    implementation(libs.retrofit.scalars)
    implementation(libs.retrofit.kotlinx.serialization)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.kotlinx.coroutines.android)

    implementation(libs.play.services.code.scanner)
}
