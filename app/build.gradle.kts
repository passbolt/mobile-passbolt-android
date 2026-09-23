import app.cash.licensee.LicenseeExtension
import com.github.triplet.gradle.play.PlayPublisherExtension

plugins {
    id("passbolt.android.application")
    id(libs.plugins.app.distribution.get().pluginId)
    alias(libs.plugins.playstore.publisher)
    id(libs.plugins.easylauncher.get().pluginId)
    id(libs.plugins.licensee.get().pluginId)
}

configure<LicenseeExtension> {
    allow("Apache-2.0")
    allow("MIT")
    allow("BSD-3-Clause")

    allowUrl("https://www.zetetic.net/sqlcipher/license/")
    allowUrl("https://developer.android.com/studio/terms.html")
    allowUrl("https://developers.google.com/ml-kit/terms")
    allowUrl("https://developer.android.com/guide/playcore/license")
    allowUrl("https://github.com/journeyapps/zxing-android-embedded/blob/master/COPYING") // it's Apache-2.0
    allowUrl("https://asm.ow2.io/license.html") // 3-Clause BSD
    allowUrl("https://opensource.org/license/mit")
    allowUrl("https://www.gnu.org/licenses/old-licenses/lgpl-2.1.en.html") // https://opensource.org/license/lgpl-2-1

    // Unicode license v3 is open source (https://opensource.org/license/unicode-license-v3)
    allowUrl("https://raw.githubusercontent.com/unicode-org/icu/main/LICENSE")

    // coming from https://jsoup.org/; it's on MIT license (https://mvnrepository.com/artifact/org.jsoup/jsoup)
    allowUrl("https://jsoup.org/license")
}

val projectVersionName: String by rootProject.extra
val projectVersionCode: Int by rootProject.extra

fun getVersionCode(): Int = System.getenv("GITLAB_BUILD_NUMBER")?.toInt() ?: projectVersionCode

android {
    namespace = "com.passbolt.mobile.android"
    defaultConfig {
        applicationId = "com.passbolt.mobile.android"
        versionCode = getVersionCode()
        versionName = projectVersionName
    }

    // connected android tests use qa config
    testBuildType = "automatedTests"

    buildTypes {
        getByName("debug") {
            applicationIdSuffix = ".debug"
            isDebuggable = true
            isMinifyEnabled = false
            firebaseDistribution(project, appId = "1:660923335137:android:08eec6c125f36ba9b092a0")
            enableUnitTestCoverage = true
            enableAndroidTestCoverage = true
        }
        create("qa") {
            initWith(getByName("debug"))
            matchingFallbacks += "release"

            applicationIdSuffix = ".qa"
            isDebuggable = false
            isMinifyEnabled = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            firebaseDistribution(project, appId = "1:660923335137:android:c7e452d7912a9e3ab092a0")
        }
        create("automatedTests") {
            initWith(getByName("debug"))
            matchingFallbacks += "debug"

            applicationIdSuffix = ".qa.automated"
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            firebaseDistribution(project, appId = "1:660923335137:android:c7e452d7912a9e3ab092a0")

            // automated tests fields
            buildConfigField("String", "PROPERTY_USER_ID", "\"${System.getenv("PASSBOLT_TEST_USER_ID")}\"")
            buildConfigField("String", "PROPERTY_USERNAME", "\"${System.getenv("PASSBOLT_TEST_USERNAME")}\"")
            buildConfigField("String", "PROPERTY_DOMAIN", "\"${System.getenv("PASSBOLT_TEST_DOMAIN")}\"")
            buildConfigField("String", "PROPERTY_FIRST_NAME", "\"${System.getenv("PASSBOLT_TEST_FIRST_NAME")}\"")
            buildConfigField("String", "PROPERTY_LAST_NAME", "\"${System.getenv("PASSBOLT_TEST_LAST_NAME")}\"")
            buildConfigField("String", "PROPERTY_AVATAR_URL", "\"${System.getenv("PASSBOLT_TEST_AVATAR_URL") ?: ""}\"")
            buildConfigField("String", "PROPERTY_KEY_FINGERPRINT", "\"${System.getenv("PASSBOLT_TEST_KEY_FINGERPRINT")}\"")
            buildConfigField("String", "PROPERTY_LOCAL_USER_UUID", "\"${System.getenv("PASSBOLT_TEST_LOCAL_USER_UUID")}\"")
            buildConfigField("String", "PROPERTY_ARMORED_KEY_BASE_64", "\"${System.getenv("PASSBOLT_TEST_ARMORED_KEY_BASE_64")}\"")
            buildConfigField("String", "PROPERTY_PASSPHRASE", "\"${System.getenv("PASSBOLT_TEST_PASSPHRASE")}\"")
        }

        getByName("release") {
            isDebuggable = false
            isMinifyEnabled = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }

    buildFeatures {
        buildConfig = true
    }
}

configure<PlayPublisherExtension> {
    track.set("production")
    releaseName.set("$projectVersionName-$projectVersionCode")
    defaultToAppBundles.set(true)
}

easylauncher {
    buildTypes {
        register("debug") {
            enable(true)
            setFilters(
                listOf(
                    customRibbon(
                        label = "debug:${getVersionCode()}",
                        labelColor = "#000000",
                        ribbonColor = "#41C1FF",
                    ),
                ),
            )
        }
        register("qa") {
            enable(true)
            setFilters(
                listOf(
                    customRibbon(
                        label = "qa:${getVersionCode()}",
                        labelColor = "#000000",
                        ribbonColor = "#0C9115",
                    ),
                ),
            )
        }
    }
}

dependencies {
    implementation(project(":architecture"))
    implementation(project(":startup"))
    implementation(project(":setup"))
    implementation(project(":auth-domain"))
    implementation(project(":auth-data"))
    implementation(project(":accounts-domain"))
    implementation(project(":accounts-data"))
    implementation(project(":uimodel"))
    implementation(project(":common"))
    implementation(project(":navigation"))
    implementation(project(":mappers"))
    implementation(project(":qrscan"))
    implementation(project(":gopenpgp"))
    implementation(project(":autofillresources"))
    implementation(project(":authentication"))
    implementation(project(":main"))
    implementation(project(":home"))
    implementation(project(":settings"))
    implementation(project(":featureflags-domain"))
    implementation(project(":featureflags-data"))
    implementation(project(":database"))
    implementation(project(":secrets-domain"))
    implementation(project(":secrets-data"))
    implementation(project(":security"))
    implementation(project(":logger"))
    implementation(project(":accountdetails"))
    implementation(project(":localization"))
    implementation(project(":folderdetails"))
    implementation(project(":groupdetails"))
    implementation(project(":coreui"))
    implementation(project(":locationdetails"))
    implementation(project(":createfolder"))
    implementation(project(":folders-domain"))
    implementation(project(":folders-data"))
    implementation(project(":groups-domain"))
    implementation(project(":groups-data"))
    implementation(project(":privatekey-domain"))
    implementation(project(":privatekey-data"))
    implementation(project(":tagsdetails"))
    implementation(project(":helpmenu"))
    implementation(project(":logs"))
    implementation(project(":resourcemoremenu"))
    implementation(project(":resources-domain"))
    implementation(project(":resources-data"))
    implementation(project(":resourcedetails"))
    implementation(project(":fulldatarefresh"))
    implementation(project(":resourcetypes"))
    implementation(project(":notifications"))
    implementation(project(":autofill"))
    implementation(project(":inappreview-domain"))
    implementation(project(":inappreview-data"))
    implementation(project(":envinfo"))
    implementation(project(":idlingresource"))
    implementation(project(":entity"))
    implementation(project(":transferaccounttoanotherdevice"))
    implementation(project(":otp"))
    implementation(project(":otpcore"))
    implementation(project(":serializers"))
    implementation(project(":resourcepicker"))
    implementation(project(":tags-domain"))
    implementation(project(":tags-data"))
    implementation(project(":scanotp"))
    implementation(project(":otpmoremenu"))
    implementation(project(":rbac-domain"))
    implementation(project(":rbac-data"))
    implementation(project(":accounts"))
    implementation(project(":passwordpolicies-data"))
    implementation(project(":passwordpolicies-domain"))
    implementation(project(":permissionsconfirmation-data"))
    implementation(project(":permissionsconfirmation-domain"))
    implementation(project(":mobiletransfer-data"))
    implementation(project(":mobiletransfer-domain"))
    implementation(project(":favourites-data"))
    implementation(project(":favourites-domain"))
    implementation(project(":passwordexpiry-data"))
    implementation(project(":passwordexpiry-domain"))
    implementation(project(":users-data"))
    implementation(project(":users-domain"))
    implementation(project(":share-data"))
    implementation(project(":share-domain"))
    implementation(project(":mfa-data"))
    implementation(project(":mfa-domain"))
    implementation(project(":resourcetypes-data"))
    implementation(project(":pwnedpasswordsapi"))
    implementation(project(":passwordgenerator"))
    implementation(project(":metadata-domain"))
    implementation(project(":metadata-data"))
    implementation(project(":biometrickey-data"))
    implementation(project(":biometrickey-domain"))
    implementation(project(":encryptedstorage"))
    implementation(project(":preferences-domain"))
    implementation(project(":preferences-data"))
    implementation(project(":passphrasememorycache"))
    implementation(project(":jsonmodel"))
    implementation(project(":createresourcemenu"))
    implementation(project(":resourceform"))
    implementation(project(":permissions"))
    implementation(project(":clipboard"))
    implementation(project("::resourcetypes-domain"))

    debugImplementation(libs.leakcanary)

    implementation(platform(libs.koin.bom))
    implementation(libs.lifecycle.process)
    implementation(libs.coil)
    implementation(libs.coil.networking)
    implementation(libs.koin)
    implementation(libs.gson)
    implementation(libs.app.startup)
    implementation(libs.espresso.idling.resource)
    implementation(libs.json.path)

    androidTestImplementation(project(":commontest"))
    testImplementation(libs.junit)
    testImplementation(libs.truth)
    testImplementation(libs.kotlin.coroutines.test)
    testImplementation(libs.androidx.navigation3.runtime)
    testImplementation(project(":commontest"))

    androidTestImplementation(project(":testtags"))
    androidTestImplementation(project(":resourcetypes-domain"))
    androidTestImplementation(project(":dto"))
    androidTestImplementation(project(":supportedresourcetypes"))
    androidTestImplementation(":gopenpgp@aar")
    androidTestImplementation(libs.mockwebserver)
    androidTestImplementation(libs.room.runtime)
    androidTestImplementation(libs.sqlite)
    androidTestImplementation(platform(libs.koin.bom))
    androidTestImplementation(libs.android.tests.runner)
    androidTestImplementation(libs.android.test.rules)
    androidTestImplementation(libs.android.test.ktx)
    androidTestImplementation(libs.koin.test)
    androidTestImplementation(libs.koin.test.junit)
    androidTestImplementation(libs.android.test.intents)
    androidTestImplementation(libs.mockito.android)
    androidTestImplementation(platform(libs.compose.bom))
    androidTestImplementation(libs.compose.ui.test.junit4)
    androidTestDebugImplementation(libs.compose.ui.test.manifest)
}
