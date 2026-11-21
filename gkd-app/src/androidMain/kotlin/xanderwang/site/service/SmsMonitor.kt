package xanderwang.site.service

import android.database.ContentObserver
import android.database.Cursor
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.provider.Telephony
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import li.gkd.app.app
import li.gkd.app.logging.LogUtils
import li.gkd.app.permission.PermissionStates
import li.gkd.app.settings.SettingsRepository

/** 常驻服务期间监听短信收件箱, 命中关键字后触发报警 */
object SmsMonitor {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var observer: SmsObserver? = null

    private val handledLock = Any()

    /** 已处理过的短信 id, 避免删除/已读等变更把同一条短信重复报警 */
    private var lastHandledId: String? = null

    private val columns = arrayOf(Telephony.Sms._ID, Telephony.Sms.BODY, Telephony.Sms.TYPE)
    private val inboxSelection = "${Telephony.Sms.TYPE} = ?"
    private val inboxArgs = arrayOf(Telephony.Sms.MESSAGE_TYPE_INBOX.toString())

    fun start() {
        if (observer != null) return
        val item = SmsObserver(Handler(Looper.getMainLooper()))
        app.contentResolver.registerContentObserver(Telephony.Sms.CONTENT_URI, true, item)
        observer = item
    }

    fun stop() {
        observer?.let(app.contentResolver::unregisterContentObserver)
        observer = null
    }

    private class SmsObserver(handler: Handler) : ContentObserver(handler) {
        override fun deliverSelfNotifications() = false

        override fun onChange(selfChange: Boolean, uri: Uri?) {
            super.onChange(selfChange, uri)
            // 行级 uri 形如 content://sms/123, 用它定位该次变更的具体短信; 删除时查不到会自动跳过
            val rowId = uri?.lastPathSegment?.takeIf { it.isNotEmpty() && it.all(Char::isDigit) }
            scope.launch {
                handleChange(rowId)
            }
        }
    }

    private fun handleChange(rowId: String?) {
        val store = SettingsRepository.settings.value
        if (!store.enableWatchSms) return
        val keyword = store.smsWatchKey
        if (keyword.isEmpty()) return
        if (!PermissionStates.readSms.updateAndGet()) return
        val sms = if (rowId != null) readSms(rowId) else readLatestInboxSms()
        if (sms == null || sms.type != Telephony.Sms.MESSAGE_TYPE_INBOX) return
        synchronized(handledLock) {
            if (sms.id == lastHandledId) return
            lastHandledId = sms.id
        }
        if (sms.content.contains(keyword, ignoreCase = true)) {
            // 日志只记事件, 不落发件人/正文/关键字
            LogUtils.i("短信命中关键字, id=${sms.id}")
            SmsAlarm.startAlarm()
        }
    }

    private fun readSms(id: String): SmsMsg? =
        querySms(Telephony.Sms.CONTENT_URI.buildUpon().appendPath(id).build(), null, null)

    private fun readLatestInboxSms(): SmsMsg? =
        querySms(Telephony.Sms.CONTENT_URI, inboxSelection, inboxArgs)

    private fun querySms(uri: Uri, selection: String?, args: Array<String>?): SmsMsg? {
        var cursor: Cursor? = null
        return try {
            cursor =
                app.contentResolver.query(
                    uri,
                    columns,
                    selection,
                    args,
                    Telephony.Sms.DEFAULT_SORT_ORDER,
                )
            if (cursor == null || !cursor.moveToFirst()) {
                null
            } else {
                SmsMsg(
                    id = cursor.getString(cursor.getColumnIndexOrThrow(Telephony.Sms._ID)),
                    content = cursor.getString(cursor.getColumnIndexOrThrow(Telephony.Sms.BODY)),
                    type = cursor.getInt(cursor.getColumnIndexOrThrow(Telephony.Sms.TYPE)),
                )
            }
        } catch (e: Exception) {
            LogUtils.e("读取短信失败", e)
            null
        } finally {
            cursor?.close()
        }
    }
}

data class SmsMsg(
    val id: String,
    val content: String,
    val type: Int,
)
