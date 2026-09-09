plugins {
    id("passbolt.android.library")
}

dependencies {
    implementation(project(":accounts-domain"))
    implementation(project(":common"))
    implementation(project(":gopenpgp"))
    implementation(project(":architecture"))
    implementation(project(":uimodel"))
    implementation(project(":serializers"))
    implementation(project(":supportedresourcetypes"))
    implementation(project(":passphrasememorycache"))
    implementation(project(":privatekey-domain"))
    implementation(project(":jsonmodel"))
    implementation(project(":preferences-domain"))

    implementation(platform(libs.koin.bom))
    implementation(libs.koin)
    implementation(libs.kotlin.coroutines)
    implementation(libs.gson)
    implementation(libs.jsonschema.friend)
    implementation(libs.json.path)
}

android {
    namespace = "com.passbolt.mobile.android.domain.secrets"
}
