package com.passbolt.mobile.android.feature.main.mainscreen

class MainPresenter : MainContract.Presenter {

    override var view: MainContract.View? = null

    override fun attach(view: MainContract.View) {
        super.attach(view)
        if (SHOW_FOLDERS_FEATURE_FLAG) {
            view.showFoldersMenu()
        }
    }

    private companion object {
        // TODO implement feature flag
        private const val SHOW_FOLDERS_FEATURE_FLAG = false
    }
}
