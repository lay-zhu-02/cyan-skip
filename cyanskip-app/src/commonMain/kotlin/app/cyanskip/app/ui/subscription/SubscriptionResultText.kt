package app.cyanskip.app.ui.subscription

import app.cyanskip.app.resources.Res
import app.cyanskip.app.resources.delete_success
import app.cyanskip.app.resources.network_unavailable
import app.cyanskip.app.resources.subscription_add_success
import app.cyanskip.app.resources.subscription_busy_retry
import app.cyanskip.app.resources.subscription_data_delete_failed
import app.cyanskip.app.resources.subscription_duplicate_link
import app.cyanskip.app.resources.subscription_edit_success
import app.cyanskip.app.resources.subscription_exists
import app.cyanskip.app.resources.subscription_file_delete_cancelled
import app.cyanskip.app.resources.subscription_file_download_failed
import app.cyanskip.app.resources.subscription_file_parsing_failed
import app.cyanskip.app.resources.subscription_file_save_failed
import app.cyanskip.app.resources.subscription_id_reserved
import app.cyanskip.app.resources.subscriptions_updated_count
import app.cyanskip.app.resources.updates_none
import app.cyanskip.app.subscription.SubscriptionResult
import app.cyanskip.app.ui.text.subscriptionMessage
import org.jetbrains.compose.resources.getString

suspend fun SubscriptionResult.message(): String? = when (this) {
    SubscriptionResult.Busy -> getString(Res.string.subscription_busy_retry)
    is SubscriptionResult.Success -> when (kind) {
        SubscriptionResult.SuccessKind.None -> null
        SubscriptionResult.SuccessKind.Deleted -> getString(Res.string.delete_success)
        SubscriptionResult.SuccessKind.Added -> getString(Res.string.subscription_add_success)
        SubscriptionResult.SuccessKind.Modified -> getString(Res.string.subscription_edit_success)
        SubscriptionResult.SuccessKind.Refreshed -> {
            if (count > 0) getString(Res.string.subscriptions_updated_count, count.toString()) else getString(Res.string.updates_none)
        }
    }

    is SubscriptionResult.Failure -> when (reason) {
        SubscriptionResult.FailureReason.DeleteData -> detailMessage(
            getString(Res.string.subscription_data_delete_failed),
            cause?.subscriptionMessage() ?: detail
        )

        SubscriptionResult.FailureReason.DeleteFile ->
            detailMessage(getString(Res.string.subscription_file_delete_cancelled), cause?.subscriptionMessage() ?: detail)

        SubscriptionResult.FailureReason.DuplicateUrl -> getString(Res.string.subscription_duplicate_link)
        SubscriptionResult.FailureReason.Download -> detailMessage(
            getString(Res.string.subscription_file_download_failed),
            cause?.subscriptionMessage() ?: detail
        )

        SubscriptionResult.FailureReason.Parse -> detailMessage(
            getString(Res.string.subscription_file_parsing_failed),
            cause?.subscriptionMessage() ?: detail
        )

        SubscriptionResult.FailureReason.AlreadyExists -> getString(Res.string.subscription_exists)
        SubscriptionResult.FailureReason.InvalidId -> getString(Res.string.subscription_id_reserved,
            detail.orEmpty()
        )

        SubscriptionResult.FailureReason.Save -> detailMessage(
            getString(Res.string.subscription_file_save_failed),
            cause?.subscriptionMessage() ?: detail
        )

        SubscriptionResult.FailureReason.NetworkUnavailable -> getString(Res.string.network_unavailable)
    }
}

private fun detailMessage(message: String, detail: String?): String =
    if (detail.isNullOrBlank()) message else "$message\n$detail"
