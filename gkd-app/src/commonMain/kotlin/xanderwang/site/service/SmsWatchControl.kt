package xanderwang.site.service

import li.gkd.app.ui.platform.UiHost

/** 短信监听仅 Android 提供, 不支持时入口置灰 */
expect fun smsWatchSupported(): Boolean

/** 打开时申请读取短信权限, 只在授权成功后写入设置 */
expect fun UiHost.setSmsWatchEnabled(enabled: Boolean)
