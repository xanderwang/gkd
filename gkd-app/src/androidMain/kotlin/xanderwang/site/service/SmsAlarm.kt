package xanderwang.site.service

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Intent
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.os.Handler
import android.os.Looper
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import li.gkd.app.app
import li.gkd.app.logging.LogUtils
import li.gkd.app.notif.PostedNotificationKey
import li.gkd.app.permission.PermissionStates
import li.gkd.app.resources.*
import li.gkd.app.service.StatusService

/** 短信报警铃声, 点击通知经常驻服务停止报警 */
object SmsAlarm {
    /** 停止报警的 intent 标记, 由 [StatusService] 的 onStartCommand 转交 */
    const val EXTRA_STOP_ALARM = "x_sms_stop_alarm"

    private const val START_DELAY_MILLIS = 1000L

    private val notificationKey = PostedNotificationKey.SmsAlarm

    private val handler = Handler(Looper.getMainLooper())

    private val player by lazy {
        val uri =
            RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
                ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
                ?: return@lazy null
        LogUtils.d("alarm uri", uri)
        // create 返回的实例已是 Prepared 状态, 不能再 prepare(), 只能 start/pause
        MediaPlayer.create(app, uri)?.apply { isLooping = true }
    }

    private val startRunnable = Runnable {
        val mediaPlayer = player ?: return@Runnable
        runCatching {
            if (!mediaPlayer.isPlaying) {
                mediaPlayer.seekTo(0)
                mediaPlayer.start()
            }
        }.onFailure { e ->
            LogUtils.e("报警铃声启动失败", e)
        }
    }

    fun startAlarm() {
        handler.removeCallbacks(startRunnable)
        handler.postDelayed(startRunnable, START_DELAY_MILLIS)
        postNotification()
    }

    fun stopAlarm() {
        handler.removeCallbacks(startRunnable)
        handler.post {
            val mediaPlayer = player
            if (mediaPlayer != null) {
                runCatching {
                    if (mediaPlayer.isPlaying) mediaPlayer.pause()
                }.onFailure { e ->
                    LogUtils.e("报警铃声停止失败", e)
                }
            }
            cancelNotification()
        }
    }

    fun handleIntent(intent: Intent?) {
        if (intent?.getBooleanExtra(EXTRA_STOP_ALARM, false) == true) {
            stopAlarm()
        }
    }

    @SuppressLint("MissingPermission")
    private fun postNotification() {
        if (!PermissionStates.notification.updateAndGet()) return
        val contentIntent =
            PendingIntent.getService(
                app,
                notificationKey.id,
                Intent(app, StatusService::class.java).putExtra(EXTRA_STOP_ALARM, true),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
        val notification =
            NotificationCompat.Builder(app, notificationKey.channel.id)
                .setSmallIcon(app.notificationSmallIcon)
                .setContentTitle(Res.string.sms_watch_alarm_title.getSync())
                .setContentText(Res.string.sms_watch_alarm_text.getSync())
                .setContentIntent(contentIntent)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setSilent(true)
                .setOngoing(true)
                .setAutoCancel(false)
                .build()
        NotificationManagerCompat.from(app).notify(notificationKey.id, notification)
    }

    private fun cancelNotification() =
        NotificationManagerCompat.from(app).cancel(notificationKey.id)
}
