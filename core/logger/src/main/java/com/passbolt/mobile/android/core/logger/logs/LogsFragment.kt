package com.passbolt.mobile.android.core.logger.logs

import android.content.Intent
import android.content.pm.PackageInfo
import android.os.Build
import android.os.Bundle
import android.view.View
import androidx.recyclerview.widget.LinearLayoutManager
import com.mikepenz.fastadapter.FastAdapter
import com.mikepenz.fastadapter.GenericItem
import com.mikepenz.fastadapter.adapters.ItemAdapter
import com.passbolt.mobile.android.common.extension.setDebouncingOnClick
import com.passbolt.mobile.android.core.extension.initDefaultToolbar
import com.passbolt.mobile.android.core.logger.LogFilesManager
import com.passbolt.mobile.android.core.logger.LogFilesManager.Companion.LOG_DIR_NAME
import com.passbolt.mobile.android.core.logger.logs.recycler.LogItem
import com.passbolt.mobile.android.core.mvp.scoped.BindingScopedFragment
import com.passbolt.mobile.android.logger.R
import com.passbolt.mobile.android.logger.databinding.FragmentLogsBinding
import org.koin.android.ext.android.inject
import org.koin.core.qualifier.named
import java.io.File

class LogsFragment : BindingScopedFragment<FragmentLogsBinding>(FragmentLogsBinding::inflate), LogsContract.View {

    private val presenter: LogsContract.Presenter by inject()
    private val modelAdapter: ItemAdapter<LogItem> by inject()
    private val fastAdapter: FastAdapter<GenericItem> by inject(named<LogItem>())
    private val packageInfo: PackageInfo by inject()
    private val logFilePath: String
        get() {
            val logsDirectory = File(requireContext().filesDir, LOG_DIR_NAME)
            return File(logsDirectory, LogFilesManager.logFileName()).absolutePath
        }
    private val appName: String
        get() = "${packageInfo.versionName}-${packageInfo.longVersionCode}"

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setListeners()
        presenter.attach(this)
        presenter.argsRetrieved(logFilePath, deviceName, osName, appName)
    }

    private fun setListeners() {
        with(binding) {
            initDefaultToolbar(toolbar)
            with(logsRecycler) {
                itemAnimator = null
                layoutManager = LinearLayoutManager(requireContext())
                adapter = fastAdapter
            }
            shareButton.setDebouncingOnClick {
                presenter.shareClick()
            }
        }
    }

    override fun showLogs(logLines: List<String>) {
        modelAdapter.set(logLines.map { LogItem(it) })
    }

    override fun showShareSheet(logs: String) {
        val sendIntent = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_TEXT, logs)
            putExtra(Intent.EXTRA_EMAIL, getString(R.string.logs_share_email))
            putExtra(Intent.EXTRA_SUBJECT, getString(R.string.logs_share_email_title))
            type = LOGS_MIME_TYPE
        }

        startActivity(
            Intent.createChooser(
                sendIntent,
                getString(R.string.logs_share_title)
            )
        )
    }

    override fun scrollLogsToPosition(position: Int) {
        binding.logsRecycler.scrollToPosition(position)
    }

    private companion object {
        private const val LOGS_MIME_TYPE = "text/plain"
        private val deviceName = "${Build.MANUFACTURER} ${Build.MODEL}"
        private val osName = "${Build.VERSION.RELEASE} (${Build.VERSION.SDK_INT})"
    }
}
