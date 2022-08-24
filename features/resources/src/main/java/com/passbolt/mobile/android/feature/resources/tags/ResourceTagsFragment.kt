package com.passbolt.mobile.android.feature.resources.tags

import android.os.Bundle
import android.view.View
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import com.mikepenz.fastadapter.FastAdapter
import com.mikepenz.fastadapter.adapters.ItemAdapter
import com.mikepenz.fastadapter.diff.FastAdapterDiffUtil
import com.passbolt.mobile.android.common.extension.visible
import com.passbolt.mobile.android.core.extension.initDefaultToolbar
import com.passbolt.mobile.android.core.ui.initialsicon.InitialsIconGenerator
import com.passbolt.mobile.android.feature.authentication.BindingScopedAuthenticatedFragment
import com.passbolt.mobile.android.feature.resources.databinding.FragmentResourceTagsBinding
import com.passbolt.mobile.android.feature.resources.tags.tagsrecycler.TagItem
import com.passbolt.mobile.android.ui.TagModel
import org.koin.android.ext.android.inject
import org.koin.core.qualifier.named

class ResourceTagsFragment :
    BindingScopedAuthenticatedFragment<FragmentResourceTagsBinding, ResourceTagsContract.View>(
        FragmentResourceTagsBinding::inflate
    ), ResourceTagsContract.View {

    override val presenter: ResourceTagsContract.Presenter by inject()
    private val args: ResourceTagsFragmentArgs by navArgs()
    private val initialsIconGenerator: InitialsIconGenerator by inject()
    private val tagsItemAdapter: ItemAdapter<TagItem> by inject(named(TAGS_ITEM_ADAPTER))
    private val fastAdapter: FastAdapter<TagItem> by inject(named(TAGS_ADAPTER))

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initDefaultToolbar(binding.toolbar)
        setUpTagsRecycler()
        presenter.attach(this)
        presenter.argsRetrieved(args.resourceId, args.mode)
    }

    private fun setUpTagsRecycler() {
        with(binding.tagsRecycler) {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = fastAdapter
        }
    }

    override fun displayTitle(name: String) {
        binding.name.text = name
    }

    override fun displayInitialsIcon(name: String, initials: String) {
        binding.icon.setImageDrawable(
            initialsIconGenerator.generate(name, initials)
        )
    }

    override fun showFavouriteStar() {
        binding.favouriteIcon.visible()
    }

    override fun showTags(tags: List<TagModel>) {
        FastAdapterDiffUtil.calculateDiff(tagsItemAdapter, tags.map { TagItem(it) })
        fastAdapter.notifyAdapterDataSetChanged()
    }
}