plugins {
    id("passbolt.android.library")
}

dependencies {
    implementation(project(":resourcetypes-domain"))
    implementation(project(":accounts-domain"))
    implementation(project(":dto"))
    implementation(project(":common"))
    implementation(project(":database"))
    implementation(project(":supportedresourcetypes"))
    implementation(project(":gopenpgp"))
    implementation(project(":metadata-domain"))
    implementation(project(":uimodel"))
    implementation(project(":privatekey-domain"))
    implementation(project(":passphrasememorycache"))
    implementation(project(":entity"))
    implementation(project(":architecture"))

    implementation(":gopenpgp@aar")
    implementation(platform(libs.koin.bom))
    implementation(libs.koin)
    implementation(libs.gson)
    implementation(libs.jsonschema.friend)

    testImplementation(project(":commontest"))
}

android {
    namespace = "com.passbolt.mobile.android.domain.serializers"
}
