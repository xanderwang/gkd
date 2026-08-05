package xanderwang.site.service

import android.app.Service
import android.content.Context
import android.content.Intent
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.os.Handler
import android.os.Looper
import android.os.Message
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import li.songe.gkd.app
import li.songe.gkd.service.StatusService
import li.songe.gkd.store.xPageStoreFlow
import li.songe.gkd.util.LogUtils
import li.songe.gkd.util.toast
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
        runCatching {
            if (alarmPlayer?.isPlaying == true) {
                alarmPlayer?.stop()
            }
        }
    }

    private fun startAlarm() {
        runCatching {
            stopAlarm()
            alarmPlayer?.prepare()
            alarmPlayer?.start()
        }
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
        }
        return false
    }

    fun startAlarm(context: Context = app) {
        // 短信监控绑定常驻服务，服务未运行时不再后台拉起，避免 BackgroundServiceStartNotAllowedException
        if (!StatusService.isRunning.value) return
        val intent = Intent(context, xServiceClass()).apply {
            putExtra(KEY_ACTION, ACTION_START_ALARM)
        }
        context.startService(intent)
    }

    /** 服务启动时调用 */
    fun start() {
        if (xPageStoreFlow.value.enableWatchSMS) {
            SmsObserver.register(xHandler)
        }
    }

    /** 开启短信监听 */
    fun startWatch() {
        if (StatusService.isRunning.value) {
            SmsObserver.register(xHandler)
        } else {
            toast("请先开启常驻服务")
        }
    }

    /** 关闭短信监听 */
    fun stopWatch() {
        SmsObserver.unregister()
    }

    /** 服务销毁时调用：注销监听、停止报警、清理通知 */
    fun stop(context: Context = app) {
        stopWatch()
        xHandler.removeCallbacksAndMessages(null)
        stopAlarm()
        cancelNotif(context, smsNotif)
    }
}