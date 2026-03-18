package com.passbolt.mobile.android.feature.otp.scanotp

import PassboltTheme
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.ui.platform.ComposeView
import androidx.core.os.BundleCompat
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.setFragmentResult
import androidx.fragment.app.setFragmentResultListener
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import com.passbolt.mobile.android.core.navigation.deeplinks.NavDeepLinkProvider
import com.passbolt.mobile.android.feature.otp.scanotp.ScanOtpNavigationKey.Scanning
import com.passbolt.mobile.android.feature.otp.scanotp.ScanOtpNavigationKey.Success
import com.passbolt.mobile.android.feature.otp.scanotp.compose.ScanOtpNavigation
import com.passbolt.mobile.android.feature.otp.scanotp.compose.ScanOtpScreen
import com.passbolt.mobile.android.feature.otp.scanotp.scanotpsuccess.ScanOtpSuccessIntent.LinkedResourceReceived
import com.passbolt.mobile.android.feature.otp.scanotp.scanotpsuccess.ScanOtpSuccessScreen
import com.passbolt.mobile.android.feature.otp.scanotp.scanotpsuccess.ScanOtpSuccessViewModel
import com.passbolt.mobile.android.resourcepicker.ResourcePickerFragment
import com.passbolt.mobile.android.ui.OtpParseResult
import com.passbolt.mobile.android.ui.ResourceModel
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf

class ScanOtpFragment :
    Fragment(),
    ScanOtpNavigation {
    private val navArgs: ScanOtpFragmentArgs by navArgs()
    private lateinit var backstackList: NavBackStack<NavKey>
    private var scannedTotpQr: OtpParseResult.OtpQr.TotpQr? = null
    private var successViewModel: ScanOtpSuccessViewModel? = null

    private val linkedResourceReceivedListener = { _: String, result: Bundle ->
        if (result.containsKey(ResourcePickerFragment.RESULT_PICKED_ACTION) &&
            result.containsKey(ResourcePickerFragment.RESULT_PICKED_RESOURCE)
        ) {
            val resource =
                requireNotNull(
                    BundleCompat.getParcelable(
                        result,
                        ResourcePickerFragment.RESULT_PICKED_RESOURCE,
                        ResourceModel::class.java,
                    ),
                )
            successViewModel?.onIntent(LinkedResourceReceived(resource))
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View =
        ComposeView(requireContext()).apply {
            setContent {
                val backStack =
                    rememberNavBackStack(Scanning).apply {
                        backstackList = this
                    }

                NavDisplay(
                    backStack = backStack,
                    onBack = {
                        if (backStack.size > 1) {
                            backStack.removeLastOrNull()
                        } else {
                            findNavController().popBackStack()
                        }
                    },
                    entryDecorators =
                        listOf(
                            rememberSaveableStateHolderNavEntryDecorator(),
                            rememberViewModelStoreNavEntryDecorator(),
                        ),
                    entryProvider = { key ->
                        when (key) {
                            is Scanning ->
                                NavEntry(key) {
                                    PassboltTheme {
                                        ScanOtpScreen(
                                            mode = navArgs.scanOtpMode,
                                            navigation = this@ScanOtpFragment,
                                        )
                                    }
                                }
                            is Success ->
                                NavEntry(key) {
                                    val scannedTotp = requireNotNull(scannedTotpQr)
                                    val viewModel: ScanOtpSuccessViewModel =
                                        koinViewModel { parametersOf(scannedTotp, navArgs.parentFolderId) }
                                    successViewModel = viewModel
                                    PassboltTheme {
                                        ScanOtpSuccessScreen(
                                            scannedTotp = scannedTotp,
                                            parentFolderId = navArgs.parentFolderId,
                                            navigation = this@ScanOtpFragment,
                                            viewModel = viewModel,
                                        )
                                    }
                                }
                            else -> error("Unsupported scan otp key: $key")
                        }
                    },
                )
            }
        }

    override fun navigateBack() {
        findNavController().popBackStack()
    }

    override fun navigateToSuccess(totpQr: OtpParseResult.OtpQr.TotpQr) {
        scannedTotpQr = totpQr
        backstackList.add(Success)
    }

    override fun setResultAndNavigateBack(totpQr: OtpParseResult.OtpQr.TotpQr) {
        setFragmentResult(
            REQUEST_SCAN_OTP_FOR_RESULT,
            bundleOf(
                EXTRA_MANUAL_CREATION_CHOSEN to false,
                EXTRA_SCANNED_OTP to totpQr,
            ),
        )
        findNavController().popBackStack()
    }

    override fun setManualCreationResultAndNavigateBack() {
        setFragmentResult(
            REQUEST_SCAN_OTP_FOR_RESULT,
            bundleOf(EXTRA_MANUAL_CREATION_CHOSEN to true),
        )
        findNavController().popBackStack()
    }

    override fun navigateToOtpList(
        totp: OtpParseResult.OtpQr.TotpQr,
        otpCreated: Boolean,
        resourceId: String,
    ) {
        setFragmentResult(
            REQUEST_SCAN_OTP_FOR_RESULT,
            bundleOf(
                EXTRA_SCANNED_OTP to totp,
                EXTRA_OTP_CREATED to otpCreated,
                EXTRA_CREATED_OTP_ID to resourceId,
            ),
        )
        findNavController().popBackStack()
    }

    override fun navigateToResourcePicker(suggestedUri: String?) {
        setFragmentResultListener(
            ResourcePickerFragment.REQUEST_PICK_RESOURCE_FOR_RESULT,
            linkedResourceReceivedListener,
        )
        findNavController().navigate(
            NavDeepLinkProvider.resourceResourcePickerDeepLinkRequest(suggestedUri),
        )
    }

    companion object {
        const val REQUEST_SCAN_OTP_FOR_RESULT = "SCAN_OTP_FOR_RESULT"

        const val EXTRA_SCANNED_OTP = "SCANNED_OTP"
        const val EXTRA_MANUAL_CREATION_CHOSEN = "MANUAL_CREATION_CHOSEN"
        const val EXTRA_OTP_CREATED = "OTP_CREATED"
        const val EXTRA_CREATED_OTP_ID = "CREATED_OTP_ID"
    }
}
