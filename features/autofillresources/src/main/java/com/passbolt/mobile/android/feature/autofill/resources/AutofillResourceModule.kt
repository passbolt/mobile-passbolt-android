package com.passbolt.mobile.android.feature.autofill.resources

import com.passbolt.mobile.android.core.navigation.AutofillMode
import com.passbolt.mobile.android.feature.autofill.resources.datasetstrategy.AutofillCallback
import com.passbolt.mobile.android.feature.autofill.resources.datasetstrategy.ReturnAccessibilityDataset
import com.passbolt.mobile.android.feature.autofill.resources.datasetstrategy.ReturnAutofillDataset
import com.passbolt.mobile.android.feature.autofill.resources.datasetstrategy.ReturnAutofillDatasetStrategy
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.Module
import org.koin.core.module.dsl.viewModel
import org.koin.core.qualifier.named

fun Module.autofillResourcesModule() {
    viewModel { params ->
        AutofillResourcesViewModel(
            getAccountsUseCase = get(),
            uri = params.getOrNull(),
            getLocalResourceUseCase = get(),
            coroutineLaunchContext = get(),
        )
    }

    scope<AutofillResourcesActivity> {
        scoped<ReturnAutofillDatasetStrategy>(
            named(AutofillMode.AUTOFILL),
        ) { (callback: AutofillCallback) ->
            ReturnAutofillDataset(
                autofillCallback = callback,
                appContext = androidContext(),
                assistStructureParser = get(),
                fillableInputsFinder = get(),
                remoteViewsFactory = get(),
            )
        }
        scoped<ReturnAutofillDatasetStrategy>(
            named(AutofillMode.ACCESSIBILITY),
        ) { (callback: AutofillCallback) ->
            ReturnAccessibilityDataset(callback)
        }
    }
}
