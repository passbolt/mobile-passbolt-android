package com.passbolt.mobile.android.feature.home.screen.snackbar

import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.platform.LocalContext
import com.passbolt.mobile.android.core.localization.R as LocalizationR

@Composable
fun AutofillConflictSnackbarEffect(
    snackbarHostState: SnackbarHostState,
    isAutofillConflictDetected: Boolean,
    onActionClick: () -> Unit,
) {
    val context = LocalContext.current
    val currentOnActionClick by rememberUpdatedState(onActionClick)

    LaunchedEffect(isAutofillConflictDetected) {
        if (!isAutofillConflictDetected) {
            return@LaunchedEffect
        }

        val result =
            snackbarHostState.showSnackbar(
                message = context.getString(LocalizationR.string.autofill_conflict_message),
                actionLabel = context.getString(LocalizationR.string.settings),
                withDismissAction = false,
                duration = SnackbarDuration.Long,
            )

        if (result == SnackbarResult.ActionPerformed) {
            currentOnActionClick()
        }
    }
}
