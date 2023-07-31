/**
 * Passbolt - Open source password manager for teams
 * Copyright (c) 2021 Passbolt SA
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU Affero General
 * Public License (AGPL) as published by the Free Software Foundation version 3.
 *
 * The name "Passbolt" is a registered trademark of Passbolt SA, and Passbolt SA hereby declines to grant a trademark
 * license to "Passbolt" pursuant to the GNU Affero General Public License version 3 Section 7(e), without a separate
 * agreement with Passbolt SA.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied
 * warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License along with this program. If not,
 * see GNU Affero General Public License v3 (http://www.gnu.org/licenses/agpl-3.0.html).
 *
 * @copyright Copyright (c) Passbolt SA (https://www.passbolt.com)
 * @license https://opensource.org/licenses/AGPL-3.0 AGPL License
 * @link https://www.passbolt.com Passbolt (tm)
 * @since v1.0
 */

package com.passbolt.mobile.android.resourcepicker

import com.passbolt.mobile.android.core.fulldatarefresh.base.DataRefreshViewReactiveContract
import com.passbolt.mobile.android.resourcepicker.model.ConfirmationModel
import com.passbolt.mobile.android.resourcepicker.model.PickResourceAction
import com.passbolt.mobile.android.ui.ResourceModel
import com.passbolt.mobile.android.ui.SelectableResourceModelWrapper

interface ResourcePickerContract {

    interface View : DataRefreshViewReactiveContract.View {
        fun showResources(
            suggestedResources: List<SelectableResourceModelWrapper>,
            resourceList: List<SelectableResourceModelWrapper>
        )
        fun showEmptyState()
        fun hideEmptyState()
        fun hideSearchEndIcon()
        fun displaySearchClearEndIcon()
        fun showDataRefreshError()
        fun enableApplyButton()
        fun clearSearchInput()
        fun showConfirmation(confirmationModel: ConfirmationModel, pickAction: PickResourceAction)
        fun setResultAndNavigateBack(pickAction: PickResourceAction, resourceModel: ResourceModel)
    }

    interface Presenter : DataRefreshViewReactiveContract.Presenter<View> {
        fun searchTextChanged(text: String)
        fun searchClearClick()
        fun argsRetrieved(suggestionUri: String?)
        fun refreshSwipe()
        fun resourcePicked(selectableResourceModel: SelectableResourceModelWrapper, isSelected: Boolean)
        fun applyClick()
        fun otpLinkConfirmed(pickAction: PickResourceAction)
    }
}
