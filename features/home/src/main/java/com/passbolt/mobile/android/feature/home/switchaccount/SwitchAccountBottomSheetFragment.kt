package com.passbolt.mobile.android.feature.home.switchaccount

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.BundleCompat
import androidx.core.os.bundleOf
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.mikepenz.fastadapter.FastAdapter
import com.mikepenz.fastadapter.GenericItem
import com.mikepenz.fastadapter.adapters.ModelAdapter
import com.mikepenz.fastadapter.diff.FastAdapterDiffUtil
import com.passbolt.mobile.android.common.dialogs.signOutAlertDialog
import com.passbolt.mobile.android.common.lifecycleawarelazy.lifecycleAwareLazy
import com.passbolt.mobile.android.core.extension.setDebouncingOnClick
import com.passbolt.mobile.android.core.navigation.ActivityIntents
import com.passbolt.mobile.android.core.navigation.AppContext
import com.passbolt.mobile.android.core.ui.progressdialog.hideProgressDialog
import com.passbolt.mobile.android.core.ui.progressdialog.showProgressDialog
import com.passbolt.mobile.android.core.ui.recyclerview.DrawableListDivider
import com.passbolt.mobile.android.feature.home.databinding.FragmentSwitchAccountBinding
import com.passbolt.mobile.android.feature.home.switchaccount.recycler.HeaderSeeDetailsClick
import com.passbolt.mobile.android.feature.home.switchaccount.recycler.HeaderSignOutClick
import com.passbolt.mobile.android.feature.home.switchaccount.recycler.ManageAccountsClick
import com.passbolt.mobile.android.feature.home.switchaccount.recycler.SwitchAccountClick
import com.passbolt.mobile.android.feature.home.switchaccount.recycler.SwitchAccountUiItemsMapper
import com.passbolt.mobile.android.ui.SwitchAccountUiModel
import org.koin.android.ext.android.inject
import org.koin.android.scope.AndroidScopeComponent
import org.koin.androidx.scope.fragmentScope
import org.koin.core.qualifier.named

class SwitchAccountBottomSheetFragment :
    BottomSheetDialogFragment(),
    AndroidScopeComponent,
    SwitchAccountContract.View {
    override val scope by fragmentScope(useParentActivityScope = false)
    private val presenter: SwitchAccountContract.Presenter by scope.inject()
    private lateinit var binding: FragmentSwitchAccountBinding
    private val modelAdapter: ModelAdapter<SwitchAccountUiModel, GenericItem> by inject()
    private val fastAdapter: FastAdapter<GenericItem> by inject(named<SwitchAccountUiModel>())
    private val listDivider: DrawableListDivider by inject()
    private var listener: Listener? = null
    private val switchAccountUiModelMapper: SwitchAccountUiItemsMapper by inject()
    private val bundledAppContext by lifecycleAwareLazy {
        requireNotNull(
            BundleCompat.getSerializable(requireArguments(), EXTRA_APP_CONTEXT, AppContext::class.java),
        )
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        binding = FragmentSwitchAccountBinding.inflate(inflater)
        return binding.root
    }

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?,
    ) {
        super.onViewCreated(view, savedInstanceState)
        setListeners()
        presenter.attach(this)
        presenter.argsRetrieved(bundledAppContext)
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        listener =
            when {
                parentFragment is Listener -> parentFragment as Listener
                activity is Listener -> activity as Listener
                else -> error("Parent must implement ${Listener::class.java.name}")
            }
    }

    override fun onResume() {
        super.onResume()
        presenter.viewResumed()
    }

    override fun onDetach() {
        listener = null
        super.onDetach()
    }

    override fun onDestroyView() {
        presenter.detach()
        super.onDestroyView()
    }

    private fun setListeners() {
        with(binding) {
            accountsRecycler.apply {
                layoutManager = LinearLayoutManager(requireContext())
                addItemDecoration(listDivider)
                adapter = fastAdapter
                fastAdapter.addEventHooks(
                    listOf(
                        HeaderSeeDetailsClick { presenter.seeDetailsClick() },
                        HeaderSignOutClick { presenter.signOutClick() },
                        ManageAccountsClick { listener?.switchAccountManageAccountClick() },
                        SwitchAccountClick { presenter.switchAccountClick(it) },
                    ),
                )
            }
            close.setDebouncingOnClick {
                dismiss()
            }
        }
    }

    override fun showAccountsList(accountsList: List<SwitchAccountUiModel>) {
        FastAdapterDiffUtil.calculateDiff(
            modelAdapter,
            accountsList.map { switchAccountUiModelMapper.mapModelToItem(it) },
        )
        fastAdapter.notifyAdapterDataSetChanged()
    }

    override fun showSignOutDialog() {
        signOutAlertDialog(requireContext()) { presenter.signOutConfirmed() }
            .show()
    }

    override fun navigateToStartup() {
        dismiss()
        requireActivity().finishAffinity()
        startActivity(
            ActivityIntents.authentication(
                requireContext(),
                ActivityIntents.AuthConfig.Startup,
            ),
        )
    }

    override fun navigateToSignInForAccount() {
        dismiss()
        listener?.switchAccountClick()
    }

    override fun showProgress() {
        showProgressDialog(childFragmentManager)
    }

    override fun hideProgress() {
        hideProgressDialog(childFragmentManager)
    }

    override fun navigateToAccountDetails() {
        startActivity(ActivityIntents.accountDetails(requireContext()))
    }

    interface Listener {
        fun switchAccountManageAccountClick()

        fun switchAccountClick()
    }

    companion object {
        private const val EXTRA_APP_CONTEXT = "APP_CONTEXT"

        fun newInstance(appContext: AppContext) =
            SwitchAccountBottomSheetFragment()
                .apply {
                    arguments =
                        bundleOf(
                            EXTRA_APP_CONTEXT to appContext,
                        )
                }
    }
}
