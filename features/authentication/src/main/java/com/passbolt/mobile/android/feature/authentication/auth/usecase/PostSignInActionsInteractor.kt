package com.passbolt.mobile.android.feature.authentication.auth.usecase

import com.passbolt.mobile.android.core.policies.usecase.PasswordExpiryPoliciesInteractor
import com.passbolt.mobile.android.core.policies.usecase.PasswordPoliciesInteractor
import com.passbolt.mobile.android.core.rbac.usecase.RbacInteractor
import com.passbolt.mobile.android.core.users.profile.UserProfileInteractor
import com.passbolt.mobile.android.entity.featureflags.FeatureFlagsModel
import com.passbolt.mobile.android.featureflags.usecase.FeatureFlagsInteractor
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import timber.log.Timber

private typealias IsSuccess = Boolean

class PostSignInActionsInteractor(
    private val featureFlagsInteractor: FeatureFlagsInteractor,
    private val rbacInteractor: RbacInteractor,
    private val userProfileInteractor: UserProfileInteractor,
    private val passwordExpiryPoliciesInteractor: PasswordExpiryPoliciesInteractor,
    private val passwordPoliciesInteractor: PasswordPoliciesInteractor
) {

    suspend fun launchPostSignInActions(
        onError: (Error) -> Unit,
        onSuccess: suspend () -> Unit
    ) {
        fetchFeatureFlagsDependencies(onError, onSuccess)
    }

    private suspend fun fetchFeatureFlagsDependencies(onError: (Error) -> Unit, onSuccess: suspend () -> Unit) {
        Timber.d("Fetching feature flags")
        when (val featureFlagsResult = featureFlagsInteractor.fetchAndSaveFeatureFlags()) {
            is FeatureFlagsInteractor.Output.Success -> {
                Timber.d("Feature flags fetched")
                coroutineScope {
                    if (awaitAll(
                            async {
                                processRbac(featureFlagsResult.featureFlags, onError)
                            },
                            async {
                                processPasswordExpirySettings(featureFlagsResult.featureFlags, onError)
                            },
                            async {
                                processPasswordPoliciesSettings(featureFlagsResult.featureFlags, onError)
                            }
                        ).all { it }
                    ) {
                        fetchUserAvatar(onError, onSuccess)
                    }
                }
            }
            is FeatureFlagsInteractor.Output.Failure -> {
                Timber.e("Failed to fetch feature flags")
                onError(Error.ConfigurationFetchError)
            }
        }
    }

    private suspend fun processPasswordExpirySettings(
        featureFlagsModel: FeatureFlagsModel,
        onError: (Error) -> Unit
    ): IsSuccess {
        return if (featureFlagsModel.isPasswordExpiryAvailable) {
            Timber.d("Password expiry available, fetching expiry settings")
            when (passwordExpiryPoliciesInteractor.fetchAndSavePasswordExpiryPolicies()) {
                is PasswordExpiryPoliciesInteractor.Output.Failure -> {
                    Timber.e("Failed to fetch password expiry policies")
                    onError(Error.ConfigurationFetchError)
                    false
                }
                is PasswordExpiryPoliciesInteractor.Output.Success -> true
            }
        } else {
            Timber.d("Password expiry not available")
            true
        }
    }

    private suspend fun processRbac(
        featureFlagsModel: FeatureFlagsModel,
        onError: (Error) -> Unit
    ): IsSuccess {
        return if (featureFlagsModel.isRbacAvailable) {
            Timber.d("RBAC available, fetching RBAC")
            when (rbacInteractor.fetchAndSaveRbacRulesFlags()) {
                is RbacInteractor.Output.Failure -> {
                    Timber.e("Failed to fetch RBAC")
                    onError(Error.ConfigurationFetchError)
                    false
                }
                is RbacInteractor.Output.Success -> true
            }
        } else {
            Timber.d("RBAC not available")
            true
        }
    }

    private suspend fun processPasswordPoliciesSettings(
        featureFlagsModel: FeatureFlagsModel,
        onError: (Error) -> Unit
    ): IsSuccess {
        return if (featureFlagsModel.arePasswordPoliciesAvailable) {
            Timber.d("Password policies available, fetching password policies settings")
            when (passwordPoliciesInteractor.fetchAndSavePasswordPolicies()) {
                is PasswordPoliciesInteractor.Output.Failure -> {
                    Timber.e("Failed to fetch password policies")
                    onError(Error.ConfigurationFetchError)
                    false
                }
                is PasswordPoliciesInteractor.Output.Success -> true
            }
        } else {
            Timber.d("Password policies not available")
            true
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
