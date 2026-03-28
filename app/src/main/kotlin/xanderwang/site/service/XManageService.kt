package xanderwang.site.service

import android.app.Service
import android.content.Context
import android.content.Intent
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.os.Handler
import android.os.Looper
import android.os.Message
import com.blankj.utilcode.util.LogUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import li.songe.gkd.app
import li.songe.gkd.service.StatusService
import xanderwang.site.notify.cancelNotif
import xanderwang.site.notify.smsNotif
import xanderwang.site.notify.startSMSNotif

fun xServiceClass(): Class<*> {
    return StatusService::class.java
}

object XManageService {

    const val KEY_ACTION = "x_action"
    const val ACTION_START_ALARM = 1000
    const val ACTION_STOP_ALARM = 1001
    const val ACTION_START_OBSERVER_SMS = 1010
    const val ACTION_STOP_OBSERVER_SMS = 1011

    val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val alarmPlayer by lazy {
        val alarmUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
            ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
            ?: return@lazy null
        LogUtils.d("alarmUri", alarmUri)
        MediaPlayer.create(app, alarmUri).apply {
            isLooping = true
        }
    }

    private fun stopAlarm() {
        if (alarmPlayer?.isPlaying == true) {
            alarmPlayer?.stop()
        }
    }

    private fun startAlarm() {
        stopAlarm()
        alarmPlayer?.prepare()
        alarmPlayer?.start()
    }

    private val xHandler = Handler(Looper.getMainLooper()) { msg ->
        LogUtils.d("handleMessage", msg)
        when (msg.what) {
            ACTION_START_ALARM -> startAlarm()
            ACTION_STOP_ALARM -> stopAlarm()
        }
        true
    }

    fun onStartCommand(service: Service, intent: Intent?): Boolean {
        val action = intent?.getIntExtra(KEY_ACTION, -1) ?: -1
        LogUtils.d("onStartCommand", action, intent)
        when (action) {
            ACTION_START_ALARM -> {
                xHandler.removeMessages(ACTION_START_ALARM)
                xHandler.sendMessageDelayed(Message.obtain().apply {
                    what = ACTION_START_ALARM
                }, 1000L)
                startSMSNotif(service, smsNotif)
            }

            ACTION_STOP_ALARM -> {
                xHandler.removeMessages(ACTION_START_ALARM)
                xHandler.removeMessages(ACTION_STOP_ALARM)
                xHandler.sendMessage(Message.obtain().apply {
                    what = ACTION_STOP_ALARM
                })
                cancelNotif(service, smsNotif)
            }

            // ACTION_START_OBSERVER_SMS -> {
            //     SmsObserver.register(service, xHandler)
            // }
            //
            // ACTION_STOP_OBSERVER_SMS -> {
            //     SmsObserver.unregister()
            // }
        }
        return false
    }

    fun startAlarm(context: Context = app) {
        val intent = Intent(context, xServiceClass()).apply {
            putExtra(KEY_ACTION, ACTION_START_ALARM)
        }
        // 给 service 发送消息
        context.startService(intent)
    }

    fun start() {
        SmsObserver.register(xHandler)
    }
}