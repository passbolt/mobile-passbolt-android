package com.passbolt.mobile.android.feature.main.mainscreen

import android.os.Bundle
import androidx.navigation.ui.setupWithNavController
import com.passbolt.mobile.android.common.lifecycleawarelazy.lifecycleAwareLazy
import com.passbolt.mobile.android.core.extension.findNavHostFragment
import com.passbolt.mobile.android.core.mvp.viewbinding.BindingActivity
import com.passbolt.mobile.android.feature.main.databinding.ActivityMainBinding

class MainActivity : BindingActivity<ActivityMainBinding>(ActivityMainBinding::inflate) {

    private val bottomNavController by lifecycleAwareLazy {
        findNavHostFragment(binding.fragmentContainer.id).navController
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding.mainNavigation.setupWithNavController(bottomNavController)
    }
}
