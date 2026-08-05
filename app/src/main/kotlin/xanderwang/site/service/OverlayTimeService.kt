package xanderwang.site.service

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
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
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.isActive
import li.songe.gkd.notif.StopServiceReceiver
import li.songe.gkd.permission.canDrawOverlaysState
import li.songe.gkd.permission.foregroundServiceSpecialUseState
import li.songe.gkd.permission.notificationState
import li.songe.gkd.service.OverlayWindowService
import li.songe.gkd.store.xPageStoreFlow
import li.songe.gkd.util.startForegroundServiceByClass
import li.songe.gkd.util.stopServiceByClass
import xanderwang.site.notify.timeNotif
import java.text.SimpleDateFormat
import java.util.Locale

private val timeFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())

class OverlayTimeService : OverlayWindowService(
    positionKey = "overlay_time"
) {
    override fun onLongClickView() {
        xPageStoreFlow.value = xPageStoreFlow.value.copy(enableTimeOverlay = false)
        stopSelf()
    }

    @Composable
    override fun ComposeContent() {
        var timeText by remember { mutableStateOf("") }
        val store by xPageStoreFlow.collectAsState()
        LaunchedEffect(Unit) {
            while (isActive) {
                val now = System.currentTimeMillis()
                timeText = timeFormat.format(now)
                // 对齐到下一秒边界，秒数变化时立即刷新
                delay(1000 - now % 1000)
            }
        }
        Text(
            text = timeText,
            modifier = Modifier
                .clip(RoundedCornerShape(4.dp))
                .background(Color.Black.copy(alpha = store.overlayBgAlpha))
                .padding(horizontal = 4.dp, vertical = 2.dp),
            fontSize = store.overlayFontSize.sp,
            fontFamily = FontFamily.Monospace,
            color = Color.White,
        )
    }

    init {
        useAliveFlow(isRunning)
        useAliveToast("悬浮时间服务")
        onCreated { timeNotif.notifyService() }
        StopServiceReceiver.autoRegister()
    }

    companion object {
        val isRunning = MutableStateFlow(false)
        fun start() {
            if (!canDrawOverlaysState.checkOrToast()) return
            startForegroundServiceByClass(OverlayTimeService::class)
        }

        fun stop() = stopServiceByClass(OverlayTimeService::class)

        /** 应用启动时静默恢复上次开启的悬浮窗 */
        fun autoStart() {
            runCatching {
                if (xPageStoreFlow.value.enableTimeOverlay &&
                    canDrawOverlaysState.updateAndGet() &&
                    notificationState.updateAndGet() &&
                    foregroundServiceSpecialUseState.updateAndGet()
                ) {
                    startForegroundServiceByClass(OverlayTimeService::class)
                }
            }
        }
    }
}
