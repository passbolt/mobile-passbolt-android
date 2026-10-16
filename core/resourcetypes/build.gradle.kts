plugins {
    id("passbolt.android.library")
}

dependencies {
    implementation(project(":accounts-domain"))
    implementation(project(":resourcetypes-domain"))
    implementation(project(":common"))
    implementation(project(":database"))
    implementation(project(":entity"))
    implementation(project(":networking"))
    implementation(project(":architecture"))
    implementation(project(":dto"))
    implementation(project(":mappers"))
    implementation(project(":supportedresourcetypes"))
    implementation(project(":uimodel"))

    implementation(platform(libs.koin.bom))
    implementation(libs.koin)
    implementation(libs.sqlite.cipher) { artifact { type = "aar" } }
}

android {
    namespace = "com.passbolt.mobile.android.core.resourcetypes"
}
