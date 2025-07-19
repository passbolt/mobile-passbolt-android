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

package com.passbolt.mobile.android.core.ui.dropdown

import android.content.Context
import android.text.Spannable
import android.text.SpannableString
import android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
import android.text.style.ForegroundColorSpan
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.LinearLayout
import androidx.core.content.res.use
import com.passbolt.mobile.android.core.ui.R
import com.passbolt.mobile.android.core.ui.databinding.ViewDropdownInputBinding
import com.passbolt.mobile.android.core.ui.textinputfield.StatefulInput

class DropDownInputView
    @JvmOverloads
    constructor(
        context: Context,
        attrs: AttributeSet? = null,
        defStyle: Int = 0,
    ) : LinearLayout(context, attrs, defStyle),
        StatefulInput {
        var title: String = ""
            set(value) {
                field = value
                binding.titleLabel.text = uiTitle
                with(binding.dropdownLayout) {
                    minWidth = binding.titleLabel.width
                    requestLayout()
                }
            }

        var isRequired: Boolean = DEFAULT_REQUIRED_STATE
            set(value) {
                field = value
                binding.titleLabel.text = uiTitle
            }

        var text: String
            get() =
                binding.input.text
                    ?.toString()
                    .orEmpty()
            set(value) = binding.input.run { setText(value) }

        var selectedItemChangedListener: ((String) -> Unit)? = null

        var items = emptyList<String>()
            set(value) {
                field = value
                with(dropdownAdapter) {
                    clear()
                    addAll(items)
                    notifyDataSetChanged()
                }
            }

        private val requiredTitle: Spannable
            get() =
                SpannableString(REQUIRED_TITLE_FORMAT.format(title)).apply {
                    setSpan( // the asterisk at the end is red
                        ForegroundColorSpan(context.getColor(R.color.red)),
                        length - 1,
                        length,
                        SPAN_EXCLUSIVE_EXCLUSIVE,
                    )
                }

        private val uiTitle: Spannable
            get() = if (!isRequired) SpannableString(title) else requiredTitle

        private val binding = ViewDropdownInputBinding.inflate(LayoutInflater.from(context), this)

        private val dropdownAdapter = ArrayAdapter<String>(context, R.layout.item_dropdown_input)

        init {
            orientation = VERTICAL
            parseAttributes(attrs)
            setInitialState()
            setFocusChangeListener()
        }

        override fun onAttachedToWindow() {
            super.onAttachedToWindow()
            (binding.dropdownLayout.editText as AutoCompleteTextView).apply {
                onItemClickListener =
                    AdapterView.OnItemClickListener { _, _, position, _ ->
                        dropdownAdapter.getItem(position)?.let {
                            selectedItemChangedListener?.invoke(it)
                        }
                    }
                setAdapter(dropdownAdapter)
            }
        }

        override fun onDetachedFromWindow() {
            (binding.dropdownLayout.editText as AutoCompleteTextView).apply {
                onItemClickListener = null
                setAdapter(null)
            }
            super.onDetachedFromWindow()
        }

        private fun parseAttributes(attrs: AttributeSet?) {
            attrs?.let {
                context.obtainStyledAttributes(attrs, R.styleable.DropDownInputView, 0, 0).use {
                    title = it.getString(R.styleable.DropDownInputView_dropdownTitle).orEmpty()
                    isRequired = it.getBoolean(R.styleable.DropDownInputView_dropdownIsRequired, DEFAULT_REQUIRED_STATE)
                }
            }
        }

        override fun setState(state: StatefulInput.State) =
            when (state) {
                is StatefulInput.State.Default -> setInitialState()
                is StatefulInput.State.Error -> setErrorState(state.message)
            }

        private fun setFocusChangeListener() {
            binding.dropdownLayout.setOnFocusChangeListener { _, hasFocus ->
                if (hasFocus) setState(StatefulInput.State.Default)
            }
        }

        private fun setErrorState(message: String) {
            with(binding) {
                titleLabel.setTextColor(context.getColor(R.color.red))
                dropdownLayout.error = message
            }
        }

        private fun setInitialState() {
            with(binding) {
                titleLabel.setTextColor(context.getColor(R.color.text_primary))
                dropdownLayout.error = ""
            }
        }

        fun setItem(item: String) {
            val itemIndex = items.indexOf(item)
            require(itemIndex != -1) { "Item not present inside set items." }
            binding.input.setText(items[itemIndex], false)
        }

        private companion object {
            private const val DEFAULT_REQUIRED_STATE = false
            private const val REQUIRED_TITLE_FORMAT = "%s *"
        }
    }
