package com.passbolt.mobile.android.core.ui.textinputfield

import android.content.Context
import android.graphics.drawable.LayerDrawable
import android.text.Spannable
import android.text.SpannableString
import android.text.Spanned
import android.text.TextWatcher
import android.text.style.ForegroundColorSpan
import android.util.AttributeSet
import android.view.LayoutInflater
import androidx.annotation.ColorRes
import androidx.annotation.StringRes
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.core.content.res.use
import androidx.core.widget.addTextChangedListener
import com.passbolt.mobile.android.core.extension.px
import com.passbolt.mobile.android.core.extension.setDebouncingOnClick
import com.passbolt.mobile.android.core.ui.R
import com.passbolt.mobile.android.core.ui.databinding.ViewPasswordGeneratorInputBinding
import com.passbolt.mobile.android.ui.PasswordStrength
import com.passbolt.mobile.android.ui.PasswordStrength.Empty
import com.passbolt.mobile.android.ui.PasswordStrength.Fair
import com.passbolt.mobile.android.ui.PasswordStrength.Strong
import com.passbolt.mobile.android.ui.PasswordStrength.VeryStrong
import com.passbolt.mobile.android.ui.PasswordStrength.VeryWeak
import com.passbolt.mobile.android.ui.PasswordStrength.Weak
import com.passbolt.mobile.android.core.localization.R as LocalizationR

/**
 * Passbolt - Open source password manager for teams
 * Copyright (c) 2021 Passbolt SA
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU Affero General
 * Public License (AGPL) as published by the Free Software Foundation version 3.
 *
 * The name "Passbolt" is a registered trademark of Passbolt SA, and Passbolt SA hereby declines to grant a trademark
 * license to "Passbolt" pursuant to the GNU Affero General Public License version 3 Section 7(e), without a separate
 * agreement with Passbolt SA.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied
 * warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License along with this program. If not,
 * see GNU Affero General Public License v3 (http://www.gnu.org/licenses/agpl-3.0.html).
 *
 * @copyright Copyright (c) Passbolt SA (https://www.passbolt.com)
 * @license https://opensource.org/licenses/AGPL-3.0 AGPL License
 * @link https://www.passbolt.com Passbolt (tm)
 * @since v1.0
 */
class PasswordGenerateInputView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = 0
) : ConstraintLayout(context, attrs, defStyle), StatefulInput {

    var title: String = ""
        set(value) {
            field = value
            binding.titleLabel.text = uiTitle
        }

    var hint: String = ""
        set(value) {
            field = value
            binding.input.hint = hint
        }

    var isRequired: Boolean = DEFAULT_REQUIRED_STATE
        set(value) {
            field = value
            binding.titleLabel.text = uiTitle
        }

    private val requiredTitle: Spannable
        get() = SpannableString(REQUIRED_TITLE_FORMAT.format(title)).apply {
            setSpan( // the asterisk at the end is red
                ForegroundColorSpan(context.getColor(R.color.red)),
                length - 1,
                length,
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            )
        }

    private val uiTitle: Spannable
        get() = if (!isRequired) SpannableString(title) else requiredTitle

    private var textWatcher: TextWatcher? = null

    private val binding = ViewPasswordGeneratorInputBinding.inflate(LayoutInflater.from(context), this)

    init {
        parseAttributes(attrs)
        setInitialState()
        setFocusChangeListener()
        setPasswordStrength(Empty, passwordEntropy = 0.0)
        setPasswordGenerateSize()
    }

    override fun onDetachedFromWindow() {
        binding.input.removeTextChangedListener(textWatcher)
        super.onDetachedFromWindow()
    }

    private fun setPasswordGenerateSize() {
        binding.generatePasswordLayout.post {
            binding.generatePasswordLayout.layoutParams.height =
                binding.textLayout.measuredHeight - GENERATE_PASSWORD_PADDING
        }
    }

    fun setPasswordStrength(
        passwordStrength: PasswordStrength,
        passwordEntropy: Double
    ) = with(binding) {
        progressBar.progress = passwordStrength.progress
        strengthDescription.text =
            if (passwordEntropy > Double.NEGATIVE_INFINITY && passwordEntropy < Double.POSITIVE_INFINITY) {
                context.getString(
                    LocalizationR.string.password_generator_known_strength_format,
                    context.getString(getEntropyText(passwordStrength)),
                    passwordEntropy
                )
            } else {
                context.getString(LocalizationR.string.password_generator_unknown_strength_format)
            }
        strengthDescription.setTextColor(ContextCompat.getColor(context, getTextColor(passwordStrength)))

        val progressBarDrawable = progressBar.progressDrawable as LayerDrawable
        val progressDrawable = progressBarDrawable.getDrawable(PROGRESS_LAYER_INDEX)
        progressDrawable.setTint(ContextCompat.getColor(context, getProgressColor(passwordStrength)))
    }

    @ColorRes
    private fun getTextColor(passwordStrength: PasswordStrength) = when (passwordStrength) {
        Empty -> R.color.text_tertiary
        else -> R.color.text_primary
    }

    @ColorRes
    private fun getProgressColor(passwordStrength: PasswordStrength) = when (passwordStrength) {
        Empty -> android.R.color.transparent
        Fair, Strong -> R.color.orange
        VeryStrong -> R.color.green
        VeryWeak, Weak -> R.color.red
    }

    @StringRes
    private fun getEntropyText(passwordStrength: PasswordStrength) = when (passwordStrength) {
        Empty -> LocalizationR.string.password_strength_empty
        Fair -> LocalizationR.string.password_strength_fair
        Strong -> LocalizationR.string.password_strength_strong
        VeryStrong -> LocalizationR.string.password_strength_very_strong
        VeryWeak -> LocalizationR.string.password_strength_very_weak
        Weak -> LocalizationR.string.password_strength_weak
    }

    fun showPassword(
        password: String,
        passwordStrength: PasswordStrength,
        entropyBits: Double
    ) {
        binding.input.setText(password)
        setPasswordStrength(passwordStrength, entropyBits)
    }

    fun setGenerateClickListener(action: () -> Unit) {
        binding.generatePasswordLayout
            .setDebouncingOnClick { action.invoke() }
    }

    fun setPasswordChangeListener(textChange: (String) -> Unit) {
        textWatcher?.let { binding.input.removeTextChangedListener(it) }
        textWatcher = binding.input.addTextChangedListener {
            textChange.invoke(it.toString())
            setInitialState()
        }
    }

    private fun parseAttributes(attrs: AttributeSet?) {
        attrs?.let {
            context.obtainStyledAttributes(attrs, R.styleable.TextInputView, 0, 0).use {
                hint = it.getString(R.styleable.TextInputView_inputHint).orEmpty()
                title = it.getString(R.styleable.TextInputView_inputTitle).orEmpty()
                isRequired = it.getBoolean(R.styleable.TextInputView_inputIsRequired, DEFAULT_REQUIRED_STATE)
            }
        }
    }

    override fun setState(state: StatefulInput.State) = when (state) {
        is StatefulInput.State.Default -> setInitialState()
        is StatefulInput.State.Error -> setErrorState(state.message)
    }

    private fun setFocusChangeListener() {
        binding.textLayout.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) setState(StatefulInput.State.Default)
        }
    }

    private fun setErrorState(message: String) {
        with(binding) {
            titleLabel.setTextColor(context.getColor(R.color.red))
            textLayout.error = message
        }
    }

    private fun setInitialState() {
        with(binding) {
            titleLabel.setTextColor(context.getColor(R.color.text_primary))
            textLayout.error = ""
        }
    }

    private companion object {
        private const val DEFAULT_REQUIRED_STATE = false
        private const val REQUIRED_TITLE_FORMAT = "%s *"
        private const val PROGRESS_LAYER_INDEX = 1
        private val GENERATE_PASSWORD_PADDING = 6.px
    }
}
