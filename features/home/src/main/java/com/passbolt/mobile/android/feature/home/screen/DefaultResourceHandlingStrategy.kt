package com.passbolt.mobile.android.feature.home.screen

import com.passbolt.mobile.android.core.navigation.AppContext
import com.passbolt.mobile.android.core.navigation.compose.AppNavigator
import com.passbolt.mobile.android.core.navigation.compose.keys.ResourceDetailsNavigationKey.ResourceDetails
import com.passbolt.mobile.android.ui.ResourceModel

class DefaultResourceHandlingStrategy(
    private val navigator: AppNavigator,
) : ResourceHandlingStrategy {
    override val appContext: AppContext = AppContext.APP

    override fun resourceItemClick(resourceModel: ResourceModel) {
        navigator.navigateToKey(ResourceDetails(resourceModel))
    }

    override fun shouldShowResourceMoreMenu() = true

    override fun shouldShowCloseButton() = false

    override fun showSuggestedModel() = ShowSuggestedModel.DoNotShow

    override fun resourcePostCreateAction(resourceId: String) {
        // no-op
    }

    override fun shouldShowFolderMoreMenu() = true
}
