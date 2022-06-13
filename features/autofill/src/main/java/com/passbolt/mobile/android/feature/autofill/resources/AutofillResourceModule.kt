package com.passbolt.mobile.android.feature.autofill.resources

import com.mikepenz.fastadapter.FastAdapter
import com.mikepenz.fastadapter.GenericItem
import com.mikepenz.fastadapter.adapters.ModelAdapter
import com.passbolt.mobile.android.core.commonresource.ResourceListUiModel
import com.passbolt.mobile.android.core.navigation.AutofillMode
import com.passbolt.mobile.android.feature.autofill.resources.datasetstrategy.ReturnAccessibilityDataset
import com.passbolt.mobile.android.feature.autofill.resources.datasetstrategy.ReturnAutofillDataset
import com.passbolt.mobile.android.feature.autofill.resources.datasetstrategy.ReturnAutofillDatasetStrategy
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.Module
import org.koin.core.parameter.parametersOf
import org.koin.core.qualifier.named

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

fun Module.autofillResourcesModule() {
    scope<AutofillResourcesActivity> {
        scoped<AutofillResourcesContract.Presenter> {
            AutofillResourcesPresenter(
                coroutineLaunchContext = get(),
                getLocalResourcesUse = get(),
                getLocalResourcesFilteredByTag = get(),
                domainProvider = get(),
                homeDataInteractor = get(),
                resourceSearch = get(),
                secretInteractor = get(),
                getSelectedAccountDataUseCase = get(),
                resourceTypeFactory = get(),
                secretParser = get(),
                getAccountsUseCase = get()
            )
        }
        scoped { (accountUiItemsMapper: ResourceUiItemsMapper) ->
            ModelAdapter(accountUiItemsMapper::mapModelToItem)
        }
        scoped { ResourceUiItemsMapper() }
        scoped(named<ResourceListUiModel>()) {
            FastAdapter.with(get<ModelAdapter<ResourceListUiModel, GenericItem>> {
                parametersOf(get<ResourceUiItemsMapper>())
            })
        }
        scoped<ReturnAutofillDatasetStrategy>(
            named(AutofillMode.AUTOFILL)
        ) { (view: AutofillResourcesContract.View) ->
            ReturnAutofillDataset(
                view = view,
                appContext = androidContext(),
                assistStructureParser = get(),
                fillableInputsFinder = get(),
                remoteViewsFactory = get()
            )
        }
        scoped<ReturnAutofillDatasetStrategy>(
            named(AutofillMode.ACCESSIBILITY)
        ) { (view: AutofillResourcesContract.View) ->
            ReturnAccessibilityDataset(view)
        }
    }
}
