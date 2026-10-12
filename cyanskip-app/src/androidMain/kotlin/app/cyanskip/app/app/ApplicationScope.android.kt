package app.cyanskip.app.app

import kotlinx.coroutines.CoroutineScope
import app.cyanskip.app.appScope

actual fun applicationScope(): CoroutineScope = appScope
