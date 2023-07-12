package com.passbolt.mobile.android.otpmoremenu

import com.passbolt.mobile.android.ui.ResourceMoreMenuModel

class OtpMoreMenuPresenter : OtpMoreMenuContract.Presenter {

    override var view: OtpMoreMenuContract.View? = null

    override fun argsRetrieved(menuModel: ResourceMoreMenuModel) {
        view?.showTitle(menuModel.title)
        processEditAndDeleteButtons(menuModel)
    }

    private fun processEditAndDeleteButtons(menuModel: ResourceMoreMenuModel) {
        if (menuModel.canDelete || menuModel.canEdit) {
            view?.showSeparator()
        }

        if (menuModel.canEdit) {
            view?.showEditButton()
        }

        if (menuModel.canDelete) {
            view?.showDeleteButton()
        }
    }
}
