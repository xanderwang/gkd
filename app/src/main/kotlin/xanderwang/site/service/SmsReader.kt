package xanderwang.site.service

import android.annotation.SuppressLint
import android.content.Context
import android.database.ContentObserver
import android.database.Cursor
import android.net.Uri
import android.os.Handler
import android.provider.Telephony
import kotlinx.coroutines.launch
import li.songe.gkd.app
import li.songe.gkd.store.xPageStoreFlow
import li.songe.gkd.util.LogUtils
import xanderwang.site.service.XManageService.serviceScope

/**
 * 监听新的消息
 * @author xander
 */
class SmsObserver(handler: Handler) : ContentObserver(handler) {

    override fun deliverSelfNotifications(): Boolean {
        return false
    }

    override fun onChange(selfChange: Boolean, uri: Uri?) {
        super.onChange(selfChange, uri)
        LogUtils.d("onChange", selfChange, uri)
        serviceScope.launch {
            readSms(app).firstOrNull()?.let {
                val msgContent = it.content
                val msgKey = xPageStoreFlow.value.msgKey
                LogUtils.d("onChange content: $msgContent ,from:${it.from}")
                if (msgContent.contains(msgKey) || msgContent.lowercase().contains(msgKey.lowercase())) {
                    XManageService.startAlarm()
                }
            }
        }
    }

    fun register() {
        app.contentResolver.registerContentObserver(Telephony.Sms.CONTENT_URI, true, this)
    }

    fun unregister() {
        app.contentResolver.unregisterContentObserver(this)
    }


    companion object {

        @SuppressLint("StaticFieldLeak")
        private var smsObserver: SmsObserver? = null

        private fun getObserver(handler: Handler): SmsObserver? {
            if (null == smsObserver) {
                smsObserver = SmsObserver(handler)
            }
            return smsObserver
        }

        private fun getObserver(): SmsObserver? {
            return smsObserver
        }

        fun register(handler: Handler) {
            unregister()
            getObserver(handler)?.register()
        }

        fun unregister() {
            getObserver()?.unregister()
        }
    }
}

/**
 * 短信信息
 * @author xanderwang
 */
data class Msg(
    /** id */
    val id: String,
    /** from */
    val from: String,
    /** content */
    val content: String
)

/**
 * 读取短信
 * @param context 上下文
 * @param limit 读取条数
 * @return 短信列表
 */
fun readSms(context: Context, limit: Int = 1): List<Msg> {
    val smsList = mutableListOf<Msg>()
    var cursor: Cursor? = null
    runCatching {
        // 定义短信 URI 和列
        val smsUri: Uri = Telephony.Sms.CONTENT_URI
        val projection = arrayOf(
            Telephony.Sms._ID,
            Telephony.Sms.ADDRESS,
            Telephony.Sms.DATE,
            Telephony.Sms.BODY
        )
        // 查询短信
        cursor = context.contentResolver.query(
            smsUri,
            projection,
            null,
            null,
            Telephony.Sms.DEFAULT_SORT_ORDER
        )
        cursor?.let {
            if (it.moveToFirst()) {
                val idIndex = it.getColumnIndex(Telephony.Sms._ID)
                val addressIndex = it.getColumnIndex(Telephony.Sms.ADDRESS)
                val bodyIndex = it.getColumnIndex(Telephony.Sms.BODY)
                do {
                    val id = it.getString(idIndex)
                    val from = it.getString(addressIndex)
                    val content = it.getString(bodyIndex)
                    smsList.add(Msg(id, from, content))
                } while (it.moveToNext() && smsList.size < limit)
            }

        }
    }.onFailure {
        // LogUtils.d("readMsg error", it)
    }
    cursor?.close()
    return smsList
}
