package xanderwang.site.service

import androidx.compose.runtime.Composable
import li.gkd.app.platform.PlatformResult
import li.gkd.app.settings.SettingsRepository

@Composable
actual fun overlayTimeAvailable(): Boolean = true

/**
 * 开启时先启动服务, 启动成功才写设置; 权限不足时保持关闭。
 * Success 表示请求已处理, 未授权属于业务前提不满足, 不是异常。
 */
actual fun setOverlayTimeEnabled(enabled: Boolean): PlatformResult<Unit> {
    if (enabled) {
        if (OverlayTimeService.start()) {
            SettingsRepository.updateSettings { it.copy(enableTimeOverlay = true) }
        }
    } else {
        SettingsRepository.updateSettings { it.copy(enableTimeOverlay = false) }
        OverlayTimeService.stop()
    }
    return PlatformResult.Success(Unit)
}
