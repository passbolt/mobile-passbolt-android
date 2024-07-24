package com.passbolt.mobile.android.locationdetails.recyclerview

import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.core.view.updatePadding
import androidx.recyclerview.widget.RecyclerView
import com.mikepenz.fastadapter.ISubItem
import com.mikepenz.fastadapter.expandable.ExpandableExtension
import com.mikepenz.fastadapter.expandable.items.AbstractExpandableItem
import com.passbolt.mobile.android.feature.locationdetails.R
import com.passbolt.mobile.android.ui.FolderModel
import com.passbolt.mobile.android.core.ui.R as CoreUiR

class ExpandableFolderItem(
    private val folderModel: FolderModel,
    private val basePaddingMultiplier: Int
) : AbstractExpandableItem<ExpandableFolderItem.ViewHolder>(),
    ISubItem<ExpandableFolderItem.ViewHolder> {

    override val type: Int
        get() = R.id.itemExpandableFolder

    override val layoutRes: Int
        get() = R.layout.item_expandable_folder

    override fun bindView(holder: ViewHolder, payloads: List<Any>) {
        super.bindView(holder, payloads)
        val p = payloads.mapNotNull { it as? String }.lastOrNull()
        if (p == ExpandableExtension.PAYLOAD_EXPAND) {
            holder.expandIcon.animate().rotation(ROTATION_EXPANDED).start()
            return
        } else if (p == ExpandableExtension.PAYLOAD_COLLAPSE) {
            holder.expandIcon.animate().rotation(ROTATION_COLLAPSED).start()
            return
        }

        with(holder) {
            view.clearAnimation()
            name.text = folderModel.name
            icon.setImageResource(
                if (folderModel.isShared) {
                    CoreUiR.drawable.ic_filled_shared_folder_with_bg
                } else {
                    CoreUiR.drawable.ic_filled_folder_with_bg
                }
            )
            expandIcon.apply {
                visibility = if (subItems.isEmpty()) View.INVISIBLE else View.VISIBLE
                rotation = if (isExpanded) ROTATION_EXPANDED else ROTATION_COLLAPSED
            }
            view.updatePadding(
                left = itemView.context.resources.getDimension(CoreUiR.dimen.dp_8).toInt() * basePaddingMultiplier
            )
        }
    }

    override fun unbindView(holder: ViewHolder) {
        super.unbindView(holder)
        holder.name.text = null
        holder.icon.clearAnimation()
    }

    override fun getViewHolder(v: View) = ViewHolder(v)

    class ViewHolder(val view: View) : RecyclerView.ViewHolder(view) {
        var name: TextView = view.findViewById(R.id.name)
        var icon: ImageView = view.findViewById(R.id.icon)
        var expandIcon: ImageView = view.findViewById(R.id.expandIcon)
    }

    private companion object {
        private const val ROTATION_EXPANDED = 0f
        private const val ROTATION_COLLAPSED = -90f
    }
}
