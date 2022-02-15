package com.passbolt.mobile.android

import android.app.Application
import coil.Coil
import coil.ImageLoader
import com.passbolt.mobile.android.core.commonresource.commonResourceModule
import com.passbolt.mobile.android.core.logger.FileLoggingTree
import com.passbolt.mobile.android.core.logger.LogFilesManager
import com.passbolt.mobile.android.core.logger.loggerModule
import com.passbolt.mobile.android.core.mvp.mvpModule
import com.passbolt.mobile.android.core.navigation.ActivityIntents
import com.passbolt.mobile.android.core.navigation.AppForegroundListener
import com.passbolt.mobile.android.core.navigation.isAuthenticated
import com.passbolt.mobile.android.core.networking.networkingModule
import com.passbolt.mobile.android.core.qrscan.barcodeScanModule
import com.passbolt.mobile.android.core.qrscan.di.cameraScanModule
import com.passbolt.mobile.android.core.security.securityModule
import com.passbolt.mobile.android.core.users.usersModule
import com.passbolt.mobile.android.database.databaseModule
import com.passbolt.mobile.android.feature.accountdetails.accountDetailsModule
import com.passbolt.mobile.android.feature.authenticationModule
import com.passbolt.mobile.android.feature.autofill.autofillModule
import com.passbolt.mobile.android.feature.folders.foldersModule
import com.passbolt.mobile.android.feature.home.homeModule
import com.passbolt.mobile.android.feature.main.mainModule
import com.passbolt.mobile.android.feature.resources.resourcesModule
import com.passbolt.mobile.android.feature.secrets.secretsModule
import com.passbolt.mobile.android.feature.settings.settingsModule
import com.passbolt.mobile.android.feature.setup.setupModule
import com.passbolt.mobile.android.feature.startup.di.startUpModule
import com.passbolt.mobile.android.featureflags.featureFlagsModule
import com.passbolt.mobile.android.gopenpgp.di.openPgpModule
import com.passbolt.mobile.android.passboltapi.passboltApiModule
import com.passbolt.mobile.android.service.linksApiModule
import com.passbolt.mobile.android.storage.storageModule
import com.passbolt.mobile.android.storage.usecase.preferences.GetGlobalPreferencesUseCase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import org.koin.android.ext.koin.androidContext
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.koin.core.context.startKoin
import timber.log.Timber

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
 * The main entry point for the Passbolt Android application.
 * Contains code for initialization of the main components.
 *
 * @property imageLoader an image loading service
 * @property appForegroundListener listener detecting when the app goes foreground
 * @property applicationScope coroutine scope for the application class
 * @property fileLoggingTree a logger instance that stores logs in a file
 * @property getGlobalPreferencesUseCase a use case for reading global application preferences
 */
class PassboltApplication : Application(), KoinComponent {

    private val imageLoader: ImageLoader by inject()
    private val appForegroundListener: AppForegroundListener by inject()
    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private val fileLoggingTree: FileLoggingTree by inject()
    private val logFilesManager: LogFilesManager by inject()
    private val getGlobalPreferencesUseCase: GetGlobalPreferencesUseCase by inject()

    /**
     * Creates the application class and initializes all components.
     */
    override fun onCreate() {
        super.onCreate()
        initKoin()
        initTimber()
        Coil.setImageLoader(imageLoader)
        registerAppForegroundListener()
    }

    /**
     * Registers a listener detecting when application goes foreground.
     * Listening is done using the application coroutine scope.
     */
    private fun registerAppForegroundListener() {
        registerActivityLifecycleCallbacks(appForegroundListener)
        applicationScope.launch {
            appForegroundListener.appWentForegroundFlow.collect {
                if (it.isAuthenticated()) {
                    it.startActivity(
                        ActivityIntents.authentication(
                            it,
                            ActivityIntents.AuthConfig.RefreshSession
                        )
                    )
                }
            }
        }
    }

    /**
     * Initializes the logging library.
     * Apart from default logger in DEBUG mode the application also uses a logger instance
     * capable of writing logs into files. This additional logger is used only if enabled in
     * the global preferences.
     */
    private fun initTimber() {
        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        }
        val logFilePath = logFilesManager.initializeLogFile()
        logFilesManager.clearIrrelevantLogFiles(logFilePath)
        fileLoggingTree.initialize(logFilePath)
        if (getGlobalPreferencesUseCase.execute(Unit).areDebugLogsEnabled &&
            !Timber.forest().contains(fileLoggingTree)
        ) {
            Timber.plant(fileLoggingTree)
        }
    }

    /**
     * Initializes the dependency injection framework.
     */
    private fun initKoin() {
        startKoin {
            androidContext(this@PassboltApplication)
            modules(
                appModule,
                openPgpModule,
                setupModule,
                mappersModule,
                mvpModule,
                networkingModule,
                barcodeScanModule,
                cameraScanModule,
                storageModule,
                passboltApiModule,
                autofillModule,
                authenticationModule,
                mainModule,
                homeModule,
                foldersModule,
                settingsModule,
                startUpModule,
                resourcesModule,
                featureFlagsModule,
                databaseModule,
                secretsModule,
                commonResourceModule,
                securityModule,
                linksApiModule,
                usersModule,
                loggerModule,
                accountDetailsModule
            )
        }
    }
}
