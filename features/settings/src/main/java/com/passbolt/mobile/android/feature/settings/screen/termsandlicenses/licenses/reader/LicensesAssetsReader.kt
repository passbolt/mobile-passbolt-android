package com.passbolt.mobile.android.feature.settings.screen.termsandlicenses.licenses.reader

import android.content.res.AssetManager
import com.google.gson.Gson
import com.passbolt.mobile.android.core.mvp.coroutinecontext.CoroutineLaunchContext
import com.passbolt.mobile.android.ui.OpenSourceLicensesModel
import kotlinx.coroutines.withContext

internal class LicensesAssetsReader(
    private val assetsManager: AssetManager,
    private val gson: Gson,
    private val coroutineLaunchContext: CoroutineLaunchContext,
) : LicensesReader {
    override suspend fun getLicenses(): OpenSourceLicensesModel =
        withContext(coroutineLaunchContext.io) {
            val licensesJson =
                assetsManager
                    .open(LICENSES_ASSET)
                    .bufferedReader()
                    .readText()
            gson.fromJson(licensesJson, OpenSourceLicensesModel::class.java)
        }

    private companion object {
        private const val LICENSES_ASSET = "licenses.json"
    }
}
