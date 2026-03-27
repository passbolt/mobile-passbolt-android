package com.passbolt.mobile.android.core.resources.actions

import com.passbolt.mobile.android.ui.ResourceModel

fun interface SecretPropertiesActionsInteractorFactory {
    fun create(resource: ResourceModel): SecretPropertiesActionsInteractor
}
