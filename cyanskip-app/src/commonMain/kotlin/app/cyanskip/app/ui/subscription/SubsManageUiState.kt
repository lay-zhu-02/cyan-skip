package app.cyanskip.app.ui.subscription

import app.cyanskip.app.subscription.RawSubscription
import app.cyanskip.db.SubsItem

data class SubsManageUiState(
    val subItems: List<SubsItem>,
    val subscriptions: Map<Long, RawSubscription>,
    val loadErrors: Map<Long, Exception>,
    val refreshErrors: Map<Long, Exception>,
)

