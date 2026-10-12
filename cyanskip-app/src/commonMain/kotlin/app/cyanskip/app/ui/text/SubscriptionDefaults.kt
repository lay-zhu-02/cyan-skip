package app.cyanskip.app.ui.text

import app.cyanskip.app.resources.Res
import app.cyanskip.app.resources.subscription_local
import app.cyanskip.app.resources.subscription_memory
import app.cyanskip.app.subscription.SubscriptionDefaults
import org.jetbrains.compose.resources.getString

suspend fun subscriptionDefaults() = SubscriptionDefaults(
    localName = getString(Res.string.subscription_local),
    memoryName = getString(Res.string.subscription_memory),
)
