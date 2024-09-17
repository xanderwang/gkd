package li.songe.gkd.service

import android.content.Context
import android.content.Intent
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.os.Handler
import android.os.Looper
import android.os.Message
import androidx.core.app.NotificationManagerCompat
import com.blankj.utilcode.util.LogUtils
import io.xanderwang.service.SmsObserver
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import li.songe.gkd.app
import li.songe.gkd.composition.CompositionExt.useLifeCycleLog
import li.songe.gkd.composition.CompositionExt.useScope
import li.songe.gkd.composition.CompositionService
import li.songe.gkd.notif.abNotif
import li.songe.gkd.notif.alarmNotif
import li.songe.gkd.notif.cancelNotif
import li.songe.gkd.notif.createAlarmNotif
import li.songe.gkd.notif.createNotif
import li.songe.gkd.notif.defaultChannel
import li.songe.gkd.util.clickCountFlow
import li.songe.gkd.util.getSubsStatus
import li.songe.gkd.util.ruleSummaryFlow
import li.songe.gkd.util.storeFlow

class ManageService : CompositionService({
    useLifeCycleLog()
    val context = this
    createNotif(context, defaultChannel.id, abNotif)
    val scope = useScope()
    scope.launch {
        combine(
            GkdAbService.isRunning,
            storeFlow,
            ruleSummaryFlow,
            clickCountFlow,
        ) { abRunning, store, ruleSummary, count ->
            if (!abRunning) return@combine "无障碍未授权"
            if (!store.enableMatch) return@combine "暂停规则匹配"
            if (store.useCustomNotifText) {
                return@combine store.customNotifText
                    .replace("\${i}", ruleSummary.globalGroups.size.toString())
                    .replace("\${k}", ruleSummary.appSize.toString())
                    .replace("\${u}", ruleSummary.appGroupSize.toString())
                    .replace("\${n}", count.toString())
            }
            return@combine getSubsStatus(ruleSummary, count)
        }.debounce(500L).stateIn(scope, SharingStarted.Eagerly, "").collect { text ->
            createNotif(
                context, defaultChannel.id, abNotif.copy(
                    text = text
                )
            )
        }
    }
    isRunning.value = true

    onStartCommand { intent, _, _ ->

        val doAction = intent?.getIntExtra(KEY_DO_ACTION, -1) ?: -1
        LogUtils.d("onStartCommand", intent, doAction)
        when (doAction) {
            MENU_START_ALARM -> {
                msgHandler.removeMessages(MENU_START_ALARM)
                msgHandler.sendMessageDelayed(Message.obtain().apply {
                    what = MENU_START_ALARM
                }, 5000L)
                createAlarmNotif(context, defaultChannel.id, alarmNotif)
            }

            MENU_STOP_ALARM -> {
                if (alarmPlayer?.isPlaying == true) {
                    alarmPlayer?.stop()
                    alarmPlayer?.prepare()
                }
                cancelNotif(context, alarmNotif)
            }

            MENU_START_OBSERVER_SMS -> {
                SmsObserver.register(Handler(), this)
            }

            MENU_STOP_OBSERVER_SMS -> {
                SmsObserver.unregister(this)
            }
        }
    }

    onDestroy {
        isRunning.value = false
    }
}) {


    companion object {

        const val KEY_DO_ACTION = "do_action"
        const val MENU_START_ALARM = 100
        const val MENU_STOP_ALARM = 101
        const val MENU_START_OBSERVER_SMS = 102
        const val MENU_STOP_OBSERVER_SMS = 103

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

        private val msgHandler = Handler(Looper.getMainLooper()) { msg ->
            LogUtils.d("handleMessage", msg)
            when (msg.what) {
                MENU_START_ALARM -> {
                    if (alarmPlayer?.isPlaying == true) {
                        alarmPlayer?.stop()
                        alarmPlayer?.prepare()
                    }
                    alarmPlayer?.start()
                }
            }
            true
        }

        fun start(context: Context = app) {
            context.startForegroundService(Intent(context, ManageService::class.java))
        }

        val isRunning = MutableStateFlow(false)


        fun stop(context: Context = app) {
            context.stopService(Intent(context, ManageService::class.java))
        }

        fun autoStart(context: Context) {
            // 在[系统重启]/[被其它高权限应用重启]时自动打开通知栏状态服务
            if (storeFlow.value.enableStatusService &&
                NotificationManagerCompat.from(context).areNotificationsEnabled() &&
                !isRunning.value
            ) {
                start(context)
            }
        }

        fun startAlarm(context: Context = app) {
            val intent = Intent(context, ManageService::class.java).apply {
                putExtra(KEY_DO_ACTION, MENU_START_ALARM)
            }
            context.startForegroundService(intent)
        }

        fun stopAlarm(context: Context = app) {
            val intent = Intent(context, ManageService::class.java).apply {
                putExtra(KEY_DO_ACTION, MENU_STOP_ALARM)
            }
            context.startForegroundService(intent)
        }

        fun startObserverSMS(context: Context = app) {
            val intent = Intent(context, ManageService::class.java).apply {
                putExtra(KEY_DO_ACTION, MENU_START_OBSERVER_SMS)
            }
            context.startForegroundService(intent)
        }

        fun stopObserverSMS(context: Context = app) {
            val intent = Intent(context, ManageService::class.java).apply {
                putExtra(KEY_DO_ACTION, MENU_STOP_OBSERVER_SMS)
            }
            context.startForegroundService(intent)
        }
    }
}

