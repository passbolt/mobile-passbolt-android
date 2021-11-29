package com.passbolt.mobile.android.core.commonresource.moremenu

import com.passbolt.mobile.android.ui.ResourceMoreMenuModel

class ResourceMoreMenuPresenter : ResourceMoreMenuContract.Presenter {
    override var view: ResourceMoreMenuContract.View? = null

    override fun argsRetrieved(menuModel: ResourceMoreMenuModel) {
        view?.showTitle(menuModel.title)
        processEditAndDeleteButtons(menuModel)
    }

    private fun processEditAndDeleteButtons(menuModel: ResourceMoreMenuModel) {
        if (menuModel.canDelete || menuModel.canEdit) {
            view?.showSeparator()
        }

        if (menuModel.canDelete) {
            view?.showDeleteButton()
        }

        if (menuModel.canEdit) {
            view?.showEditButton()
        }
    }
}
