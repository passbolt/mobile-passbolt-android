package com.passbolt.mobile.android.feature.settings.screen.defaultfilter

import com.passbolt.mobile.android.storage.usecase.preferences.UpdateAccountPreferencesUseCase
import com.passbolt.mobile.android.ui.DefaultFilterModel

class DefaultFilterPresenter(
    private val updateAccountPreferencesUseCase: UpdateAccountPreferencesUseCase
) : DefaultFilterContract.Presenter {

    override var view: DefaultFilterContract.View? = null

    override fun argsRetrieved(selectedFilter: DefaultFilterModel) {
        view?.apply {
            showFiltersList(DefaultFilterModel.values())
            selectFilterSilently(selectedFilter)
        }
    }

    override fun defaultFilterSelectionChanged(filterModel: DefaultFilterModel, isSelected: Boolean) {
        if (isSelected) {
            updateAccountPreferencesUseCase.execute(
                UpdateAccountPreferencesUseCase.Input(userSetHomeView = filterModel)
            )
        }
    }
}
