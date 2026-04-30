package com.passbolt.mobile.android.core.ui.dialogs

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.passbolt.mobile.android.core.localization.R as LocalizationR

@Composable
fun PermissionDeleteAlertDialog(
    isVisible: Boolean,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    if (isVisible) {
        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text(stringResource(LocalizationR.string.are_you_sure)) },
            text = { Text(stringResource(LocalizationR.string.permission_deletion_dialog_message)) },
            confirmButton = {
                TextButton(onClick = onConfirm) {
                    Text(stringResource(LocalizationR.string.confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = onDismiss) {
                    Text(stringResource(LocalizationR.string.cancel))
                }
            },
        )
    }
}
