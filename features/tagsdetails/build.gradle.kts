plugins {
    id("passbolt.android.library")
    id(libs.plugins.compose.compiler.get().pluginId)
}

dependencies {
    implementation(project(":architecture"))
    implementation(project(":coreui"))
    implementation(project(":common"))
    implementation(project(":secrets-domain"))
    implementation(project(":uimodel"))
    implementation(project(":networking"))
    implementation(project(":database"))
    implementation(project(":featureflags-domain"))
    implementation(project(":security"))
    implementation(project(":authentication"))
    implementation(project(":mappers"))
    implementation(project(":localization"))
    implementation(project(":groupdetails"))
    implementation(project(":gopenpgp"))
    implementation(project(":permissions"))
    implementation(project(":locationdetails"))
    implementation(project(":fulldatarefresh"))
    implementation(project(":navigation"))
    implementation(project(":resources-domain"))
    implementation(project(":jsonmodel"))

    implementation(platform(libs.koin.bom))
    implementation(libs.koin)
    implementation(libs.koin.compose)
    implementation(platform(libs.compose.bom))
    implementation(libs.compose.foundation)
    implementation(libs.compose.ui.graphics)
    implementation(libs.compose.material3)
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.compose.lifecycle.viewmodel)
    implementation(libs.accompanist.drawablepainter)
    implementation(libs.androidx.navigation3.runtime)

    debugImplementation(libs.compose.ui.tooling)

    testImplementation(project(":commontest"))
    testImplementation(libs.gson)
    testImplementation(libs.json.path)
}

android {
    namespace = "com.passbolt.mobile.android.feature.tagsdetails"
    buildFeatures {
        compose = true
    }
}
