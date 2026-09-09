plugins {
    id("passbolt.android.library")
    id(libs.plugins.compose.compiler.get().pluginId)
}

dependencies {
    implementation(project(":accounts-domain"))
    implementation(project(":architecture"))
    implementation(project(":coreui"))
    implementation(project(":common"))
    implementation(project(":mappers"))
    implementation(project(":entity"))
    implementation(project(":auth-domain"))
    implementation(project(":mfa-domain"))
    implementation(project(":dto"))
    implementation(project(":gopenpgp"))
    implementation(project(":navigation"))
    implementation(project(":featureflags-domain"))
    implementation(project(":database"))
    implementation(project(":uimodel"))
    implementation(project(":security"))
    implementation(project(":logger"))
    implementation(project(":localization"))
    implementation(project(":featureflagserror"))
    implementation(project(":helpmenu"))
    implementation(project(":logs"))
    implementation(project(":inappreview-domain"))
    implementation(project(":idlingresource"))
    implementation(project(":rbac-domain"))
    implementation(project(":biometrickey-domain"))
    implementation(project(":privatekey-domain"))
    implementation(project(":testtags"))
    implementation(project(":passphrasememorycache"))
    implementation(project(":encryptedstorage"))
    implementation(project(":preferences-domain"))
    implementation(project(":clipboard"))
    implementation(project(":secrets-domain"))

    implementation(libs.androidx.navigation3.runtime)
    implementation(libs.androidx.core)
    implementation(libs.biometric)
    implementation(platform(libs.koin.bom))
    implementation(libs.koin)
    implementation(libs.koin.compose)
    implementation(libs.gson)
    implementation(libs.fusionauth.jwt)
    implementation(libs.yubikit.android)
    implementation(libs.espresso.idling.resource)
    implementation(platform(libs.compose.bom))
    implementation(libs.compose.foundation)
    implementation(libs.compose.activity)
    implementation(libs.compose.material3)
    implementation(libs.compose.ui.tooling)
    debugImplementation(libs.compose.ui.tooling.preview)

    testImplementation(project(":commontest"))
    testImplementation(platform(libs.koin.bom))
    testImplementation(libs.koin.test.junit)
    testImplementation(libs.kotlin.reflect)
}

android {
    namespace = "com.passbolt.mobile.android.feature.authentication"
    buildFeatures {
        compose = true
    }
}
