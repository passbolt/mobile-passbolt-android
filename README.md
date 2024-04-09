	      ____                  __          ____
	     / __ \____  _____ ____/ /_  ____  / / /_
	    / /_/ / __ `/ ___/ ___/ __ \/ __ \/ / __/
	   / ____/ /_/ (__  |__  ) /_/ / /_/ / / /_
	  /_/    \__,_/____/____/_.___/\____/_/\__/

	Open source password manager for teams
	(c) 2021 Passbolt SA
	https://www.passbolt.com

## License

Passbolt - Open source password manager for teams

(c) 2021 Passbolt SA

This program is free software: you can redistribute it and/or modify it under the terms of the GNU Affero General
Public License (AGPL) as published by the Free Software Foundation version 3.

The name "Passbolt" is a registered trademark of Passbolt SA, and Passbolt SA hereby declines to grant a trademark
license to "Passbolt" pursuant to the GNU Affero General Public License version 3 Section 7(e), without a separate
agreement with Passbolt SA.

This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied
warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See GNU Affero General Public License for more details.

You should have received a copy of the GNU Affero General Public License along with this program. If not,
see [GNU Affero General Public License v3](http://www.gnu.org/licenses/agpl-3.0.html).

## About this repository

This repository contains the code of the Android mobile application.

### Reporting a security Issue

If you've found a security related issue in Passbolt, please don't open an issue in GitHub.
Instead contact us at security@passbolt.com. In the spirit of responsible disclosure we ask that the reporter keep the
issue confidential until we announce it.

The passbolt team will take the following actions:

- Try to first reproduce the issue and confirm the vulnerability.
- Acknowledge to the reporter that we’ve received the issue and are working on a fix.
- Get a fix/patch prepared and create associated automated tests.
- Prepare a post describing the vulnerability, and the possible exploits.
- Release new versions of all affected major versions.
- Prominently feature the problem in the release announcement.
- Provide credits in the release announcement to the reporter if they so desire.

# How to build locally

## With Android Studio (recommended)

1. Launch [Android Studio](https://developer.android.com/studio) and open the cloned project
2. Make sure that Android SDK with version `30` is installed to compile the project
3. Wait until project configuration finishes (couple of minutes) and click `Sync with Gradle files` icon (top right toolbar - elephant
   with blue arrow)
4. Open the `Build Variants` tab (bottom left vertical pane) and under the `:app` module select `Active Build Variant` as `debug`
5. Prepare a device for launch - at minimum `Android 10 (API 30)` is required
    1. [create and launch Android emulator](https://developer.android.com/studio/run/managing-avds) **or**
    2. [set up and launch on a real device](https://developer.android.com/studio/run/device)
6. Hit the `Run` arrow (green play icon in the top center)

## Without Android Studio

1. Download [Android build tools](https://developer.android.com/studio#downloads) - scroll to `Command line tools only`
2. Using the downloaded command line
   tools [install the build tools](https://developer.android.com/studio/command-line/sdkmanager#install_packages) for `API 30` required
   to compile the project
3. Open terminal and navigate to cloned project root directory
4. Use [Gradle Wrapper](https://docs.gradle.org/current/userguide/gradle_wrapper.html) to build the project from
   terminal `./gradlew assembleDebug` (during first build the Wrapper will also download and setup Gradle if not present) - the built
   application will be available at `{project-dir}/app/build/outputs/apk/debug`
5. To install on a connected device (see above section 4.1 or 4.2) execute `./gradlew installDebug`

# How run verifications locally

1. Navigate to project root directory
2. Execute `./gradlew detekt ktlint lintDebug unitTest koverMergedHtmlReport licenseeRelease dependencyUpdates buildHealth`

You can also run each check individually if needed:

* `detekt` and `ktlint` - run static analysis for kotlin
* `lintRelease` - run Android linter
* `unitTest` - execute all unit tests
* `koverMergedHtmlReport` - generate unit test coverage report
* `licenseeRelease` - check if all dependencies have appropriate licenses
* `dependencyUpdates` - check if any dependencies have updates in the release channel
* `buildHealth` - produce a report about unused dependencies or incorrect dependency declaration

To execute Android instrumented tests connect your device and execute:
`./gradlew connectedAndroidTest`
Note for instrumented tests run a set of environment variables with test user must be set on the machine that builds the application:

* `PASSBOLT_TEST_USERNAME` - ID of the user on the server
* `PASSBOLT_TEST_USER_ID` - username of the user on the server
* `PASSBOLT_TEST_DOMAIN` - server domain
* `PASSBOLT_TEST_FIRST_NAME` - first name of the user
* `PASSBOLT_TEST_LAST_NAME` - last name of the user
* `PASSBOLT_TEST_AVATAR_URL` - URL of the user avatar (optional)
* `PASSBOLT_TEST_KEY_FINGERPRINT` - user's key fingerprint
* `PASSBOLT_TEST_ARMORED_KEY_BASE_64` - base64 of user's armored key
* `PASSBOLT_TEST_PASSPHRASE` - user's key passphrase
* `PASSBOLT_TEST_LOCAL_USER_UUID` - a random uuid

# How run instrumented tests with SauceLabs cloud

Example running `saucectl` with espresso.

### What You'll Need

The steps below illustrate one of the quickest ways to get set up. If you'd like a more in-depth guide, please check out
SauceLabs [documentation](https://docs.saucelabs.com/testrunner-toolkit/installation).

### Install `saucectl`

```shell
curl -L https://saucelabs.github.io/saucectl/install | bash
```

### Install `saucectl` using Homebrew (macOS)

```shell
brew tap saucelabs/saucectl
brew install saucectl
```

⚠ Make sure saucectl version is newer than **v0.44.0**

### Set Your Sauce Labs Credentials

```shell
saucectl configure
```

### Prepare test artifacts

```
./gradlew --no-daemon --build-cache assembleDebug assembleAndroidTest
```

### Running Tests

```shell
saucectl run
```

![sauce cloud example](https://github.com/saucelabs/saucectl-espresso-example/blob/master/assets/sauce_cloud_example.gif?raw=true)

### Sauce cloud support

Espresso only works on sauce cloud for both Android Emulators and Real Devices.

[Docker mode](https://docs.saucelabs.com/testrunner-toolkit/configuration/common-syntax/index.html#mode) is not supported.

### The Config

Go to `.sauce/config.yml` if you'd like to see how saucectl is configured for this repository.

# Credits

https://www.passbolt.com/credits
