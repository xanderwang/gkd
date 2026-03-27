package xanderwang.site.service

import android.annotation.SuppressLint
import android.content.Context
import android.database.ContentObserver
import android.database.Cursor
import android.net.Uri
import android.os.Handler
import android.provider.Telephony
import com.blankj.utilcode.util.LogUtils
import li.songe.gkd.store.xPageStoreFlow

/**
 * 监听新的消息
 * @author xander
 */
class SmsObserver(val context: Context, handler: Handler) : ContentObserver(handler) {

    override fun deliverSelfNotifications(): Boolean {
        return false
    }

    override fun onChange(selfChange: Boolean, uri: Uri?) {
        super.onChange(selfChange, uri)
        LogUtils.d("onChange", selfChange, uri)
        readSms(context).firstOrNull()?.let {
            val msgContent = it.content
            val msgKey = xPageStoreFlow.value.msgKey
            LogUtils.d("onChange content: $msgContent ,from:${it.from}")
            if (msgContent.contains(msgKey) || msgContent.lowercase().contains(msgKey.lowercase())) {
                XManageService.startAlarm()
            }
        }
    }

    fun register() {
        context.contentResolver.registerContentObserver(Telephony.Sms.CONTENT_URI, true, this)
    }

    fun unregister() {
        context.contentResolver.unregisterContentObserver(this)
    }


    companion object {

        @SuppressLint("StaticFieldLeak")
        private var smsObserver: SmsObserver? = null

        private fun getObserver(context: Context? = null, handler: Handler? = null): SmsObserver? {
            if (null == smsObserver && null != context && null != handler) {
                smsObserver = SmsObserver(context, handler)
            }
            return smsObserver
        }

        fun register(context: Context, handler: Handler) {
            unregister()
            getObserver(context, handler)?.register()
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
        val cursor: Cursor? = context.contentResolver.query(
            smsUri,
            projection,
            null,
            null,
            Telephony.Sms.DEFAULT_SORT_ORDER
        )
        cursor?.let {
            if (cursor.moveToFirst()) {
                val idIndex = cursor.getColumnIndex(Telephony.Sms._ID)
                val addressIndex = cursor.getColumnIndex(Telephony.Sms.ADDRESS)
                val bodyIndex = cursor.getColumnIndex(Telephony.Sms.BODY)
                do {
                    val id = cursor.getString(idIndex)
                    val from = cursor.getString(addressIndex)
                    val content = cursor.getString(bodyIndex)
                    smsList.add(Msg(id, from, content))
                } while (cursor.moveToNext() && smsList.size < limit)
            }
            it.close()
        }
    }.onFailure {
        LogUtils.e("readMsg error", it)
    }
    return smsList
}
