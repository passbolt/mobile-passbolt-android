package com.passbolt.mobile.android.feature.authentication.auth.usecase

import com.passbolt.mobile.android.core.policies.usecase.PoliciesInteractor
import com.passbolt.mobile.android.core.rbac.usecase.RbacInteractor
import com.passbolt.mobile.android.core.users.profile.UserProfileInteractor
import com.passbolt.mobile.android.featureflags.usecase.FeatureFlagsInteractor
import timber.log.Timber

private typealias IsSuccess = Boolean

class PostSignInActionsInteractor(
    private val featureFlagsInteractor: FeatureFlagsInteractor,
    private val rbacInteractor: RbacInteractor,
    private val userProfileInteractor: UserProfileInteractor,
    private val policiesInteractor: PoliciesInteractor
) {

    suspend fun launchPostSignInActions(
        onError: (Error) -> Unit,
        onSuccess: suspend () -> Unit
    ) {
        fetchFeatureFlagsAndRbac(onError, onSuccess)
    }

    private suspend fun fetchFeatureFlagsAndRbac(onError: (Error) -> Unit, onSuccess: suspend () -> Unit) {
        Timber.d("Fetching feature flags")
        when (val featureFlagsResult = featureFlagsInteractor.fetchAndSaveFeatureFlags()) {
            is FeatureFlagsInteractor.Output.Success -> {
                Timber.d("Feature flags fetched")
                var isRbacProcessedSuccessful = true
                var isPasswordExpiryProcessedSuccessful = true
                if (featureFlagsResult.featureFlags.isRbacAvailable) {
                    isRbacProcessedSuccessful = fetchRbac(onError)
                }
                if (featureFlagsResult.featureFlags.isPasswordExpiryAvailable) {
                    isPasswordExpiryProcessedSuccessful = fetchPasswordExpirySettings(onError)
                }
                if (isRbacProcessedSuccessful && isPasswordExpiryProcessedSuccessful) {
                    fetchUserAvatar(onError, onSuccess)
                }
            }
            is FeatureFlagsInteractor.Output.Failure -> {
                Timber.e("Failed to fetch feature flags")
                onError(Error.ConfigurationFetchError)
            }
        }
    }

    private suspend fun fetchPasswordExpirySettings(onError: (Error) -> Unit): IsSuccess {
        Timber.d("Password expiry available, fetching expiry settings")
        return when (policiesInteractor.fetchAndSavePasswordExpiryPolicies()) {
            is PoliciesInteractor.Output.Failure -> {
                Timber.e("Failed to fetch password expiry policies")
                onError(Error.ConfigurationFetchError)
                false
            }
            is PoliciesInteractor.Output.Success -> true
        }
    }

    private suspend fun fetchRbac(onError: (Error) -> Unit): IsSuccess {
        Timber.d("RBAC available, fetching RBAC")
        return when (rbacInteractor.fetchAndSaveRbacRulesFlags()) {
            is RbacInteractor.Output.Failure -> {
                Timber.e("Failed to fetch RBAC")
                onError(Error.ConfigurationFetchError)
                false
            }
            is RbacInteractor.Output.Success -> true
        }
    }

    private suspend fun fetchUserAvatar(onError: (Error) -> Unit, onSuccess: suspend () -> Unit) {
        Timber.d("Fetching user profile")
        when (val result = userProfileInteractor.fetchAndUpdateUserProfile()) {
            is UserProfileInteractor.Output.Failure -> {
                Timber.e("Failed to update user profile: ${result.message}")
                onError(Error.UserProfileFetchError)
            }
            is UserProfileInteractor.Output.Success -> {
                Timber.d("User profile updated successfully")
            }
        }
        // ignore profile fetch errors
        onSuccess()
    }

    sealed class Error {
        data object ConfigurationFetchError : Error()
        data object UserProfileFetchError : Error()
    }
}
