package com.passbolt.mobile.android.feature.otp.screen.recycler

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import com.mikepenz.fastadapter.FastAdapter
import com.mikepenz.fastadapter.binding.AbstractBindingItem
import com.mikepenz.fastadapter.listeners.ClickEventHook
import com.passbolt.mobile.android.common.extension.isInFuture
import com.passbolt.mobile.android.core.extension.asBinding
import com.passbolt.mobile.android.core.mvp.coroutinecontext.CoroutineLaunchContext
import com.passbolt.mobile.android.core.resources.resourceicon.ResourceIconProvider
import com.passbolt.mobile.android.core.ui.controller.TotpViewController
import com.passbolt.mobile.android.core.ui.controller.TotpViewController.StateParameters
import com.passbolt.mobile.android.core.ui.controller.TotpViewController.TimeParameters
import com.passbolt.mobile.android.core.ui.controller.TotpViewController.ViewParameters
import com.passbolt.mobile.android.feature.otp.R
import com.passbolt.mobile.android.feature.otp.databinding.ItemOtpBinding
import com.passbolt.mobile.android.ui.OtpItemWrapper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import com.passbolt.mobile.android.core.localization.R as LocalizationR
import com.passbolt.mobile.android.core.ui.R as CoreUiR

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
class OtpItem(
    val otpModel: OtpItemWrapper,
) : AbstractBindingItem<ItemOtpBinding>(),
    KoinComponent {
    override val type: Int
        get() = R.id.itemOtp

    private val coroutineLaunchContext: CoroutineLaunchContext by inject()
    private val job = SupervisorJob()
    private val scope = CoroutineScope(job + coroutineLaunchContext.ui)
    private val resourceIconProvider: ResourceIconProvider by inject()
    private val totpViewController: TotpViewController by inject()

    override fun createBinding(
        inflater: LayoutInflater,
        parent: ViewGroup?,
    ): ItemOtpBinding = ItemOtpBinding.inflate(inflater, parent, false)

    override fun bindView(
        binding: ItemOtpBinding,
        payloads: List<Any>,
    ) {
        super.bindView(binding, payloads)
        with(binding) {
            setupTitleAndExpiry(this)
            scope.launch {
                icon.setImageDrawable(
                    resourceIconProvider.getResourceIcon(
                        binding.root.context,
                        otpModel.resource,
                    ),
                )
            }
            eye.isVisible = !otpModel.isVisible && !otpModel.isRefreshing
            totpViewController.updateView(
                ViewParameters(binding.progress, binding.otp, binding.generationInProgress),
                StateParameters(otpModel.isRefreshing, otpModel.isVisible, otpModel.otpValue),
                TimeParameters(otpModel.otpExpirySeconds, otpModel.remainingSecondsCounter),
            )
        }
    }

    override fun unbindView(binding: ItemOtpBinding) {
        scope.coroutineContext.cancelChildren()
        super.unbindView(binding)
    }

    private fun setupTitleAndExpiry(binding: ItemOtpBinding) {
        otpModel.resource.expiry.let { expiry ->
            if (expiry == null || expiry.isInFuture()) {
                binding.name.text = otpModel.resource.metadataJsonModel.name
                binding.indicatorIcon.setImageDrawable(null)
            } else {
                binding.name.text =
                    binding.root.context.getString(
                        LocalizationR.string.name_expired,
                        otpModel.resource.metadataJsonModel.name,
                    )
                binding.indicatorIcon.setImageDrawable(
                    ContextCompat.getDrawable(
                        binding.root.context,
                        CoreUiR.drawable.ic_excl_indicator,
                    ),
                )
            }
        }
    }

    class ItemClick(
        private val clickListener: (OtpItemWrapper) -> Unit,
    ) : ClickEventHook<OtpItem>() {
        override fun onBind(viewHolder: RecyclerView.ViewHolder): View? =
            viewHolder.asBinding<ItemOtpBinding> {
                it.itemOtp
            }

        override fun onClick(
            v: View,
            position: Int,
            fastAdapter: FastAdapter<OtpItem>,
            item: OtpItem,
        ) {
            clickListener.invoke(item.otpModel)
        }
    }

    class ItemMoreClick(
        private val clickListener: (OtpItemWrapper) -> Unit,
    ) : ClickEventHook<OtpItem>() {
        override fun onBind(viewHolder: RecyclerView.ViewHolder): View? =
            viewHolder.asBinding<ItemOtpBinding> {
                it.more
            }

        override fun onClick(
            v: View,
            position: Int,
            fastAdapter: FastAdapter<OtpItem>,
            item: OtpItem,
        ) {
            clickListener.invoke(item.otpModel)
        }
    }
}
