package com.passbolt.mobile.android.logs

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.core.content.FileProvider.getUriForFile
import androidx.recyclerview.widget.LinearLayoutManager
import com.mikepenz.fastadapter.FastAdapter
import com.mikepenz.fastadapter.GenericItem
import com.mikepenz.fastadapter.adapters.ItemAdapter
import com.passbolt.mobile.android.common.extension.setDebouncingOnClick
import com.passbolt.mobile.android.core.extension.initDefaultToolbar
import com.passbolt.mobile.android.core.logger.LogFilesManager
import com.passbolt.mobile.android.core.mvp.scoped.BindingScopedFragment
import com.passbolt.mobile.android.logs.databinding.FragmentLogsBinding
import com.passbolt.mobile.android.logs.recycler.LogItem
import org.koin.android.ext.android.inject
import org.koin.core.qualifier.named
import java.io.File

class LogsFragment : BindingScopedFragment<FragmentLogsBinding>(FragmentLogsBinding::inflate), LogsContract.View {

    private val presenter: LogsContract.Presenter by inject()
    private val modelAdapter: ItemAdapter<LogItem> by inject()
    private val fastAdapter: FastAdapter<GenericItem> by inject(named<LogItem>())
    private val logFileManager: LogFilesManager by inject()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setListeners()
        presenter.attach(this)
        presenter.argsRetrieved(logFileManager.logFilePath())
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

    override fun showShareSheet(logFile: File) {
        val contentUri = getUriForFile(
            requireContext(),
            "com.passbolt.mobile.android.core.logger.logsfileprovider",
            logFile
        )

        val sendIntent = Intent().apply {
            action = Intent.ACTION_SEND
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            type = LOGS_MIME_TYPE

            putExtra(Intent.EXTRA_STREAM, contentUri)
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
    }
}
