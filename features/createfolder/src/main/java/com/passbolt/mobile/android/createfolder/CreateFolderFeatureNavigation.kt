package com.passbolt.mobile.android.createfolder

import PassboltTheme
import com.passbolt.mobile.android.core.navigation.compose.AppNavigator
import com.passbolt.mobile.android.core.navigation.compose.base.EntryProviderInstaller
import com.passbolt.mobile.android.core.navigation.compose.base.FeatureModuleNavigation
import com.passbolt.mobile.android.core.navigation.compose.keys.CreateFolderNavigationKey.CreateFolder
import com.passbolt.mobile.android.core.navigation.compose.results.CreateFolderCompleteResult
import com.passbolt.mobile.android.core.navigation.compose.results.NavigationResultEventBus
import com.passbolt.mobile.android.core.navigation.compose.results.ResultEventBus
import org.koin.compose.koinInject

class CreateFolderFeatureNavigation : FeatureModuleNavigation {
    override fun provideEntryProviderInstaller(): EntryProviderInstaller =
        {
            entry<CreateFolder> { key ->
                val navigator: AppNavigator = koinInject()
                val resultBus = NavigationResultEventBus.current

                PassboltTheme {
                    CreateFolderScreen(
                        parentFolderId = key.parentFolderId,
                        navigation = CreateFolderNavigator(navigator, resultBus),
                    )
                }
            }
        }
}

private class CreateFolderNavigator(
    private val navigator: AppNavigator,
    private val resultBus: ResultEventBus,
) : CreateFolderNavigation {
    override fun navigateUp() {
        navigator.navigateBack()
    }

    override fun folderCreated(folderName: String) {
        resultBus.sendResult(
            result = CreateFolderCompleteResult(folderName),
        )
        navigator.navigateBack()
    }
}
