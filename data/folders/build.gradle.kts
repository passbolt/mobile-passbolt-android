plugins {
    id("passbolt.android.library")
}

dependencies {
    implementation(project(":folders-domain"))
    implementation(project(":architecture"))
    implementation(project(":common"))
    implementation(project(":networking"))
    implementation(project(":dto"))
    implementation(project(":database"))
    implementation(project(":entity"))
    implementation(project(":mappers"))
    implementation(project(":uimodel"))

    implementation(platform(libs.koin.bom))
    implementation(libs.koin)
    implementation(libs.kotlin.coroutines)
    implementation(libs.room.paging)
    implementation(libs.paging.runtime)
    implementation(libs.sqlite.cipher) { artifact { type = "aar" } }
}

android {
    namespace = "com.passbolt.mobile.android.data.folders"
}
