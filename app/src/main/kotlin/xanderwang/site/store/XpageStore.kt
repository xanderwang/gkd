package xanderwang.site.store

import kotlinx.serialization.Serializable

@Serializable
data class XpageStore(
    val enableWatchSMS: Boolean = true,
    val msgKey: String = "交警",
)