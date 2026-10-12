package app.cyanskip.app.ui.subscription

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.update
import app.cyanskip.app.subscription.RawSubscription
import app.cyanskip.app.subscription.SubscriptionJson.json
import app.cyanskip.app.subscription.SubscriptionRepository
import app.cyanskip.app.ui.navigation.CategoryEditorRoute
import app.cyanskip.app.ui.state.BaseViewModel

data class CategoryEditorDraft(
    val originalCategory: String?,
    val originalName: String,
    val originalDescription: String,
    val name: String = originalName,
    val description: String = originalDescription,
)

class CategoryEditorViewModel(
    private val route: CategoryEditorRoute,
) : BaseViewModel() {
    val draft: StateFlow<CategoryEditorDraft?>
        field = MutableStateFlow(null)

    private fun prepareSubscription(subscription: RawSubscription): RawSubscription {
        if (draft.value == null) reloadDraft(subscription)
        return subscription
    }

    fun reloadDraft(subscription: RawSubscription) {
        val category = subscription.categories.find { it.key == route.categoryKey }
        draft.value = CategoryEditorDraft(
            originalCategory = category?.let { json.encodeToString(it) },
            originalName = category?.name.orEmpty(),
            originalDescription = category?.desc.orEmpty(),
        )
    }

    fun setName(value: String) {
        draft.update { it?.copy(name = value) }
    }

    fun setDescription(value: String) {
        draft.update { it?.copy(description = value) }
    }

    val uiState = RequiredSubscription(
        route.subsId,
        scope
    ).buildUiState(initialValue = ::prepareSubscription) { flowOf(prepareSubscription(it)) }
    private val saveSession = EditorSaveSession<Boolean>()

    suspend fun save(subscription: RawSubscription, name: String, description: String): Boolean =
        saveSession.save {
            SubscriptionRepository.saveCategory(subscription, route.categoryKey, name, description)
        }
}
