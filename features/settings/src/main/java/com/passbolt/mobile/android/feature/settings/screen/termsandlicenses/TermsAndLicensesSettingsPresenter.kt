package com.passbolt.mobile.android.feature.settings.screen.termsandlicenses

import com.passbolt.mobile.android.core.mvp.coroutinecontext.CoroutineLaunchContext
import com.passbolt.mobile.android.entity.featureflags.FeatureFlagsModel
import com.passbolt.mobile.android.storage.usecase.featureflags.GetFeatureFlagsUseCase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

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
class TermsAndLicensesSettingsPresenter(
    private val getFeatureFlagsUseCase: GetFeatureFlagsUseCase,
    coroutineLaunchContext: CoroutineLaunchContext
) : TermsAndLicensesSettingsContract.Presenter {

    override var view: TermsAndLicensesSettingsContract.View? = null
    private val job = SupervisorJob()
    private val scope = CoroutineScope(job + coroutineLaunchContext.ui)
    private lateinit var featureFlags: FeatureFlagsModel

    override fun attach(view: TermsAndLicensesSettingsContract.View) {
        super.attach(view)
        handleFeatureFlagsUrls()
    }

    override fun viewResumed() {
        scope.launch {
            val latestFeatureFlags = getFeatureFlagsUseCase.execute(Unit).featureFlags
            if (::featureFlags.isInitialized && latestFeatureFlags != featureFlags) {
                handleFeatureFlagsUrls()
            }
        }
    }

    private fun handleFeatureFlagsUrls() {
        scope.launch {
            featureFlags = getFeatureFlagsUseCase.execute(Unit).featureFlags
            if (featureFlags.privacyPolicyUrl.isNullOrBlank()) {
                view?.disablePrivacyPolicySetting()
            } else {
                view?.enablePrivacyPolicySetting()
            }
            if (featureFlags.termsAndConditionsUrl.isNullOrBlank()) {
                view?.disableTermsAndConditionsSetting()
            } else {
                view?.enableTermsAndConditionsButton()
            }
        }
    }

    override fun licensesClick() {
        view?.navigateToLicenses()
    }

    override fun termsAndConditionsClick() {
        // if url is null the button is hidden
        view?.openUrl(requireNotNull(featureFlags.termsAndConditionsUrl))
    }

    override fun privacyPolicyClick() {
        // if url is null the button is hidden
        view?.openUrl(requireNotNull(featureFlags.privacyPolicyUrl))
    }
}
