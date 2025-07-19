package com.passbolt.mobile.android.core.ui.compose.progressdialog

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.passbolt.mobile.android.core.ui.R

@Composable
fun ProgressDialog(isVisible: Boolean) {
    if (isVisible) {
        Dialog(onDismissRequest = { /* Disable dismiss */ }) {
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.surface,
            ) {
                Box(
                    modifier =
                        Modifier
                            .size(104.dp)
                            .padding(16.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator(color = colorResource(R.color.primary))
                }
            }
        }
    }
}
