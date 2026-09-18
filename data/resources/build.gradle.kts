plugins {
    id("passbolt.android.library")
}

dependencies {
    implementation(project(":resourcetypes-domain"))
    implementation(project(":resources-domain"))
    implementation(project(":architecture"))
    implementation(project(":common"))
    implementation(project(":networking"))
    implementation(project(":dto"))
    implementation(project(":database"))
    implementation(project(":entity"))
    implementation(project(":mappers"))
    implementation(project(":uimodel"))
    implementation(project(":jsonmodel"))
    implementation(project(":supportedresourcetypes"))

    implementation(libs.gson)
    implementation(platform(libs.koin.bom))
    implementation(libs.koin)
    implementation(libs.room.paging)
    implementation(libs.sqlite.cipher) { artifact { type = "aar" } }
    implementation(libs.paging.runtime)
    implementation(libs.kotlin.coroutines)
}

android {
    namespace = "com.passbolt.mobile.android.data.resources"
}
