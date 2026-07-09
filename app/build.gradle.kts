import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    // Added in Fase 1
    alias(libs.plugins.hilt.android)
    alias(libs.plugins.ksp)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.navigation.safeargs)
}

// ==========================================
// Lectura segura de variables de entorno
// Prioridad: local.properties (dev) > gradle.properties global (CI) > default
// ==========================================
val localProperties = Properties().apply {
    val f = rootProject.file("local.properties")
    if (f.exists()) f.inputStream().use { load(it) }
}

fun envVar(name: String, default: String = ""): String {
    return localProperties.getProperty(name)
        ?: (project.findProperty(name) as String?)
        ?: System.getenv(name)
        ?: default
}

android {
    flavorDimensions += "environment"
    namespace = "com.loresuelvo.consumer"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.loresuelvo.consumer"
        minSdk = 24
        targetSdk = 35

        // Added in Fase 1: HiltTestRunner for instrumented tests with Hilt
        testInstrumentationRunner = "com.loresuelvo.consumer.HiltTestRunner"
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

    // Added in Fase 1: enable Robolectric + Android resources in unit tests.
    testOptions {
        unitTests {
            isIncludeAndroidResources = true
            isReturnDefaultValues = true
        }
    }

    productFlavors {

        create("dev") {
            dimension = "environment"
            applicationIdSuffix = ".dev"
            versionNameSuffix = "-dev"

            val auth0Domain = envVar("AUTH0_DOMAIN", "loresuelvo-dev.auth0.com")
            val auth0ClientId = envVar("AUTH0_CLIENT_ID")
            val auth0Scheme = envVar("AUTH0_SCHEME", "com.loresuelvo.consumer")
            val apiUrl = envVar("API_URL", "http://10.0.2.2:8080")

            buildConfigField("String", "API_URL", "\"$apiUrl\"")
            buildConfigField("String", "AUTH0_DOMAIN", "\"$auth0Domain\"")
            buildConfigField("String", "AUTH0_CLIENT_ID", "\"$auth0ClientId\"")
            buildConfigField("String", "AUTH0_SCHEME", "\"$auth0Scheme\"")

            manifestPlaceholders["auth0Domain"] = auth0Domain
            manifestPlaceholders["auth0Scheme"] = auth0Scheme
        }

        create("staging") {
            dimension = "environment"
            applicationIdSuffix = ".staging"
            versionNameSuffix = "-staging"

            val auth0Domain = envVar("AUTH0_DOMAIN_STAGING")
            val auth0ClientId = envVar("AUTH0_CLIENT_ID_STAGING")
            val auth0Scheme = envVar("AUTH0_SCHEME_STAGING", "com.loresuelvo.consumer.staging")
            val apiUrl = envVar("API_URL_STAGING")

            buildConfigField("String", "API_URL", "\"$apiUrl\"")
            buildConfigField("String", "AUTH0_DOMAIN", "\"$auth0Domain\"")
            buildConfigField("String", "AUTH0_CLIENT_ID", "\"$auth0ClientId\"")
            buildConfigField("String", "AUTH0_SCHEME", "\"$auth0Scheme\"")

            manifestPlaceholders["auth0Domain"] = auth0Domain
            manifestPlaceholders["auth0Scheme"] = auth0Scheme
        }

        create("prod") {
            dimension = "environment"
            // sin suffix: este va a Play Store

            val auth0Domain = envVar("AUTH0_DOMAIN_PROD")
            val auth0ClientId = envVar("AUTH0_CLIENT_ID_PROD")
             val auth0Scheme = envVar("AUTH0_SCHEME_PROD", "com.loresuelvo.consumer.prod")
            val apiUrl = envVar("API_URL_PROD")

            buildConfigField("String", "API_URL", "\"$apiUrl\"")
            buildConfigField("String", "AUTH0_DOMAIN", "\"$auth0Domain\"")
            buildConfigField("String", "AUTH0_CLIENT_ID", "\"$auth0ClientId\"")
            buildConfigField("String", "AUTH0_SCHEME", "\"$auth0Scheme\"")

            manifestPlaceholders["auth0Domain"] = auth0Domain
            manifestPlaceholders["auth0Scheme"] = auth0Scheme
        }
    }
}

// ==========================================
// Validación fail-fast: solo exige las vars
// del flavor que realmente se está compilando
// ==========================================
gradle.taskGraph.whenReady {
    val runningTasks = allTasks.map { it.name }

    val requiredForStaging = listOf(
        "AUTH0_DOMAIN_STAGING", "AUTH0_CLIENT_ID_STAGING",
        "AUTH0_SCHEME_STAGING", "API_URL_STAGING"
    )
    val requiredForProd = listOf(
        "AUTH0_DOMAIN_PROD", "AUTH0_CLIENT_ID_PROD",
        "AUTH0_SCHEME_PROD", "API_URL_PROD"
    )

    if (runningTasks.any { it.contains("Staging") }) {
        requiredForStaging.forEach {
            check(envVar(it).isNotBlank()) { "Falta la variable $it para build de STAGING" }
        }
    }
    if (runningTasks.any { it.contains("Prod") }) {
        requiredForProd.forEach {
            check(envVar(it).isNotBlank()) { "Falta la variable $it para build de PROD" }
        }
    }
}

dependencies {
    // Icons & Core
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.activity.compose)

    // Compose BOM (Bill of Materials) - Controla las versiones de todas las librerías de Compose
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.junit.ktx)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.security.crypto)

    // Hilt (added in Fase 1)
    implementation(libs.hilt.android)
    implementation(libs.androidx.hilt.navigation.compose)
    ksp(libs.hilt.android.compiler)

    // Networking (added in Fase 1)
    implementation(libs.retrofit)
    implementation(libs.retrofit.kotlinx.serialization.converter)
    implementation(libs.okhttp)
    implementation(libs.okhttp.logging.interceptor)
    implementation(libs.kotlinx.serialization.json)

    // Unit Testing (Capa de Dominio - src/test)
    testImplementation(libs.junit)
    testImplementation(libs.mockk)
    testImplementation(libs.turbine)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.robolectric)
    testImplementation(libs.okhttp.mockwebserver)
    testImplementation(libs.cucumber.java)
    testImplementation(libs.cucumber.junit)

    // UI Testing (Capa de Aceptación/UI - src/androidTest)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.espresso.intents)
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    // Hilt testing (added in Fase 1)
    androidTestImplementation(libs.hilt.android.testing)
    kspAndroidTest(libs.hilt.android.compiler)

    // Debugging (Previews y Manifest para tests)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)

    // Auth0
    implementation("com.auth0.android:auth0:2.11.0")
    testImplementation(kotlin("test"))
}
