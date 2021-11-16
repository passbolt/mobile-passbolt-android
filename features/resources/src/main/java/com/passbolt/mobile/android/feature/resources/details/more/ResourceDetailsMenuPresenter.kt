package com.passbolt.mobile.android.feature.resources.details.more

class ResourceDetailsMenuPresenter : ResourceDetailsMenuContract.Presenter {

    override var view: ResourceDetailsMenuContract.View? = null
    private lateinit var resourceMenuModel: ResourceDetailsMenuModel

    override fun argsRetrieved(menuModel: ResourceDetailsMenuModel) {
        resourceMenuModel = menuModel
        view?.showTitle(resourceMenuModel.title)
    }

    override fun closeClick() {
        view?.close()
    }
}
