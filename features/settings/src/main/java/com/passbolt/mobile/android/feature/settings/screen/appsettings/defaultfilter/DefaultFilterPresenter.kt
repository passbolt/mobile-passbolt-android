package com.passbolt.mobile.android.feature.settings.screen.appsettings.defaultfilter

import com.passbolt.mobile.android.core.preferences.usecase.GetHomeDisplayViewPrefsUseCase
import com.passbolt.mobile.android.core.preferences.usecase.HomeDisplayViewPrefsValidator
import com.passbolt.mobile.android.core.preferences.usecase.UpdateHomeDisplayViewPrefsUseCase
import com.passbolt.mobile.android.ui.DefaultFilterModel

class DefaultFilterPresenter(
    private val updateHomeDisplayViewPrefsUseCase: UpdateHomeDisplayViewPrefsUseCase,
    private val homeDisplayViewPrefsValidator: HomeDisplayViewPrefsValidator,
    private val getHomeDisplayViewPrefsUseCase: GetHomeDisplayViewPrefsUseCase
) : DefaultFilterContract.Presenter {

    override var view: DefaultFilterContract.View? = null

    override fun argsRetrieved(selectedFilter: DefaultFilterModel) {
        view?.apply {
            showFiltersList(homeDisplayViewPrefsValidator.validatedDefaultFiltersList())
            selectFilterSilently(selectedFilter)
        }
    }

    override fun viewResume() {
        view?.selectFilterSilently(
            getHomeDisplayViewPrefsUseCase.execute(Unit).userSetHomeView
        )
    }

    override fun defaultFilterSelectionChanged(filterModel: DefaultFilterModel, isSelected: Boolean) {
        if (isSelected) {
            updateHomeDisplayViewPrefsUseCase.execute(
                UpdateHomeDisplayViewPrefsUseCase.Input(userSetHomeView = filterModel)
            )
        }
    }
}
