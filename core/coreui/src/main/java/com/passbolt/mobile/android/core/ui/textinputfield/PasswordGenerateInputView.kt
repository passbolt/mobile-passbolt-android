package com.passbolt.mobile.android.core.ui.textinputfield

import android.content.Context
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
import com.passbolt.mobile.android.common.px
import com.passbolt.mobile.android.core.ui.R
import com.passbolt.mobile.android.core.ui.databinding.ViewPasswordGeneratorInputBinding

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
open class PasswordGenerateInputView @JvmOverloads constructor(
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

    protected val binding = ViewPasswordGeneratorInputBinding.inflate(LayoutInflater.from(context), this)

    init {
        parseAttributes(attrs)
        setInitialState()
        setFocusChangeListener()
        setPasswordStrength(PasswordStrength.Empty)
        setPasswordGenerateSize()
    }

    override fun onDetachedFromWindow() {
        binding.input.removeTextChangedListener(textWatcher)
        super.onDetachedFromWindow()
    }

    private fun setPasswordGenerateSize() {
        binding.passwordGenerate.post {
            binding.passwordGenerate.layoutParams.height = binding.textLayout.measuredHeight - GENERATE_PASSWORD_PADDING
        }
    }

    fun setPasswordStrength(passwordStrength: PasswordStrength) = with(binding) {
        progressBar.progress = passwordStrength.progress
        strengthDescription.setText(passwordStrength.text)
        strengthDescription.setTextColor(ContextCompat.getColor(context, passwordStrength.textColor))
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
        is StatefulInput.State.Initial -> setInitialState()
        is StatefulInput.State.Error -> setErrorState(state.message)
    }

    fun setIsEmptyListener(textChange: (Boolean) -> Unit) {
        textWatcher = binding.input.addTextChangedListener {
            textChange.invoke(it.isNullOrEmpty())
            setInitialState()
        }
    }

    private fun setFocusChangeListener() {
        binding.textLayout.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) setState(StatefulInput.State.Initial)
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

    fun getInputBytes(): ByteArray =
        binding.input.text?.toString()?.toByteArray() ?: byteArrayOf()

    @Suppress("MagicNumber")
    sealed class PasswordStrength(
        val progress: Int,
        @StringRes val text: Int,
        @ColorRes val textColor: Int = R.color.text_primary
    ) {
        object Empty : PasswordStrength(0, R.string.password_strength_empty, R.color.text_tertiary)
        object VeryWeak : PasswordStrength(1, R.string.password_strength_very_weak)
        object Weak : PasswordStrength(60, R.string.password_strength_weak)
        object Fair : PasswordStrength(80, R.string.password_strength_fair)
        object Strong : PasswordStrength(112, R.string.password_strength_strong)
        object VeryStrong : PasswordStrength(128, R.string.password_strength_very_strong)
    }

    private companion object {
        private const val DEFAULT_REQUIRED_STATE = false
        private const val REQUIRED_TITLE_FORMAT = "%s *"
        private val GENERATE_PASSWORD_PADDING = 6.px
    }
}
