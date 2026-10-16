plugins {
    id("passbolt.android.library")
}

dependencies {
    implementation(project(":users-domain"))
    implementation(project(":architecture"))
    implementation(project(":networking"))
    implementation(project(":dto"))
    implementation(project(":database"))
    implementation(project(":entity"))

    implementation(platform(libs.koin.bom))
    implementation(libs.koin)
}

android {
    namespace = "com.passbolt.mobile.android.data.users"
}
