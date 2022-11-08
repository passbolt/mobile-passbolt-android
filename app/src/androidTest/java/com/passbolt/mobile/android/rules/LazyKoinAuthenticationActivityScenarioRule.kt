package com.passbolt.mobile.android.rules

import android.app.Activity
import android.content.Intent
import androidx.test.core.app.ActivityScenario
import com.passbolt.mobile.android.accountinit.AccountInitializer
import com.passbolt.mobile.android.initializers.KoinInitializer
import org.junit.rules.ExternalResource
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.koin.core.context.loadKoinModules
import org.koin.core.context.unloadKoinModules
import org.koin.core.module.Module

/**
 * Allows to launch an activity lazy so that setup can be done before the activity is opened.
 * That way Koin module overrides can be injected on time (there is no way to do it inside
 * Koin Application as the instrumented applications has its own Koin Application.
 */
class LazyKoinAuthenticationActivityScenarioRule<A : Activity>(
    private val launchActivity: Boolean,
    koinOverrideModules: List<Module>? = null,
    startActivityIntentSupplier: () -> Intent
) : ExternalResource(), KoinComponent {

    private var scenarioSupplier: () -> ActivityScenario<A>
    private var scenario: ActivityScenario<A>? = null
    private var scenarioLaunched: Boolean = false
    private var overrideModules: List<Module>? = koinOverrideModules
    private val accountInitializer: AccountInitializer by inject()

    init {
        scenarioSupplier = { ActivityScenario.launch(startActivityIntentSupplier()) }
    }

    override fun before() {
        overrideModules?.let { loadKoinModules(it) }
        accountInitializer.initializeAccount()
        if (launchActivity) {
            launchActivity()
        }
    }

    override fun after() {
        scenario?.close()
        overrideModules?.let {
            unloadKoinModules(it)
            loadKoinModules(KoinInitializer.appModules)
        }
    }

    private fun launchActivity(newIntent: Intent? = null) {
        if (scenarioLaunched) throw IllegalStateException("Scenario has already been launched!")

        newIntent?.let { scenarioSupplier = { ActivityScenario.launch(it) } }

        scenario = scenarioSupplier()
        scenarioLaunched = true
    }
}

inline fun <reified A : Activity> lazyActivitySetupScenarioRule(
    launchActivity: Boolean = true,
    koinOverrideModules: List<Module>? = null,
    noinline intentSupplier: () -> Intent,
): LazyKoinAuthenticationActivityScenarioRule<A> =
    LazyKoinAuthenticationActivityScenarioRule(launchActivity, koinOverrideModules, intentSupplier)
