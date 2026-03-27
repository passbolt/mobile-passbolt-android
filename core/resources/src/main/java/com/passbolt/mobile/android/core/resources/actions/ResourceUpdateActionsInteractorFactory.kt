package com.passbolt.mobile.android.core.resources.actions

import com.passbolt.mobile.android.ui.ResourceModel

fun interface ResourceUpdateActionsInteractorFactory {
    fun create(resource: ResourceModel): ResourceUpdateActionsInteractor
}
