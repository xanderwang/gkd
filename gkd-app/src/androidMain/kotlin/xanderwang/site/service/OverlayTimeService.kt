package xanderwang.site.service

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import java.text.SimpleDateFormat
import java.util.Locale
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.isActive
import li.gkd.app.notif.NotificationCatalog
import li.gkd.app.permission.PermissionStates
import li.gkd.app.resources.*
import li.gkd.app.service.OverlayWindowService
import li.gkd.app.service.useServicePresence
import li.gkd.app.settings.SettingsRepository
import li.gkd.app.util.IntentUtils

/** 在所有界面显示当前时间的悬浮窗, 长按关闭 */
class OverlayTimeService :
    OverlayWindowService(
        positionKey = "overlay_time",
    ) {
    override fun onLongClickView() {
        SettingsRepository.updateSettings { it.copy(enableTimeOverlay = false) }
        stopSelf()
    }

    @Composable
    override fun ComposeContent() {
        val store by SettingsRepository.settings.collectAsStateWithLifecycle()
        val timeFormat = remember { SimpleDateFormat("HH:mm:ss", Locale.getDefault()) }
        var timeText by remember { mutableStateOf(timeFormat.format(System.currentTimeMillis())) }
        LaunchedEffect(timeFormat) {
            while (isActive) {
                val now = System.currentTimeMillis()
                timeText = timeFormat.format(now)
                // 对齐到下一秒边界, 秒数变化时立即刷新
                delay(1000 - now % 1000)
            }
        }
        Text(
            text = timeText,
            modifier =
                Modifier.clip(RoundedCornerShape(4.dp))
                    .background(Color.Black.copy(alpha = store.overlayBgAlpha))
                    .padding(horizontal = 4.dp, vertical = 2.dp),
            fontSize = store.overlayFontSize.sp,
            fontFamily = FontFamily.Monospace,
            color = Color.White,
        )
    }

    init {
        // 停止服务广播已由 OverlayWindowService 注册, 这里不重复
        useServicePresence(
            stateFlow = isRunning,
            name = Res.string.overlay_time_service,
        )
        onCreated {
            NotificationCatalog.overlayTime().startForeground()
        }
        // 服务被通知栏停止或异常销毁时, 开关不能继续显示为开启
        onDestroyed {
            SettingsRepository.updateSettings { it.copy(enableTimeOverlay = false) }
        }
    }

    companion object {
        val isRunning: StateFlow<Boolean>
            field = MutableStateFlow(false)

        /** 权限不足时不启动也不写设置, 返回是否已交给系统启动 */
        fun start(): Boolean {
            if (!PermissionStates.drawOverlays.checkOrToast()) return false
            IntentUtils.startForegroundService(OverlayTimeService::class)
            return true
        }

        fun stop() = IntentUtils.stopService(OverlayTimeService::class)

        /** 应用启动/开机后按上次状态静默恢复悬浮窗 */
        fun autoStart() {
            if (!SettingsRepository.settings.value.enableTimeOverlay) return
            if (!PermissionStates.drawOverlays.updateAndGet()) return
            if (!PermissionStates.notification.updateAndGet()) return
            if (!PermissionStates.foregroundServiceSpecialUse.updateAndGet()) return
            IntentUtils.startForegroundService(OverlayTimeService::class)
        }
    }
}
