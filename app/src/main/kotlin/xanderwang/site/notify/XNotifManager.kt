package xanderwang.site.notify

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

fun startTrafficNotif(service: Service, notif: Notif) {
    val trafficIntent = Intent(service, XManageService::class.java).apply {
        putExtra(XManageService.KEY_ACTION, XManageService.ACTION_STOP_ALARM)
    }
    val pendingTrafficIntent = PendingIntent.getService(
        service, notif.id, trafficIntent, FLAG_IMMUTABLE or FLAG_UPDATE_CURRENT
    )
    val builder = NotificationCompat.Builder(service, notif.channel.id)
        .setSmallIcon(notif.smallIcon)
        .setContentTitle(notif.title)
        .setContentText(notif.text)
        .setContentIntent(pendingTrafficIntent)
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

fun cancelNotif(context: Context, notif: Notif) {
    NotificationManagerCompat.from(context).cancel(notif.id)
}