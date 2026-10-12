package app.cyanskip.app.ui.home

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.Alignment
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.cyanskip.app.resources.*
import app.cyanskip.app.settings.SettingsRepository
import app.cyanskip.app.state.Loadable
import app.cyanskip.app.subscription.SubscriptionRepository
import app.cyanskip.app.subscription.SubscriptionResult
import app.cyanskip.app.ui.MainViewModel
import app.cyanskip.app.ui.component.*
import app.cyanskip.app.ui.navigation.*
import app.cyanskip.app.ui.option.UpdateTimeOption
import app.cyanskip.app.ui.option.findOption
import app.cyanskip.app.ui.share.launchUi
import app.cyanskip.app.ui.subscription.message
import app.cyanskip.app.ui.text.subscriptionMessageResource
import app.cyanskip.app.util.ToastUtils
import app.cyanskip.db.LOCAL_SUBS_ID
import org.jetbrains.compose.resources.stringResource

@Composable
fun subsManagePage(vm: HomeViewModel, onNavigateBack: (() -> Unit)? = null): ScaffoldExt {
    val mainVm = MainViewModel.requireCurrent()
    val loadable by vm.subscriptionsState.collectAsStateWithLifecycle()
    val state = loadable.value
    val store by SettingsRepository.settings.collectAsStateWithLifecycle()
    val refreshing by SubscriptionRepository.updating.collectAsStateWithLifecycle()
    var settingsMenu by remember { mutableStateOf(false) }
    fun refresh(id: Long? = null) {
        vm.scope.launchUi {
            SubscriptionRepository.refresh(id).message()?.let { ToastUtils.show(it) }
        }
    }
    fun addSubscription() {
        vm.scope.launchUi {
            val url = mainVm.subsLinkDialog.request() ?: return@launchUi
            val result = SubscriptionRepository.addOrModifyRemote(url)
            result.message()?.let { message ->
                if (result is SubscriptionResult.Failure) mainVm.textDialog.showText("$url\n\n$message")
                else ToastUtils.show(message)
            }
        }
    }
    if (settingsMenu) {
        CsSettingsDialog(onDismissRequest = { settingsMenu = false },
            title = stringResource(Res.string.subscription_settings)) {
            CsTextMenu(title = stringResource(Res.string.subscriptions_update),
                option = UpdateTimeOption.objects.findOption(store.updateSubsInterval),
                onOptionChange = { vm.setUpdateInterval(it.value) })
        }
    }
    return ScaffoldExt(
        navItem = BottomNavItem.SubsManage,
        topBar = {
            CsTopAppBar(title = { Text(stringResource(Res.string.lite_rules)) },
                navigationIcon = {
                    if (onNavigateBack != null) CsIconButton(CsIcons.ArrowBack, onNavigateBack)
                },
                actions = {
                    CsIconButton(CsIcons.Autorenew, enabled = !refreshing, onClick = { refresh() },
                        contentDescription = stringResource(Res.string.lite_refresh_rules))
                    CsIconButton(CsIcons.Settings, onClick = { settingsMenu = true },
                        contentDescription = stringResource(Res.string.subscription_settings))
                })
        },
    ) { padding ->
        Column(Modifier.padding(padding)) {
            CsMatchingBanner(store.enableMatch, vm::enableMatching)
            if (refreshing) LinearProgressIndicator(Modifier.fillMaxWidth())
            LazyColumn(Modifier.fillMaxSize()) {
                if (loadable !is Loadable.Ready) {
                    item {
                        Text((loadable as? Loadable.Failure)?.cause?.subscriptionMessageResource()
                            ?: stringResource(Res.string.loading_progress), Modifier.padding(20.dp))
                    }
                }
                items(state?.subItems.orEmpty(), key = { it.id }) { item ->
                    val subscription = state?.subscriptions?.get(item.id)
                    val local = item.id == LOCAL_SUBS_ID
                    val title = if (local) stringResource(Res.string.lite_local_rules_title)
                        else subscription?.name ?: item.id.toString()
                    Card(Modifier.padding(horizontal = 16.dp, vertical = 6.dp).fillMaxWidth().heightIn(min = RuleSourceCardMinHeight),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)) {
                        Column(Modifier.padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Column(Modifier.weight(1f).padding(end = 12.dp)) {
                                    Text(title, style = MaterialTheme.typography.titleMedium)
                                    Text(stringResource(if (local) Res.string.lite_local_source_hint else Res.string.lite_remote_source_hint),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                CsSwitch(key = item.id, checked = item.enable,
                                    modifier = Modifier.semantics { contentDescription = title },
                                    onCheckedChange = { enabled -> vm.scope.launchUi {
                                        vm.requestSubscriptionEnabled(item, enabled)
                                    } })
                            }
                            Text(stringResource(Res.string.lite_source_counts,
                                (subscription?.apps?.size ?: 0).toString(),
                                (subscription?.appGroups?.size ?: 0).toString(),
                                (subscription?.globalGroups?.size ?: 0).toString()),
                                Modifier.padding(top = 8.dp), style = MaterialTheme.typography.bodyMedium)
                            val error = state?.loadErrors?.get(item.id) ?: state?.refreshErrors?.get(item.id)
                            if (error != null) Text(error.subscriptionMessageResource(),
                                Modifier.padding(top = 8.dp), color = MaterialTheme.colorScheme.error)
                            FlowRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                TextButton(onClick = { mainVm.navigator.navigate(SubsAppListRoute(item.id)) }) {
                                    Text(stringResource(Res.string.app_rules))
                                }
                                TextButton(onClick = { mainVm.navigator.navigate(SubsGlobalGroupListRoute(item.id)) }) {
                                    Text(stringResource(Res.string.lite_global_rules))
                                }
                                if (local) {
                                    CsIconButton(CsIcons.Add,
                                        contentDescription = stringResource(Res.string.lite_add_local_rule),
                                        onClick = { mainVm.navigator.navigate(
                                            UpsertRuleGroupRoute(LOCAL_SUBS_ID, appId = "", forward = true)) })
                                } else {
                                    CsIconButton(CsIcons.Autorenew, enabled = !refreshing,
                                        contentDescription = stringResource(Res.string.lite_update_source),
                                        onClick = { refresh(item.id) })
                                }
                                CsIconButton(CsIcons.MoreVert,
                                    contentDescription = stringResource(Res.string.lite_manage_source),
                                    onClick = { mainVm.subsSheet.show(item.id) })
                            }
                        }
                    }
                }
                item(key = "add-subscription") { AddSubscriptionCard(::addSubscription) }
                csPageBottomSpace()
            }
        }
    }
}

private val RuleSourceCardMinHeight = 164.dp

@Composable
private fun AddSubscriptionCard(onClick: () -> Unit) {
    val outline = MaterialTheme.colorScheme.outline
    Card(
        onClick = onClick,
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
            .fillMaxWidth().heightIn(min = RuleSourceCardMinHeight)
            .drawWithCache {
                val stroke = 1.dp.toPx()
                val dash = PathEffect.dashPathEffect(floatArrayOf(6.dp.toPx(), 4.dp.toPx()))
                onDrawWithContent {
                    drawContent()
                    drawRoundRect(outline, topLeft = Offset(stroke / 2, stroke / 2),
                        size = Size(size.width - stroke, size.height - stroke),
                        cornerRadius = CornerRadius(12.dp.toPx()),
                        style = Stroke(width = stroke, pathEffect = dash))
                }
            },
    ) {
        Column(Modifier.fillMaxWidth().heightIn(min = RuleSourceCardMinHeight).padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterVertically)) {
            CsIcon(CsIcons.Add, modifier = Modifier.size(32.dp), contentDescription = null,
                tint = MaterialTheme.colorScheme.primary)
            Text(stringResource(Res.string.lite_add_subscription), style = MaterialTheme.typography.titleMedium)
            Text(stringResource(Res.string.lite_add_subscription_hint),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
