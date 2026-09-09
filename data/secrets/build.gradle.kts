plugins {
    id("passbolt.android.library")
}

dependencies {
    implementation(project(":secrets-domain"))
    implementation(project(":architecture"))
    implementation(project(":networking"))
    implementation(project(":dto"))
    implementation(project(":database"))
    implementation(project(":entity"))

    implementation(platform(libs.koin.bom))
    implementation(libs.koin)
    implementation(libs.room.core)
}

android {
    namespace = "com.passbolt.mobile.android.data.secrets"
}
