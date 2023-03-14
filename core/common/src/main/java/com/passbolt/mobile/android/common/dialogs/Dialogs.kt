package com.passbolt.mobile.android.common.dialogs

import android.content.Context
import androidx.appcompat.app.AlertDialog
import com.passbolt.mobile.android.common.R

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
        .setTitle(R.string.are_you_sure)
        .setMessage(R.string.logout_dialog_message)
        .setPositiveButton(R.string.logout_dialog_sign_out) { _, _ -> confirmAction() }
        .setNegativeButton(R.string.cancel) { _, _ -> }
        .create()

fun permissionDeletionConfirmationAlertDialog(context: Context, confirmAction: () -> Unit) =
    AlertDialog.Builder(context)
        .setTitle(R.string.are_you_sure)
        .setMessage(R.string.permission_deletion_dialog_message)
        .setPositiveButton(R.string.confirm) { _, _ -> confirmAction() }
        .setNegativeButton(R.string.cancel) { _, _ -> }
        .create()

fun encryptionErrorAlertDialog(context: Context, message: String) =
    AlertDialog.Builder(context)
        .setTitle(R.string.dialog_encryption_error_title)
        .setMessage(context.getString(R.string.dialog_encryption_error_message, message))
        .setPositiveButton(R.string.ok) { _, _ -> }
        .create()

fun accessibilityServiceConsentDialog(context: Context, confirmAction: () -> Unit) =
    AlertDialog.Builder(context)
        .setTitle(R.string.dialog_accessibility_consent_title)
        .setMessage(R.string.dialog_accessibility_consent_message)
        .setPositiveButton(R.string.consent) { _, _ -> confirmAction() }
        .setNegativeButton(R.string.cancel) { _, _ -> }
        .create()

fun cancelTransferAccountAlertDialog(context: Context, confirmAction: () -> Unit) =
    AlertDialog.Builder(context)
        .setTitle(R.string.are_you_sure)
        .setMessage(R.string.transfer_account_stop_confirmation_dialog_message)
        .setPositiveButton(R.string.transfer_account_stop_button) { _, _ -> confirmAction() }
        .setNegativeButton(R.string.cancel) { _, _ -> }
        .create()

fun configureFingerprintFirstDialog(context: Context, confirmAction: () -> Unit) =
    AlertDialog.Builder(context)
        .setTitle(R.string.settings_add_first_fingerprint_title)
        .setMessage(R.string.settings_add_first_fingerprint)
        .setPositiveButton(R.string.settings_add_first_fingerprint_settings) { _, _ -> confirmAction() }
        .setNegativeButton(R.string.cancel) { _, _ -> }
        .setCancelable(false)
        .create()

fun disableFingerprintConfirmationDialog(context: Context, confirmAction: () -> Unit, cancelAction: () -> Unit) =
    AlertDialog.Builder(context)
        .setTitle(R.string.are_you_sure)
        .setMessage(R.string.settings_disable_fingerprint_confirmation_message)
        .setPositiveButton(R.string.settings_disable) { _, _ -> confirmAction() }
        .setNegativeButton(R.string.cancel) { _, _ -> cancelAction() }
        .create()

fun keyChangesDetectedAlertDialog(context: Context, confirmAction: () -> Unit) =
    AlertDialog.Builder(context)
        .setTitle(R.string.fingerprint_biometric_changed_title)
        .setMessage(R.string.fingerprint_authenticate_again)
        .setPositiveButton(R.string.got_it) { _, _ -> confirmAction() }
        .setCancelable(false)
        .create()

fun confirmTotpDeletionAlertDialog(context: Context, confirmAction: () -> Unit) =
    AlertDialog.Builder(context)
        .setTitle(R.string.are_you_sure)
        .setMessage(R.string.fingerprint_authenticate_again)
        .setPositiveButton(R.string.cancel) { _, _ -> }
        .setNegativeButton(R.string.otp_delete_totp) { _, _ -> confirmAction() }
        .create()
