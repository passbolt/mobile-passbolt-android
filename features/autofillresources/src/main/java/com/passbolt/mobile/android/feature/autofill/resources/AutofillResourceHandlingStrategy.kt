package com.passbolt.mobile.android.feature.autofill.resources

import com.passbolt.mobile.android.core.navigation.AppContext
import com.passbolt.mobile.android.feature.home.screen.ResourceHandlingStrategy
import com.passbolt.mobile.android.feature.home.screen.ShowSuggestedModel
import com.passbolt.mobile.android.ui.ResourceModel

class AutofillResourceHandlingStrategy(
    private val autofillUri: String?,
    private val onItemClick: (ResourceModel) -> Unit,
    private val onResourceCreated: (String) -> Unit,
) : ResourceHandlingStrategy {
    override val appContext: AppContext = AppContext.AUTOFILL

    override fun resourceItemClick(resourceModel: ResourceModel) {
        onItemClick(resourceModel)
    }

    override fun shouldShowResourceMoreMenu() = false

    override fun shouldShowFolderMoreMenu() = false

    override fun shouldShowCloseButton() = true

    override fun showSuggestedModel() = autofillUri?.let { ShowSuggestedModel.Show(it) } ?: ShowSuggestedModel.DoNotShow

    override fun resourcePostCreateAction(resourceId: String) {
        onResourceCreated(resourceId)
    }
}
