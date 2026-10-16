plugins {
    id("passbolt.android.library")
    id(libs.plugins.compose.compiler.get().pluginId)
}

dependencies {
    implementation(project(":common"))
    implementation(project(":architecture"))
    implementation(project(":mappers"))
    implementation(project(":uimodel"))
    implementation(project(":coreui"))
    implementation(project(":database"))
    implementation(project(":localization"))
    implementation(project(":fulldatarefresh"))
    implementation(project(":authentication"))
    implementation(project(":supportedresourcetypes"))
    implementation(project(":resources-domain"))
    implementation(project(":jsonmodel"))
    implementation(project(":navigation"))

    implementation(libs.androidx.core)
    implementation(platform(libs.koin.bom))
    implementation(libs.koin)
    implementation(libs.koin.compose)
    implementation(platform(libs.compose.bom))
    implementation(libs.compose.foundation)
    implementation(libs.compose.icons)
    implementation(libs.compose.material3)
    implementation(libs.compose.lifecycle.viewmodel)
    implementation(libs.compose.ui.tooling)
    debugImplementation(libs.compose.ui.tooling.preview)
    implementation(libs.compose.activity)
    implementation(libs.accompanist.drawablepainter)
    implementation(libs.androidx.navigation3.ui)
    implementation(libs.androidx.navigation3.runtime)
    implementation(libs.androidx.lifecycle.viewmodel.navigation3)
    implementation(libs.paging.compose)

    testImplementation(project(":commontest"))
    testImplementation(libs.gson)
    testImplementation(libs.json.path)
}

android {
    namespace = "com.passbolt.mobile.android.feature.resourcepicker"
    buildFeatures {
        compose = true
    }
}
