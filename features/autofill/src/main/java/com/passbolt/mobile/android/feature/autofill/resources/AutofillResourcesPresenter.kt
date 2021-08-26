package com.passbolt.mobile.android.feature.autofill.resources

import android.app.assist.AssistStructure
import android.service.autofill.Dataset
import android.view.View
import android.view.autofill.AutofillId
import android.view.autofill.AutofillValue
import com.passbolt.mobile.android.common.search.SearchableMatcher
import com.passbolt.mobile.android.core.commonresource.ResourceInteractor
import com.passbolt.mobile.android.core.mvp.coroutinecontext.CoroutineLaunchContext
import com.passbolt.mobile.android.database.usecase.GetLocalResourcesUseCase
import com.passbolt.mobile.android.dto.response.ResourceResponseDto
import com.passbolt.mobile.android.feature.autofill.StructureParser
import com.passbolt.mobile.android.feature.autofill.service.CheckUrlLinksUseCase
import com.passbolt.mobile.android.feature.autofill.service.ParsedStructure
import com.passbolt.mobile.android.mappers.ResourceModelMapper
import com.passbolt.mobile.android.storage.usecase.input.UserIdInput
import com.passbolt.mobile.android.storage.usecase.selectedaccount.GetSelectedAccountUseCase
import com.passbolt.mobile.android.ui.ResourceModel
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
class AutofillResourcesPresenter(
    coroutineLaunchContext: CoroutineLaunchContext,
    private val structureParser: StructureParser,
    private val getLocalResourcesUse: GetLocalResourcesUseCase,
    private val getSelectedAccountUseCase: GetSelectedAccountUseCase,
    private val checkUrlLinksUseCase: CheckUrlLinksUseCase,
    private val resourcesInteractor: ResourceInteractor,
    private val fetchAndUpdateDatabaseUseCase: FetchAndUpdateDatabaseUseCase,
    private val resourceModelMapper: ResourceModelMapper,
    private val resourceSearch: SearchableMatcher
) : AutofillResourcesContract.Presenter {

    override var view: AutofillResourcesContract.View? = null
    private lateinit var parsedStructure: Set<ParsedStructure>
    private val job = SupervisorJob()
    private val scope = CoroutineScope(job + coroutineLaunchContext.ui)
    private var allItemsList: List<ResourceModel> = emptyList()
    private var currentSearchText: String = ""

    override fun attach(view: AutofillResourcesContract.View) {
        super.attach(view)
        view.startAuthActivity()
    }

    override fun userAuthenticated() {
        scope.launch {
            // TODO
            val links = checkUrlLinksUseCase.execute(
                CheckUrlLinksUseCase.Input(
                    "https://www.instagram.com"
                )
            )
            val accountId = requireNotNull(getSelectedAccountUseCase.execute(Unit).selectedAccount)
            allItemsList = getLocalResourcesUse.execute(UserIdInput(accountId)).resources
            if (allItemsList.isEmpty()) {
                fetchResources()
            } else {
                view?.showResources(allItemsList)
            }
        }
    }

    override fun refreshSwipe() {
        fetchResources()
    }

    private fun fetchResources(localListIsEmpty: Boolean = false) {
        scope.launch {
            when (val result = resourcesInteractor.fetchResourcesWithTypes()) {
                is ResourceInteractor.Output.Failure -> {
                    if (localListIsEmpty) {
                        view?.showFullScreenError()
                    } else {
                        view?.showGeneralError()
                    }
                }
                is ResourceInteractor.Output.Success -> {
                    if (result.resources.isEmpty()) {
                        updateLocalDatabase(result.resources)
                        view?.showEmptyList()
                    } else {
                        updateLocalDatabase(result.resources)
                        view?.showResources(allItemsList)
                    }
                }
            }
        }
    }

    private suspend fun updateLocalDatabase(resources: List<ResourceResponseDto>) {
        allItemsList = resources.map { resourceModelMapper.map(it) }
        fetchAndUpdateDatabaseUseCase.execute(FetchAndUpdateDatabaseUseCase.Input(allItemsList))
    }

    override fun returnClick(resourceModel: ResourceModel) {
        val usernameParsedAssistStructure = structureParser.extractHint(View.AUTOFILL_HINT_USERNAME, parsedStructure)
        val passwordParsedAssistStructure = structureParser.extractHint(View.AUTOFILL_HINT_PASSWORD, parsedStructure)

        if (passwordParsedAssistStructure == null || usernameParsedAssistStructure == null) {
            view?.navigateBack()
            return
        }

        val dataSet = createDataSet(
            usernameParsedAssistStructure.id,
            passwordParsedAssistStructure.id,
            resourceModel.username
        )
        view?.returnData(dataSet)
    }

    override fun argsReceived(structure: AssistStructure) {
        parsedStructure = structureParser.parse(structure)
    }

    override fun searchTextChange(text: String) {
        currentSearchText = text
        filterList()
    }

    private fun filterList() {
        val filtered = allItemsList.filter {
            resourceSearch.matches(it, currentSearchText)
        }
        if (filtered.isEmpty()) {
            view?.showSearchEmptyList()
        } else {
            view?.showResources(filtered)
        }
    }

    private fun createDataSet(
        usernameId: AutofillId,
        passwordId: AutofillId,
        usernameValue: String
    ) =
        Dataset.Builder()
            .setValue(
                usernameId,
                AutofillValue.forText(usernameValue)
            )
            .setValue(
                passwordId,
                AutofillValue.forText("Mocked password")
            )
            .build()
}
