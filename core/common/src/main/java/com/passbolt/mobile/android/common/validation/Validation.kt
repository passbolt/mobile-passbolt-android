package com.passbolt.mobile.android.common.validation

fun validation(block: Validation.() -> Unit) = Validation().apply(block).run()

class Validation {
    private var onValid: () -> Unit = {}
    private var onInvalid: () -> Unit = {}
    private val validations = mutableListOf<ValueValidation<*>>()

    fun <T> of(
        value: T,
        block: ValueValidation<T>.() -> Unit,
    ) {
        validations.add(ValueValidation(value).apply(block))
    }

    fun onValid(block: () -> Unit) {
        onValid = block
    }

    fun onInvalid(block: () -> Unit) {
        onInvalid = block
    }

    fun run() {
        var allValid = true
        validations.forEach { validation ->
            val value = validation.value
            val ruleSets = validation.ruleSets
            val ruleSetValid = ruleSets.map { it.run(value) }.all { it }
            allValid = allValid and ruleSetValid
        }
        if (allValid) onValid() else onInvalid()
    }

    @Suppress("SpreadOperator", "DataClassContainsFunctions")
    data class ValueValidation<T>(
        val value: T,
        val ruleSets: MutableList<RuleSet<T>> = mutableListOf(),
    ) {
        fun withRules(
            vararg rules: Rule<T>,
            block: RuleSet<T>.() -> Unit = {},
        ) {
            val application = RuleSet(*rules).apply(block)
            ruleSets.add(application)
        }
    }

    class RuleSet<T>(
        private vararg val rules: Rule<T>,
        var onValid: () -> Unit = {},
        var onInvalid: () -> Unit = {},
    ) {
        fun onValid(action: () -> Unit) {
            this.onValid = action
        }

        fun onInvalid(action: () -> Unit) {
            this.onInvalid = action
        }

        internal fun run(value: Any?): Boolean {
            val valid = rules.map { it.condition(value as T) }.all { it }
            if (valid) onValid() else onInvalid()
            return valid
        }
    }
}
