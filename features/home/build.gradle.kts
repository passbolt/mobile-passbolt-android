plugins {
    id("passbolt.android.library")
    id(libs.plugins.compose.compiler.get().pluginId)
    id(libs.plugins.kotlin.serialization.get().pluginId)
    alias(libs.plugins.screenshot)
}

dependencies {
    implementation(project(":accounts-domain"))
    implementation(project(":architecture"))
    implementation(project(":uimodel"))
    implementation(project(":navigation"))
    implementation(project(":coreui"))
    implementation(project(":common"))
    implementation(project(":networking"))
    implementation(project(":mappers"))
    implementation(project(":resources-domain"))
    implementation(project(":database"))
    implementation(project(":secrets-domain"))
    implementation(project(":authentication"))
    implementation(project(":entity"))
    implementation(project(":localization"))
    implementation(project(":featureflags-domain"))
    implementation(project(":folderdetails"))
    implementation(project(":groupdetails"))
    implementation(project(":users-domain"))
    implementation(project(":gopenpgp"))
    implementation(project(":createfolder"))
    implementation(project(":resourcemoremenu"))
    implementation(project(":resourcedetails"))
    implementation(project(":fulldatarefresh"))
    implementation(project(":idlingresource"))
    implementation(project(":supportedresourcetypes"))
    implementation(project(":folders-domain"))
    implementation(project(":groups-domain"))
    implementation(project(":tags-domain"))
    implementation(project(":scanotp"))
    implementation(project(":otpmoremenu"))
    implementation(project(":otpcore"))
    implementation(project(":rbac-domain"))
    implementation(project(":serializers"))
    implementation(project(":autofill"))
    implementation(project(":preferences-domain"))
    implementation(project(":jsonmodel"))
    implementation(project(":createresourcemenu"))
    implementation(project(":resourceform"))
    implementation(project(":permissions"))
    implementation(project(":clipboard"))
    implementation(project(":metadata-domain"))
    implementation(project(":testtags"))

    implementation(platform(libs.koin.bom))
    implementation(libs.koin)
    implementation(libs.koin.compose)
    implementation(libs.espresso.idling.resource)
    implementation(platform(libs.compose.bom))
    implementation(libs.compose.icons)
    implementation(libs.compose.material3)
    implementation(libs.compose.lifecycle.viewmodel)
    implementation(libs.compose.ui.tooling)
    debugImplementation(libs.compose.ui.tooling.preview)
    implementation(libs.compose.activity)
    implementation(libs.compose.foundation)
    implementation(libs.accompanist.drawablepainter)
    implementation(libs.androidx.navigation3.ui)
    implementation(libs.androidx.navigation3.runtime)
    implementation(libs.androidx.lifecycle.viewmodel.navigation3)
    implementation(libs.paging.compose)

    testImplementation(libs.gson)
    testImplementation(libs.json.path)
    testImplementation(project(":commontest"))

    screenshotTestImplementation(project(":screenshottest"))
    screenshotTestImplementation(libs.screenshot.validation.api)
    screenshotTestImplementation(libs.compose.ui.tooling)
}

android {
    namespace = "com.passbolt.mobile.android.feature.home"
    buildFeatures {
        compose = true
    }
    experimentalProperties["android.experimental.enableScreenshotTest"] = true

    // tolerate sub-pixel antialiasing differences between macOS (dev) and Linux (CI) layoutlib renderers
    screenshotTests {
        imageDifferenceThreshold = 0.001f
    }
}
