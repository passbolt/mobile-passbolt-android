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
    implementation(project(":networking"))
    implementation(project(":database"))
    implementation(project(":gopenpgp"))
    implementation(project(":localization"))
    implementation(project(":resources-domain"))
    implementation(project(":supportedresourcetypes"))
    implementation(project(":fulldatarefresh"))
    implementation(project(":authentication"))
    implementation(project(":entity"))
    implementation(project(":rbac-domain"))
    implementation(project(":idlingresource"))
    implementation(project(":featureflags-domain"))
    implementation(project(":jsonmodel"))
    implementation(project(":secrets-domain"))
    implementation(project(":preferences-domain"))
    implementation(project(":accounts-domain"))

    implementation(libs.androidx.core)
    implementation(platform(libs.koin.bom))
    implementation(libs.koin)
    implementation(libs.koin.compose)
    implementation(libs.room.core)
    implementation(libs.espresso.idling.resource)
    implementation(platform(libs.compose.bom))
    implementation(libs.compose.material3)
    implementation(libs.compose.lifecycle.viewmodel)
    implementation(libs.compose.ui.tooling)
    debugImplementation(libs.compose.ui.tooling.preview)
    implementation(libs.compose.foundation)

    testImplementation(project(":commontest"))
}

android {
    namespace = "com.passbolt.mobile.android.feature.resourcemoremenu"
    buildFeatures {
        compose = true
    }
}
