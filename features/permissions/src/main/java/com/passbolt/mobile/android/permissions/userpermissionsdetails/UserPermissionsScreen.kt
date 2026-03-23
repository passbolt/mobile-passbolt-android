package com.passbolt.mobile.android.permissions.userpermissionsdetails

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.passbolt.mobile.android.core.compose.SideEffectDispatcher
import com.passbolt.mobile.android.core.formatter.FingerprintFormatter
import com.passbolt.mobile.android.core.navigation.compose.AppNavigator
import com.passbolt.mobile.android.core.navigation.compose.results.NavigationResultEventBus
import com.passbolt.mobile.android.core.ui.button.PrimaryButton
import com.passbolt.mobile.android.core.ui.circularimage.CircularProfileImage
import com.passbolt.mobile.android.core.ui.dialogs.PermissionDeleteAlertDialog
import com.passbolt.mobile.android.core.ui.header.ItemWithHeader
import com.passbolt.mobile.android.core.ui.permissions.PermissionLabel
import com.passbolt.mobile.android.core.ui.permissions.PermissionSelector
import com.passbolt.mobile.android.core.ui.topbar.BackNavigationIcon
import com.passbolt.mobile.android.core.ui.topbar.TitleAppBar
import com.passbolt.mobile.android.permissions.navigation.UserPermissionDeletedResult
import com.passbolt.mobile.android.permissions.navigation.UserPermissionModifiedResult
import com.passbolt.mobile.android.permissions.userpermissionsdetails.UserPermissionsIntent.CancelPermissionDelete
import com.passbolt.mobile.android.permissions.userpermissionsdetails.UserPermissionsIntent.ConfirmPermissionDelete
import com.passbolt.mobile.android.permissions.userpermissionsdetails.UserPermissionsIntent.DeletePermission
import com.passbolt.mobile.android.permissions.userpermissionsdetails.UserPermissionsIntent.GoBack
import com.passbolt.mobile.android.permissions.userpermissionsdetails.UserPermissionsIntent.Save
import com.passbolt.mobile.android.permissions.userpermissionsdetails.UserPermissionsIntent.SelectPermission
import com.passbolt.mobile.android.permissions.userpermissionsdetails.UserPermissionsSideEffect.NavigateBack
import com.passbolt.mobile.android.permissions.userpermissionsdetails.UserPermissionsSideEffect.SetDeletePermissionResult
import com.passbolt.mobile.android.permissions.userpermissionsdetails.UserPermissionsSideEffect.SetUpdatedPermissionResult
import com.passbolt.mobile.android.ui.PermissionModelUi
import com.passbolt.mobile.android.ui.PermissionsMode
import com.passbolt.mobile.android.ui.UserModel
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.koinInject
import org.koin.core.parameter.parametersOf
import com.passbolt.mobile.android.core.localization.R as LocalizationR
import com.passbolt.mobile.android.core.ui.R as CoreUiR

@Composable
fun UserPermissionsScreen(
    permission: PermissionModelUi.UserPermissionModel,
    mode: PermissionsMode,
    modifier: Modifier = Modifier,
    viewModel: UserPermissionsViewModel = koinViewModel(parameters = { parametersOf(mode, permission) }),
    navigator: AppNavigator = koinInject(),
) {
    val state = viewModel.viewState.collectAsStateWithLifecycle()
    val resultBus = NavigationResultEventBus.current

    UserPermissionsScreen(
        state = state.value,
        onIntent = viewModel::onIntent,
        modifier = modifier,
    )

    SideEffectDispatcher(viewModel.sideEffect) { effect ->
        when (effect) {
            NavigateBack -> navigator.navigateBack()
            is SetUpdatedPermissionResult -> {
                resultBus.sendResult(result = UserPermissionModifiedResult(effect.permission))
                navigator.navigateBack()
            }
            is SetDeletePermissionResult -> {
                resultBus.sendResult(result = UserPermissionDeletedResult(effect.permission))
                navigator.navigateBack()
            }
        }
    }
}

@Composable
private fun UserPermissionsScreen(
    state: UserPermissionsState,
    onIntent: (UserPermissionsIntent) -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TitleAppBar(
                title = stringResource(LocalizationR.string.user_permission_permission),
                navigationIcon = { BackNavigationIcon(onBackClick = { onIntent(GoBack) }) },
            )
        },
    ) { paddingValues ->
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
        ) {
            Column(
                modifier =
                    Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Spacer(modifier = Modifier.height(32.dp))

                UserHeader(user = state.user)

                Spacer(modifier = Modifier.height(32.dp))

                ItemWithHeader(
                    headerText = stringResource(LocalizationR.string.user_permission_permission),
                    modifier = Modifier.padding(horizontal = 16.dp),
                ) {
                    Spacer(modifier = Modifier.height(24.dp))
                    if (state.isEditMode) {
                        PermissionSelector(
                            selectedPermission = state.permission.permission,
                            onPermissionSelect = { onIntent(SelectPermission(it)) },
                        )
                    } else {
                        PermissionLabel(permission = state.permission.permission)
                    }
                }
            }

            if (state.isEditMode) {
                // TODO migrate to bottom app bar after all from this module are compose
                SaveLayout(onIntent)
            }
        }

        PermissionDeleteAlertDialog(
            isVisible = state.isDeleteConfirmationVisible,
            onConfirm = { onIntent(ConfirmPermissionDelete) },
            onDismiss = { onIntent(CancelPermissionDelete) },
        )
    }
}

@Composable
private fun UserHeader(
    user: UserModel?,
    modifier: Modifier = Modifier,
    disabledUserAlpha: Float = 0.5f,
    fingerprintFormatter: FingerprintFormatter = koinInject(),
) {
    val isDisabled = user?.disabled == true
    val alpha = if (isDisabled) disabledUserAlpha else 1f

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier.fillMaxWidth(),
    ) {
        CircularProfileImage(
            imageUrl = user?.profile?.avatarUrl,
            width = 96.dp,
            height = 96.dp,
        )

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text =
                if (isDisabled) {
                    stringResource(LocalizationR.string.name_suspended, user.fullName)
                } else {
                    user?.fullName.orEmpty()
                },
            style = MaterialTheme.typography.headlineSmall,
            textAlign = TextAlign.Center,
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 32.dp)
                    .alpha(alpha),
        )

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = user?.userName.orEmpty(),
            style = MaterialTheme.typography.bodyMedium,
            color = colorResource(CoreUiR.color.text_secondary),
            modifier = Modifier.alpha(alpha),
        )

        Spacer(modifier = Modifier.height(24.dp))

        if (user?.gpgKey?.fingerprint != null) {
            Text(
                text =
                    fingerprintFormatter.formatWithRawFallback(
                        user.gpgKey.fingerprint,
                        appendMiddleSpacing = false,
                    ),
                style =
                    MaterialTheme.typography.bodyMedium.copy(
                        fontFamily = FontFamily(Font(CoreUiR.font.inconsolata)),
                        fontSize = 18.sp,
                    ),
                color = colorResource(CoreUiR.color.text_secondary),
            )
        }
    }
}

@Composable
private fun SaveLayout(onIntent: (UserPermissionsIntent) -> Unit) {
    Surface(
        shadowElevation = 24.dp,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(top = 16.dp),
        ) {
            TextButton(onClick = { onIntent(DeletePermission) }) {
                Icon(
                    painter = painterResource(CoreUiR.drawable.ic_trash),
                    contentDescription = null,
                    tint = colorResource(CoreUiR.color.text_secondary),
                    modifier = Modifier.size(20.dp),
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = stringResource(LocalizationR.string.user_permission_delete),
                    color = colorResource(CoreUiR.color.text_secondary),
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 16.sp,
                )
            }

            PrimaryButton(
                text = stringResource(LocalizationR.string.apply),
                onClick = { onIntent(Save) },
                modifier =
                    Modifier
                        .padding(horizontal = 16.dp)
                        .padding(top = 16.dp, bottom = 16.dp),
            )
        }
    }
}
