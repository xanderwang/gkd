package xanderwang.site.notify

import li.songe.gkd.notif.Notif
import xanderwang.site.service.OverlayTimeService
import kotlin.lazy

/** 短消息通知 */
val smsNotif by lazy {
    Notif(
        channel = smsChannel,
        id = 1000,
        title = "监听到报警短信...",
        text = "点击停止报警铃声",
        ongoing = true,
        autoCancel = false
    )
}

/** 悬浮时间通知 */
val timeNotif by lazy {
    Notif(
        id = 1001,
        title = "悬浮时间服务正在运行",
        text = "长按悬浮窗停止服务",
        stopService = OverlayTimeService::class,
    )
}