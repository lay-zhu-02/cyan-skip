package app.cyanskip.app.ui.subscription

import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import app.cyanskip.app.resources.Res
import app.cyanskip.app.resources.category_missing
import app.cyanskip.app.rule.CategoryPolicy
import app.cyanskip.app.rule.CategorySetting
import app.cyanskip.app.rule.RuleGroupConfigService
import app.cyanskip.app.state.MutexState
import app.cyanskip.app.subscription.RawSubscription
import app.cyanskip.app.subscription.SubscriptionRepository
import app.cyanskip.app.ui.navigation.SubsCategoryRoute
import app.cyanskip.app.ui.state.BaseViewModel
import app.cyanskip.app.ui.text.getSync
import app.cyanskip.db.SubscriptionConfigStore

data class CategorySummary(
    val category: RawSubscription.RawCategory,
    val appCount: Int,
    val groupCount: Int,
    val enabledGroupCount: Int,
    val setting: CategorySetting,
)

data class SubsCategoryUiState(
    val subscription: RawSubscription,
    val categories: List<CategorySummary>,
)

class SubsCategoryViewModel(
    route: SubsCategoryRoute,
) : BaseViewModel() {
    private val mutation = MutexState()
    val busyFlow: StateFlow<Boolean> get() = mutation.state

    suspend fun runAction(action: suspend () -> Unit) {
        mutation.tryWithStateLock(action)
    }

    suspend fun setSettings(state: SubsCategoryUiState, keys: Set<Int>, setting: CategorySetting) {
        val expected = state.categories.filter { it.category.key in keys }
            .associate { it.category.key to it.setting }
        check(expected.size == keys.size) { Res.string.category_missing.getSync() }
        RuleGroupConfigService.setCategorySettings(state.subscription, expected, setting)
    }

    suspend fun deleteCategories(state: SubsCategoryUiState, keys: Set<Int>) {
        SubscriptionRepository.deleteCategories(state.subscription, keys)
    }

    val uiState = RequiredSubscription(route.subsItemId, scope).buildUiState { subscription ->
        SubscriptionConfigStore.observe().map { snapshot ->
            val categoryConfigs = snapshot.categoryConfigs.filter { it.subsId == subscription.id }
                .associateBy { it.categoryKey }
            val groupConfigs = snapshot.appGroupConfigs.filter { it.subsId == subscription.id }
                .associateBy { it.appId to it.groupKey }
            SubsCategoryUiState(subscription, subscription.categories.map { category ->
                val apps = subscription.getCategoryApps(category.key)
                val config = categoryConfigs[category.key]
                CategorySummary(
                    category = category,
                    appCount = apps.size,
                    groupCount = apps.sumOf { it.groups.size },
                    enabledGroupCount = apps.sumOf { app ->
                        app.groups.count { group ->
                            RuleGroupConfigService.policy.getGroupEnabled(
                                group,
                                groupConfigs[app.id to group.key],
                                category,
                                config
                            )
                        }
                    },
                    setting = CategoryPolicy.setting(config),
                )
            })
        }
    }

}
