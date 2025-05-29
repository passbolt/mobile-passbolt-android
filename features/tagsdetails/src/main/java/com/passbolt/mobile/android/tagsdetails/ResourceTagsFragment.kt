package com.passbolt.mobile.android.tagsdetails

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import com.mikepenz.fastadapter.FastAdapter
import com.mikepenz.fastadapter.adapters.ItemAdapter
import com.mikepenz.fastadapter.diff.FastAdapterDiffUtil
import com.passbolt.mobile.android.core.extension.initDefaultToolbar
import com.passbolt.mobile.android.core.extension.showSnackbar
import com.passbolt.mobile.android.core.extension.visible
import com.passbolt.mobile.android.core.navigation.ActivityIntents
import com.passbolt.mobile.android.core.ui.initialsicon.InitialsIconGenerator
import com.passbolt.mobile.android.feature.authentication.BindingScopedAuthenticatedFragment
import com.passbolt.mobile.android.feature.tagsdetails.databinding.FragmentResourceTagsBinding
import com.passbolt.mobile.android.tagsdetails.tagsrecycler.TagItem
import com.passbolt.mobile.android.ui.TagModel
import org.koin.android.ext.android.inject
import org.koin.core.qualifier.named
import com.passbolt.mobile.android.core.localization.R as LocalizationR
import com.passbolt.mobile.android.core.ui.R as CoreUiR

class ResourceTagsFragment :
    BindingScopedAuthenticatedFragment<FragmentResourceTagsBinding, ResourceTagsContract.View>(
        FragmentResourceTagsBinding::inflate,
    ),
    ResourceTagsContract.View {
    override val presenter: ResourceTagsContract.Presenter by inject()
    private val args: ResourceTagsFragmentArgs by navArgs()
    private val initialsIconGenerator: InitialsIconGenerator by inject()
    private val tagsItemAdapter: ItemAdapter<TagItem> by inject(named(TAGS_ITEM_ADAPTER))
    private val fastAdapter: FastAdapter<TagItem> by inject(named(TAGS_ADAPTER))

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?,
    ) {
        super.onViewCreated(view, savedInstanceState)
        requiredBinding.swipeRefresh.isEnabled = false
        initDefaultToolbar(requiredBinding.toolbar)
        setUpTagsRecycler()
        presenter.attach(this)
        presenter.argsRetrieved(args.resourceId, args.mode)
    }

    override fun onDestroyView() {
        presenter.detach()
        super.onDestroyView()
    }

    override fun onResume() {
        super.onResume()
        presenter.resume(this)
    }

    override fun onPause() {
        presenter.pause()
        super.onPause()
    }

    private fun setUpTagsRecycler() {
        with(requiredBinding.tagsRecycler) {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = fastAdapter
        }
    }

    override fun displayTitle(name: String) {
        requiredBinding.name.text = name
    }

    override fun displayInitialsIcon(
        name: String,
        initials: String,
    ) {
        requiredBinding.icon.setImageDrawable(
            initialsIconGenerator.generate(name, initials),
        )
    }

    override fun showFavouriteStar() {
        requiredBinding.favouriteIcon.visible()
    }

    override fun showTags(tags: List<TagModel>) {
        FastAdapterDiffUtil.calculateDiff(tagsItemAdapter, tags.map { TagItem(it) })
        fastAdapter.notifyAdapterDataSetChanged()
    }

    override fun hideRefreshProgress() {
        requiredBinding.swipeRefresh.isRefreshing = false
    }

    override fun showRefreshProgress() {
        requiredBinding.swipeRefresh.isRefreshing = true
    }

    override fun showDataRefreshError() {
        showSnackbar(
            LocalizationR.string.common_data_refresh_error,
            backgroundColor = CoreUiR.color.red,
        )
    }

    override fun showContentNotAvailable() {
        Toast.makeText(requireContext(), LocalizationR.string.content_not_available, Toast.LENGTH_SHORT).show()
    }

    override fun navigateToHome() {
        requireActivity().startActivity(ActivityIntents.bringHome(requireContext()))
    }
}
