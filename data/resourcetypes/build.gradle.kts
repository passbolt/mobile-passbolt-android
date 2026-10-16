plugins {
    id("passbolt.android.library")
}

dependencies {
    implementation(project(":resourcetypes-domain"))
    implementation(project(":architecture"))
    implementation(project(":networking"))
    implementation(project(":dto"))
    implementation(project(":database"))
    implementation(project(":entity"))
    implementation(project(":common"))

    implementation(platform(libs.koin.bom))
    implementation(libs.koin)
}

android {
    namespace = "com.passbolt.mobile.android.data.resourcetypes"
}
