import java.util.Properties

plugins {
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.jetbrainsKotlinAndroid)
    id("com.google.gms.google-services")
    id("kotlin-kapt")
}


android {
    namespace = "com.example.aplikaceprochronickpacienty"
    compileSdk = 34

    packagingOptions {
        resources.excludes.add("META-INF/*")
    }

    buildFeatures {
        buildConfig = true
    }

    defaultConfig {
        applicationId = "com.example.aplikaceprochronickpacienty"
        minSdk = 26
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        val properties = Properties()
        properties.load(project.rootProject.file("local.properties").inputStream())

        buildConfigField("String", "OPENAI_API_KEY", properties.getProperty("OPENAI_API_KEY"))

    }

    buildTypes {
        release {
            isMinifyEnabled = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }

    kotlinOptions {
        jvmTarget = "1.8"
    }

    buildFeatures {
        viewBinding = true
    }

//    sourceSets {
//
//        val main = getByName("main")
//
//        main.resources.srcDirs("src/main/jniLibs")
//    }

    dependencies {

        implementation(libs.androidx.core.ktx)
        implementation(libs.androidx.appcompat)
        implementation(libs.material)
        implementation(libs.androidx.constraintlayout)
        implementation(libs.androidx.lifecycle.livedata.ktx)
        implementation(libs.androidx.lifecycle.viewmodel.ktx)
        implementation(libs.androidx.navigation.fragment.ktx)
        implementation(libs.androidx.navigation.ui.ktx)
        implementation(libs.google.material)
        implementation(libs.androidx.activity)
        implementation(libs.androidx.annotation)
        implementation(libs.firebase.database.ktx)
        testImplementation(libs.junit)
        androidTestImplementation(libs.androidx.junit)
        androidTestImplementation(libs.androidx.espresso.core)

        //Dialogflow ES
        implementation("com.google.cloud:google-cloud-dialogflow:2.1.0")
        implementation("io.grpc:grpc-okhttp:1.30.0")

        //Kotlin
        implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.6.4")

        //OpenAI
        implementation("com.aallam.openai:openai-client:3.6.2")

        // Compose Runtime
        implementation("androidx.compose.runtime:runtime:1.5.4")
        implementation("androidx.compose.runtime:runtime-livedata:1.5.4")
        implementation("androidx.compose.runtime:runtime-rxjava2:1.5.4")

        // OkHttp
        implementation("com.squareup.okhttp3:okhttp:4.12.0")

        // Firebase database
        implementation(libs.firebase.auth)
        implementation("com.google.android.gms:play-services-auth:20.7.0")
        implementation(platform("com.google.firebase:firebase-bom:32.7.0"))

        // Room database
        val room_version = "2.6.1"
        implementation("androidx.room:room-ktx:$room_version")
        kapt("androidx.room:room-compiler:$room_version")

        // Lifecycle components
        implementation("androidx.lifecycle:lifecycle-extensions:2.2.0")
        implementation("androidx.lifecycle:lifecycle-common-java8:2.2.0")
        implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.2.0")

        // CSV
        implementation("com.github.doyaaaaaken:kotlin-csv-jvm:1.9.2")

        // Donut
        implementation("app.futured.donut:donut:2.2.0")

        // Charts
        implementation("com.diogobernardino:williamchart:3.10.1")

        // PDF Viewer
        implementation("com.github.mhiew:android-pdf-viewer:3.2.0-beta.3")

        // Rounded ImageView
        implementation("com.makeramen:roundedimageview:2.3.0")

        // LangChain
        implementation("dev.langchain4j:langchain4j:0.27.1")
        implementation("dev.langchain4j:langchain4j-open-ai:0.27.1")
        implementation("dev.langchain4j:langchain4j-embeddings-all-minilm-l6-v2:0.27.1")
        implementation("org.tinylog:tinylog-impl:2.6.2")
        implementation("org.tinylog:slf4j-tinylog:2.6.2")

    }
}