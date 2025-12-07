package xanderwang.site.store

import kotlinx.serialization.Serializable

@Serializable
data class XpageStore(
    val enableWatchMsg: Boolean = true,
    val msgKey: String = "交警",
)