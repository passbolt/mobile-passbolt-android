plugins {
    id("passbolt.android.library")
    id(libs.plugins.compose.compiler.get().pluginId)
    alias(libs.plugins.screenshot)
}

dependencies {
    implementation(project(":architecture"))
    implementation(project(":navigation"))
    implementation(project(":coreui"))
    implementation(project(":common"))
    implementation(project(":secrets-domain"))
    implementation(project(":uimodel"))
    implementation(project(":networking"))
    implementation(project(":database"))
    implementation(project(":entity"))
    implementation(project(":featureflags-domain"))
    implementation(project(":security"))
    implementation(project(":authentication"))
    implementation(project(":mappers"))
    implementation(project(":localization"))
    implementation(project(":groupdetails"))
    implementation(project(":gopenpgp"))
    implementation(project(":permissions"))
    implementation(project(":locationdetails"))
    implementation(project(":resourcemoremenu"))
    implementation(project(":resources-domain"))
    implementation(project(":passwordgenerator"))
    implementation(project(":tagsdetails"))
    implementation(project(":fulldatarefresh"))
    implementation(project(":idlingresource"))
    implementation(project(":otpcore"))
    implementation(project(":supportedresourcetypes"))
    implementation(project(":folders-domain"))
    implementation(project(":otpmoremenu"))
    implementation(project(":scanotp"))
    implementation(project(":rbac-domain"))
    implementation(project(":serializers"))
    implementation(project(":jsonmodel"))
    implementation(project(":resourceform"))
    implementation(project(":clipboard"))
    implementation(project(":metadata-domain"))
    implementation(project(":testtags"))

    implementation(platform(libs.koin.bom))
    implementation(libs.koin)
    implementation(libs.koin.compose)
    implementation(libs.espresso.idling.resource)
    implementation(libs.jsonschema.friend)
    implementation(platform(libs.compose.bom))
    implementation(libs.compose.foundation)
    implementation(libs.compose.material3)
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.compose.lifecycle.viewmodel)
    implementation(libs.coil.compose)
    implementation(libs.accompanist.drawablepainter)
    implementation(libs.androidx.navigation3.runtime)

    debugImplementation(libs.compose.ui.tooling)
    debugImplementation(libs.compose.ui.test.manifest)

    testImplementation(project(":commontest"))
    testImplementation(project(":passphrasememorycache"))
    testImplementation(libs.gson)
    testImplementation(libs.json.path)

    screenshotTestImplementation(project(":screenshottest"))
    screenshotTestImplementation(project(":jsonmodel"))
    screenshotTestImplementation(libs.screenshot.validation.api)
    screenshotTestImplementation(libs.compose.ui.tooling)
}

android {
    namespace = "com.passbolt.mobile.android.feature.resources"
    buildFeatures {
        compose = true
    }
    experimentalProperties["android.experimental.enableScreenshotTest"] = true

    // tolerate sub-pixel antialiasing differences between macOS (dev) and Linux (CI) layoutlib renderers
    screenshotTests {
        imageDifferenceThreshold = 0.001f
    }
}
