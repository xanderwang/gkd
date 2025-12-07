package xanderwang.site.permission

import com.hjq.permissions.XXPermissions
import com.hjq.permissions.permission.PermissionLists
import li.songe.gkd.app
import li.songe.gkd.permission.AuthReason
import li.songe.gkd.permission.PermissionState
import li.songe.gkd.permission.asyncRequestPermission

val canReadSmsState by lazy {
    val permission = PermissionLists.getReadSmsPermission()
    PermissionState(
        name = "短信权限",
        check = {
            XXPermissions.isGrantedPermission(app, permission)
        },
        request = {
            asyncRequestPermission(it, permission)
        },
        reason = AuthReason(
            text = { "当前操作需要「读取短信权限」\n您需要前往应用权限设置打开此权限" },
            confirm = {
                XXPermissions.startPermissionActivity(app, permission)
            }
        ),
    )
}