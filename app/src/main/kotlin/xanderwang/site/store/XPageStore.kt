package xanderwang.site.store

import kotlinx.serialization.Serializable

@Serializable
data class XPageStore(
    val enableWatchSMS: Boolean = true,
    val msgKey: String = "交警",
    val enableTimeOverlay: Boolean = false,
    val overlayBgAlpha: Float = 0.2f,
    val overlayFontSize: Float = 14f,
)