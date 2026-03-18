package com.passbolt.mobile.android.feature.resourceform.additionalsecrets.password

import com.passbolt.mobile.android.core.passwordgenerator.SecretGenerator
import com.passbolt.mobile.android.core.passwordgenerator.entropy.EntropyCalculator
import com.passbolt.mobile.android.core.policies.usecase.GetPasswordPoliciesUseCase
import com.passbolt.mobile.android.mappers.EntropyViewMapper
import org.koin.core.module.dsl.factoryOf
import org.koin.dsl.module
import org.mockito.Mockito.mock

internal val mockGetPasswordPoliciesUseCase = mock<GetPasswordPoliciesUseCase>()
internal val mockSecretGenerator = mock<SecretGenerator>()
internal val mockEntropyCalculator = mock<EntropyCalculator>()

internal val testPasswordFormModule =
    module {
        factoryOf(::EntropyViewMapper)
        single { mockEntropyCalculator }
        single { mockGetPasswordPoliciesUseCase }
        single { mockSecretGenerator }
        factory { params ->
            PasswordFormViewModel(
                mode = params.get(),
                passwordModel = params.get(),
                entropyViewMapper = get(),
                entropyCalculator = get(),
                getPasswordPoliciesUseCase = get(),
                secretGenerator = get(),
            )
        }
    }
