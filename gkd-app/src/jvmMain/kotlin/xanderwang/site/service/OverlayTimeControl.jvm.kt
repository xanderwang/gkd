package xanderwang.site.service

import androidx.compose.runtime.Composable
import li.gkd.app.platform.PlatformResult

@Composable
actual fun overlayTimeAvailable(): Boolean = false

actual fun setOverlayTimeEnabled(enabled: Boolean): PlatformResult<Unit> =
    PlatformResult.Unsupported
