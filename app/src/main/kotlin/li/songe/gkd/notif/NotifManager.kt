package li.songe.gkd.notif

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import li.songe.gkd.MainActivity
import li.songe.gkd.service.ManageService

fun createChannel(context: Context, notifChannel: NotifChannel) {
    val importance = NotificationManager.IMPORTANCE_LOW
    val channel = NotificationChannel(notifChannel.id, notifChannel.name, importance)
    channel.description = notifChannel.desc
    val notificationManager = NotificationManagerCompat.from(context)
    notificationManager.createNotificationChannel(channel)
}

fun createNotif(context: Service, channelId: String, notif: Notif) {
    val intent = Intent(context, MainActivity::class.java).apply {
        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
    }
    val pendingIntent = PendingIntent.getActivity(
        context, notif.id, intent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
    )

    val builder = NotificationCompat.Builder(context, channelId)
        .setSmallIcon(notif.smallIcon)
        .setContentTitle(notif.title).setContentText(notif.text).setContentIntent(pendingIntent)
        .setPriority(NotificationCompat.PRIORITY_DEFAULT).setOngoing(notif.ongoing)
        .setAutoCancel(notif.autoCancel)

    val notification = builder.build()
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        context.startForeground(
            notif.id, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_MANIFEST
        )
    } else {
        context.startForeground(notif.id, notification)
    }
}

@SuppressLint("LaunchActivityFromNotification")
fun createAlarmNotif(context: Service, channelId: String, notif: Notif) {
    val intent = Intent(context, ManageService::class.java).apply {
        putExtra(ManageService.KEY_DO_ACTION, ManageService.MENU_STOP_ALARM)
    }
    val pendingIntent = PendingIntent.getService(
        context, notif.id, intent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
    )

    val builder = NotificationCompat.Builder(context, channelId)
        .setSmallIcon(notif.smallIcon)
        .setContentTitle(notif.title)
        .setContentText(notif.text)
        .setContentIntent(pendingIntent)
        .setPriority(NotificationCompat.PRIORITY_DEFAULT)
        .setOngoing(notif.ongoing)
        .setAutoCancel(notif.autoCancel)

    val notification = builder.build()
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        context.startForeground(
            notif.id, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_MANIFEST
        )
    } else {
        context.startForeground(notif.id, notification)
    }
}

fun cancelNotif(context: Service, notif: Notif) {
    NotificationManagerCompat.from(context).cancel(notif.id)
}