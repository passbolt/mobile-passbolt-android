plugins {
    id("passbolt.android.library")
}

dependencies {
    implementation(project(":groups-domain"))
    implementation(project(":architecture"))
    implementation(project(":networking"))
    implementation(project(":dto"))
    implementation(project(":database"))
    implementation(project(":entity"))
    implementation(project(":users-domain"))

    implementation(platform(libs.koin.bom))
    implementation(libs.koin)
    implementation(libs.kotlin.coroutines)
    implementation(libs.room.paging)
}

android {
    namespace = "com.passbolt.mobile.android.data.groups"
}
