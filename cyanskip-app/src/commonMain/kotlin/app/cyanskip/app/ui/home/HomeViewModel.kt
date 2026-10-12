package app.cyanskip.app.ui.home

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import app.cyanskip.app.ui.MainViewModel
import app.cyanskip.app.ui.share.launchUi
import app.cyanskip.app.ui.subscription.message
import app.cyanskip.app.resources.*
import org.jetbrains.compose.resources.getString
import app.cyanskip.app.util.ToastUtils
import app.cyanskip.app.app.AppInfoRepository
import app.cyanskip.app.rule.ruleGroupState
import app.cyanskip.app.settings.SettingsRepository
import app.cyanskip.app.state.Loadable
import app.cyanskip.app.state.MutexState
import app.cyanskip.app.subscription.SubscriptionRepository
import app.cyanskip.app.subscription.SubscriptionResult
import app.cyanskip.app.ui.state.BaseViewModel
import app.cyanskip.app.ui.subscription.SubsManageUiState
import app.cyanskip.db.Db
import app.cyanskip.db.LOCAL_SUBS_ID
import app.cyanskip.db.SubsItem

/** Business state owned by the HomeRoute navigation entry. */
class HomeViewModel : BaseViewModel() {
    private val defaultSubscriptionMutex = MutexState()
    val defaultSubscriptionBusy: StateFlow<Boolean> get() = defaultSubscriptionMutex.state

    fun subscribeDefault() = scope.launchUi {
        defaultSubscriptionMutex.tryWithStateLock {
            val mainVm = MainViewModel.requireCurrent()
            if (!mainVm.dialogRequests.confirm(
                    title = getString(Res.string.lite_default_subscription),
                    text = getString(Res.string.lite_default_subscription_confirm, DEFAULT_SUBSCRIPTION_URL),
                    dismissOnRequest = true,
                )) return@tryWithStateLock
            val existing = Db.subsItemDao.queryAll().firstOrNull { it.updateUrl == DEFAULT_SUBSCRIPTION_URL }
            val result = if (existing != null) SubscriptionResult.Success()
                else SubscriptionRepository.addOrModifyRemote(DEFAULT_SUBSCRIPTION_URL)
            if (result is SubscriptionResult.Success) {
                Db.subsItemDao.queryAll().firstOrNull { it.updateUrl == DEFAULT_SUBSCRIPTION_URL }?.let {
                    SubscriptionRepository.requestEnabled(it.id, true)
                }
            }
            result.message()?.let(ToastUtils::show)
        }
    }

    val showBackup: StateFlow<Boolean>
        field = MutableStateFlow(false)

    fun setShowBackup(value: Boolean) {
        showBackup.value = value
    }

    val appListState: StateFlow<AppListPageState>
        field = MutableStateFlow(AppListPageState())

    init {
        scope.launch {
            appListState.map { it.search }.distinctUntilChanged().debounce(200).collect { query ->
                appListState.update { it.copy(query = query) }
            }
        }
    }

    fun setAppSearchText(value: String) {
        appListState.update { it.copy(search = value.trim()) }
    }

    fun closeAppSearch() {
        appListState.update { it.copy(search = "", searchOpen = false) }
    }

    fun toggleAppSearch() {
        appListState.update {
            if (!it.searchOpen) it.copy(searchOpen = true)
            else if (it.search.isEmpty()) it.copy(searchOpen = false)
            else it.copy(search = "")
        }
    }

    fun toggleAppListEdit(blocked: Set<String>) {
        appListState.update { it.copy(editingFilter = if (it.editingFilter == null) blocked else null) }
    }

    fun closeAppListEdit() {
        appListState.update { it.copy(editingFilter = null) }
    }

    fun leaveAppList() {
        appListState.update {
            it.copy(searchOpen = it.searchOpen && it.search.isNotEmpty(), editingFilter = null)
        }
    }

    val homeState = HomeState()
    val latestState = HomeDataSources.observeLatest().stateInit(Loadable.Loading)

    val appsState = AppListSources.observe(
        AppInfoRepository.state,
        ruleGroupState,
        Db.actionLogDao.queryLatestUniqueAppIds(),
        Db.appLastVisitDao.query(),
        globalGroupCounts = { it.groups.appIdToGlobalGroupCount },
        appGroups = { it.groups.appIdToAllGroups },
    ).stateInit(Loadable.Loading)

    private val batchMutex = MutexState()
    val batchBusyFlow: StateFlow<Boolean> get() = batchMutex.state

    suspend fun runBatchAction(action: suspend () -> Unit) {
        batchMutex.tryWithStateLock(action)
    }

    val subscriptionsState: StateFlow<Loadable<SubsManageUiState>> =
        SubscriptionRepository.snapshotFlow.flatMapLatest { snapshotState ->
            when (snapshotState) {
                Loadable.Loading -> flowOf(Loadable.Loading)
                is Loadable.Failure -> flowOf(snapshotState)
                is Loadable.Ready -> Db.subsItemDao.query().map { subItems ->
                    SubsManageUiState(
                        subItems = subItems,
                        subscriptions = snapshotState.value.subscriptions,
                        loadErrors = snapshotState.value.loadErrors,
                        refreshErrors = snapshotState.value.updateErrors,
                    )
                }.map<SubsManageUiState, Loadable<SubsManageUiState>> { Loadable.Ready(it) }
                    .catch { emit(Loadable.Failure(it)) }
            }
        }.stateIn(scope, SharingStarted.Eagerly, Loadable.Loading)

    fun setUpdateInterval(value: Long) {
        SettingsRepository.updateSettings { it.copy(updateSubsInterval = value) }
    }

    fun toggleMatching() {
        SettingsRepository.updateSettings { it.copy(enableMatch = !it.enableMatch) }
    }

    fun enableMatching() {
        SettingsRepository.updateSettings { it.copy(enableMatch = true) }
    }

    suspend fun refreshSubscriptions() = SubscriptionRepository.refresh()

    suspend fun deleteSubscriptions(ids: Set<Long>): SubscriptionResult =
        SubscriptionRepository.delete(*(ids - LOCAL_SUBS_ID).toLongArray())

    suspend fun updateOrder(items: List<SubsItem>) = Db.subsItemDao.batchUpdateOrder(items)

    suspend fun requestSubscriptionEnabled(item: SubsItem, enabled: Boolean) {
        SubscriptionRepository.requestEnabled(item.id, enabled)
    }
}

const val DEFAULT_SUBSCRIPTION_URL = "https://gkd-subscription-667.pages.dev/gkd.json5"
