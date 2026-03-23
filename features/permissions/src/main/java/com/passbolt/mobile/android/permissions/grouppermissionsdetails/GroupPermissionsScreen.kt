package com.passbolt.mobile.android.permissions.grouppermissionsdetails

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.passbolt.mobile.android.core.compose.SideEffectDispatcher
import com.passbolt.mobile.android.core.navigation.compose.AppNavigator
import com.passbolt.mobile.android.core.navigation.compose.keys.GroupDetailsNavigationKey.GroupMembers
import com.passbolt.mobile.android.core.navigation.compose.results.NavigationResultEventBus
import com.passbolt.mobile.android.core.ui.R
import com.passbolt.mobile.android.core.ui.button.PrimaryButton
import com.passbolt.mobile.android.core.ui.dialogs.PermissionDeleteAlertDialog
import com.passbolt.mobile.android.core.ui.header.ItemWithHeader
import com.passbolt.mobile.android.core.ui.permissions.PermissionLabel
import com.passbolt.mobile.android.core.ui.permissions.PermissionSelector
import com.passbolt.mobile.android.core.ui.topbar.BackNavigationIcon
import com.passbolt.mobile.android.core.ui.topbar.TitleAppBar
import com.passbolt.mobile.android.permissions.grouppermissionsdetails.GroupPermissionsIntent.CancelPermissionDelete
import com.passbolt.mobile.android.permissions.grouppermissionsdetails.GroupPermissionsIntent.ConfirmPermissionDelete
import com.passbolt.mobile.android.permissions.grouppermissionsdetails.GroupPermissionsIntent.DeletePermission
import com.passbolt.mobile.android.permissions.grouppermissionsdetails.GroupPermissionsIntent.GoBack
import com.passbolt.mobile.android.permissions.grouppermissionsdetails.GroupPermissionsIntent.Save
import com.passbolt.mobile.android.permissions.grouppermissionsdetails.GroupPermissionsIntent.SeeGroupMembers
import com.passbolt.mobile.android.permissions.grouppermissionsdetails.GroupPermissionsSideEffect.NavigateBack
import com.passbolt.mobile.android.permissions.grouppermissionsdetails.GroupPermissionsSideEffect.NavigateToGroupMembers
import com.passbolt.mobile.android.permissions.grouppermissionsdetails.GroupPermissionsSideEffect.SetDeletePermissionResult
import com.passbolt.mobile.android.permissions.grouppermissionsdetails.GroupPermissionsSideEffect.SetUpdatedPermissionResult
import com.passbolt.mobile.android.permissions.grouppermissionsdetails.ui.GroupMembersSection
import com.passbolt.mobile.android.permissions.navigation.GroupPermissionDeletedResult
import com.passbolt.mobile.android.permissions.navigation.GroupPermissionModifiedResult
import com.passbolt.mobile.android.ui.PermissionModelUi
import com.passbolt.mobile.android.ui.PermissionsMode
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.koinInject
import org.koin.core.parameter.parametersOf
import com.passbolt.mobile.android.core.localization.R as LocalizationR
import com.passbolt.mobile.android.core.ui.R as CoreUiR

@Composable
fun GroupPermissionsScreen(
    permission: PermissionModelUi.GroupPermissionModel,
    mode: PermissionsMode,
    modifier: Modifier = Modifier,
    viewModel: GroupPermissionsViewModel = koinViewModel(parameters = { parametersOf(mode, permission) }),
    navigator: AppNavigator = koinInject(),
) {
    val state = viewModel.viewState.collectAsStateWithLifecycle()
    val resultBus = NavigationResultEventBus.current

    GroupPermissionsScreen(
        state = state.value,
        onIntent = viewModel::onIntent,
        modifier = modifier,
    )

    SideEffectDispatcher(viewModel.sideEffect) { effect ->
        when (effect) {
            NavigateBack -> navigator.navigateBack()
            is NavigateToGroupMembers ->
                navigator.navigateToKey(GroupMembers(effect.groupId))
            is SetUpdatedPermissionResult -> {
                resultBus.sendResult(result = GroupPermissionModifiedResult(effect.permission))
                navigator.navigateBack()
            }
            is SetDeletePermissionResult -> {
                resultBus.sendResult(result = GroupPermissionDeletedResult(effect.permission))
                navigator.navigateBack()
            }
        }
    }
}

@Composable
private fun GroupPermissionsScreen(
    state: GroupPermissionsState,
    onIntent: (GroupPermissionsIntent) -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TitleAppBar(
                title = stringResource(LocalizationR.string.group_permission_permission),
                navigationIcon = { BackNavigationIcon(onBackClick = { onIntent(GoBack) }) },
            )
        },
    ) { paddingValues ->
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(bottom = paddingValues.calculateBottomPadding()),
        ) {
            Column(
                modifier =
                    Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                        .padding(top = paddingValues.calculateTopPadding()),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Image(
                    painter = painterResource(id = CoreUiR.drawable.ic_filled_group_with_bg),
                    contentDescription = stringResource(LocalizationR.string.group_members_title),
                    modifier =
                        Modifier
                            .size(96.dp)
                            .align(Alignment.CenterHorizontally),
                )

                Text(
                    text = state.groupPermission.group.groupName,
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onBackground,
                    textAlign = TextAlign.Center,
                    modifier =
                        Modifier
                            .padding(top = 16.dp)
                            .align(Alignment.CenterHorizontally),
                )

                Spacer(modifier = Modifier.height(32.dp))

                ItemWithHeader(
                    headerText = stringResource(LocalizationR.string.group_permission_group_members),
                    modifier = Modifier.padding(horizontal = 16.dp),
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .clickable(onClick = { onIntent(SeeGroupMembers) })
                                .padding(start = 4.dp, top = 8.dp),
                    ) {
                        GroupMembersSection(
                            users = state.users,
                            modifier = Modifier.weight(1f),
                        )

                        Spacer(modifier = Modifier.width(8.dp))

                        Icon(
                            painter = painterResource(R.drawable.ic_arrow_right),
                            contentDescription = null,
                            tint = colorResource(R.color.icon_tint),
                            modifier = Modifier.size(width = 8.dp, height = 16.dp),
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                ItemWithHeader(
                    headerText = stringResource(LocalizationR.string.user_permission_permission),
                    modifier = Modifier.padding(horizontal = 16.dp),
                ) {
                    Spacer(modifier = Modifier.height(32.dp))
                    if (state.isEditMode) {
                        PermissionSelector(
                            selectedPermission = state.groupPermission.permission,
                            onPermissionSelect = { onIntent(GroupPermissionsIntent.SelectPermission(it)) },
                        )
                    } else {
                        PermissionLabel(permission = state.groupPermission.permission)
                    }
                }
            }

            if (state.isEditMode) {
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
private fun SaveLayout(onIntent: (GroupPermissionsIntent) -> Unit) {
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
