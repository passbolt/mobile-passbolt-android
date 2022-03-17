package com.passbolt.mobile.android.feature.main.mainscreen

import android.os.Bundle
import androidx.navigation.ui.setupWithNavController
import com.passbolt.mobile.android.common.lifecycleawarelazy.lifecycleAwareLazy
import com.passbolt.mobile.android.core.extension.findNavHostFragment
import com.passbolt.mobile.android.core.mvp.scoped.BindingScopedActivity
import com.passbolt.mobile.android.feature.home.screen.DataRefreshStatus
import com.passbolt.mobile.android.feature.home.screen.HomeDataRefreshExecutor
import com.passbolt.mobile.android.feature.main.databinding.ActivityMainBinding
import kotlinx.coroutines.flow.Flow
import org.koin.android.ext.android.inject

class MainActivity : BindingScopedActivity<ActivityMainBinding>(ActivityMainBinding::inflate), HomeDataRefreshExecutor {

    private val presenter: MainContract.Presenter by inject()

    private val bottomNavController by lifecycleAwareLazy {
        findNavHostFragment(binding.fragmentContainer.id).navController
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding.mainNavigation.setupWithNavController(bottomNavController)
        presenter.performFullDataRefresh()
    }

    override fun performFullDataRefresh(): Flow<DataRefreshStatus.Finished> =
        presenter.performFullDataRefresh()

    override fun supplyFullDataRefreshStatusFlow(): Flow<DataRefreshStatus.Finished> =
        presenter.dataRefreshFinishedStatusFlow
}
