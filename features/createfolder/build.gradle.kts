plugins {
    id("passbolt.android.library")
    id(libs.plugins.compose.compiler.get().pluginId)
}

dependencies {
    implementation(project(":common"))
    implementation(project(":uimodel"))
    implementation(project(":coreui"))
    implementation(project(":architecture"))
    implementation(project(":authentication"))
    implementation(project(":database"))
    implementation(project(":groupdetails"))
    implementation(project(":mappers"))
    implementation(project(":permissions"))
    implementation(project(":folderdetails"))
    implementation(project(":networking"))
    implementation(project(":folders-domain"))
    implementation(project(":idlingresource"))
    implementation(project(":users-domain"))
    implementation(project(":localization"))
    implementation(project(":accounts-domain"))
    implementation(project(":navigation"))
    implementation(project(":testtags"))

    implementation(platform(libs.koin.bom))
    implementation(libs.koin)
    implementation(libs.koin.compose)
    implementation(libs.sqlite)
    implementation(libs.espresso.idling.resource)

    implementation(platform(libs.compose.bom))
    implementation(libs.compose.foundation)
    implementation(libs.compose.material3)
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.compose.activity)
    implementation(libs.compose.lifecycle.viewmodel)
    implementation(libs.coil.compose)
    implementation(libs.androidx.navigation3.runtime)
    debugImplementation(libs.compose.ui.tooling)

    testImplementation(project(":commontest"))
    testImplementation(project(":passphrasememorycache"))
    testImplementation(project(":secrets-domain"))
}

android {
    namespace = "com.passbolt.mobile.android.feature.createfolder"
    buildFeatures {
        compose = true
    }
}
