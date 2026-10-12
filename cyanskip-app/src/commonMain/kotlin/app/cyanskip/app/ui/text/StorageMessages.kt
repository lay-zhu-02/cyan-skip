package app.cyanskip.app.ui.text

import app.cyanskip.app.resources.Res
import app.cyanskip.app.resources.archive_file_count_exceeded
import app.cyanskip.app.resources.archive_file_too_large
import app.cyanskip.app.resources.archive_path_invalid
import app.cyanskip.app.resources.archive_path_outside_destination
import app.cyanskip.app.resources.archive_total_size_exceeded
import app.cyanskip.app.resources.backup_archive_too_large
import app.cyanskip.app.resources.backup_duplicate_subscription_id
import app.cyanskip.app.resources.backup_invalid_archive
import app.cyanskip.app.resources.backup_reselect_file
import app.cyanskip.app.resources.backup_version_unsupported
import app.cyanskip.app.resources.file_too_large
import app.cyanskip.app.resources.snapshot_files_incomplete
import app.cyanskip.app.resources.subscription_file_id_mismatch_detail
import app.cyanskip.app.resources.subscription_invalid_filename
import app.cyanskip.app.storage.ArchiveIssue
import app.cyanskip.app.storage.BackupIssue
import app.cyanskip.app.storage.SnapshotIssue
import app.cyanskip.app.storage.StorageException
import app.cyanskip.app.storage.FileOperationIssue
import org.jetbrains.compose.resources.StringResource

fun Throwable.displayMessage(): String = when (this) {
    is app.cyanskip.app.subscription.SubscriptionException -> messageSync()
    is app.cyanskip.app.subscription.SubscriptionPersistence.DeleteException -> cause?.displayMessage() ?: toString()
    is app.cyanskip.app.storage.FileTooLargeException -> Res.string.file_too_large.getSync()
    is StorageException -> issue.resource.getSync(*arguments)
    else -> message ?: toString()
}

val FileOperationIssue.resource: StringResource
    get() = when (this) {
        is ArchiveIssue -> when (this) {
            ArchiveIssue.FileCountExceeded -> Res.string.archive_file_count_exceeded
            ArchiveIssue.FileTooLarge -> Res.string.archive_file_too_large
            ArchiveIssue.PathInvalid -> Res.string.archive_path_invalid
            ArchiveIssue.PathOutsideDestination -> Res.string.archive_path_outside_destination
            ArchiveIssue.TotalSizeExceeded -> Res.string.archive_total_size_exceeded
        }
        is BackupIssue -> when (this) {
            BackupIssue.ArchiveTooLarge -> Res.string.backup_archive_too_large
            BackupIssue.DuplicateSubscriptionId -> Res.string.backup_duplicate_subscription_id
            BackupIssue.InvalidArchive -> Res.string.backup_invalid_archive
            BackupIssue.SourceAccessDenied -> Res.string.backup_reselect_file
            BackupIssue.VersionUnsupported -> Res.string.backup_version_unsupported
            BackupIssue.SubscriptionFileIdMismatch -> Res.string.subscription_file_id_mismatch_detail
            BackupIssue.SubscriptionInvalidFilename -> Res.string.subscription_invalid_filename
        }
        is SnapshotIssue -> when (this) {
            SnapshotIssue.FilesIncomplete -> Res.string.snapshot_files_incomplete
        }
    }
