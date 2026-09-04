/**
 * Passbolt - Open source password manager for teams
 * Copyright (c) 2021 Passbolt SA
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU Affero General
 * Public License (AGPL) as published by the Free Software Foundation version 3.
 *
 * The name "Passbolt" is a registered trademark of Passbolt SA, and Passbolt SA hereby declines to grant a trademark
 * license to "Passbolt" pursuant to the GNU Affero General Public License version 3 Section 7(e), without a separate
 * agreement with Passbolt SA.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied
 * warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License along with this program. If not,
 * see GNU Affero General Public License v3 (http://www.gnu.org/licenses/agpl-3.0.html).
 *
 * @copyright Copyright (c) Passbolt SA (https://www.passbolt.com)
 * @license https://opensource.org/licenses/AGPL-3.0 AGPL License
 * @link https://www.passbolt.com Passbolt (tm)
 * @since v1.0
 */

package com.passbolt.mobile.android.feature.startup.deprecatedoswarning

import com.google.common.truth.Truth.assertThat
import com.passbolt.mobile.android.core.envinfo.EnvInfo
import com.passbolt.mobile.android.core.envinfo.EnvInfoProvider
import com.passbolt.mobile.android.domain.preferences.GlobalPreferencesUpdate
import com.passbolt.mobile.android.domain.preferences.PreferencesDefaults
import com.passbolt.mobile.android.domain.preferences.usecase.GetGlobalPreferencesUseCase
import com.passbolt.mobile.android.domain.preferences.usecase.UpdateGlobalPreferencesUseCase
import com.passbolt.mobile.android.feature.startup.BuildConfig
import com.passbolt.mobile.android.ui.GlobalPreferencesUiModel
import org.junit.Test
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

class DeprecatedOsWarningInteractorTest {
    private val envInfoProvider = mock<EnvInfoProvider>()
    private val getGlobalPreferencesUseCase = mock<GetGlobalPreferencesUseCase>()
    private val updateGlobalPreferencesUseCase = mock<UpdateGlobalPreferencesUseCase>()

    private val interactor =
        DeprecatedOsWarningInteractor(
            envInfoProvider = envInfoProvider,
            getGlobalPreferencesUseCase = getGlobalPreferencesUseCase,
            updateGlobalPreferencesUseCase = updateGlobalPreferencesUseCase,
        )

    @Test
    fun `warning should be shown on deprecated os when not hidden`() {
        stubSdkInt(DEPRECATED_SDK)
        whenever(getGlobalPreferencesUseCase.execute(Unit)) doReturn globalPreferences(hiddenForSdk = null)

        assertThat(interactor.shouldShowDeprecatedOsWarning()).isTrue()
    }

    @Test
    fun `warning should not be shown on fully supported os`() {
        stubSdkInt(BuildConfig.MIN_FULLY_SUPPORTED_SDK)
        whenever(getGlobalPreferencesUseCase.execute(Unit)) doReturn globalPreferences(hiddenForSdk = null)

        assertThat(interactor.shouldShowDeprecatedOsWarning()).isFalse()
    }

    @Test
    fun `preferences should not be read on fully supported os`() {
        stubSdkInt(BuildConfig.MIN_FULLY_SUPPORTED_SDK)

        interactor.shouldShowDeprecatedOsWarning()

        verify(getGlobalPreferencesUseCase, never()).execute(Unit)
    }

    @Test
    fun `warning should not be shown when hidden for current os`() {
        stubSdkInt(DEPRECATED_SDK)
        whenever(getGlobalPreferencesUseCase.execute(Unit)) doReturn globalPreferences(hiddenForSdk = DEPRECATED_SDK)

        assertThat(interactor.shouldShowDeprecatedOsWarning()).isFalse()
    }

    @Test
    fun `warning should be shown again after os upgrade to another deprecated version`() {
        stubSdkInt(DEPRECATED_SDK)
        whenever(getGlobalPreferencesUseCase.execute(Unit)) doReturn globalPreferences(hiddenForSdk = OLDER_DEPRECATED_SDK)

        assertThat(interactor.shouldShowDeprecatedOsWarning()).isTrue()
    }

    @Test
    fun `hiding warning should persist current sdk`() {
        stubSdkInt(DEPRECATED_SDK)

        interactor.hideDeprecatedOsWarning()

        verify(updateGlobalPreferencesUseCase).execute(GlobalPreferencesUpdate(deprecatedOsWarningHiddenForSdk = DEPRECATED_SDK))
    }

    private fun stubSdkInt(sdkInt: Int) {
        val envInfo = EnvInfo(deviceName = "deviceName", osName = "osName", sdkInt = sdkInt, appName = "appName")
        whenever(envInfoProvider.provideEnvInfo()) doReturn envInfo
    }

    private fun globalPreferences(hiddenForSdk: Int?) =
        GlobalPreferencesUiModel(
            areDebugLogsEnabled = false,
            debugLogFileCreationDateTime = null,
            debugLogLastAppVersion = null,
            isHideRootDialogEnabled = false,
            isAuthRequiredOnEveryEntry = false,
            apiFetchPageSize = PreferencesDefaults.API_FETCH_PAGE_SIZE,
            isApiFetchPageSizeManuallySet = false,
            accessibilityPoliciesConsentGiven = false,
            deprecatedOsWarningHiddenForSdk = hiddenForSdk,
            isCopyTotpOnAutofillEnabled = false,
        )

    private companion object {
        private const val DEPRECATED_SDK = BuildConfig.MIN_FULLY_SUPPORTED_SDK - 1
        private const val OLDER_DEPRECATED_SDK = BuildConfig.MIN_FULLY_SUPPORTED_SDK - 2
    }
}
