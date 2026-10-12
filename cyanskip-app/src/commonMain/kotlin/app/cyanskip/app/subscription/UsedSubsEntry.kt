package app.cyanskip.app.subscription

import app.cyanskip.db.SubsItem

data class UsedSubsEntry(
    val subsItem: SubsItem,
    val subscription: RawSubscription,
)
