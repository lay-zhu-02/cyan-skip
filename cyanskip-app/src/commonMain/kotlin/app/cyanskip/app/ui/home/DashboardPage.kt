package app.cyanskip.app.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.cyanskip.app.resources.*
import app.cyanskip.app.rule.RuleGroupSnapshot
import app.cyanskip.app.rule.ruleGroupState
import app.cyanskip.app.settings.SettingsRepository
import app.cyanskip.app.state.Loadable
import app.cyanskip.app.ui.MainViewModel
import app.cyanskip.app.ui.component.*
import app.cyanskip.app.ui.navigation.LiteSettingsRoute
import app.cyanskip.app.ui.platform.openA11ySettings
import app.cyanskip.app.ui.platform.openRecents
import app.cyanskip.app.ui.settings.appVersion
import app.cyanskip.app.ui.style.itemHorizontalPadding
import app.cyanskip.app.ui.style.surfaceCardColors
import org.jetbrains.compose.resources.stringResource

@Composable
fun dashboardSummary(state: Loadable<RuleGroupSnapshot>, actionCount: Long): String =
    when (state) {
        Loadable.Loading -> ""
        is Loadable.Failure -> stringResource(Res.string.data_load_failed)
        is Loadable.Ready -> {
            val groups = state.value.groups
            val rules =
                listOfNotNull(
                        if (groups.globalGroups.isNotEmpty())
                            stringResource(
                                Res.string.subscription_global_count,
                                groups.globalGroups.size.toString(),
                            )
                        else null,
                        if (groups.appGroupSize > 0)
                            stringResource(
                                Res.string.subscription_app_rule_counts,
                                groups.appSize.toString(),
                                groups.appGroupSize.toString(),
                            )
                        else null,
                    )
                    .joinToString("/")
                    .ifEmpty { stringResource(Res.string.rules_empty) }
            if (actionCount > 0)
                stringResource(
                    Res.string.action_count_summary,
                    rules,
                    actionCount.toString(),
                )
            else rules
        }
    }

@Composable
fun dashboardPage(vm: HomeViewModel): ScaffoldExt {
    val mainVm = MainViewModel.requireCurrent()
    val appName = appVersion().appName
    val store by SettingsRepository.settings.collectAsStateWithLifecycle()
    val platform = dashboardPlatformState()
    val rules by ruleGroupState.collectAsStateWithLifecycle()
    val actionCount by SettingsRepository.actionCount.collectAsStateWithLifecycle()
    val rulesReady = rules.value?.groups?.let { it.appGroupSize > 0 || it.globalGroups.isNotEmpty() } == true
    val serviceReady = platform.a11yEnabled
    val ready = rulesReady && serviceReady && store.enableMatch
    val progress = listOf(rulesReady, serviceReady, store.enableMatch).count { it }
    val scroll = rememberColumnScrollState()
    ResetPageScrollOnRequest(vm.homeState, BottomNavItem.Dashboard, scroll::resetScrollAndAwait)
    return ScaffoldExt(
        BottomNavItem.Dashboard,
        modifier = Modifier.nestedScroll(scroll.scrollBehavior.nestedScrollConnection),
        topBar = {
            CsTopAppBar(
                scrollBehavior = scroll.scrollBehavior,
                title = { Text(appName) },
                actions = {
                    CsIconButton(
                        imageVector = CsIcons.Settings,
                        contentDescription = stringResource(Res.string.settings_title),
                        onClick = { mainVm.navigator.navigate(LiteSettingsRoute) },
                    )
                },
            )
        },
    ) { padding ->
        Column(
            Modifier.verticalScroll(scroll.scrollState).padding(padding)
                .padding(horizontal = itemHorizontalPadding),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(stringResource(Res.string.lite_tagline),
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.padding(vertical = 12.dp))
            Text(stringResource(Res.string.lite_intro),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(
                stringResource(if (ready) Res.string.lite_setup_ready else Res.string.lite_setup_progress,
                    progress.toString()),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
            )
            SetupStepCard(
                number = 1,
                title = stringResource(Res.string.lite_setup_accessibility),
                status = stringResource(if (serviceReady) Res.string.lite_step_done else Res.string.lite_step_todo),
                complete = serviceReady,
                checked = serviceReady,
                onClickLabel = stringResource(Res.string.lite_setup_accessibility),
                onClick = ::openA11ySettings,
                hint = if (serviceReady) null else {
                    {
                        SetupStepList(
                            steps = listOf(
                                stringResource(Res.string.lite_accessibility_step_open),
                                stringResource(Res.string.lite_accessibility_step_find),
                                stringResource(Res.string.lite_accessibility_step_enable),
                                stringResource(Res.string.lite_accessibility_step_back),
                            ),
                            note = stringResource(Res.string.lite_accessibility_step_note),
                        )
                    }
                },
            )
            SetupStepCard(
                number = 2,
                title = stringResource(Res.string.lite_matching),
                status = stringResource(if (store.enableMatch) Res.string.lite_step_done else Res.string.lite_setup_paused),
                complete = store.enableMatch,
                checked = store.enableMatch,
                onClickLabel = stringResource(Res.string.item_toggle_description, stringResource(Res.string.lite_matching)),
                onClick = vm::toggleMatching,
                hint = { SetupStepHintText(stringResource(Res.string.lite_setup_switch_hint)) },
            )
            val subscribing by vm.defaultSubscriptionBusy.collectAsStateWithLifecycle()
            SetupStepCard(
                number = 3,
                title = stringResource(Res.string.lite_setup_rules),
                status = stringResource(when {
                    subscribing || rules is Loadable.Loading -> Res.string.loading_progress
                    rules is Loadable.Failure -> Res.string.data_load_failed
                    rulesReady -> Res.string.lite_step_done
                    else -> Res.string.lite_step_todo
                }),
                complete = rulesReady,
                enabled = !subscribing,
                onClickLabel = stringResource(if (rulesReady) Res.string.lite_rules else Res.string.lite_default_subscription),
                onClick = {
                    if (rulesReady) vm.homeState.selectTab(BottomNavItem.SubsManage)
                    else vm.subscribeDefault()
                },
                hint = {
                    SetupStepHintText(
                        if (rulesReady) dashboardSummary(rules, actionCount)
                        else stringResource(Res.string.lite_default_subscription_hint)
                    )
                },
            )
            Text(stringResource(Res.string.lite_setup_no_match),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(vertical = 4.dp))
            Text(
                stringResource(Res.string.lite_optional_section),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(top = 8.dp),
            )
            CsTextSwitch(
                title = stringResource(Res.string.lite_hide_recents),
                subtitle = stringResource(Res.string.lite_hide_recents_hint),
                checked = store.excludeFromRecents,
                onCheckedChange = { value ->
                    SettingsRepository.updateSettings { it.copy(excludeFromRecents = value) }
                },
            )
            OptionalActionCard(
                title = stringResource(Res.string.lite_lock_task),
                hint = stringResource(Res.string.lite_lock_task_hint),
                onClick = ::openRecents,
            )
            CsPageBottomSpace()
        }
    }
}

@Composable
private fun SetupStepCard(
    number: Int,
    title: String,
    status: String,
    complete: Boolean,
    checked: Boolean? = null,
    enabled: Boolean = true,
    onClickLabel: String,
    onClick: () -> Unit,
    hint: (@Composable () -> Unit)? = null,
) {
    Card(
        modifier = Modifier.fillMaxWidth().semantics(mergeDescendants = true) {
            this.onClick(label = onClickLabel, action = null)
            stateDescription = status
        },
        shape = MaterialTheme.shapes.large,
        colors = surfaceCardColors,
        onClick = onClick,
        enabled = enabled,
    ) {
        Row(Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(Modifier.size(32.dp).clip(CircleShape)
                .background(if (complete) MaterialTheme.colorScheme.primaryContainer
                    else MaterialTheme.colorScheme.surfaceContainerHighest),
                contentAlignment = Alignment.Center) {
                if (complete) CsIcon(CsIcons.CheckCircle, contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(22.dp))
                else Text(number.toString(), style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(title, style = MaterialTheme.typography.titleMedium)
                Text(status, style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary)
            }
            if (checked != null) CsSwitch(checked = checked, onCheckedChange = null)
            else CsIcon(CsIcons.KeyboardArrowRight, contentDescription = null)
        }
        if (hint != null) Column(
            Modifier.padding(start = 16.dp, end = 16.dp, bottom = 16.dp),
        ) { hint() }
    }
}

@Composable
private fun SetupStepHintText(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

/** Numbered hints continue the step numbering, so opening system settings counts as the first step. */
@Composable
private fun SetupStepList(
    steps: List<String>,
    note: String,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        steps.forEachIndexed { index, step ->
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.Top,
            ) {
                Box(
                    Modifier.size(20.dp).clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        (index + 1).toString(),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                }
                Text(
                    step,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f),
                )
            }
        }
        if (note.isNotEmpty()) Text(
            note,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun OptionalActionCard(
    title: String,
    hint: String,
    onClick: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = surfaceCardColors,
        onClick = onClick,
    ) {
        Row(
            Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(title, style = MaterialTheme.typography.titleMedium)
                Text(
                    hint,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            CsIcon(CsIcons.KeyboardArrowRight, contentDescription = null)
        }
    }
}
