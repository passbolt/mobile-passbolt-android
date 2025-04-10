package com.passbolt.mobile.android.initializers

import android.content.Context
import androidx.annotation.VisibleForTesting
import androidx.startup.Initializer
import com.passbolt.mobile.android.appModule
import com.passbolt.mobile.android.common.commonModule
import com.passbolt.mobile.android.core.accounts.accountsCoreModule
import com.passbolt.mobile.android.core.authenticationcore.authenticationCoreModule
import com.passbolt.mobile.android.core.autofill.autofillModule
import com.passbolt.mobile.android.core.commonfolders.foldersModule
import com.passbolt.mobile.android.core.commongroups.groupsModule
import com.passbolt.mobile.android.core.coreUiModule
import com.passbolt.mobile.android.core.envinfo.envInfoModule
import com.passbolt.mobile.android.core.fulldatarefresh.fullDataRefreshModule
import com.passbolt.mobile.android.core.idlingresource.idlingResourcesModule
import com.passbolt.mobile.android.core.inappreview.inAppReviewModule
import com.passbolt.mobile.android.core.logger.loggerModule
import com.passbolt.mobile.android.core.mvp.architectureModule
import com.passbolt.mobile.android.core.networking.networkingModule
import com.passbolt.mobile.android.core.notifications.notificationsModule
import com.passbolt.mobile.android.core.otpcore.otpCoreModule
import com.passbolt.mobile.android.core.passphrasememorycache.passphraseMemoryCacheModule
import com.passbolt.mobile.android.core.passwordgenerator.passwordGeneratorModule
import com.passbolt.mobile.android.core.policiesModule
import com.passbolt.mobile.android.core.preferences.preferencesModule
import com.passbolt.mobile.android.core.qrscan.barcodeScanModule
import com.passbolt.mobile.android.core.rbacModule
import com.passbolt.mobile.android.core.resources.resourcesModule
import com.passbolt.mobile.android.core.resourcetypes.resourceTypesModule
import com.passbolt.mobile.android.core.secrets.secretsModule
import com.passbolt.mobile.android.core.security.securityModule
import com.passbolt.mobile.android.core.tags.tagsModule
import com.passbolt.mobile.android.core.users.usersModule
import com.passbolt.mobile.android.createFolderModule
import com.passbolt.mobile.android.createresourcemenu.createResourceMenuModule
import com.passbolt.mobile.android.database.databaseModule
import com.passbolt.mobile.android.encryptedstorage.encryptedStorageModule
import com.passbolt.mobile.android.feature.accountdetails.accountDetailsModule
import com.passbolt.mobile.android.feature.authenticationModule
import com.passbolt.mobile.android.feature.autofill.autofillResourcesModule
import com.passbolt.mobile.android.feature.home.homeModule
import com.passbolt.mobile.android.feature.main.mainModule
import com.passbolt.mobile.android.feature.otp.otpMainModule
import com.passbolt.mobile.android.feature.otp.scanOtpMainModule
import com.passbolt.mobile.android.feature.resourcedetails.resourceDetailsModule
import com.passbolt.mobile.android.feature.resourceform.resourceFormModule
import com.passbolt.mobile.android.feature.settings.settingsModule
import com.passbolt.mobile.android.feature.setup.setupModule
import com.passbolt.mobile.android.feature.startup.startUpModule
import com.passbolt.mobile.android.feature.transferaccounttoanotherdevice.transferAccountToAnotherDeviceModule
import com.passbolt.mobile.android.featureflags.featureFlagsModule
import com.passbolt.mobile.android.folderDetailsModule
import com.passbolt.mobile.android.gopenpgp.openPgpModule
import com.passbolt.mobile.android.groupDetailsModule
import com.passbolt.mobile.android.helpMenuModule
import com.passbolt.mobile.android.jsonmodel.jsonModelModule
import com.passbolt.mobile.android.linksapi.linksApiModule
import com.passbolt.mobile.android.locationDetailsModule
import com.passbolt.mobile.android.logsModule
import com.passbolt.mobile.android.mappersModule
import com.passbolt.mobile.android.metadata.metadataModule
import com.passbolt.mobile.android.otpMainMoreMenuModule
import com.passbolt.mobile.android.passboltapi.passboltApiModule
import com.passbolt.mobile.android.permissions.permissions.permissionsModule
import com.passbolt.mobile.android.permissions.permissionsModule
import com.passbolt.mobile.android.pwnedpasswordsapi.pwnedPasswordsApiModule
import com.passbolt.mobile.android.resourceMoreMenuModule
import com.passbolt.mobile.android.resourcePickerModule
import com.passbolt.mobile.android.resourceTagsModule
import com.passbolt.mobile.android.serializers.serializersModule
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin

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

/**
 * Initializes the dependency injection framework.
 */
@Suppress("unused")
class KoinInitializer : Initializer<Unit> {

    override fun create(context: Context) {
        startKoin {
            androidContext(context)
            modules(appModules)
        }
    }

    override fun dependencies(): MutableList<Class<out Initializer<*>>> =
        mutableListOf()

    companion object {
        @VisibleForTesting
        val appModules = listOf(
            appModule,
            openPgpModule,
            setupModule,
            mappersModule,
            architectureModule,
            networkingModule,
            barcodeScanModule,
            passboltApiModule,
            autofillResourcesModule,
            authenticationModule,
            homeModule,
            settingsModule,
            startUpModule,
            resourcesModule,
            featureFlagsModule,
            databaseModule,
            secretsModule,
            resourceDetailsModule,
            securityModule,
            linksApiModule,
            usersModule,
            loggerModule,
            accountDetailsModule,
            foldersModule,
            folderDetailsModule,
            mainModule,
            groupsModule,
            commonModule,
            coreUiModule,
            locationDetailsModule,
            createFolderModule,
            groupDetailsModule,
            resourceTagsModule,
            helpMenuModule,
            logsModule,
            resourceMoreMenuModule,
            fullDataRefreshModule,
            resourceTypesModule,
            notificationsModule,
            autofillModule,
            inAppReviewModule,
            envInfoModule,
            idlingResourcesModule,
            transferAccountToAnotherDeviceModule,
            otpMainModule,
            otpCoreModule,
            serializersModule,
            resourcePickerModule,
            tagsModule,
            scanOtpMainModule,
            otpMainMoreMenuModule,
            rbacModule,
            accountsCoreModule,
            policiesModule,
            pwnedPasswordsApiModule,
            passwordGeneratorModule,
            metadataModule,
            encryptedStorageModule,
            authenticationCoreModule,
            preferencesModule,
            passphraseMemoryCacheModule,
            jsonModelModule,
            createResourceMenuModule,
            resourceFormModule,
            permissionsModule
        )
    }
}
