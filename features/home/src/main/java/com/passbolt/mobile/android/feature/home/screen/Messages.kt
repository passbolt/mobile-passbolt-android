package com.passbolt.mobile.android.feature.home.screen

import android.content.Context
import androidx.annotation.DrawableRes
import com.passbolt.mobile.android.ui.Folder.Child
import com.passbolt.mobile.android.ui.Folder.Root
import com.passbolt.mobile.android.ui.HomeDisplayViewModel
import com.passbolt.mobile.android.ui.HomeDisplayViewModel.AllItems
import com.passbolt.mobile.android.ui.HomeDisplayViewModel.Expiry
import com.passbolt.mobile.android.ui.HomeDisplayViewModel.Favourites
import com.passbolt.mobile.android.ui.HomeDisplayViewModel.Folders
import com.passbolt.mobile.android.ui.HomeDisplayViewModel.Groups
import com.passbolt.mobile.android.ui.HomeDisplayViewModel.OwnedByMe
import com.passbolt.mobile.android.ui.HomeDisplayViewModel.RecentlyModified
import com.passbolt.mobile.android.ui.HomeDisplayViewModel.SharedWithMe
import com.passbolt.mobile.android.ui.HomeDisplayViewModel.Tags
import com.passbolt.mobile.android.core.localization.R as LocalizationR
import com.passbolt.mobile.android.core.ui.R as CoreUiR

@Suppress("CyclomaticComplexMethod")
internal fun getAppBarTitle(
    context: Context,
    state: HomeState,
): String =
    when (state.homeView) {
        is Folders ->
            when (state.homeView.activeFolder) {
                is Child -> state.homeView.activeFolderName.orEmpty()
                is Root -> context.getString(LocalizationR.string.filters_menu_folders)
            }
        is Groups ->
            if (state.homeView.activeGroupId == null) {
                context.getString(LocalizationR.string.filters_menu_groups)
            } else {
                state.homeView.activeGroupName.orEmpty()
            }
        is Tags ->
            if (state.homeView.activeTagId == null) {
                context.getString(LocalizationR.string.filters_menu_tags)
            } else {
                state.homeView.activeTagName.orEmpty()
            }
        AllItems -> context.getString(LocalizationR.string.filters_menu_all_items)
        Expiry -> context.getString(LocalizationR.string.filters_menu_expiry)
        Favourites -> context.getString(LocalizationR.string.filters_menu_favourites)
        OwnedByMe -> context.getString(LocalizationR.string.filters_menu_owned_by_me)
        RecentlyModified -> context.getString(LocalizationR.string.filters_menu_recently_modified)
        SharedWithMe -> context.getString(LocalizationR.string.filters_menu_shared_with_me)
        HomeDisplayViewModel.NotLoaded -> context.getString(LocalizationR.string.filters_menu_loading)
    }

@DrawableRes
internal fun getAppBarIconResId(state: HomeState): Int =
    when (state.homeView) {
        AllItems -> CoreUiR.drawable.ic_list
        Expiry -> CoreUiR.drawable.ic_calendar_clock
        Favourites -> CoreUiR.drawable.ic_star
        is Folders -> if (state.homeView.isActiveFolderShared == true) CoreUiR.drawable.ic_shared_folder else CoreUiR.drawable.ic_folder
        is Groups -> CoreUiR.drawable.ic_group
        OwnedByMe -> CoreUiR.drawable.ic_person
        RecentlyModified -> CoreUiR.drawable.ic_clock
        SharedWithMe -> CoreUiR.drawable.ic_share
        is Tags -> if (state.homeView.isActiveTagShared == true) CoreUiR.drawable.ic_shared_tag else CoreUiR.drawable.ic_tag
        HomeDisplayViewModel.NotLoaded -> CoreUiR.drawable.ic_password_generate
    }
