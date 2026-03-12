package com.passbolt.mobile.android.core.ui.dialogs

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.passbolt.mobile.android.core.localization.R

@Composable
fun CameraRequiredAlertDialog(
    isVisible: Boolean,
    onDismissRequest: () -> Unit,
) {
    if (isVisible) {
        AlertDialog(
            onDismissRequest = onDismissRequest,
            title = { Text(stringResource(R.string.transfer_details_camera_required_dialog_title)) },
            text = { Text(stringResource(R.string.transfer_details_camera_required_dialog_message)) },
            confirmButton = {
                TextButton(onClick = onDismissRequest) {
                    Text(stringResource(R.string.ok))
                }
            },
        )
    }
}
