package com.passbolt.mobile.android.feature.resourceform.main

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
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.passbolt.mobile.android.core.navigation.compose.ResourceFormHostNavigation
import com.passbolt.mobile.android.core.navigation.compose.ResourceFormNavigation
import com.passbolt.mobile.android.feature.otp.scanotp.ScanOtpFragment
import com.passbolt.mobile.android.feature.otp.scanotp.ScanOtpMode
import com.passbolt.mobile.android.ui.OtpParseResult

// TODO MOB-3690: Remove fragment wrapper - use pure Compose navigation, eliminate findNavController/setFragmentResult
class ResourceFormFragment :
    Fragment(),
    ResourceFormHostNavigation {
    private val navArgs: ResourceFormFragmentArgs by navArgs()

    private var scanOtpResultCallback: ((Boolean, OtpParseResult.OtpQr.TotpQr?) -> Unit)? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View =
        ComposeView(requireContext()).apply {
            setContent {
                ResourceFormNavigation(
                    mode = navArgs.mode,
                    hostNavigation = this@ResourceFormFragment,
                )
            }
        }

    override fun onDestroyView() {
        scanOtpResultCallback = null
        super.onDestroyView()
    }

    override fun navigateBack() {
        findNavController().popBackStack()
    }

    override fun navigateBackWithCreateSuccess(
        name: String,
        resourceId: String,
    ) {
        setFragmentResult(
            REQUEST_RESOURCE_FORM,
            bundleOf(
                EXTRA_RESOURCE_CREATED to true,
                EXTRA_RESOURCE_NAME to name,
                EXTRA_CREATED_RESOURCE_ID to resourceId,
            ),
        )
        findNavController().popBackStack()
    }

    override fun navigateBackWithEditSuccess(name: String) {
        setFragmentResult(
            REQUEST_RESOURCE_FORM,
            bundleOf(
                EXTRA_RESOURCE_EDITED to true,
                EXTRA_RESOURCE_NAME to name,
            ),
        )
        findNavController().popBackStack()
    }

    override fun navigateToScanOtp(resultCallback: (Boolean, OtpParseResult.OtpQr.TotpQr?) -> Unit) {
        scanOtpResultCallback = resultCallback
        setFragmentResultListener(ScanOtpFragment.REQUEST_SCAN_OTP_FOR_RESULT) { _, result ->
            scanOtpResultCallback?.invoke(
                result.getBoolean(ScanOtpFragment.EXTRA_MANUAL_CREATION_CHOSEN),
                BundleCompat.getParcelable(
                    result,
                    ScanOtpFragment.EXTRA_SCANNED_OTP,
                    OtpParseResult.OtpQr.TotpQr::class.java,
                ),
            )
            scanOtpResultCallback = null
        }
        findNavController().navigate(
            ResourceFormFragmentDirections.actionResourceFormFragmentToScanOtp(ScanOtpMode.SCAN_FOR_RESULT),
        )
    }

    companion object {
        const val REQUEST_RESOURCE_FORM = "RESOURCE_FORM"

        const val EXTRA_RESOURCE_CREATED = "RESOURCE_CREATED"
        const val EXTRA_CREATED_RESOURCE_ID = "CREATED_RESOURCE_ID"
        const val EXTRA_RESOURCE_EDITED = "RESOURCE_EDITED"
        const val EXTRA_RESOURCE_NAME = "RESOURCE_NAME"
    }
}
