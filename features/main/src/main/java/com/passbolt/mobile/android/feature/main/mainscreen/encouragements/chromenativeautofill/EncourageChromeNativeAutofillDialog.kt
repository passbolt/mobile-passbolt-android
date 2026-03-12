package com.passbolt.mobile.android.feature.main.mainscreen.encouragements.chromenativeautofill

import PassboltTheme
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.fromHtml
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.passbolt.mobile.android.core.ui.button.PrimaryButton
import com.passbolt.mobile.android.core.ui.circlestepsview.CircleStepIcon
import com.passbolt.mobile.android.core.ui.circlestepsview.CircleStepItemModel
import com.passbolt.mobile.android.core.ui.circlestepsview.CircleStepsView
import com.passbolt.mobile.android.core.localization.R as LocalizationR
import com.passbolt.mobile.android.core.ui.R as CoreUiR

@Composable
fun EncourageChromeNativeAutofillDialog(
    onGoToChromeSettings: () -> Unit,
    onClose: () -> Unit,
) {
    Dialog(
        onDismissRequest = onClose,
        properties =
            DialogProperties(
                dismissOnBackPress = true,
                dismissOnClickOutside = false,
                usePlatformDefaultWidth = false,
            ),
    ) {
        EncourageChromeNativeAutofillContent(
            onGoToChromeSettings = onGoToChromeSettings,
            onClose = onClose,
        )
    }
}

@Composable
private fun EncourageChromeNativeAutofillContent(
    onGoToChromeSettings: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val steps =
        stringArrayResource(LocalizationR.array.dialog_encourage_chrome_native_autofill_setup_steps)
            .mapIndexed { index, text ->
                CircleStepItemModel(
                    text = AnnotatedString.fromHtml(text),
                    icon = CHROME_NATIVE_AUTOFILL_SETUP_STEPS_ICONS.getOrNull(index),
                )
            }

    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background,
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp),
        ) {
            IconButton(
                onClick = onClose,
                modifier =
                    Modifier
                        .align(Alignment.End)
                        .padding(top = 16.dp),
            ) {
                Image(
                    painter = painterResource(CoreUiR.drawable.ic_close),
                    contentDescription = null,
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            Text(
                text = stringResource(LocalizationR.string.dialog_encourage_chrome_autofill_title),
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onBackground,
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(bottom = 48.dp),
                textAlign = TextAlign.Center,
            )

            CircleStepsView(
                steps = steps,
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(start = 16.dp),
            )

            Spacer(modifier = Modifier.weight(1f))

            PrimaryButton(
                text = stringResource(LocalizationR.string.dialog_encourage_chrome_autofill_go_to_settings),
                onClick = onGoToChromeSettings,
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(bottom = 24.dp),
            )

            TextButton(
                onClick = onClose,
                modifier =
                    Modifier
                        .align(Alignment.CenterHorizontally)
                        .padding(bottom = 32.dp),
            ) {
                Text(
                    text = stringResource(LocalizationR.string.common_maybe_later),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onBackground,
                )
            }
        }
    }
}

private val passboltWithBgIcon =
    CircleStepIcon.Content {
        Box(
            modifier =
                Modifier
                    .size(24.dp)
                    .background(Color.White, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Image(
                painter = painterResource(CoreUiR.drawable.ic_logo),
                contentDescription = null,
                modifier = Modifier.size(16.dp),
            )
        }
    }

private val CHROME_NATIVE_AUTOFILL_SETUP_STEPS_ICONS: List<CircleStepIcon> =
    listOf(
        passboltWithBgIcon,
        CircleStepIcon.Drawable(CoreUiR.drawable.ic_chrome),
        passboltWithBgIcon,
    )

@Preview(showBackground = true)
@Composable
private fun EncourageChromeNativeAutofillContentPreview() {
    PassboltTheme {
        EncourageChromeNativeAutofillContent(
            onGoToChromeSettings = {},
            onClose = {},
        )
    }
}
