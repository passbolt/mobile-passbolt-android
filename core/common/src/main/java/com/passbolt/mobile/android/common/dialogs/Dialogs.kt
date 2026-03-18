package com.passbolt.mobile.android.common.dialogs

import android.content.Context
import androidx.appcompat.app.AlertDialog
import com.passbolt.mobile.android.core.localization.R as LocalizationR

fun accessibilityServiceConsentDialog(
    context: Context,
    confirmAction: () -> Unit,
) = AlertDialog
    .Builder(context)
    .setTitle(LocalizationR.string.dialog_accessibility_consent_title)
    .setMessage(LocalizationR.string.dialog_accessibility_consent_message)
    .setPositiveButton(LocalizationR.string.consent) { _, _ -> confirmAction() }
    .setNegativeButton(LocalizationR.string.cancel) { _, _ -> }
    .create()

fun serverNotReachableAlertDialog(
    context: Context,
    domain: String,
) = AlertDialog
    .Builder(context)
    .setTitle(LocalizationR.string.dialog_server_not_reachable_title)
    .setMessage(context.getString(LocalizationR.string.dialog_server_not_reachable_message, domain))
    .setPositiveButton(context.getString(LocalizationR.string.dialog_server_not_reachable_got_it)) { _, _ -> }
    .create()

fun rootWarningAlertDialog(
    context: Context,
    onApprove: () -> Unit = {},
) = AlertDialog
    .Builder(context)
    .setTitle(LocalizationR.string.root_warning_title)
    .setMessage(LocalizationR.string.root_warning_message)
    .setPositiveButton(LocalizationR.string.root_warning_ackowledge) { _, _ -> onApprove() }
    .setCancelable(false)
    .create()

fun yubikeyScanFailedAlertDialog(context: Context) =
    AlertDialog
        .Builder(context)
        .setTitle(LocalizationR.string.dialog_mfa_scan_youbikey_failed_title)
        .setMessage(LocalizationR.string.dialog_mfa_scan_youbikey_failed_message)
        .setPositiveButton(LocalizationR.string.got_it) { _, _ -> }
        .setCancelable(false)
        .create()

fun yubikeyNotFromCurrentUserAlertDialog(context: Context) =
    AlertDialog
        .Builder(context)
        .setTitle(LocalizationR.string.dialog_mfa_scan_youbikey_not_from_current_user_title)
        .setMessage(LocalizationR.string.dialog_mfa_scan_youbikey_not_from_current_user_message)
        .setPositiveButton(LocalizationR.string.got_it) { _, _ -> }
        .setCancelable(false)
        .create()

fun unableToGeneratePasswordAlertDialog(
    context: Context,
    requiredEntropy: Int,
) = AlertDialog
    .Builder(context)
    .setTitle(LocalizationR.string.dialog_unable_to_generate_password_title)
    .setMessage(context.getString(LocalizationR.string.dialog_unable_to_generate_password_message, requiredEntropy))
    .setPositiveButton(LocalizationR.string.got_it) { _, _ -> }
    .setCancelable(false)
    .create()

fun pwnedPasswordAlertDialog(
    context: Context,
    onProceed: () -> Unit,
) = AlertDialog
    .Builder(context)
    .setTitle(LocalizationR.string.dialog_confirm_password_title)
    .setMessage(LocalizationR.string.dialog_confirm_password_message_data_breach)
    .setPositiveButton(LocalizationR.string.edit_password) { _, _ -> }
    .setNegativeButton(LocalizationR.string.proceed) { _, _ -> onProceed() }
    .setCancelable(false)
    .create()

fun weakPasswordAlertDialog(
    context: Context,
    onProceed: () -> Unit,
) = AlertDialog
    .Builder(context)
    .setTitle(LocalizationR.string.dialog_confirm_password_title)
    .setMessage(LocalizationR.string.dialog_confirm_password_message_low_entropy)
    .setPositiveButton(LocalizationR.string.edit_password) { _, _ -> }
    .setNegativeButton(LocalizationR.string.proceed) { _, _ -> onProceed() }
    .setCancelable(false)
    .create()

fun provideCameraPermissionInSettingsDialog(
    context: Context,
    onSettingsClick: () -> Unit,
) = AlertDialog
    .Builder(context)
    .setTitle(LocalizationR.string.transfer_details_camera_access_dialog_title)
    .setMessage(LocalizationR.string.transfer_details_camera_access_dialog_message)
    .setPositiveButton(LocalizationR.string.settings) { _, _ -> onSettingsClick() }
    .setNegativeButton(LocalizationR.string.cancel) { _, _ -> }
    .create()

fun cameraRequiredDialog(context: Context) =
    AlertDialog
        .Builder(context)
        .setTitle(LocalizationR.string.transfer_details_camera_required_dialog_title)
        .setMessage(LocalizationR.string.transfer_details_camera_required_dialog_message)
        .setPositiveButton(LocalizationR.string.ok) { _, _ -> }
        .create()
