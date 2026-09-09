plugins {
    id("passbolt.android.library")
    id(libs.plugins.compose.compiler.get().pluginId)
    id(libs.plugins.kotlin.serialization.get().pluginId)
}

dependencies {
    implementation(project(":accounts-domain"))
    implementation(project(":architecture"))
    implementation(project(":coreui"))
    implementation(project(":common"))
    implementation(project(":authentication"))
    implementation(project(":navigation"))
    implementation(project(":security"))
    implementation(project(":dto"))
    implementation(project(":gopenpgp"))
    implementation(project(":mobiletransfer-domain"))
    implementation(project(":uimodel"))
    implementation(project(":idlingresource"))
    implementation(project(":localization"))
    implementation(project(":auth-domain"))
    implementation(project(":privatekey-domain"))
    implementation(project(":testtags"))

    implementation(platform(libs.koin.bom))
    implementation(libs.koin)
    implementation(libs.koin.compose)
    implementation(libs.gson)
    implementation(libs.kotlin.serializationjson)
    implementation(libs.qr.generation)
    implementation(libs.okio)
    implementation(libs.espresso.idling.resource)
    implementation(platform(libs.compose.bom))
    implementation(libs.compose.foundation)
    implementation(libs.compose.icons)
    implementation(libs.compose.material3)
    implementation(libs.compose.lifecycle.viewmodel)
    implementation(libs.compose.ui.tooling)
    debugImplementation(libs.compose.ui.tooling.preview)
    implementation(libs.compose.activity)
    implementation(libs.androidx.navigation3.ui)
    implementation(libs.androidx.navigation3.runtime)
    implementation(libs.androidx.lifecycle.viewmodel.navigation3)

    testImplementation(project(":commontest"))
    testImplementation(project(":passphrasememorycache"))
    testImplementation(project(":secrets-domain"))
}

android {
    namespace = "com.passbolt.mobile.android.feature.transferaccounttoanotherdevice"
    buildFeatures {
        compose = true
    }
}
