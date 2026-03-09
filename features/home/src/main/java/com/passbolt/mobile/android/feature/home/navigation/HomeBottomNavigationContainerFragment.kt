package com.passbolt.mobile.android.feature.home.navigation

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.ui.platform.ComposeView
import androidx.fragment.app.Fragment
import com.passbolt.mobile.android.core.navigation.compose.HomeNavigation
import com.passbolt.mobile.android.core.preferences.usecase.GetHomeDisplayViewPrefsUseCase
import com.passbolt.mobile.android.mappers.HomeDisplayViewMapper
import org.koin.android.ext.android.inject

class HomeBottomNavigationContainerFragment : Fragment() {
    private val filterPreferencesUseCase: GetHomeDisplayViewPrefsUseCase by inject()
    private val homeDisplayMapper: HomeDisplayViewMapper by inject()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        val filterPreferences = filterPreferencesUseCase.execute(Unit)
        val initialHomeDisplay =
            homeDisplayMapper.map(
                filterPreferences.userSetHomeView,
                filterPreferences.lastUsedHomeView,
            )

        return ComposeView(requireContext()).apply {
            setContent {
                HomeNavigation(initialHomeDisplay)
            }
        }
    }
}
