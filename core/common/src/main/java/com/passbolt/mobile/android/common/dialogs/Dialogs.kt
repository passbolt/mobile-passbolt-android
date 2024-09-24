package com.passbolt.mobile.android.common.dialogs

import android.content.Context
import androidx.appcompat.app.AlertDialog
import com.passbolt.mobile.android.core.localization.R as LocalizationR

/**
 * Passbolt - Open source password manager for teams
 * Copyright (c) 2021 Passbolt SA
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU Affero General
 * Public License (AGPL) as published by the Free Software Foundation version 3.
 *
 * The name "Passbolt" is a registered trademark of Passbolt SA, and Passbolt SA hereby declines to grant a trademark
 * license to "Passbolt" pursuant to the GNU Affero General Public License version 3 Section 7(e), without a separate
 * agreement with Passbolt SA.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied
 * warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License along with this program. If not,
 * see GNU Affero General Public License v3 (http://www.gnu.org/licenses/agpl-3.0.html).
 *
 * @copyright Copyright (c) Passbolt SA (https://www.passbolt.com)
 * @license https://opensource.org/licenses/AGPL-3.0 AGPL License
 * @link https://www.passbolt.com Passbolt (tm)
 * @since v1.0
 */

fun signOutAlertDialog(context: Context, confirmAction: () -> Unit) =
    AlertDialog.Builder(context)
        .setTitle(LocalizationR.string.are_you_sure)
        .setMessage(LocalizationR.string.logout_dialog_message)
        .setPositiveButton(LocalizationR.string.logout_dialog_sign_out) { _, _ -> confirmAction() }
        .setNegativeButton(LocalizationR.string.cancel) { _, _ -> }
        .create()

fun permissionDeletionConfirmationAlertDialog(context: Context, confirmAction: () -> Unit) =
    AlertDialog.Builder(context)
        .setTitle(LocalizationR.string.are_you_sure)
        .setMessage(LocalizationR.string.permission_deletion_dialog_message)
        .setPositiveButton(LocalizationR.string.confirm) { _, _ -> confirmAction() }
        .setNegativeButton(LocalizationR.string.cancel) { _, _ -> }
        .create()

fun encryptionErrorAlertDialog(context: Context, message: String) =
    AlertDialog.Builder(context)
        .setTitle(LocalizationR.string.dialog_encryption_error_title)
        .setMessage(context.getString(LocalizationR.string.dialog_encryption_error_message, message))
        .setPositiveButton(LocalizationR.string.ok) { _, _ -> }
        .create()

fun accessibilityServiceConsentDialog(context: Context, confirmAction: () -> Unit) =
    AlertDialog.Builder(context)
        .setTitle(LocalizationR.string.dialog_accessibility_consent_title)
        .setMessage(LocalizationR.string.dialog_accessibility_consent_message)
        .setPositiveButton(LocalizationR.string.consent) { _, _ -> confirmAction() }
        .setNegativeButton(LocalizationR.string.cancel) { _, _ -> }
        .create()

fun cancelTransferAccountAlertDialog(context: Context, confirmAction: () -> Unit) =
    AlertDialog.Builder(context)
        .setTitle(LocalizationR.string.are_you_sure)
        .setMessage(LocalizationR.string.transfer_account_stop_confirmation_dialog_message)
        .setPositiveButton(LocalizationR.string.transfer_account_stop_button) { _, _ -> confirmAction() }
        .setNegativeButton(LocalizationR.string.cancel) { _, _ -> }
        .create()

fun configureFingerprintFirstDialog(context: Context, confirmAction: () -> Unit) =
    AlertDialog.Builder(context)
        .setTitle(LocalizationR.string.settings_add_first_fingerprint_title)
        .setMessage(LocalizationR.string.settings_add_first_fingerprint)
        .setPositiveButton(LocalizationR.string.settings_add_first_fingerprint_settings) { _, _ -> confirmAction() }
        .setNegativeButton(LocalizationR.string.cancel) { _, _ -> }
        .setCancelable(false)
        .create()

fun disableFingerprintConfirmationDialog(context: Context, confirmAction: () -> Unit, cancelAction: () -> Unit) =
    AlertDialog.Builder(context)
        .setTitle(LocalizationR.string.are_you_sure)
        .setMessage(LocalizationR.string.settings_disable_fingerprint_confirmation_message)
        .setPositiveButton(LocalizationR.string.settings_disable) { _, _ -> confirmAction() }
        .setNegativeButton(LocalizationR.string.cancel) { _, _ -> cancelAction() }
        .create()

fun keyChangesDetectedAlertDialog(context: Context, confirmAction: () -> Unit) =
    AlertDialog.Builder(context)
        .setTitle(LocalizationR.string.fingerprint_biometric_changed_title)
        .setMessage(LocalizationR.string.fingerprint_authenticate_again)
        .setPositiveButton(LocalizationR.string.got_it) { _, _ -> confirmAction() }
        .setCancelable(false)
        .create()

fun confirmResourceDeletionAlertDialog(context: Context, confirmAction: () -> Unit) =
    AlertDialog.Builder(context)
        .setTitle(LocalizationR.string.are_you_sure)
        .setMessage(LocalizationR.string.resource_will_be_deleted)
        .setPositiveButton(LocalizationR.string.cancel) { _, _ -> }
        .setNegativeButton(LocalizationR.string.delete) { _, _ -> confirmAction() }
        .create()

fun confirmTotpDeletionAlertDialog(context: Context, confirmAction: () -> Unit) =
    AlertDialog.Builder(context)
        .setTitle(LocalizationR.string.are_you_sure)
        .setMessage(LocalizationR.string.otp_delete_confirmation)
        .setPositiveButton(LocalizationR.string.cancel) { _, _ -> }
        .setNegativeButton(LocalizationR.string.otp_delete_totp) { _, _ -> confirmAction() }
        .create()

fun serverNotReachableAlertDialog(context: Context, domain: String) =
    AlertDialog.Builder(context)
        .setTitle(LocalizationR.string.dialog_server_not_reachable_title)
        .setMessage(context.getString(LocalizationR.string.dialog_server_not_reachable_message, domain))
        .setPositiveButton(context.getString(LocalizationR.string.dialog_server_not_reachable_got_it)) { _, _ -> }
        .create()

fun rootWarningAlertDialog(context: Context, onApprove: () -> Unit = {}) =
    AlertDialog.Builder(context)
        .setTitle(LocalizationR.string.root_warning_title)
        .setMessage(LocalizationR.string.root_warning_message)
        .setPositiveButton(LocalizationR.string.root_warning_ackowledge) { _, _ -> onApprove() }
        .setCancelable(false)
        .create()

fun yubikeyScanFailedAlertDialog(context: Context) =
    AlertDialog.Builder(context)
        .setTitle(LocalizationR.string.dialog_mfa_scan_youbikey_failed_title)
        .setMessage(LocalizationR.string.dialog_mfa_scan_youbikey_failed_message)
        .setPositiveButton(LocalizationR.string.got_it) { _, _ -> }
        .setCancelable(false)
        .create()

fun yubikeyNotFromCurrentUserAlertDialog(context: Context) =
    AlertDialog.Builder(context)
        .setTitle(LocalizationR.string.dialog_mfa_scan_youbikey_not_from_current_user_title)
        .setMessage(LocalizationR.string.dialog_mfa_scan_youbikey_not_from_current_user_message)
        .setPositiveButton(LocalizationR.string.got_it) { _, _ -> }
        .setCancelable(false)
        .create()

fun unableToGeneratePasswordAlertDialog(context: Context, requiredEntropy: Int) =
    AlertDialog.Builder(context)
        .setTitle(LocalizationR.string.dialog_unable_to_generate_password_title)
        .setMessage(context.getString(LocalizationR.string.dialog_unable_to_generate_password_message, requiredEntropy))
        .setPositiveButton(LocalizationR.string.got_it) { _, _ -> }
        .setCancelable(false)
        .create()

fun pwnedPasswordAlertDialog(context: Context, onProceed: () -> Unit) =
    AlertDialog.Builder(context)
        .setTitle(LocalizationR.string.dialog_confirm_password_title)
        .setMessage(LocalizationR.string.dialog_confirm_password_message_data_breach)
        .setPositiveButton(LocalizationR.string.edit_password) { _, _ -> }
        .setNegativeButton(LocalizationR.string.proceed) { _, _ -> onProceed() }
        .setCancelable(false)
        .create()

fun weakPasswordAlertDialog(context: Context, onProceed: () -> Unit) =
    AlertDialog.Builder(context)
        .setTitle(LocalizationR.string.dialog_confirm_password_title)
        .setMessage(LocalizationR.string.dialog_confirm_password_message_low_entropy)
        .setPositiveButton(LocalizationR.string.edit_password) { _, _ -> }
        .setNegativeButton(LocalizationR.string.proceed) { _, _ -> onProceed() }
        .setCancelable(false)
        .create()

fun setupExitConfirmationDialog(context: Context, onExit: () -> Unit) =
    AlertDialog.Builder(context)
        .setTitle(LocalizationR.string.are_you_sure)
        .setMessage(LocalizationR.string.scan_qr_exit_confirmation_dialog_message)
        .setPositiveButton(LocalizationR.string.cancel) { _, _ -> }
        .setNegativeButton(LocalizationR.string.stop_scanning) { _, _ -> onExit() }
        .create()

fun qrCodesInformationDialog(context: Context) =
    AlertDialog.Builder(context)
        .setTitle(LocalizationR.string.scan_qr_exit_information_dialog_title)
        .setMessage(LocalizationR.string.scan_qr_exit_information_dialog_message)
        .setPositiveButton(LocalizationR.string.got_it) { _, _ -> }
        .create()

fun howToCreateAccountDialog(context: Context) =
    AlertDialog.Builder(context, com.passbolt.mobile.android.core.ui.R.style.AlertDialogTheme)
        .setTitle(LocalizationR.string.welcome_create_account_dialog_title)
        .setMessage(LocalizationR.string.welcome_create_account_dialog_message)
        .setPositiveButton(LocalizationR.string.got_it) { _, _ -> }
        .create()
