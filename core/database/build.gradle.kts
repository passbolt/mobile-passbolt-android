plugins {
    id("passbolt.android.library")
    alias(libs.plugins.kotlin.ksp)
    alias(libs.plugins.room)
}

dependencies {
    implementation(project(":auth-domain"))
    implementation(project(":accounts-domain"))
    implementation(project(":entity"))
    implementation(project(":common"))

    implementation(platform(libs.koin.bom))
    implementation(libs.koin)
    api(libs.room.runtime)
    implementation(libs.sqlite.cipher) { artifact { type = "aar" } }
    ksp(libs.room.compiler)
    implementation(libs.sqlite)
    implementation(libs.gson)
    implementation(libs.paging.runtime)
    implementation(libs.room.paging)

    androidTestImplementation(platform(libs.koin.bom))
    androidTestImplementation(libs.koin.test)
    androidTestImplementation(libs.koin.test.junit)
    androidTestImplementation(libs.kotlin.coroutines.test)
    androidTestImplementation(libs.android.tests.runner)
    androidTestImplementation(libs.android.test.rules)
    androidTestImplementation(libs.room.testing)
}

android {
    namespace = "com.passbolt.mobile.android.core.database"

    sourceSets {
        getByName("androidTest").assets.directories.add("$projectDir/schemas")
    }
    packaging {
        resources.excludes += "META-INF/{AL2.0,LGPL2.1}"
    }
}

room3 {
    schemaDirectory("$projectDir/schemas")
}
