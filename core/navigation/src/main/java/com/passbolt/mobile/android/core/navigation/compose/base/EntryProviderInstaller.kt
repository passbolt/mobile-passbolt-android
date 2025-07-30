package com.passbolt.mobile.android.core.navigation.compose.base

import androidx.navigation3.runtime.EntryProviderBuilder
import androidx.navigation3.runtime.NavKey

typealias EntryProviderInstaller = EntryProviderBuilder<NavKey>.() -> Unit
