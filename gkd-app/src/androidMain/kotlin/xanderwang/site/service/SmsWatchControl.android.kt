package xanderwang.site.service

import li.gkd.app.permission.PermissionStates
import li.gkd.app.resources.*
import li.gkd.app.settings.SettingsRepository
import li.gkd.app.ui.androidState
import li.gkd.app.ui.platform.UiHost
import li.gkd.app.ui.share.launchUi
import li.gkd.app.util.ToastUtils

actual fun smsWatchSupported(): Boolean = true

actual fun UiHost.setSmsWatchEnabled(enabled: Boolean) {
    if (!enabled) {
        SettingsRepository.updateSettings { it.copy(enableWatchSms = false) }
        ToastUtils.show(Res.string.sms_watch_disable_toast.getSync())
        return
    }
    mainVm.scope.launchUi {
        if (mainVm.androidState.permissionRequests.ensurePermissions(PermissionStates.readSms)) {
            SettingsRepository.updateSettings { it.copy(enableWatchSms = true) }
            ToastUtils.show(Res.string.sms_watch_enable_toast.getSync())
        }
    }
}
