package xanderwang.site.notify

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.app.PendingIntent.FLAG_IMMUTABLE
import android.app.PendingIntent.FLAG_UPDATE_CURRENT
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import li.songe.gkd.notif.Notif
import xanderwang.site.service.XManageService

/** 开始短信报警通知 */
fun startSMSNotif(service: Service, notif: Notif) {
    val smsIntent = Intent(service, XManageService::class.java).apply {
        putExtra(XManageService.KEY_ACTION, XManageService.ACTION_STOP_ALARM)
    }
    startNotif(service, notif, smsIntent)
    notif.notifySelf()
}

/** 开始通知 */
@SuppressLint("LaunchActivityFromNotification")
fun startNotif(service: Service, notif: Notif, notifIntent: Intent) {
    val flags = FLAG_UPDATE_CURRENT or if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        FLAG_IMMUTABLE
    } else {
        0
    }
    val pendingIntent = PendingIntent.getService(
        service, notif.id, notifIntent, flags
    )
    val builder = NotificationCompat.Builder(service, notif.channel.id)
        .setSmallIcon(notif.smallIcon)
        .setContentTitle(notif.title)
        .setContentText(notif.text)
        .setContentIntent(pendingIntent)
        .setPriority(NotificationCompat.PRIORITY_HIGH)
        .setOngoing(notif.ongoing)
        .setAutoCancel(notif.autoCancel)
    val notification = builder.build()
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        service.startForeground(
            notif.id, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_MANIFEST
        )
    } else {
        service.startForeground(notif.id, notification)
    }
}

/** 取消通知 */
fun cancelNotif(context: Context, notif: Notif) {
    NotificationManagerCompat.from(context).cancel(notif.id)
}