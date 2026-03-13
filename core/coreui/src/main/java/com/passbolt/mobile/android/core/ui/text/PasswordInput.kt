package com.passbolt.mobile.android.core.ui.text

import PassboltTheme
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldColors
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.passbolt.mobile.android.core.ui.R
import com.passbolt.mobile.android.core.ui.extensions.optionalTestTag
import com.passbolt.mobile.android.core.ui.textinputfield.StatefulInput
import com.passbolt.mobile.android.core.ui.textinputfield.StatefulInput.State.Default
import com.passbolt.mobile.android.core.ui.textinputfield.StatefulInput.State.Error

@Composable
fun PasswordInput(
    title: String,
    text: String,
    onTextChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    hint: String = "",
    isRequired: Boolean = false,
    state: StatefulInput.State = Default,
    testTag: String? = null,
    colors: TextFieldColors =
        MaterialTheme.colorScheme.surfaceVariant.let {
            OutlinedTextFieldDefaults.colors(
                focusedContainerColor = it,
                unfocusedContainerColor = it,
                errorContainerColor = it,
            )
        },
) {
    var passwordVisible by rememberSaveable { mutableStateOf(false) }

    val titleColor = if (state is Error) colorResource(R.color.red) else MaterialTheme.colorScheme.onBackground
    val label =
        if (isRequired) {
            buildAnnotatedString {
                append("$title ")
                withStyle(SpanStyle(color = colorResource(R.color.red))) { append("*") }
            }
        } else {
            buildAnnotatedString { append(title) }
        }

    Column(modifier = modifier) {
        Text(text = label, color = titleColor, style = MaterialTheme.typography.labelMedium)
        Spacer(modifier = Modifier.height(4.dp))
        OutlinedTextField(
            value = text,
            onValueChange = onTextChange,
            placeholder = { Text(hint) },
            isError = state is Error,
            visualTransformation =
                if (passwordVisible) {
                    VisualTransformation.None
                } else {
                    PasswordVisualTransformation()
                },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            trailingIcon = {
                IconButton(
                    onClick = { passwordVisible = !passwordVisible },
                    modifier = Modifier.testTag(PasswordInputTestTags.VISIBILITY_TOGGLE),
                ) {
                    Icon(
                        painter =
                            painterResource(
                                if (passwordVisible) {
                                    R.drawable.ic_eye_invisible
                                } else {
                                    R.drawable.ic_eye_visible
                                },
                            ),
                        contentDescription = null,
                    )
                }
            },
            singleLine = true,
            modifier =
                Modifier
                    .fillMaxWidth()
                    .optionalTestTag(testTag),
            colors = colors,
        )
        if (state is Error) {
            Text(
                text = state.message,
                color = colorResource(R.color.red),
                style = MaterialTheme.typography.labelSmall,
            )
        }
    }
}

object PasswordInputTestTags {
    const val VISIBILITY_TOGGLE = "password_input_visibility_toggle"
}

@Preview(showBackground = true)
@Composable
private fun PasswordInputPreview() {
    PassboltTheme {
        PasswordInput(
            title = "Password",
            text = "p@ssb0lt!",
            onTextChange = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun PasswordInputEmptyPreview() {
    PassboltTheme {
        PasswordInput(
            title = "Password",
            text = "",
            hint = "Enter password",
            onTextChange = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun PasswordInputRequiredPreview() {
    PassboltTheme {
        PasswordInput(
            title = "Password",
            text = "secret",
            isRequired = true,
            onTextChange = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun PasswordInputErrorPreview() {
    PassboltTheme {
        PasswordInput(
            title = "Password",
            text = "",
            isRequired = true,
            state = Error("Password is required"),
            onTextChange = {},
        )
    }
}
