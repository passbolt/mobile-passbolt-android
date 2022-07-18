package com.passbolt.mobile.android.feature.settings.screen.defaultfilter

import com.passbolt.mobile.android.storage.usecase.preferences.GetHomeDisaplyViewPrefsUseCase
import com.passbolt.mobile.android.storage.usecase.preferences.HomeDisplayViewPrefsValidator
import com.passbolt.mobile.android.storage.usecase.preferences.UpdateHomeDisplayViewPrefsUseCase
import com.passbolt.mobile.android.ui.DefaultFilterModel

class DefaultFilterPresenter(
    private val updateHomeDisplayViewPrefsUseCase: UpdateHomeDisplayViewPrefsUseCase,
    private val homeDisplayViewPrefsValidator: HomeDisplayViewPrefsValidator,
    private val getHomeDisaplyViewPrefsUseCase: GetHomeDisaplyViewPrefsUseCase
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
            getHomeDisaplyViewPrefsUseCase.execute(Unit).userSetHomeView
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
