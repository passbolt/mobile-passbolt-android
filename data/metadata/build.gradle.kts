plugins {
    id("passbolt.android.library")
}

dependencies {
    implementation(project(":metadata-domain"))
    implementation(project(":architecture"))
    implementation(project(":networking"))
    implementation(project(":dto"))
    implementation(project(":encryptedstorage"))
    implementation(project(":database"))
    implementation(project(":entity"))

    implementation(platform(libs.koin.bom))
    implementation(libs.koin)
    implementation(libs.kotlin.coroutines)
}

android {
    namespace = "com.passbolt.mobile.android.data.metadata"
}
