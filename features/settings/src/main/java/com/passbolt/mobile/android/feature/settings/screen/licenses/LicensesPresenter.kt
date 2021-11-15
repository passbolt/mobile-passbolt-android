package com.passbolt.mobile.android.feature.settings.screen.licenses

import com.google.gson.Gson
import com.passbolt.mobile.android.ui.OpenSourceLicensesModel

class LicensesPresenter(
    private val gson: Gson
) : LicensesContract.Presenter {

    override var view: LicensesContract.View? = null

    override fun argsRetrieved(licensesJson: String) {
        val licensesData = gson.fromJson(licensesJson, OpenSourceLicensesModel::class.java)
        view?.showLicenses(licensesData)
    }
}
