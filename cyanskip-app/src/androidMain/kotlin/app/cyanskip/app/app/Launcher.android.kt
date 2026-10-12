package app.cyanskip.app.app

import kotlinx.coroutines.flow.StateFlow

actual val launcherAppIdFlow: StateFlow<String>
    get() = app.cyanskip.app.a11y.launcherAppIdFlow
