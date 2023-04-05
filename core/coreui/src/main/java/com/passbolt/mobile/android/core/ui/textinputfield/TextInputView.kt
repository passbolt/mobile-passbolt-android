package com.passbolt.mobile.android.core.ui.textinputfield

import android.content.Context
import android.text.InputType
import android.text.Spannable
import android.text.SpannableString
import android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
import android.text.TextWatcher
import android.text.style.ForegroundColorSpan
import android.util.AttributeSet
import android.view.LayoutInflater
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.res.use
import androidx.core.widget.addTextChangedListener
import com.google.android.material.textfield.TextInputLayout
import com.passbolt.mobile.android.core.ui.R
import com.passbolt.mobile.android.core.ui.databinding.ViewTextInputBinding

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

open class TextInputView @JvmOverloads constructor(
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

    var text: String
        get() = binding.input.text?.toString().orEmpty()
        set(value) = binding.input.run { setText(value) }

    private val requiredTitle: Spannable
        get() = SpannableString(REQUIRED_TITLE_FORMAT.format(title)).apply {
            setSpan( // the asterisk at the end is red
                ForegroundColorSpan(context.getColor(R.color.red)),
                length - 1,
                length,
                SPAN_EXCLUSIVE_EXCLUSIVE
            )
        }

    private val uiTitle: Spannable
        get() = if (!isRequired) SpannableString(title) else requiredTitle

    private var textWatcher: TextWatcher? = null

    protected val binding = ViewTextInputBinding.inflate(LayoutInflater.from(context), this)

    init {
        parseAttributes(attrs)
        setInitialState()
        setFocusChangeListener()
    }

    override fun onDetachedFromWindow() {
        binding.input.removeTextChangedListener(textWatcher)
        super.onDetachedFromWindow()
    }

    open fun enableSecretInput() = with(binding.textLayout) {
        editText?.apply {
            isSingleLine = true
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
            isSaveEnabled = false
        }
        endIconMode = TextInputLayout.END_ICON_PASSWORD_TOGGLE
    }

    fun setDefaultHint(name: String) {
        hint = String.format(resources.getString(R.string.input_default_hint), name)
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

    fun setIsEmptyListener(textChange: (Boolean) -> Unit) {
        textWatcher = binding.input.addTextChangedListener {
            textChange.invoke(it.isNullOrEmpty())
            setInitialState()
        }
    }

    fun setTextChangeListener(textChange: (String) -> Unit) {
        textWatcher = binding.input.addTextChangedListener {
            textChange.invoke(it.toString())
            setInitialState()
        }
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

    fun setInputType(inputType: Int) {
        binding.input.inputType = inputType
    }

    fun getInputBytes(): ByteArray =
        binding.input.text?.toString()?.toByteArray() ?: byteArrayOf()

    fun setInputType(inputType: Int) {
        binding.input.inputType = inputType
    }

    private companion object {
        private const val DEFAULT_REQUIRED_STATE = false
        private const val REQUIRED_TITLE_FORMAT = "%s *"
    }
}
