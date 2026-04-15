plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.google.gms.google.services)
}

android {
    namespace = "com.example.myapplication"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.example.myapplication"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
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
}

dependencies {
    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)
    implementation(libs.firebase.auth)
    implementation(libs.firebase.database)

    // ✅ הוספת הספרייה שחסרה לך כדי שהיומן יעבוד
    implementation("com.github.prolificinteractive:material-calendarview:2.0.1")
    // ✅ הוספת ThreeTenABP לפתרון שגיאת ה-LocalDate
    implementation("com.jakewharton.threetenabp:threetenabp:1.4.4")

    implementation(libs.recyclerview)
    implementation("androidx.cardview:cardview:1.0.0")
}