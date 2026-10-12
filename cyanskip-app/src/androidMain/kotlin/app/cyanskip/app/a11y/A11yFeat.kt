package app.cyanskip.app.a11y

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.view.accessibility.AccessibilityEvent
import androidx.core.content.ContextCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.launch
import app.cyanskip.app.app
import app.cyanskip.app.appScope
import app.cyanskip.app.settings.SettingsRepository.settings
import app.cyanskip.app.subscription.SubscriptionRepository
import app.cyanskip.app.subscription.SubscriptionResult
import app.cyanskip.app.ui.option.UpdateTimeOption
import app.cyanskip.app.ui.rule.statusText
import app.cyanskip.app.util.LogUtils
import app.cyanskip.app.util.launchLogged

fun onA11yFeatEvent(event: AccessibilityEvent) = event.run {
    if (event.packageName == launcherAppId) {
        watchAutoUpdateSubs()
    }
}

private var nextUpdateSubsTime = 0L
private var autoRefreshPending = false
private fun watchAutoUpdateSubs() {
    val interval = settings.value.updateSubsInterval
    if (interval <= 0 || autoRefreshPending) return
    val currentTime = System.currentTimeMillis()
    if (currentTime < nextUpdateSubsTime) return
    autoRefreshPending = true
    appScope.launchLogged {
        try {
            val result = SubscriptionRepository.refresh()
            if (result !is SubscriptionResult.Busy) {
                nextUpdateSubsTime = currentTime + if (
                    result is SubscriptionResult.Failure &&
                    result.reason == SubscriptionResult.FailureReason.NetworkUnavailable
                ) {
                    5 * 60_000L
                } else {
                    interval.coerceAtLeast(UpdateTimeOption.Everyday.value)
                }
            }
        } finally {
            autoRefreshPending = false
        }
    }
}

private fun initRuleChangedLog() {
    appScope.launch(Dispatchers.Default) {
        activityRuleFlow.debounce(300).drop(1).collect {
            if (settings.value.enableMatch && it.currentRules.isNotEmpty()) {
                LogUtils.d(it.topActivity, *it.currentRules.map { r ->
                    r.statusText()
                }.toTypedArray())
            }
        }
    }
}

var isInteractive = true
    private set
private val screenStateReceiver = object : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        LogUtils.d("screenStateReceiver->${action}")
        isInteractive = when (action) {
            Intent.ACTION_SCREEN_ON -> true
            Intent.ACTION_SCREEN_OFF -> false
            Intent.ACTION_USER_PRESENT -> true
            else -> isInteractive
        }
        if (isInteractive) {
            val t = System.currentTimeMillis()
            if (t - appChangeTime > 500) { // 37.872(a11y) -> 38.228(onReceive)
                A11yRuntime.onScreenForcedActive()
            }
        }
    }
}

private fun initScreenStateReceiver() {
    isInteractive = app.powerManager.isInteractive
    ContextCompat.registerReceiver(
        app,
        screenStateReceiver,
        IntentFilter().apply {
            addAction(Intent.ACTION_SCREEN_ON)
            addAction(Intent.ACTION_SCREEN_OFF)
            addAction(Intent.ACTION_USER_PRESENT)
        },
        ContextCompat.RECEIVER_EXPORTED
    )
}

fun initA11yFeat() {
    initRuleChangedLog()
    initScreenStateReceiver()
}
