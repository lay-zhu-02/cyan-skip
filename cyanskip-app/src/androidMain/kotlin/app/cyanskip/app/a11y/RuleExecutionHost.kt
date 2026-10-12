package app.cyanskip.app.a11y

import app.cyanskip.app.app.AppInfoRepository
import app.cyanskip.app.rule.RuleDelayScheduler
import app.cyanskip.app.rule.RuleMatchEnvironment
import app.cyanskip.app.rule.RuleRuntime

object RuleExecutionHost {
    val delays = RuleDelayScheduler()
    val runtime = RuleRuntime(
        environment = {
            RuleMatchEnvironment(
                launcherAppId,
                AppInfoRepository.snapshot?.systemApps.orEmpty()
            )
        },
        cancelPending = delays::cancel,
    )
}
