package com.passbolt.mobile.android.feature.home.screen

import com.passbolt.mobile.android.ui.HomeDisplayViewModel

interface HomeNavigation {
    val resourceHandlingStrategy: ResourceHandlingStrategy

    fun navigateToChild(homeView: HomeDisplayViewModel)

    fun navigateBack()

    fun navigateToRoot(homeView: HomeDisplayViewModel)
}
