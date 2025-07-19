package com.passbolt.mobile.android.rules

import androidx.test.espresso.IdlingRegistry
import androidx.test.espresso.IdlingResource
import org.junit.rules.TestRule
import org.junit.runner.Description
import org.junit.runners.model.Statement
import org.koin.core.component.KoinComponent

/**
 * Rule for using during instrumented tests that instructs Espresso to register and unregister provided
 * idling resources.
 */
open class IdlingResourceRule(
    private val idlingResources: Array<IdlingResource>,
) : TestRule,
    KoinComponent {
    override fun apply(
        base: Statement,
        description: Description,
    ) = object : Statement() {
        override fun evaluate() {
            IdlingRegistry.getInstance().register(*idlingResources)
            base.evaluate()
            IdlingRegistry.getInstance().unregister(*idlingResources)
        }
    }
}
