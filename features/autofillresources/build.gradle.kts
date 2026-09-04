plugins {
    id("passbolt.android.library")
    id(libs.plugins.compose.compiler.get().pluginId)
}

dependencies {
    implementation(project(":accounts-domain"))
    implementation(project(":architecture"))
    implementation(project(":coreui"))
    implementation(project(":common"))
    implementation(project(":navigation"))
    implementation(project(":database"))
    implementation(project(":networking"))
    implementation(project(":uimodel"))
    implementation(project(":mappers"))
    implementation(project(":secrets-domain"))
    implementation(project(":authentication"))
    implementation(project(":resources-domain"))
    implementation(project(":localization"))
    implementation(project(":home"))
    implementation(project(":security"))
    implementation(project(":gopenpgp"))
    implementation(project(":notifications"))
    implementation(project(":autofill"))
    implementation(project(":jsonmodel"))
    implementation(project(":entity"))
    implementation(project(":otpcore"))
    implementation(project(":clipboard"))
    implementation(project(":preferences-domain"))
    implementation(project(":otp"))
    implementation(project(":supportedresourcetypes"))

    implementation(platform(libs.koin.bom))
    implementation(libs.koin)
    implementation(libs.koin.compose)
    implementation(platform(libs.compose.bom))
    implementation(libs.compose.material3)
    implementation(libs.compose.lifecycle.viewmodel)
    implementation(libs.compose.activity)
    implementation(libs.compose.foundation)
    implementation(libs.glance.appwidget)
    implementation(libs.glance.material3)

    testImplementation(project(":commontest"))
    testImplementation(libs.gson)
    testImplementation(libs.json.path)
}

android {
    namespace = "com.passbolt.mobile.android.feature.autofill"
    buildFeatures {
        compose = true
        viewBinding = true
    }
}
