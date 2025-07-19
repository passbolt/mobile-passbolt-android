package com.passbolt.mobile.android.rules

import android.app.Activity
import android.content.Intent
import androidx.test.core.app.ActivityScenario
import com.passbolt.mobile.android.initializers.KoinInitializer
import org.junit.rules.ExternalResource
import org.koin.core.component.KoinComponent
import org.koin.core.context.loadKoinModules
import org.koin.core.context.unloadKoinModules
import org.koin.core.module.Module

/**
 * Allows to launch an activity lazy so that setup can be done before the activity is opened.
 * That way Koin module overrides can be injected on time (there is no way to do it inside
 * Koin Application as the instrumented applications has its own Koin Application.
 */
class LazyKoinActivityScenarioRule<A : Activity> :
    ExternalResource,
    KoinComponent {
    constructor(
        launchActivity: Boolean,
        koinOverrideModules: List<Module>? = null,
        startActivityIntentSupplier: () -> Intent,
    ) {
        this.launchActivity = launchActivity
        this.overrideModules = koinOverrideModules
        scenarioSupplier = { ActivityScenario.launch(startActivityIntentSupplier()) }
    }

    constructor(
        launchActivity: Boolean,
        startActivityIntent: Intent,
        koinOverrideModules: List<Module>? = null,
    ) {
        this.launchActivity = launchActivity
        this.overrideModules = koinOverrideModules
        scenarioSupplier = { ActivityScenario.launch(startActivityIntent) }
    }

    constructor(
        launchActivity: Boolean,
        startActivityClass: Class<A>,
        koinOverrideModules: List<Module>? = null,
    ) {
        this.launchActivity = launchActivity
        this.overrideModules = koinOverrideModules
        scenarioSupplier = { ActivityScenario.launch(startActivityClass) }
    }

    private val launchActivity: Boolean
    private var scenarioSupplier: () -> ActivityScenario<A>
    private var scenario: ActivityScenario<A>? = null
    private var scenarioLaunched: Boolean = false
    private var overrideModules: List<Module>? = null

    override fun before() {
        overrideModules?.let { loadKoinModules(it) }
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

inline fun <reified A : Activity> lazyActivityScenarioRule(
    launchActivity: Boolean = true,
    koinOverrideModules: List<Module>? = null,
    noinline intentSupplier: () -> Intent,
): LazyKoinActivityScenarioRule<A> = LazyKoinActivityScenarioRule(launchActivity, koinOverrideModules, intentSupplier)

inline fun <reified A : Activity> lazyActivityScenarioRule(
    launchActivity: Boolean = true,
    intent: Intent? = null,
    koinOverrideModules: List<Module>? = null,
): LazyKoinActivityScenarioRule<A> =
    if (intent == null) {
        LazyKoinActivityScenarioRule(launchActivity, A::class.java, koinOverrideModules)
    } else {
        LazyKoinActivityScenarioRule(launchActivity, intent, koinOverrideModules)
    }
