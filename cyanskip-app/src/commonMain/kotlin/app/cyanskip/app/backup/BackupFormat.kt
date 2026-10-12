package app.cyanskip.app.backup

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import app.cyanskip.app.storage.BackupIssue
import app.cyanskip.app.storage.StorageException
import app.cyanskip.db.SubsAppConfig
import app.cyanskip.db.SubsAppGroupConfig
import app.cyanskip.db.SubsCategoryConfig
import app.cyanskip.db.SubsGlobalGroupConfig
import app.cyanskip.db.SubsItem
import app.cyanskip.db.SubscriptionConfigSnapshot

// Archive versions are independent of Room schema versions.
private object BackupVersion {
    const val Current = 2
}

@Serializable
data class BackupDatabaseData(
    val formatVersion: Int = BackupVersion.Current,
    val subsItems: List<BackupSubsItem> = emptyList(),
    val appConfigs: List<BackupAppConfig> = emptyList(),
    val categoryConfigs: List<BackupCategoryConfig> = emptyList(),
    val appGroupConfigs: List<BackupAppGroupConfig> = emptyList(),
    val globalGroupConfigs: List<BackupGlobalGroupConfig> = emptyList(),
) {
    fun toSnapshot() = SubscriptionConfigSnapshot(
        subsItems = subsItems.map { it.toEntity() },
        appConfigs = appConfigs.map { it.toEntity() },
        categoryConfigs = categoryConfigs.map { it.toEntity() },
        appGroupConfigs = appGroupConfigs.map { it.toEntity() },
        globalGroupConfigs = globalGroupConfigs.map { it.toEntity() },
    )

    companion object {
        fun fromSnapshot(snapshot: SubscriptionConfigSnapshot) = BackupDatabaseData(
            subsItems = snapshot.subsItems.map(BackupSubsItem::fromEntity),
            appConfigs = snapshot.appConfigs.map(BackupAppConfig::fromEntity),
            categoryConfigs = snapshot.categoryConfigs.map(BackupCategoryConfig::fromEntity),
            appGroupConfigs = snapshot.appGroupConfigs.map(BackupAppGroupConfig::fromEntity),
            globalGroupConfigs = snapshot.globalGroupConfigs.map(BackupGlobalGroupConfig::fromEntity),
        )
    }
}

@Serializable
data class BackupSubsItem(
    val id: Long,
    val ctime: Long,
    val mtime: Long,
    val enable: Boolean = false,
    val enableUpdate: Boolean = true,
    val order: Int,
    val updateUrl: String? = null,
) {
    fun toEntity() = SubsItem(
        id = id,
        ctime = ctime,
        mtime = mtime,
        enable = enable,
        enableUpdate = enableUpdate,
        order = order,
        updateUrl = updateUrl,
    )

    companion object {
        fun fromEntity(entity: SubsItem) = BackupSubsItem(
            id = entity.id,
            ctime = entity.ctime,
            mtime = entity.mtime,
            enable = entity.enable,
            enableUpdate = entity.enableUpdate,
            order = entity.order,
            updateUrl = entity.updateUrl,
        )
    }
}

@Serializable
data class BackupAppConfig(
    val subsId: Long,
    val appId: String,
    val enable: Boolean,
) {
    fun toEntity() = SubsAppConfig(
        subsId = subsId,
        appId = appId,
        enable = enable,
    )

    companion object {
        fun fromEntity(entity: SubsAppConfig) = BackupAppConfig(
            subsId = entity.subsId,
            appId = entity.appId,
            enable = entity.enable,
        )
    }
}

@Serializable
data class BackupCategoryConfig(
    val subsId: Long,
    val categoryKey: Int,
    val enable: Boolean? = null,
) {
    fun toEntity() = SubsCategoryConfig(
        subsId = subsId,
        categoryKey = categoryKey,
        enable = enable,
    )

    companion object {
        fun fromEntity(entity: SubsCategoryConfig) = BackupCategoryConfig(
            subsId = entity.subsId,
            categoryKey = entity.categoryKey,
            enable = entity.enable,
        )
    }
}

@Serializable
data class BackupAppGroupConfig(
    val subsId: Long,
    val appId: String,
    val groupKey: Int,
    val enable: Boolean? = null,
    val exclude: String = "",
) {
    fun toEntity() = SubsAppGroupConfig(
        subsId = subsId,
        appId = appId,
        groupKey = groupKey,
        enable = enable,
        exclude = exclude,
    )

    companion object {
        fun fromEntity(entity: SubsAppGroupConfig) = BackupAppGroupConfig(
            subsId = entity.subsId,
            appId = entity.appId,
            groupKey = entity.groupKey,
            enable = entity.enable,
            exclude = entity.exclude,
        )
    }
}

@Serializable
data class BackupGlobalGroupConfig(
    val subsId: Long,
    val groupKey: Int,
    val enable: Boolean? = null,
    val exclude: String = "",
    val matchAnyApp: Boolean? = null,
) {
    fun toEntity() = SubsGlobalGroupConfig(
        subsId = subsId,
        groupKey = groupKey,
        enable = enable,
        exclude = exclude,
        matchAnyApp = matchAnyApp,
    )

    companion object {
        fun fromEntity(entity: SubsGlobalGroupConfig) = BackupGlobalGroupConfig(
            subsId = entity.subsId,
            groupKey = entity.groupKey,
            enable = entity.enable,
            exclude = entity.exclude,
            matchAnyApp = entity.matchAnyApp,
        )
    }
}

object BackupFormat {
    private val json = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
        encodeDefaults = true
    }

    fun encode(data: BackupDatabaseData): String = json.encodeToString(data)

    fun decode(
        text: String,
    ): BackupDatabaseData {
        val root = json.parseToJsonElement(text).jsonObject
        return when (val version =
            root["formatVersion"]?.jsonPrimitive?.int) {
            BackupVersion.Current -> json.decodeFromJsonElement(
                BackupDatabaseData.serializer(),
                root
            )

            else -> throw StorageException(BackupIssue.VersionUnsupported, version)
        }
    }
}
