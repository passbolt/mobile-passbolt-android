plugins {
    id("passbolt.android.library")
    id(libs.plugins.compose.compiler.get().pluginId)
}

dependencies {
    implementation(project(":common"))
    implementation(project(":architecture"))
    implementation(project(":mappers"))
    implementation(project(":uimodel"))
    implementation(project(":networking"))
    implementation(project(":database"))
    implementation(project(":gopenpgp"))
    implementation(project(":featureflags-domain"))
    implementation(project(":authentication"))
    implementation(project(":coreui"))
    implementation(project(":permissions"))
    implementation(project(":locationdetails"))
    implementation(project(":navigation"))
    implementation(project(":fulldatarefresh"))
    implementation(project(":folders-domain"))
    implementation(project(":localization"))
    implementation(project(":rbac-domain"))

    implementation(platform(libs.koin.bom))
    implementation(libs.koin)
    implementation(libs.koin.compose)

    implementation(platform(libs.compose.bom))
    implementation(libs.compose.foundation)
    implementation(libs.compose.material3)
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.compose.activity)
    implementation(libs.compose.lifecycle.viewmodel)
    implementation(libs.coil.compose)
    implementation(libs.androidx.navigation3.runtime)

    debugImplementation(libs.compose.ui.tooling)
    debugImplementation(libs.compose.ui.test.manifest)

    testImplementation(project(":commontest"))
}

android {
    namespace = "com.passbolt.mobile.android.feature.folderdetails"
    buildFeatures {
        compose = true
    }
}
