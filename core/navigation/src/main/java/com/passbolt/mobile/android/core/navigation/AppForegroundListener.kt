package com.passbolt.mobile.android.core.navigation

import android.app.Activity
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

class AppForegroundListener : StartedStoppedCallback() {

    private var startedActivities = 0
    private var _appWentForegroundFlow = MutableSharedFlow<Activity>(
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )

    val appWentForegroundFlow = _appWentForegroundFlow
        .asSharedFlow()

    override fun onActivityStarted(activity: Activity) {
        if (++startedActivities == 1) {
            _appWentForegroundFlow.tryEmit(activity)
        }
    }

    override fun onActivityStopped(activity: Activity) {
        --startedActivities
    }
}
