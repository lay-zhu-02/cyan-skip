package app.cyanskip.app.ui.crash

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.Card
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import app.cyanskip.app.crash.CrashData
import app.cyanskip.app.network.AppLinks
import app.cyanskip.app.resources.Res
import app.cyanskip.app.resources.android_version_description
import app.cyanskip.app.resources.crash_app_description
import app.cyanskip.app.resources.crash_details_collapse
import app.cyanskip.app.resources.crash_details_expand
import app.cyanskip.app.resources.crash_device_description
import app.cyanskip.app.resources.crash_report_delete
import app.cyanskip.app.resources.crash_report_delete_confirmation
import app.cyanskip.app.resources.crash_report_delete_current
import app.cyanskip.app.resources.crash_reports
import app.cyanskip.app.resources.crash_reports_clear
import app.cyanskip.app.resources.crash_reports_clear_confirmation
import app.cyanskip.app.resources.crash_reports_empty
import app.cyanskip.app.resources.crash_thread_description
import app.cyanskip.app.resources.crash_version_thread
import app.cyanskip.app.resources.delete_success
import app.cyanskip.app.resources.exception_message_empty
import app.cyanskip.app.resources.exception_unknown
import app.cyanskip.app.resources.feedback_report
import app.cyanskip.app.resources.logs_export
import app.cyanskip.app.resources.stack_trace
import app.cyanskip.app.state.Loadable
import app.cyanskip.app.time.format
import app.cyanskip.app.ui.MainViewModel
import app.cyanskip.app.ui.component.CsCopyTextCard
import app.cyanskip.app.ui.component.CsEmptyState
import app.cyanskip.app.ui.component.CsExpandableSection
import app.cyanskip.app.ui.component.CsFixedTimeText
import app.cyanskip.app.ui.component.CsIconButton
import app.cyanskip.app.ui.component.CsIcons
import app.cyanskip.app.ui.component.CsPageBottomSpace
import app.cyanskip.app.ui.component.CsScaffold
import app.cyanskip.app.ui.component.CsTopAppBar
import app.cyanskip.app.ui.component.rememberListScrollState
import app.cyanskip.app.ui.share.noRippleClickable
import app.cyanskip.app.ui.style.itemHorizontalPadding
import app.cyanskip.app.ui.style.itemVerticalPadding
import app.cyanskip.app.ui.style.scaffoldPadding
import app.cyanskip.app.ui.style.surfaceCardColors
import app.cyanskip.app.ui.text.displayMessage
import app.cyanskip.app.util.ToastUtils
import app.cyanskip.app.util.copyText
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.stringResource

@Composable
fun CrashReportPage() {
    val mainVm = MainViewModel.requireCurrent()
    val vm = viewModel { CrashReportViewModel(mainVm.takeCrashDataList()) }
    val records by vm.crashDataState.collectAsStateWithLifecycle()
    val expandedCrashId by vm.expandedCrashId.collectAsStateWithLifecycle()
    fun delete(record: CrashData?) {
        vm.scope.launch {
            try {
                if (
                    mainVm.dialogRequests.confirm(
                        title =
                            getString(
                                if (record == null) Res.string.crash_reports_clear
                                else Res.string.crash_report_delete
                            ),
                        text =
                            getString(
                                if (record == null) Res.string.crash_reports_clear_confirmation
                                else Res.string.crash_report_delete_confirmation
                            ),
                        error = true,
                    )
                ) {
                    if (record == null) vm.deleteAllCrashes() else vm.deleteCrash(record)
                    if (record == null || expandedCrashId == record.id) vm.setExpandedCrashId(null)
                    ToastUtils.show(getString(Res.string.delete_success))
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                ToastUtils.show(e.displayMessage())
            }
        }
    }

    val crashDataList = records.value.orEmpty()
    val pageScrollState = rememberListScrollState()
    val scrollBehavior = pageScrollState.scrollBehavior
    val listState = pageScrollState.listState
    pageScrollState.ResetOnChange(crashDataList.isNotEmpty())
    CsScaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            CsTopAppBar(
                scrollBehavior = scrollBehavior,
                navigationIcon = {
                    CsIconButton(
                        imageVector = CsIcons.ArrowBack,
                        onClick = mainVm.navigator::pop,
                    )
                },
                title = {
                    Text(
                        text = stringResource(Res.string.crash_reports),
                        modifier =
                            Modifier.noRippleClickable(onClick = pageScrollState::resetScroll),
                    )
                },
                actions = {
                    if (crashDataList.isNotEmpty()) {
                        CsIconButton(
                            imageVector = CsIcons.Delete,
                            contentDescription = stringResource(Res.string.crash_reports_clear),
                            onClick = { delete(null) },
                        )
                    }
                },
            )
        },
        bottomBar = {
            if (crashDataList.isNotEmpty()) {
                BottomAppBar {
                    Spacer(modifier = Modifier.weight(1f))
                    TextButton(onClick = { mainVm.textDialog.showUrl(AppLinks.Issues) }) {
                        Text(text = stringResource(Res.string.feedback_report))
                    }
                    Spacer(modifier = Modifier.width(itemHorizontalPadding))
                    TextButton(onClick = mainVm.shareLog::show) {
                        Text(text = stringResource(Res.string.logs_export))
                    }
                    Spacer(modifier = Modifier.width(itemHorizontalPadding))
                }
            }
        },
    ) { contentPadding ->
        LazyColumn(
            modifier = Modifier.scaffoldPadding(contentPadding).fillMaxSize(),
            state = listState,
            verticalArrangement = Arrangement.spacedBy(itemVerticalPadding),
        ) {
            items(
                items = crashDataList,
                key = { it.id },
            ) { crashData ->
                val expanded = expandedCrashId == crashData.id
                CrashReportCard(
                    crashData = crashData,
                    onCopy = ::copyText,
                    expanded = expanded,
                    onToggle = {
                        vm.setExpandedCrashId(if (expanded) null else crashData.id)
                    },
                    onDelete = { delete(crashData) },
                )
            }
            item(key = "crash-report-footer") {
                if (crashDataList.isEmpty() && records !is Loadable.Loading) {
                    CsEmptyState(
                        text = stringResource(Res.string.crash_reports_empty)
                    )
                }
                CsPageBottomSpace()
            }
        }
    }
}

@Composable
private fun CrashReportCard(
    crashData: CrashData,
    expanded: Boolean,
    onToggle: () -> Unit,
    onDelete: () -> Unit,
    onCopy: (String) -> Unit,
) {
    val exceptionName =
        crashData.name.substringAfterLast('.').ifBlank {
            stringResource(Res.string.exception_unknown)
        }
    val message = crashData.message?.takeIf { it.isNotBlank() }
    val timeText =
        remember(crashData.mtime) {
            crashData.mtime.format("yyyy-MM-dd HH:mm:ss")
        }
    val supportingColor = MaterialTheme.colorScheme.onSurfaceVariant
    Card(
        modifier = Modifier.padding(horizontal = itemHorizontalPadding / 2).fillMaxWidth(),
        shape = MaterialTheme.shapes.extraSmall,
        colors = surfaceCardColors,
    ) {
        CsExpandableSection(
            expanded = expanded,
            onToggle = onToggle,
            expandLabel = stringResource(Res.string.crash_details_expand),
            collapseLabel = stringResource(Res.string.crash_details_collapse),
            header = { indicator ->
                Column(
                    modifier = Modifier.fillMaxWidth().padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = exceptionName,
                            modifier = Modifier.weight(1f),
                            style = MaterialTheme.typography.titleMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        indicator()
                    }
                    Text(
                        text = message ?: stringResource(Res.string.exception_message_empty),
                        style = MaterialTheme.typography.bodyMedium,
                        color =
                            if (message == null) {
                                MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                            } else {
                                MaterialTheme.colorScheme.onSurface
                            },
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text =
                                stringResource(
                                    Res.string.crash_version_thread,
                                    crashData.versionName,
                                    crashData.versionCode,
                                    crashData.thread,
                                ),
                            modifier = Modifier.weight(1f),
                            style = MaterialTheme.typography.bodySmall,
                            color = supportingColor,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        CsFixedTimeText(
                            text = timeText,
                            style = MaterialTheme.typography.bodySmall,
                            color = supportingColor,
                        )
                    }
                }
            },
        ) {
            HorizontalDivider()
            Column(
                modifier = Modifier.padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Box(modifier = Modifier.fillMaxWidth()) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text =
                                stringResource(
                                    Res.string.crash_device_description,
                                    crashData.device,
                                ),
                            style = MaterialTheme.typography.bodySmall,
                            color = supportingColor,
                        )
                        Text(
                            text =
                                stringResource(
                                    Res.string.android_version_description,
                                    crashData.androidVersionName,
                                    crashData.androidVersionCode,
                                ),
                            modifier = Modifier.padding(end = 56.dp),
                            style = MaterialTheme.typography.bodySmall,
                            color = supportingColor,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            text =
                                stringResource(
                                    Res.string.crash_app_description,
                                    crashData.versionName,
                                    crashData.versionCode,
                                ),
                            modifier = Modifier.padding(end = 56.dp),
                            style = MaterialTheme.typography.bodySmall,
                            color = supportingColor,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            text =
                                stringResource(
                                    Res.string.crash_thread_description,
                                    crashData.thread,
                                ),
                            modifier = Modifier.padding(end = 56.dp),
                            style = MaterialTheme.typography.bodySmall,
                            color = supportingColor,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    Box(modifier = Modifier.align(Alignment.BottomEnd)) {
                        CsIconButton(
                            imageVector = CsIcons.Delete,
                            onClick = onDelete,
                            contentDescription =
                                stringResource(Res.string.crash_report_delete_current),
                        )
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = stringResource(Res.string.stack_trace),
                    style = MaterialTheme.typography.titleSmall,
                )
                CsCopyTextCard(
                    text = crashData.stackTrace,
                    onCopy = onCopy,
                    modifier = Modifier.heightIn(max = 320.dp),
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    textStyle = MaterialTheme.typography.bodySmall,
                )
            }
        }
    }
}
