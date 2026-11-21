package xanderwang.site.service

import li.gkd.app.ui.platform.UiHost

actual fun smsWatchSupported(): Boolean = false

/** 桌面端无短信收件箱, 入口已置灰, 这里不做任何事 */
actual fun UiHost.setSmsWatchEnabled(enabled: Boolean) = Unit
