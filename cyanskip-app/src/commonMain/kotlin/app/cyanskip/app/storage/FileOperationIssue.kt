package app.cyanskip.app.storage

sealed interface FileOperationIssue {
    val name: String
}

enum class ArchiveIssue : FileOperationIssue {
    FileCountExceeded,
    FileTooLarge,
    PathInvalid,
    PathOutsideDestination,
    TotalSizeExceeded,
}

enum class BackupIssue : FileOperationIssue {
    ArchiveTooLarge,
    DuplicateSubscriptionId,
    InvalidArchive,
    SourceAccessDenied,
    VersionUnsupported,
    SubscriptionFileIdMismatch,
    SubscriptionInvalidFilename,
}

enum class SnapshotIssue : FileOperationIssue {
    FilesIncomplete,
}
