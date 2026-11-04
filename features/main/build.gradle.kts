plugins {
    id("passbolt.android.library")
    id(libs.plugins.compose.compiler.get().pluginId)
}

dependencies {
    implementation(project(":accounts-domain"))
    implementation(project(":architecture"))
    implementation(project(":uimodel"))
    implementation(project(":coreui"))
    implementation(project(":home"))
    implementation(project(":settings"))
    implementation(project(":common"))
    implementation(project(":localization"))
    implementation(project(":authentication"))
    implementation(project(":security"))
    implementation(project(":fulldatarefresh"))
    implementation(project(":inappreview-domain"))
    implementation(project(":otp"))
    implementation(project(":database"))
    implementation(project(":entity"))
    implementation(project(":scanotp"))
    implementation(project(":resourcepicker"))
    implementation(project(":featureflags-domain"))
    implementation(project(":autofillresources"))
    implementation(project(":preferences-domain"))
    implementation(project(":navigation"))
    implementation(project(":autofill"))
    implementation(project(":testtags"))

    implementation(libs.fragment)

    implementation(platform(libs.koin.bom))
    implementation(libs.koin)
    implementation(libs.koin.compose)
    implementation(libs.inappupdate.core)
    implementation(libs.inappupdate.ktx)
    implementation(libs.inappreview.core)
    implementation(libs.inappreview.ktx)
    implementation(libs.androidx.navigation3.runtime)

    implementation(platform(libs.compose.bom))
    implementation(libs.compose.activity)
    implementation(libs.compose.foundation)
    implementation(libs.compose.ui.graphics)
    implementation(libs.compose.material3)
    implementation(libs.compose.lifecycle.viewmodel)
    implementation(libs.compose.ui.tooling)
    debugImplementation(libs.compose.ui.tooling.preview)

    testImplementation(project(":commontest"))
}

android {
    namespace = "com.passbolt.mobile.android.feature.main"
    buildFeatures {
        compose = true
    }
}
