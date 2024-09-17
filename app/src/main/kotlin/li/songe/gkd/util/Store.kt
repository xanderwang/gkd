package li.songe.gkd.util

import com.blankj.utilcode.util.LogUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import li.songe.gkd.META
import li.songe.gkd.appScope

private inline fun <reified T> createStorageFlow(
    key: String,
    crossinline init: () -> T,
): MutableStateFlow<T> {
    val str = kv.getString(key, null)
    val initValue = if (str != null) {
        try {
            json.decodeFromString<T>(str)
        } catch (e: Exception) {
            e.printStackTrace()
            LogUtils.d(e)
            null
        }
    } else {
        null
    }
    val stateFlow = MutableStateFlow(initValue ?: init())
    appScope.launch {
        stateFlow.drop(1).collect {
            withContext(Dispatchers.IO) {
                kv.encode(key, json.encodeToString(it))
            }
        }
    }
    return stateFlow
}

@Serializable
data class Store(
    val enableService: Boolean = true,
    val enableMatch: Boolean = true,
    val enableStatusService: Boolean = true,
    val excludeFromRecents: Boolean = false,
    val captureScreenshot: Boolean = false,
    val httpServerPort: Int = 8888,
    val updateSubsInterval: Long = UpdateTimeOption.Everyday.value,
    val captureVolumeChange: Boolean = false,
    val autoCheckAppUpdate: Boolean = META.updateEnabled,
    val toastWhenClick: Boolean = true,
    val clickToast: String = "GKD",
    val autoClearMemorySubs: Boolean = true,
    val hideSnapshotStatusBar: Boolean = false,
    val enableShizukuActivity: Boolean = false,
    val enableShizukuClick: Boolean = false,
    val log2FileSwitch: Boolean = true,
    val enableDarkTheme: Boolean? = null,
    val enableDynamicColor: Boolean = true,
    val enableAbFloatWindow: Boolean = true,
    val sortType: Int = SortTypeOption.SortByName.value,
    val showSystemApp: Boolean = true,
    val showHiddenApp: Boolean = false,
    val showSaveSnapshotToast: Boolean = true,
    val useSystemToast: Boolean = false,
    val useCustomNotifText: Boolean = false,
    val customNotifText: String = "\${i}全局/\${k}应用/\${u}规则组/\${n}触发",
    val enableActivityLog: Boolean = false,
    val updateChannel: Int = if (META.versionName.contains("beta")) UpdateChannelOption.Beta.value else UpdateChannelOption.Stable.value,
    val msgContentKey: String = "上海交警",
)

val storeFlow by lazy {
    createStorageFlow("store-v2") { Store() }.apply {
        if (UpdateTimeOption.allSubObject.all { it.value != value.updateSubsInterval }) {
            update {
                it.copy(
                    updateSubsInterval = UpdateTimeOption.Everyday.value
                )
            }
        }
    }
}

@Serializable
data class RecordStore(
    val clickCount: Int = 0,
)

val recordStoreFlow by lazy {
    createStorageFlow("record_store-v2") { RecordStore() }
}

val clickCountFlow by lazy {
    recordStoreFlow.map(appScope) { r -> r.clickCount }
}

fun increaseClickCount(n: Int = 1) {
    recordStoreFlow.update {
        it.copy(
            clickCount = it.clickCount + n
        )
    }
}

@Serializable
data class PrivacyStore(
    val githubCookie: String? = null,
)

val privacyStoreFlow by lazy {
    createStorageFlow("privacy_store") { PrivacyStore() }
}

private fun createLongFlow(key: String, defaultValue: Long): MutableStateFlow<Long> {
    val stateFlow = MutableStateFlow(kv.getLong(key, defaultValue))
    appScope.launch {
        stateFlow.drop(1).collect {
            withContext(Dispatchers.IO) { kv.encode(key, it) }
        }
    }
    return stateFlow
}

val lastRestartA11yServiceTimeFlow by lazy {
    createLongFlow("last_restart_a11y_service_time", 0)
}

fun initStore() {
    storeFlow.value
    recordStoreFlow.value
}

