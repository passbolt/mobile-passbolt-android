package com.passbolt.mobile.android.feature.autofill.resources

import com.passbolt.mobile.android.core.navigation.AppContext
import com.passbolt.mobile.android.feature.home.screen.ResourceHandlingStrategy
import com.passbolt.mobile.android.feature.home.screen.ShowSuggestedModel
import com.passbolt.mobile.android.ui.ResourceModel

class AutofillResourceHandlingStrategy(
    private val presenter: AutofillResourcesContract.Presenter,
    private val autofillUri: String?,
) : ResourceHandlingStrategy {
    override val appContext = AppContext.AUTOFILL

    override fun resourceItemClick(resourceModel: ResourceModel) {
        presenter.itemClick(resourceModel)
    }

    override fun shouldShowResourceMoreMenu() = false

    override fun shouldShowFolderMoreMenu() = false

    override fun shouldShowCloseButton() = true

    override fun showSuggestedModel() =
        autofillUri?.let {
            ShowSuggestedModel.Show(it)
        } ?: ShowSuggestedModel.DoNotShow

    override fun resourcePostCreateAction(resourceId: String) {
        presenter.newResourceCreated(resourceId)
    }
}
