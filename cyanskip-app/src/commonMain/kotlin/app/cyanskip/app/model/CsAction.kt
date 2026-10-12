package app.cyanskip.app.model

import kotlinx.serialization.Serializable
import app.cyanskip.app.subscription.RawSubscription

@Serializable
data class CsAction(
    val selector: String,
    val fastQuery: Boolean = false,
    val action: String? = null,
    override val position: RawSubscription.Position? = null,
    override val swipeArg: RawSubscription.SwipeArg? = null,
) : RawSubscription.LocationProps

@Serializable
data class ActionResult(
    val action: String,
    val result: Boolean,
    val shell: Boolean = false,
    val position: Pair<Float, Float>? = null,
)

