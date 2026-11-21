package xanderwang.site.service

import androidx.compose.runtime.Composable
import li.gkd.app.platform.PlatformResult

/** 当前平台是否提供悬浮窗服务, 不支持时不展示入口 */
@Composable
expect fun overlayTimeAvailable(): Boolean

expect fun setOverlayTimeEnabled(enabled: Boolean): PlatformResult<Unit>
