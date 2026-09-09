plugins {
    id("passbolt.android.library")
}

android {
    namespace = "com.passbolt.mobile.android.core.fulldatarefresh"
}

dependencies {
    implementation(project(":resourcetypes-domain"))
    implementation(project(":groups-domain"))
    implementation(project(":users-domain"))
    implementation(project(":architecture"))
    implementation(project(":accounts-domain"))
    implementation(project(":resources-domain"))
    implementation(project(":folders-domain"))
    implementation(project(":idlingresource"))
    implementation(project(":authentication"))
    implementation(project(":metadata-domain"))
    implementation(project(":common"))
    implementation(project(":entity"))
    implementation(project(":featureflags-domain"))
    implementation(project(":database"))
    implementation(project(":coreui"))
    implementation(project(":notifications"))
    implementation(project(":secrets-domain"))
    implementation(project(":networking"))

    implementation(platform(libs.koin.bom))
    implementation(libs.koin)
    implementation(libs.room.core)
    implementation(libs.espresso.idling.resource)
    implementation(libs.androidx.lifecycle.service)

    testImplementation(project(":commontest"))
}
