package com.passbolt.mobile.android.feature.resourcedetails.update

import android.annotation.SuppressLint
import com.passbolt.mobile.android.common.extension.erase
import com.passbolt.mobile.android.common.validation.Rule
import com.passbolt.mobile.android.common.validation.StringMaxLength
import com.passbolt.mobile.android.common.validation.StringNotBlank
import com.passbolt.mobile.android.common.validation.Validation
import com.passbolt.mobile.android.common.validation.validation
import com.passbolt.mobile.android.core.fulldatarefresh.base.DataRefreshViewReactivePresenter
import com.passbolt.mobile.android.core.idlingresource.CreateResourceIdlingResource
import com.passbolt.mobile.android.core.idlingresource.UpdateResourceIdlingResource
import com.passbolt.mobile.android.core.mvp.coroutinecontext.CoroutineLaunchContext
import com.passbolt.mobile.android.core.passwordgenerator.SecretGenerator
import com.passbolt.mobile.android.core.passwordgenerator.entropy.Entropy
import com.passbolt.mobile.android.core.passwordgenerator.entropy.EntropyCalculator
import com.passbolt.mobile.android.core.passwordgenerator.usecase.CheckPasswordPropertiesUseCase
import com.passbolt.mobile.android.core.resources.actions.ResourceCreateActionsInteractor
import com.passbolt.mobile.android.core.resources.actions.ResourceUpdateActionResult
import com.passbolt.mobile.android.core.resources.actions.ResourceUpdateActionsInteractor
import com.passbolt.mobile.android.core.resources.actions.performResourceCreateAction
import com.passbolt.mobile.android.core.resources.actions.performResourceUpdateAction
import com.passbolt.mobile.android.core.resourcetypes.usecase.db.ResourceTypeIdToSlugMappingProvider
import com.passbolt.mobile.android.core.secrets.usecase.decrypt.SecretInteractor
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
import com.passbolt.mobile.android.serializers.jsonschema.SchemaEntity
import com.passbolt.mobile.android.storage.usecase.policies.GetPasswordPoliciesUseCase
import com.passbolt.mobile.android.supportedresourceTypes.ContentType
import com.passbolt.mobile.android.ui.PasswordGeneratorTypeModel
import com.passbolt.mobile.android.ui.ResourceField
import com.passbolt.mobile.android.ui.ResourceModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import org.koin.core.component.get
import org.koin.core.parameter.parametersOf
import retrofit2.HttpException
import timber.log.Timber
import java.net.HttpURLConnection.HTTP_NOT_FOUND
import java.util.UUID

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
    private val secretGenerator: SecretGenerator,
    private val entropyViewMapper: EntropyViewMapper,
    private val editFieldsModelCreator: EditFieldsModelCreator,
    private val newFieldsModelCreator: NewFieldsModelCreator,
    private val secretInteractor: SecretInteractor,
    private val fieldNamesMapper: FieldNamesMapper,
    private val createResourceIdlingResource: CreateResourceIdlingResource,
    private val updateResourceIdlingResource: UpdateResourceIdlingResource,
    private val getPasswordPoliciesUseCase: GetPasswordPoliciesUseCase,
    private val entropyCalculator: EntropyCalculator,
    private val checkPasswordPropertiesUseCase: CheckPasswordPropertiesUseCase,
    private val idToSlugMappingProvider: ResourceTypeIdToSlugMappingProvider
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
        if (!::fields.isInitialized) {
            createForm()
        }
    }

    private fun createForm() {
        scope.launch {
            view?.showProgress()
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
            ResourceUpdateType.CREATE -> newFieldsModelCreator.create(ContentType.PasswordAndDescription)
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

        view?.clearInputsContainer()
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

    private suspend fun handlePasswordInput(it: ResourceValue) {
        val initialValueEntropy = it.value?.let { password ->
            entropyCalculator.getSecretEntropy(password)
        } ?: 0.0

        view?.addPasswordInput(
            fieldNamesMapper.mapFieldNameToUiName(it.field.name),
            it.uiTag,
            it.field.isRequired,
            it.value,
            entropyViewMapper.map(Entropy.parse(initialValueEntropy)),
            initialValueEntropy
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
                    scope.launch {
                        if (getPasswordPoliciesUseCase.execute(Unit).isExternalDictionaryCheckEnabled) {
                            checkPasswordStatus()
                        } else {
                            updateResource()
                        }
                    }
                }
            }
        }
    }

    private suspend fun checkPasswordStatus() {
        val passwordField = if (isSimplePasswordEdit()) SECRET_FIELD else PASSWORD_FIELD
        when (checkPasswordPropertiesUseCase.execute(
            CheckPasswordPropertiesUseCase.Input(getFieldValue(passwordField)!!)
        )
        ) {
            CheckPasswordPropertiesUseCase.Output.Fine -> updateResource()
            is CheckPasswordPropertiesUseCase.Output.Pwned -> view?.showConfirmPwnedPassword()
            is CheckPasswordPropertiesUseCase.Output.Weak -> view?.showConfirmWeakPassword()
            is CheckPasswordPropertiesUseCase.Output.Failure<*> -> {
                Timber.d("Failed to check password status")
                // if external service is down, proceed with password update without checking
                updateResource()
            }
        }
    }

    private suspend fun isSimplePasswordEdit() = if (existingResource == null) {
        false
    } else {
        val slug = idToSlugMappingProvider.provideMappingForSelectedAccount()[
            UUID.fromString(existingResource!!.resourceTypeId)
        ]
        ContentType.fromSlug(slug!!).isSimplePassword()
    }

    override fun onPwnedOrWeakPasswordConfirmed() {
        updateResource()
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
        val resourceCreateActionsInteractor = get<ResourceCreateActionsInteractor> {
            parametersOf(needSessionRefreshFlow, sessionRefreshedFlow)
        }
        performResourceCreateAction(
            action = {
                resourceCreateActionsInteractor.createPasswordAndDescriptionResource(
                    resourceName = getFieldValue(NAME_FIELD)!!, // validated to be not null
                    resourceUsername = getFieldValue(USERNAME_FIELD),
                    resourceUri = getFieldValue(URI_FIELD),
                    resourceParentFolderId = resourceParentFolderId,
                    password = getFieldValue(PASSWORD_FIELD)!!, // validated to be not null
                    description = getFieldValue(DESCRIPTION_FIELD)
                )
            },
            doOnFailure = { view?.showError() },
            doOnCryptoFailure = { view?.showEncryptionError(it) },
            doOnSchemaValidationFailure = ::handleSchemaValidationFailure,
            doOnShareFailure = { view?.showShareFailure() },
            doOnSuccess = { view?.closeWithCreateSuccessResult(it.resourceName, it.resourceId) }
        )
    }

    private fun handleSchemaValidationFailure(entity: SchemaEntity) {
        when (entity) {
            SchemaEntity.RESOURCE -> view?.showJsonResourceSchemaValidationError()
            SchemaEntity.SECRET -> view?.showJsonSecretSchemaValidationError()
        }
    }

    private suspend fun editResource() {
        val existingResource = requireNotNull(existingResource) { "In edit mode but existing resource not present" }
        val resourceUpdateActionsInteractor = get<ResourceUpdateActionsInteractor> {
            parametersOf(existingResource, needSessionRefreshFlow, sessionRefreshedFlow)
        }
        performResourceUpdateAction(
            action = { getUpdateOperation(existingResource, resourceUpdateActionsInteractor) },
            doOnFailure = { view?.showError() },
            doOnCryptoFailure = { view?.showEncryptionError(it) },
            doOnSchemaValidationFailure = ::handleSchemaValidationFailure,
            doOnSuccess = { view?.closeWithEditSuccessResult(existingResource.name) }
        )
    }

    @SuppressLint("StopShip")
    private suspend fun getUpdateOperation(
        existingResource: ResourceModel,
        resourceUpdateActionsInteractor: ResourceUpdateActionsInteractor
    ): Flow<ResourceUpdateActionResult> {
        val slug = idToSlugMappingProvider.provideMappingForSelectedAccount()[
            UUID.fromString(existingResource.resourceTypeId)
        ]
        return when (val contentType = ContentType.fromSlug(slug!!)) {
            is ContentType.PasswordString ->
                resourceUpdateActionsInteractor.updateSimplePasswordResource(
                    resourceName = getFieldValue(NAME_FIELD)!!, // validated to be not null
                    resourceUsername = getFieldValue(USERNAME_FIELD),
                    resourceUri = getFieldValue(URI_FIELD),
                    resourceParentFolderId = resourceParentFolderId,
                    password = getFieldValue(SECRET_FIELD)!!, // validated to be not null
                    description = getFieldValue(DESCRIPTION_FIELD)
                )
            is ContentType.PasswordAndDescription ->
                resourceUpdateActionsInteractor.updatePasswordAndDescriptionResource(
                    resourceName = getFieldValue(NAME_FIELD)!!, // validated to be not null
                    resourceUsername = getFieldValue(USERNAME_FIELD),
                    resourceUri = getFieldValue(URI_FIELD),
                    password = getFieldValue(PASSWORD_FIELD)!!, // validated to be not null
                    description = getFieldValue(DESCRIPTION_FIELD),
                    resourceParentFolderId = resourceParentFolderId
                )

            is ContentType.V5DefaultWithTotp ->
                resourceUpdateActionsInteractor.updateLinkedTotpResourcePasswordFields(
                    resourceName = getFieldValue(NAME_FIELD)!!, // validated to be not null
                    resourceUsername = getFieldValue(USERNAME_FIELD),
                    resourceUri = getFieldValue(URI_FIELD),
                    resourceParentFolderId = resourceParentFolderId,
                    password = getFieldValue(PASSWORD_FIELD)!!, // validated to be not null
                    description = getFieldValue(DESCRIPTION_FIELD)
                )
            is ContentType.Totp ->
                throw IllegalArgumentException("Unsupported resource type on update form: $contentType")
            ContentType.PasswordDescriptionTotp -> TODO()
            ContentType.V5Default -> TODO()
            ContentType.V5PasswordString -> TODO()
            ContentType.V5TotpStandalone -> TODO()
        }
    }

    private fun getFieldValue(fieldName: String) =
        fields.find { it.field.name == fieldName }?.value

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
        scope.launch {
            val passwordPolicies = getPasswordPoliciesUseCase.execute(Unit)
            val secretGenerationResult = when (passwordPolicies.defaultGenerator) {
                PasswordGeneratorTypeModel.PASSWORD ->
                    secretGenerator.generatePassword(passwordPolicies.passwordGeneratorSettings)
                PasswordGeneratorTypeModel.PASSPHRASE ->
                    secretGenerator.generatePassphrase(passwordPolicies.passphraseGeneratorSettings)
            }
            when (secretGenerationResult) {
                is SecretGenerator.SecretGenerationResult.FailedToGenerateLowEntropy ->
                    view?.showUnableToGeneratePassword(secretGenerationResult.minimumEntropyBits)
                is SecretGenerator.SecretGenerationResult.Success -> view?.showPassword(
                    tag,
                    secretGenerationResult.password,
                    secretGenerationResult.entropy,
                    entropyViewMapper.map(Entropy.parse(secretGenerationResult.entropy))
                )
            }
        }
    }

    override fun passwordTextChanged(tag: String, password: String) {
        scope.launch {
            fields.find {
                it.field.name == PASSWORD_FIELD || it.field.name == SECRET_FIELD
            }?.value = password
            val entropy = entropyCalculator.getSecretEntropy(password)
            view?.showPasswordStrength(tag, entropyViewMapper.map(Entropy.parse(entropy)), entropy)
        }
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
}
