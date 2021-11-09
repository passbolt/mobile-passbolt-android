package com.passbolt.mobile.android.core.commonresource.moremenu

import com.passbolt.mobile.android.ui.ResourceMoreMenuModel

class ResourceMoreMenuPresenter : ResourceMoreMenuContract.Presenter {
    override var view: ResourceMoreMenuContract.View? = null

    override fun argsRetrieved(menuModel: ResourceMoreMenuModel) {
        view?.showTitle(menuModel.title)

        if (menuModel.canDelete) {
            view?.showSeparator()
            view?.showDeleteButton()
        }
    }
}
