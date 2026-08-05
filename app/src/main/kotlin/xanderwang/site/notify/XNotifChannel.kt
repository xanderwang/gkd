package xanderwang.site.notify

import li.songe.gkd.notif.NotifChannel

/** 短消息通道 */
val smsChannel by lazy {
    NotifChannel(
        id = "1000",
        name = "短信监控服务",
        desc = "监控短信内容，检测到关键字后发出报警。"
    )
}