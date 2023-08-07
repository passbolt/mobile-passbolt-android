package com.passbolt.mobile.android.feature.resourcedetails.update

import com.passbolt.mobile.android.common.extension.erase
import com.passbolt.mobile.android.common.validation.Rule
import com.passbolt.mobile.android.common.validation.StringMaxLength
import com.passbolt.mobile.android.common.validation.StringNotBlank
import com.passbolt.mobile.android.common.validation.Validation
import com.passbolt.mobile.android.common.validation.validation
import com.passbolt.mobile.android.core.commonfolders.usecase.db.GetLocalParentFolderPermissionsToApplyToNewItemUseCase
import com.passbolt.mobile.android.core.commonfolders.usecase.db.ItemIdResourceId
import com.passbolt.mobile.android.core.fulldatarefresh.base.DataRefreshViewReactivePresenter
import com.passbolt.mobile.android.core.idlingresource.CreateResourceIdlingResource
import com.passbolt.mobile.android.core.idlingresource.UpdateResourceIdlingResource
import com.passbolt.mobile.android.core.mvp.authentication.AuthenticatedUseCaseOutput
import com.passbolt.mobile.android.core.mvp.authentication.AuthenticationState
import com.passbolt.mobile.android.core.mvp.coroutinecontext.CoroutineLaunchContext
import com.passbolt.mobile.android.core.passwordgenerator.PasswordGenerator
import com.passbolt.mobile.android.core.resources.interactor.create.CreatePasswordAndDescriptionResourceInteractor
import com.passbolt.mobile.android.core.resources.interactor.create.CreateResourceInteractor
import com.passbolt.mobile.android.core.resources.interactor.update.UpdateLinkedTotpResourceInteractor
import com.passbolt.mobile.android.core.resources.interactor.update.UpdatePasswordAndDescriptionResourceInteractor
import com.passbolt.mobile.android.core.resources.interactor.update.UpdateResourceInteractor
import com.passbolt.mobile.android.core.resources.interactor.update.UpdateSimplePasswordResourceInteractor
import com.passbolt.mobile.android.core.resources.usecase.ResourceShareInteractor
import com.passbolt.mobile.android.core.resources.usecase.db.AddLocalResourcePermissionsUseCase
import com.passbolt.mobile.android.core.resources.usecase.db.AddLocalResourceUseCase
import com.passbolt.mobile.android.core.resources.usecase.db.UpdateLocalResourceUseCase
import com.passbolt.mobile.android.core.resourcetypes.ResourceTypeFactory
import com.passbolt.mobile.android.core.resourcetypes.ResourceTypeFactory.ResourceTypeEnum.PASSWORD_DESCRIPTION_TOTP
import com.passbolt.mobile.android.core.resourcetypes.ResourceTypeFactory.ResourceTypeEnum.PASSWORD_WITH_DESCRIPTION
import com.passbolt.mobile.android.core.resourcetypes.ResourceTypeFactory.ResourceTypeEnum.SIMPLE_PASSWORD
import com.passbolt.mobile.android.core.resourcetypes.ResourceTypeFactory.ResourceTypeEnum.STANDALONE_TOTP
import com.passbolt.mobile.android.core.secrets.usecase.decrypt.SecretInteractor
import com.passbolt.mobile.android.core.secrets.usecase.decrypt.parser.SecretParser
import com.passbolt.mobile.android.entity.resource.ResourceField
import com.passbolt.mobile.android.feature.authentication.session.runAuthenticatedOperation
import com.passbolt.mobile.android.feature.resourcedetails.ResourceMode
import com.passbolt.mobile.android.feature.resourcedetails.update.fieldsgenerator.EditFieldsModelCreator
import com.passbolt.mobile.android.feature.resourcedetails.update.fieldsgenerator.FieldNamesMapper
import com.passbolt.mobile.android.feature.resourcedetails.update.fieldsgenerator.FieldNamesMapper.Companion.DESCRIPTION_FIELD
import com.passbolt.mobile.android.feature.resourcedetails.update.fieldsgenerator.FieldNamesMapper.Companion.NAME_FIELD
import com.passbolt.mobile.android.feature.resourcedetails.update.fieldsgenerator.FieldNamesMapper.Companion.PASSWORD_FIELD
import com.passbolt.mobile.android.feature.resourcedetails.update.fieldsgenerator.FieldNamesMapper.Companion.SECRET_FIELD
import com.passbolt.mobile.android.feature.resourcedetails.update.fieldsgenerator.FieldNamesMapper.Companion.URI_FIELD
import com.passbolt.mobile.android.feature.resourcedetails.update.fieldsgenerator.FieldNamesMapper.Companion.USERNAME_FIELD
import com.passbolt.mobile.android.feature.resourcedetails.update.fieldsgenerator.NewFieldsModelCreator
import com.passbolt.mobile.android.feature.resourcedetails.update.fieldsgenerator.ResourceUpdateType
import com.passbolt.mobile.android.ui.DecryptedSecretOrError
import com.passbolt.mobile.android.ui.ResourceModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.launch
import retrofit2.HttpException
import timber.log.Timber
import java.net.HttpURLConnection.HTTP_NOT_FOUND

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
class UpdateResourcePresenter(
    coroutineLaunchContext: CoroutineLaunchContext,
    private val passwordGenerator: PasswordGenerator,
    private val entropyViewMapper: EntropyViewMapper,
    private val createPasswordAndDescriptionResourceInteractor: CreatePasswordAndDescriptionResourceInteractor,
    private val addLocalResourceUseCase: AddLocalResourceUseCase,
    private val updateLocalResourceUseCase: UpdateLocalResourceUseCase,
    private val updateSimplePasswordResourceInteractor: UpdateSimplePasswordResourceInteractor,
    private val updatePasswordAndDescriptionResourceInteractor: UpdatePasswordAndDescriptionResourceInteractor,
    private val updateLinkedTotpResourceInteractor: UpdateLinkedTotpResourceInteractor,
    private val resourceTypeFactory: ResourceTypeFactory,
    private val editFieldsModelCreator: EditFieldsModelCreator,
    private val newFieldsModelCreator: NewFieldsModelCreator,
    private val secretInteractor: SecretInteractor,
    private val fieldNamesMapper: FieldNamesMapper,
    private val resourceShareInteractor: ResourceShareInteractor,
    private val getLocalParentFolderPermissionsToApplyUseCase: GetLocalParentFolderPermissionsToApplyToNewItemUseCase,
    private val addLocalResourcePermissionsUseCase: AddLocalResourcePermissionsUseCase,
    private val createResourceIdlingResource: CreateResourceIdlingResource,
    private val updateResourceIdlingResource: UpdateResourceIdlingResource,
    private val secretParser: SecretParser
) : DataRefreshViewReactivePresenter<UpdateResourceContract.View>(coroutineLaunchContext),
    UpdateResourceContract.Presenter {

    override var view: UpdateResourceContract.View? = null

    private val job = SupervisorJob()
    private val scope = CoroutineScope(job + coroutineLaunchContext.ui)

    private lateinit var fields: List<ResourceValue>
    private lateinit var resourceUpdateType: ResourceUpdateType
    private var existingResource: ResourceModel? = null
    private var existingSecret: ByteArray? = null
    private var resourceParentFolderId: String? = null

    override fun argsRetrieved(mode: ResourceMode, resource: ResourceModel?, resourceParentFolderId: String?) {
        this.resourceParentFolderId = resourceParentFolderId
        this.resourceUpdateType = ResourceUpdateType.from(mode)
        this.existingResource = resource

        setupUi()
    }

    override fun refreshAction() {
        view?.showProgress()
        view?.clearInputFields()
        scope.launch {
            when (resourceUpdateType) {
                ResourceUpdateType.CREATE -> createInputFields()
                ResourceUpdateType.EDIT -> {
                    updateResourceIdlingResource.setIdle(false)
                    val existingResource = requireNotNull(existingResource)
                    doAfterFetchAndDecrypt(existingResource.resourceId,
                        action = {
                            existingSecret = it
                            createInputFields(existingResource, it)
                        },
                        itemMissingErrorAction = {
                            view?.showContentNotAvailable()
                            view?.navigateHome()
                        },
                        genericErrorAction = {
                            Timber.e("Error during secret fetching")
                            view?.showError()
                        }
                    )
                    updateResourceIdlingResource.setIdle(true)
                }
            }
            view?.hideProgress()
        }
    }

    override fun refreshFailureAction() {
        view?.showDataRefreshError()
    }

    private fun setupUi() {
        when (resourceUpdateType) {
            ResourceUpdateType.CREATE -> {
                view?.apply {
                    showCreateTitle()
                    showCreateButton()
                }
            }
            ResourceUpdateType.EDIT -> {
                view?.apply {
                    showEditTitle()
                    showEditButton()
                }
            }
        }
    }

    private suspend fun createInputFields(
        existingResource: ResourceModel? = null,
        existingResourceSecret: ByteArray? = null
    ) {
        fields = when (resourceUpdateType) {
            ResourceUpdateType.CREATE -> newFieldsModelCreator.create()
            ResourceUpdateType.EDIT -> {
                val resource = requireNotNull(existingResource)
                val secret = requireNotNull(existingResourceSecret)
                try {
                    editFieldsModelCreator.create(resource, secret)
                } catch (exception: ClassCastException) {
                    Timber.e("Secret data has invalid type, quitting edit form")
                    view?.showInvalidSecretDataAndNavigateBack()
                    return
                }
            }
        }

        fields.forEach {
            when (it.field.name) {
                in listOf(PASSWORD_FIELD, SECRET_FIELD) -> handlePasswordInput(it)
                DESCRIPTION_FIELD -> handleDescriptionField(it)
                else -> handleTextInput(it)
            }
        }
    }

    private fun handleTextInput(it: ResourceValue) {
        view?.addTextInput(
            fieldNamesMapper.mapFieldNameToUiName(it.field.name),
            it.field.isSecret,
            it.uiTag,
            it.field.isRequired,
            it.value
        )
    }

    private fun handleDescriptionField(it: ResourceValue) {
        view?.addDescriptionInput(
            fieldNamesMapper.mapFieldNameToUiName(it.field.name),
            it.field.isSecret,
            it.uiTag,
            it.field.isRequired,
            it.value
        )
    }

    private suspend fun doAfterFetchAndDecrypt(
        resourceId: String,
        action: suspend (ByteArray) -> Unit,
        genericErrorAction: () -> Unit,
        itemMissingErrorAction: () -> Unit = genericErrorAction
    ) {
        when (val output =
            runAuthenticatedOperation(needSessionRefreshFlow, sessionRefreshedFlow) {
                secretInteractor.fetchAndDecrypt(resourceId)
            }
        ) {
            is SecretInteractor.Output.Success -> {
                action(output.decryptedSecret)
            }
            is SecretInteractor.Output.FetchFailure -> {
                if ((output.exception as? HttpException)?.code() == HTTP_NOT_FOUND) {
                    itemMissingErrorAction()
                } else {
                    genericErrorAction()
                }
            }
            else -> genericErrorAction.invoke()
        }
    }

    private fun handlePasswordInput(it: ResourceValue) {
        val initialValueEntropy = PasswordGenerator.Entropy.parse(
            it.value?.let {
                passwordGenerator.getEntropy(it)
            } ?: 0.0
        )
        view?.addPasswordInput(
            fieldNamesMapper.mapFieldNameToUiName(it.field.name),
            it.uiTag,
            it.field.isRequired,
            it.value,
            entropyViewMapper.map(initialValueEntropy)
        )
    }

    override fun updateClick() {
        validation {
            fields.forEach { pair ->
                val rules = getRules(pair.field)
                of(pair.value.orEmpty()) {
                    rules.forEach { rule ->
                        validateRules(rule, pair)
                    }
                }
                onValid {
                    updateResource()
                }
            }
        }
    }

    override fun textChanged(tag: String, value: String) {
        fields.find { it.field.name == tag }?.value = value
    }

    private fun updateResource() {
        view?.showProgress()
        scope.launch {
            when (resourceUpdateType) {
                ResourceUpdateType.CREATE -> {
                    createResourceIdlingResource.setIdle(false)
                    createResource()
                    createResourceIdlingResource.setIdle(true)
                }
                ResourceUpdateType.EDIT -> {
                    updateResourceIdlingResource.setIdle(false)
                    editResource()
                    updateResourceIdlingResource.setIdle(true)
                }
            }
            view?.hideProgress()
        }
    }

    private suspend fun createResource() {
        when (val result = runAuthenticatedOperation(needSessionRefreshFlow, sessionRefreshedFlow) {
            createPasswordAndDescriptionResourceInteractor.execute(
                createResourceCommonCreateInput(),
                createResourceCustomInput()
            )
        }) {
            is CreateResourceInteractor.Output.Failure<*> -> view?.showError()
            is CreateResourceInteractor.Output.OpenPgpError -> view?.showEncryptionError(result.message)
            is CreateResourceInteractor.Output.PasswordExpired -> {
                /* will not happen in BaseAuthenticatedPresenter */
            }
            is CreateResourceInteractor.Output.Success -> {
                addLocalResourceUseCase.execute(AddLocalResourceUseCase.Input(result.resource.resourceModel))
                addLocalResourcePermissionsUseCase.execute(
                    AddLocalResourcePermissionsUseCase.Input(listOf(result.resource))
                )
                resourceParentFolderId?.let {
                    applyFolderPermissionsToCreatedResource(result.resource.resourceModel, it)
                } ?: run {
                    view?.closeWithCreateSuccessResult(
                        result.resource.resourceModel.name,
                        result.resource.resourceModel.resourceId
                    )
                }
            }
        }
    }

    private suspend fun applyFolderPermissionsToCreatedResource(
        resource: ResourceModel,
        resourceParentFolderId: String
    ) {
        val newPermissionsToApply = getLocalParentFolderPermissionsToApplyUseCase.execute(
            GetLocalParentFolderPermissionsToApplyToNewItemUseCase.Input(
                resourceParentFolderId,
                ItemIdResourceId(resource.resourceId)
            )
        ).permissions

        when (runAuthenticatedOperation(needSessionRefreshFlow, sessionRefreshedFlow) {
            resourceShareInteractor.simulateAndShareResource(resource.resourceId, newPermissionsToApply)
        }) {
            is ResourceShareInteractor.Output.SecretDecryptFailure -> view?.showSecretDecryptFailure()
            is ResourceShareInteractor.Output.SecretEncryptFailure -> view?.showSecretEncryptFailure()
            is ResourceShareInteractor.Output.SecretFetchFailure -> view?.showSecretFetchFailure()
            is ResourceShareInteractor.Output.ShareFailure -> view?.showShareFailure()
            is ResourceShareInteractor.Output.SimulateShareFailure -> view?.showShareSimulationFailure()
            is ResourceShareInteractor.Output.Success -> {
                view?.closeWithCreateSuccessResult(resource.name, resource.resourceId)
            }
            is ResourceShareInteractor.Output.Unauthorized -> {
                // handled automatically in runAuthenticatedOperation
            }
        }
    }

    private suspend fun editResource() {
        val existingResource = requireNotNull(existingResource) { "In edit mode but existing resource not present" }

        val updateOperation =
            when (val resourceTypeEnum = resourceTypeFactory.getResourceTypeEnum(existingResource.resourceTypeId)) {
                SIMPLE_PASSWORD -> suspend {
                    updateSimplePasswordResourceInteractor.execute(
                        createCommonUpdateInput(existingResource),
                        createSimplePasswordUpdateInput()
                    )
                }
                PASSWORD_WITH_DESCRIPTION -> suspend {
                    updatePasswordAndDescriptionResourceInteractor.execute(
                        createCommonUpdateInput(existingResource),
                        createPasswordAndDescriptionUpdateInput()
                    )
                }
                PASSWORD_DESCRIPTION_TOTP -> suspend {
                    try {
                        updateLinkedTotpResourceInteractor.execute(
                            createCommonUpdateInput(existingResource),
                            createPasswordDescriptionTotpUpdateInput()
                        )
                    } catch (exception: ClassCastException) {
                        Timber.e("Secret data has invalid type, quitting edit form")
                        view?.showInvalidSecretDataAndNavigateBack()
                        object : AuthenticatedUseCaseOutput {
                            override val authenticationState = AuthenticationState.Authenticated
                        }
                    }
                }
                STANDALONE_TOTP ->
                    throw IllegalArgumentException("Unsupported resource type on update form: $resourceTypeEnum")
            }

        when (
            val editResourceResult = runAuthenticatedOperation(needSessionRefreshFlow, sessionRefreshedFlow) {
                updateOperation.invoke()
            }) {
            is UpdateResourceInteractor.Output.Failure<*> -> {
                view?.showError()
            }
            is UpdateResourceInteractor.Output.OpenPgpError -> {
                view?.showEncryptionError(editResourceResult.message)
            }
            is UpdateResourceInteractor.Output.PasswordExpired -> {
                /* will not happen in BaseAuthenticatedPresenter */
            }
            is UpdateResourceInteractor.Output.Success -> {
                updateLocalResourceUseCase.execute(
                    UpdateLocalResourceUseCase.Input(editResourceResult.resource)
                )
                view?.closeWithEditSuccessResult(existingResource.name)
            }
        }
    }

    @Throws(ClassCastException::class)
    private suspend fun createPasswordDescriptionTotpUpdateInput():
            UpdateLinkedTotpResourceInteractor.UpdateToLinkedTotpInput {

        val existingTotpSecret = secretParser.extractTotpData(
            existingResource!!.resourceTypeId,
            existingSecret!!
        ) as DecryptedSecretOrError.DecryptedSecret
        return UpdateLinkedTotpResourceInteractor.UpdateToLinkedTotpInput(
            period = existingTotpSecret.secret.period,
            digits = existingTotpSecret.secret.digits,
            algorithm = existingTotpSecret.secret.algorithm,
            secretKey = existingTotpSecret.secret.key,
            existingSecret = existingSecret!!,
            existingResourceTypeId = existingResource!!.resourceTypeId,
            password = getFieldValue(PASSWORD_FIELD)!!,
            description = getFieldValue(DESCRIPTION_FIELD)
        )
    }

    private fun createCommonUpdateInput(existingResource: ResourceModel) =
        UpdateResourceInteractor.CommonInput(
            resourceId = existingResource.resourceId,
            resourceName = getFieldValue(NAME_FIELD)!!, // validated to be not null
            resourceUsername = getFieldValue(USERNAME_FIELD),
            resourceUri = getFieldValue(URI_FIELD),
            resourceParentFolderId = resourceParentFolderId
        )

    private fun createSimplePasswordUpdateInput() =
        UpdateSimplePasswordResourceInteractor.UpdateSimplePasswordInput(
            password = getFieldValue(SECRET_FIELD)!!,
            description = getFieldValue(DESCRIPTION_FIELD)
        )

    private fun createPasswordAndDescriptionUpdateInput() =
        UpdatePasswordAndDescriptionResourceInteractor.UpdatePasswordAndDescriptionInput(
            password = getFieldValue(PASSWORD_FIELD)!!,
            description = getFieldValue(DESCRIPTION_FIELD)
        )

    private fun getFieldValue(fieldName: String) =
        fields.find { it.field.name == fieldName }?.value

    private fun createResourceCommonCreateInput() =
        CreateResourceInteractor.CommonInput(
            resourceName = getFieldValue(NAME_FIELD)!!, // validated to be not null
            resourceUsername = getFieldValue(USERNAME_FIELD),
            resourceUri = getFieldValue(URI_FIELD),
            resourceParentFolderId = resourceParentFolderId
        )

    private fun createResourceCustomInput() =
        CreatePasswordAndDescriptionResourceInteractor.CreatePasswordAndDescriptionInput(
            password = getFieldValue(PASSWORD_FIELD)!!, // validated to be not null
            getFieldValue(DESCRIPTION_FIELD)
        )

    private fun Validation.ValueValidation<String>.validateRules(
        rule: Rule<String>,
        resourceValue: ResourceValue
    ) {
        withRules(rule) {
            when (rule) {
                is StringNotBlank -> {
                    onInvalid {
                        view?.showEmptyValueError(resourceValue.field.name)
                    }
                }
                is StringMaxLength -> {
                    onInvalid {
                        view?.showTooLongError(resourceValue.field.name)
                    }
                }
            }
        }
    }

    override fun passwordGenerateClick(tag: String) {
        val generatedPassword = passwordGenerator.generate(targetEntropy = DEFAULT_ENTROPY)
        view?.showPassword(tag, generatedPassword, entropyViewMapper.map(DEFAULT_ENTROPY))
    }

    override fun passwordTextChanged(tag: String, password: String) {
        fields.find {
            it.field.name == PASSWORD_FIELD || it.field.name == SECRET_FIELD
        }?.value = password
        val entropy = PasswordGenerator.Entropy.parse(
            passwordGenerator.getEntropy(password)
        )
        view?.showPasswordStrength(tag, entropyViewMapper.map(entropy))
    }

    private fun getRules(field: ResourceField): List<Rule<String>> {
        val rules = mutableListOf<Rule<String>>()
        if (field.isRequired) {
            rules.add(StringNotBlank)
        }
        field.maxLength?.let {
            rules.add(StringMaxLength(it))
        }
        return rules
    }

    override fun detach() {
        existingSecret?.erase()
        scope.coroutineContext.cancelChildren()
        super<DataRefreshViewReactivePresenter>.detach()
    }

    // TODO add support for fully dynamic request model
    companion object {
        private val DEFAULT_ENTROPY = PasswordGenerator.Entropy.VERY_STRONG
    }
}
