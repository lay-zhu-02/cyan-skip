package app.cyanskip.app.ui.snapshot

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import app.cyanskip.app.platform.PlatformResult
import app.cyanskip.app.resources.Res
import app.cyanskip.app.resources.delete_success
import app.cyanskip.app.resources.platform_action_unsupported
import app.cyanskip.app.resources.save_success
import app.cyanskip.app.resources.saving_progress
import app.cyanskip.app.resources.screenshot_replace_success
import app.cyanskip.app.resources.screenshot_size_mismatch
import app.cyanskip.app.resources.snapshot_delete
import app.cyanskip.app.resources.snapshot_delete_confirmation
import app.cyanskip.app.snapshot.SnapshotStore
import app.cyanskip.app.snapshot.platform.saveImageToAlbum
import app.cyanskip.app.storage.FileExports
import app.cyanskip.app.storage.readFileBytes
import app.cyanskip.app.ui.MainViewModel
import app.cyanskip.app.ui.component.DialogRequests
import app.cyanskip.app.ui.platform.UiHost
import app.cyanskip.app.ui.share.launchUi
import app.cyanskip.app.ui.text.displayMessage
import app.cyanskip.app.util.LogUtils
import app.cyanskip.app.util.ToastUtils
import app.cyanskip.db.Snapshot
import org.jetbrains.compose.resources.getString

/** Both hosts use the same confirmation and feedback; only native file actions differ. */
class SnapshotActionCoordinator(
    private val platform: SnapshotPlatformActions,
    private val scope: CoroutineScope,
    private val dialogs: DialogRequests,
    private val fileScope: CoroutineScope,
    private val onReplaced: (Long) -> Unit,
    private val onBeforeDelete: () -> Unit,
    private val onDeleteFailed: () -> Unit,
) : SnapshotActions {

    override fun share(snapshot: Snapshot) {
        fileScope.launchUi {
            // A share target may read the archive after this call returns; cache expiry owns cleanup.
            val archive =
                SnapshotStore.createArchive(snapshot.id, snapshot.appId, snapshot.activityId)
            if (platform.share(archive) == PlatformResult.Unsupported) {
                SnapshotStore.deleteArchive(archive)
                ToastUtils.show(getString(Res.string.platform_action_unsupported))
            }
        }
    }

    override fun replace(snapshot: Snapshot) {
        fileScope.launchUi {
            val source = platform.pickImage() ?: return@launchUi
            if (SnapshotStore.replaceScreenshot(snapshot, readFileBytes(source))) {
                onReplaced(snapshot.id)
                ToastUtils.show(getString(Res.string.screenshot_replace_success))
            } else ToastUtils.show(getString(Res.string.screenshot_size_mismatch))
        }
    }

    override fun saveToDownloads(snapshot: Snapshot) = saveOne(snapshot, false)
    override fun saveToAlbum(snapshot: Snapshot) = saveOne(snapshot, true)

    private fun saveOne(snapshot: Snapshot, album: Boolean) {
        fileScope.launchUi {
            if (!platform.ensureSavePermission()) return@launchUi
            ToastUtils.show(getString(Res.string.saving_progress))
            when (val result = save(snapshot, album)) {
                is PlatformResult.Success -> if (result.value) ToastUtils.show(getString(Res.string.save_success))
                PlatformResult.Unsupported -> ToastUtils.show(getString(Res.string.platform_action_unsupported))
            }
        }
    }

    override suspend fun saveSelectedToDownloads(snapshots: List<Snapshot>) =
        saveSelected(snapshots, false)

    override suspend fun saveSelectedToAlbum(snapshots: List<Snapshot>) =
        saveSelected(snapshots, true)

    private suspend fun saveSelected(snapshots: List<Snapshot>, album: Boolean): Int? {
        if (!platform.ensureSavePermission()) return null
        ToastUtils.show(getString(Res.string.saving_progress))
        var saved = 0
        for (snapshot in snapshots) {
            try {
                when (val result = save(snapshot, album)) {
                    is PlatformResult.Success -> if (result.value) saved++ else break
                    PlatformResult.Unsupported -> {
                        ToastUtils.show(getString(Res.string.platform_action_unsupported))
                        return null
                    }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                LogUtils.d(e)
            }
        }
        return saved
    }

    private suspend fun save(snapshot: Snapshot, album: Boolean): PlatformResult<Boolean> =
        if (album) {
            when (saveImageToAlbum(SnapshotStore.screenshotFile(snapshot.id))) {
                is PlatformResult.Success -> PlatformResult.Success(true)
                PlatformResult.Unsupported -> PlatformResult.Unsupported
            }
        } else {
            FileExports.withTemporaryFile(
                create = {
                    SnapshotStore.createArchive(
                        snapshot.id,
                        snapshot.appId,
                        snapshot.activityId
                    )
                },
                delete = SnapshotStore::deleteArchive,
                consume = platform::save,
            )
        }

    override fun delete(snapshot: Snapshot) {
        scope.launch {
            try {
                if (dialogs.confirm(
                        title = getString(Res.string.snapshot_delete),
                        text = getString(Res.string.snapshot_delete_confirmation),
                        error = true,
                    )
                ) {
                    onBeforeDelete()
                    try {
                        SnapshotStore.delete(snapshot)
                    } catch (e: CancellationException) {
                        throw e
                    } catch (e: Exception) {
                        onDeleteFailed(); throw e
                    }
                    ToastUtils.show(getString(Res.string.delete_success))
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                ToastUtils.show(e.displayMessage())
            }
        }
    }
}

fun UiHost.snapshotActions(
    mainVm: MainViewModel,
    vm: SnapshotViewModel,
    onReplaced: (Long) -> Unit,
    beforeDelete: () -> Unit,
    deleteFailed: () -> Unit,
): SnapshotActions {
    return SnapshotActionCoordinator(
        platform = snapshotPlatformActions(),
        scope = mainVm.scope,
        dialogs = mainVm.dialogRequests,
        fileScope = vm.scope,
        onReplaced = onReplaced,
        onBeforeDelete = beforeDelete,
        onDeleteFailed = deleteFailed,
    )
}
