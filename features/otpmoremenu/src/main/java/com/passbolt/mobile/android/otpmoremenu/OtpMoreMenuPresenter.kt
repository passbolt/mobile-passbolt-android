package com.passbolt.mobile.android.otpmoremenu

import com.passbolt.mobile.android.ui.OtpMoreMenuModel

class OtpMoreMenuPresenter : OtpMoreMenuContract.Presenter {

    override var view: OtpMoreMenuContract.View? = null

    override fun argsRetrieved(menuModel: OtpMoreMenuModel) {
        view?.showTitle(menuModel.title)
        processEditAndDeleteButtons(menuModel)
    }

    private fun processEditAndDeleteButtons(menuModel: OtpMoreMenuModel) {
        if (menuModel.canDelete || menuModel.canEdit) {
            view?.showSeparator()
        }

        if (menuModel.canEdit) {
            view?.showEditButton()
        }

        if (menuModel.canDelete) {
            view?.showDeleteButton()
        }

        if (menuModel.canShow) {
            view?.showShowOtpButton()
        }
    }
}
