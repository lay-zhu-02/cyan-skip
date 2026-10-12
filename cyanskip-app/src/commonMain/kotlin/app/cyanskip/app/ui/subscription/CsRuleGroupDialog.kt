package app.cyanskip.app.ui.subscription

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.width
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.contentDescription
import app.cyanskip.app.ui.component.CsTriStateSwitch
import app.cyanskip.app.ui.icon.ResetSettings
import app.cyanskip.app.resources.settings_reset_default
import app.cyanskip.app.resources.global_rule_default_apps_follow_enabled
import app.cyanskip.app.resources.global_rule_default_apps_follow_disabled
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.ui.Alignment
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.cyanskip.app.model.ExcludeData
import app.cyanskip.app.resources.Res
import app.cyanskip.app.resources.global_rule_default_apps_enabled
import app.cyanskip.app.resources.global_rule_default_apps_disabled
import app.cyanskip.app.resources.global_rule_default_apps
import app.cyanskip.app.resources.config_loading
import app.cyanskip.app.resources.dialog_close
import app.cyanskip.app.resources.rule_content
import app.cyanskip.app.resources.rule_delete
import app.cyanskip.app.resources.rule_edit
import app.cyanskip.app.resources.rule_enable
import app.cyanskip.app.resources.rule_enable_in_app
import app.cyanskip.app.resources.rule_key_description
import app.cyanskip.app.resources.rule_pre_keys_description
import app.cyanskip.app.resources.rule_source
import app.cyanskip.app.resources.rule_view_examples
import app.cyanskip.app.resources.rule_view_parent_list
import app.cyanskip.app.resources.rule_view_source
import app.cyanskip.app.resources.subscription_settings
import app.cyanskip.app.rule.RuleControlState
import app.cyanskip.app.rule.toRuleGroupTarget
import app.cyanskip.app.rule.RuleSetting
import app.cyanskip.app.subscription.RawSubscription
import app.cyanskip.app.ui.component.CsCopyableText
import app.cyanskip.app.ui.component.CsGroupNameText
import app.cyanskip.app.ui.component.CsIcon
import app.cyanskip.app.ui.component.CsIconButton
import app.cyanskip.app.ui.component.CsIcons
import app.cyanskip.app.ui.component.CsLazyCopyableText
import app.cyanskip.app.ui.component.CsRuleExclusionsCard
import app.cyanskip.app.ui.component.CsRuleSettingsContent
import app.cyanskip.app.ui.component.CsRuleSettingsSheet
import app.cyanskip.app.ui.component.CsScaffold
import app.cyanskip.app.ui.component.CsTopAppBar
import app.cyanskip.app.ui.component.CsTwoLineText
import app.cyanskip.app.ui.component.removeRuleCategoryPrefix
import app.cyanskip.app.ui.icon.RuleList
import app.cyanskip.app.ui.navigation.AppNavigator
import app.cyanskip.app.ui.navigation.ImagePreviewItem
import app.cyanskip.app.ui.navigation.ImagePreviewRoute
import app.cyanskip.app.ui.navigation.SubsAppGroupListRoute
import app.cyanskip.app.ui.navigation.SubsCategoryGroupRoute
import app.cyanskip.app.ui.navigation.SubsGlobalGroupListRoute
import app.cyanskip.app.ui.platform.CsFullscreenDialog
import app.cyanskip.app.ui.share.LocalDarkTheme
import app.cyanskip.app.ui.style.JSON5_LARGE_TEXT_THRESHOLD
import app.cyanskip.app.ui.style.getJson5AnnotatedString
import app.cyanskip.app.ui.style.scaffoldPadding
import app.cyanskip.app.ui.text.getSync
import app.cyanskip.app.util.copyText
import org.jetbrains.compose.resources.stringResource

@Composable
fun CsRuleGroupDialog(
    navigator: AppNavigator,
    openSubscription: (Long) -> Unit,
    ruleControl: RuleControlDialogState,
    subs: RawSubscription,
    group: RawSubscription.RawGroupProps,
    appId: String?,
    excludeData: ExcludeData,
    excludeAppId: String?,
    onDismissRequest: () -> Unit,
    onClickEdit: () -> Unit = {},
    onClickEditExclude: () -> Unit,
    control: RuleControlState?,
    onSettingChange: (RuleSetting) -> Unit,
    matchAnyApp: Boolean?,
    onMatchAnyAppChange: (Boolean?) -> Unit,
    onClickDelete: () -> Unit = {},
    sheetState: SheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
) {
    // Keep the header width stable during dismissal after navigating to the list.
    val sourceRoute = remember { navigator.topRoute }
    val category = if (group is RawSubscription.RawAppGroup) {
        subs.getCategory(group.name)?.takeIf { it.name.isNotBlank() }
    } else null
    val title = category?.let { group.name.removeRuleCategoryPrefix(it.name) }
        ?.takeIf { it.isNotEmpty() } ?: group.name
    val targetRoute = remember(subs.id, appId, group.groupType, group.key) {
        if (group is RawSubscription.RawGlobalGroup) {
            SubsGlobalGroupListRoute(
                subsItemId = subs.id,
                focusGroupKey = group.key
            )
        } else {
            SubsAppGroupListRoute(
                subsItemId = subs.id,
                appId = appId.toString(),
                focusGroupKey = group.key
            )
        }
    }
    val inTargetList = when (val current = sourceRoute) {
        is SubsAppGroupListRoute -> group is RawSubscription.RawAppGroup &&
                current.subsItemId == subs.id && current.appId == appId

        is SubsGlobalGroupListRoute -> group is RawSubscription.RawGlobalGroup &&
                current.subsItemId == subs.id

        else -> false
    }
    var menu by remember { mutableStateOf(false) }
    var showSource by remember(group) { mutableStateOf(false) }
    CsRuleSettingsSheet(
        sheetState = sheetState,
        title = title,
        titleContent = {
            CsGroupNameText(
                text = title,
                isGlobal = group is RawSubscription.RawGlobalGroup,
                style = MaterialTheme.typography.titleMedium,
            )
        },
        subtitle = group.desc?.takeIf { it.isNotBlank() },
        footerLeadingContent = if (category != null) {
            {
                AssistChip(
                    onClick = {
                        val route = SubsCategoryGroupRoute(subs.id, category.key)
                        onDismissRequest()
                        navigator.navigate(route)
                    },
                    label = { Text(category.name, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                    modifier = Modifier.minimumInteractiveComponentSize(),
                    trailingIcon = {
                        CsIcon(
                            CsIcons.KeyboardArrowRight, Modifier.size(16.dp),
                            contentDescription = null
                        )
                    },
                    shape = MaterialTheme.shapes.extraLarge,
                    colors = AssistChipDefaults.assistChipColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                        labelColor = MaterialTheme.colorScheme.onSecondaryContainer,
                        trailingIconContentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                    ),
                    border = null,
                )
            }
        } else null,
        onDismissRequest = onDismissRequest,
        actions = {
            if (!inTargetList) {
                CsIconButton(
                    imageVector = RuleList,
                    contentDescription = stringResource(Res.string.rule_view_parent_list),
                    modifier = Modifier.size(48.dp),
                    onClick = {
                        onDismissRequest()
                        navigator.navigate(targetRoute)
                    },
                )
            }
            if (subs.isLocal) {
                CsIconButton(
                    imageVector = CsIcons.Edit,
                    contentDescription = stringResource(Res.string.rule_edit),
                    modifier = Modifier.size(48.dp),
                    onClick = onClickEdit,
                )
            }
            Box {
                CsIconButton(
                    imageVector = CsIcons.MoreVert, modifier = Modifier.size(48.dp),
                    onClick = { menu = true })
                DropdownMenu(menu, onDismissRequest = { menu = false }) {
                    DropdownMenuItem(
                        text = { Text(stringResource(Res.string.rule_view_source)) },
                        onClick = {
                            menu = false
                            showSource = true
                        })
                    if (group.allExampleUrls.isNotEmpty()) {
                        DropdownMenuItem(
                            text = { Text(stringResource(Res.string.rule_view_examples)) },
                            onClick = {
                                menu = false
                                onDismissRequest()
                                navigator.navigate(
                                    ImagePreviewRoute(
                                        title = group.name,
                                        items = buildRuleGroupPreviewItems(group)
                                    )
                                )
                            })
                    }
                    DropdownMenuItem(
                        text = { Text(stringResource(Res.string.subscription_settings)) },
                        onClick = {
                            menu = false
                            onDismissRequest()
                            openSubscription(subs.id)
                        })
                    if (subs.isLocal) {
                        DropdownMenuItem(
                            text = { Text(stringResource(Res.string.rule_delete)) },
                            onClick = { menu = false; onClickDelete() })
                    }
                }
            }
        },
    ) {
        if (control == null) {
            Text(stringResource(Res.string.config_loading))
        } else {
            CsRuleSettingsContent(
                control, onSettingChange,
                title = if (group is RawSubscription.RawGlobalGroup && appId != null) stringResource(
                    Res.string.rule_enable_in_app
                ) else stringResource(Res.string.rule_enable),
                onViewControl = {
                    ruleControl.show(group.toRuleGroupTarget(subs.id, appId))
                }
            )
            if (group is RawSubscription.RawGlobalGroup && appId == null) {
                DefaultAppScopeSetting(group, matchAnyApp, onMatchAnyAppChange)
            }
            CsRuleExclusionsCard(excludeData, excludeAppId, onClick = onClickEditExclude)
        }
    }
    if (showSource) {
        RuleSourceDialog(group, onDismissRequest = { showSource = false })
    }
}


@Composable
private fun DefaultAppScopeSetting(
    group: RawSubscription.RawGlobalGroup,
    matchAnyApp: Boolean?,
    onChange: (Boolean?) -> Unit,
) {
    val subscriptionDefault = group.matchAnyApp ?: true
    val checked = matchAnyApp ?: subscriptionDefault
    val title = stringResource(Res.string.global_rule_default_apps)
    val description = stringResource(when (matchAnyApp) {
        true -> Res.string.global_rule_default_apps_enabled
        false -> Res.string.global_rule_default_apps_disabled
        null -> when (subscriptionDefault) {
            true -> Res.string.global_rule_default_apps_follow_enabled
            false -> Res.string.global_rule_default_apps_follow_disabled
        }
    })
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
    ) {
        Row(
            Modifier.fillMaxWidth().height(IntrinsicSize.Min).heightIn(min = 64.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = title,
                modifier = Modifier.weight(1f)
                    .padding(start = 16.dp, end = 8.dp, top = 16.dp, bottom = 16.dp),
                style = MaterialTheme.typography.titleMedium,
            )
            CsIconButton(
                imageVector = ResetSettings,
                contentDescription = stringResource(Res.string.settings_reset_default),
                enabled = matchAnyApp != null,
                colors = IconButtonDefaults.iconButtonColors(
                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                ),
                onClick = { onChange(null) },
            )
            CsTriStateSwitch(
                checked = checked,
                onCheckedChange = { onChange(checked != true) },
                modifier = Modifier.width(84.dp).fillMaxHeight().semantics {
                    contentDescription = title
                },
                stateDescription = description,
            )
        }
    }
}

@Composable
private fun RuleSourceDialog(
    group: RawSubscription.RawGroupProps,
    onDismissRequest: () -> Unit,
) {
    // Syntax highlighting is only needed after the user opens the source viewer.
    val source = group.cacheStr
    val darkTheme = LocalDarkTheme.current
    val annotatedText = remember(source, darkTheme) { getJson5AnnotatedString(source, darkTheme) }
    CsFullscreenDialog(onDismissRequest) {
        CsScaffold(topBar = {
            CsTopAppBar(
                title = {
                    CsTwoLineText(
                        title = stringResource(Res.string.rule_source),
                        subtitle = group.name
                    )
                },
                actions = {
                    CsIconButton(
                        CsIcons.Close, onClick = onDismissRequest,
                        contentDescription = stringResource(Res.string.dialog_close)
                    )
                },
            )
        }) { contentPadding ->
            val textModifier = Modifier.scaffoldPadding(contentPadding).padding(16.dp)
                .fillMaxSize().clip(MaterialTheme.shapes.large)
                .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            if (source.length > JSON5_LARGE_TEXT_THRESHOLD) {
                CsLazyCopyableText(
                    onCopy = ::copyText,
                    text = annotatedText,
                    modifier = textModifier,
                    contentPadding = PaddingValues(16.dp),
                    textStyle = MaterialTheme.typography.bodySmall,
                    contentColor = MaterialTheme.colorScheme.onSurface,
                    textContentDescription = stringResource(Res.string.rule_content)
                )
            } else {
                CsCopyableText(
                    onCopy = ::copyText,
                    text = annotatedText,
                    textToCopy = source,
                    modifier = textModifier,
                    contentPadding = PaddingValues(16.dp),
                    textStyle = MaterialTheme.typography.bodySmall,
                    contentColor = MaterialTheme.colorScheme.onSurface,
                    textContentDescription = stringResource(Res.string.rule_content)
                )
            }
        }
    }
}

// 规则组示例图需要保留“图片属于哪个子规则”的上下文，预览页才能显示更具体的标题。
private fun buildRuleGroupPreviewItems(
    group: RawSubscription.RawGroupProps
): List<ImagePreviewItem> {
    val uriTitlesMap = linkedMapOf<String, LinkedHashSet<String>>()

    fun addPreviewItem(uri: String, title: String?) {
        val titles = uriTitlesMap.getOrPut(uri) { linkedSetOf() }
        title?.takeIf { it.isNotBlank() }?.let(titles::add)
    }

    group.exampleUrls.orEmpty().forEach { uri ->
        addPreviewItem(
            uri = uri,
            title = group.name,
        )
    }
    group.rules.forEach { rule ->
        val ruleTitle = buildRulePreviewTitle(rule)
        rule.exampleUrls.orEmpty().forEach { uri ->
            addPreviewItem(
                uri = uri,
                title = ruleTitle,
            )
        }
    }

    return uriTitlesMap.map { (uri, titles) ->
        ImagePreviewItem(
            uri = uri,
            titles = titles.toList(),
        )
    }
}

private fun buildRulePreviewTitle(rule: RawSubscription.RawRuleProps): String? {
    return when {
        !rule.name.isNullOrBlank() -> rule.name
        rule.key != null -> Res.string.rule_key_description.getSync(rule.key)
        !rule.preKeys.isNullOrEmpty() -> Res.string.rule_pre_keys_description.getSync(
            (rule.preKeys as Iterable<Any?>).joinToString(
                ","
            )
        )

        else -> null
    }
}
