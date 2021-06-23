package com.passbolt.mobile.android.feature.main.mainscreen

import android.os.Bundle
import androidx.navigation.ui.setupWithNavController
import com.passbolt.mobile.android.common.lifecycleawarelazy.lifecycleAwareLazy
import com.passbolt.mobile.android.core.extension.findNavHostFragment
import com.passbolt.mobile.android.core.mvp.scoped.BindingScopedActivity
import com.passbolt.mobile.android.feature.settings.R
import com.passbolt.mobile.android.feature.settings.databinding.ActivityMainBinding
import org.koin.android.ext.android.inject

class MainActivity : BindingScopedActivity<ActivityMainBinding>(ActivityMainBinding::inflate), MainContract.View {

    private val presenter: MainContract.Presenter by inject()
    private val bottomNavController by lifecycleAwareLazy {
        findNavHostFragment(binding.fragmentContainer.id).navController
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        presenter.attach(this)
        setupNavListeners()
    }

    override fun onDestroy() {
        presenter.detach()
        super.onDestroy()
    }

    private fun setupNavListeners() {
        binding.mainNavigation.setupWithNavController(bottomNavController)
    }

    override fun showFoldersMenu() {
        binding.mainNavigation.menu.findItem(R.id.foldersNav).isVisible = true
    }
}
