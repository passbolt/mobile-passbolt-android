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

package com.passbolt.mobile.android.core.ui.itemwithheaderandaction

import android.content.Context
import android.graphics.Typeface
import android.text.util.Linkify
import android.util.AttributeSet
import android.view.LayoutInflater
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.core.content.res.use
import com.passbolt.mobile.android.core.extension.gone
import com.passbolt.mobile.android.core.extension.setDebouncingOnClick
import com.passbolt.mobile.android.core.extension.visible
import com.passbolt.mobile.android.core.ui.R
import com.passbolt.mobile.android.core.ui.databinding.ViewItemWithTitleAndHeaderBinding

class ItemWithHeaderAndActionView
    @JvmOverloads
    constructor(
        context: Context,
        attrs: AttributeSet? = null,
        defStyle: Int = 0,
    ) : ConstraintLayout(context, attrs, defStyle) {
        var actionClickListener: (() -> Unit)? = null
        var textValue = ""
            set(value) {
                field = value
                binding.value.text = value
            }
        var actionIcon: ActionIcon = ActionIcon.NONE
            set(value) {
                field = value
                setupActionIcon(value)
            }
        var isValueSecret = false
            set(value) {
                field = value
                setupIsSecret(value)
            }

        private val binding = ViewItemWithTitleAndHeaderBinding.inflate(LayoutInflater.from(context), this)
        private val regularFont = ResourcesCompat.getFont(context, R.font.inter)
        private val secretFont = ResourcesCompat.getFont(context, R.font.inconsolata)

        init {
            parseAttributes(attrs)
            setupListeners()
        }

        private fun parseAttributes(attrs: AttributeSet?) {
            attrs?.let {
                context.obtainStyledAttributes(attrs, R.styleable.ItemWithHeaderAndActionView, 0, 0).use {
                    setupActionIcon(
                        ActionIcon.fromInt(
                            it.getInt(
                                R.styleable.ItemWithHeaderAndActionView_itemWithHeaderAndActionView_action,
                                ActionIcon.NONE.value,
                            ),
                        ),
                    )
                    setupAutoLink(
                        it.getBoolean(
                            R.styleable.ItemWithHeaderAndActionView_itemWithHeaderAndActionView_shouldUseWebLink,
                            false,
                        ),
                    )
                    setupIsSecret(
                        it.getBoolean(
                            R.styleable.ItemWithHeaderAndActionView_itemWithHeaderAndActionView_isValueSecret,
                            false,
                        ),
                    )
                    setupTitle(
                        it.getString(R.styleable.ItemWithHeaderAndActionView_itemWithHeaderAndActionView_title),
                    )
                }
            }
        }

        private fun setupTitle(title: String?) {
            binding.title.text = title.orEmpty()
        }

        private fun setupListeners() {
            binding.actionIcon.setDebouncingOnClick {
                actionClickListener?.invoke()
            }
        }

        private fun setupIsSecret(isTextSecret: Boolean) {
            val fontFamily = if (isTextSecret) secretFont else regularFont
            binding.value.typeface = Typeface.create(fontFamily, FONT_WEIGHT, false)
        }

        private fun setupActionIcon(actionIcon: ActionIcon) {
            binding.actionIcon.setImageDrawable(
                when (actionIcon) {
                    ActionIcon.NONE -> null
                    ActionIcon.VIEW -> ContextCompat.getDrawable(context, R.drawable.ic_eye_visible)
                    ActionIcon.COPY -> ContextCompat.getDrawable(context, R.drawable.ic_copy)
                    ActionIcon.HIDE -> ContextCompat.getDrawable(context, R.drawable.ic_eye_invisible)
                },
            )
        }

        private fun setupAutoLink(shouldUseAutoLink: Boolean) {
            if (shouldUseAutoLink) {
                Linkify.addLinks(binding.value, Linkify.WEB_URLS)
            }
        }

        fun setTextIsSelectable(isSelectable: Boolean) {
            binding.value.setTextIsSelectable(isSelectable)
        }

        fun conceal() {
            binding.conceal.visible()
        }

        fun show() {
            binding.conceal.gone()
        }

        private companion object {
            private const val FONT_WEIGHT = 400
        }
    }
