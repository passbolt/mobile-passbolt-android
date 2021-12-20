package com.passbolt.mobile.android.core.logger.logs.recycler

import android.view.LayoutInflater
import android.view.ViewGroup
import com.mikepenz.fastadapter.binding.AbstractBindingItem
import com.passbolt.mobile.android.logger.R
import com.passbolt.mobile.android.logger.databinding.ItemLogBinding

class LogItem(
    private val logLine: String
) : AbstractBindingItem<ItemLogBinding>() {

    override val type: Int
        get() = R.id.itemLog

    override fun createBinding(inflater: LayoutInflater, parent: ViewGroup?): ItemLogBinding {
        return ItemLogBinding.inflate(inflater, parent, false)
    }

    override fun bindView(binding: ItemLogBinding, payloads: List<Any>) {
        super.bindView(binding, payloads)
        with(binding) {
            itemLog.text = logLine
        }
    }
}
