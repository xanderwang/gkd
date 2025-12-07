package xanderwang.site.notify

import li.songe.gkd.notif.Notif
import xanderwang.site.service.OverlayTimeService
import kotlin.lazy

val smsNotif by lazy {
    Notif(
        channel = smsChannel,
        id = 1000,
        title = "短信内容监控",
        text = "短信内容监控服务",
        ongoing = true,
        autoCancel = false
    )
}


val timeNotif by lazy {
    Notif(
        id = 1001,
        title = "快照按钮服务正在运行",
        text = "点击按钮捕获快照",
        uri = "gkd://page/1",
        stopService = OverlayTimeService::class,
    )
}