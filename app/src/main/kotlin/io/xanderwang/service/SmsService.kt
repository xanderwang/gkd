package io.xanderwang.service

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.database.ContentObserver
import android.database.Cursor
import android.net.Uri
import android.os.Handler
import android.provider.Telephony
import android.telephony.SmsMessage
import android.widget.Toast
import com.blankj.utilcode.util.LogUtils
import li.songe.gkd.service.ManageService
import li.songe.gkd.util.storeFlow

/**
 * 监听新的消息
 * @author xander
 */
class SmsObserver(handler: Handler, val context: Context) : ContentObserver(handler) {

    override fun deliverSelfNotifications(): Boolean {
        return false
    }

    override fun onChange(selfChange: Boolean, uri: Uri?) {
        super.onChange(selfChange, uri)
        LogUtils.d("onChange", selfChange, uri)
        readMsg(context).firstOrNull()?.let {
            val msgContent = it.content
            val msgKey = storeFlow.value.msgContentKey
            LogUtils.d("onChange content: $msgContent ,from:${it.from}")
            if (msgContent.contains(msgKey) || msgContent.lowercase().contains(msgKey.lowercase())) {
                ManageService.startAlarm()
            }
        }
    }


    companion object {

        @SuppressLint("StaticFieldLeak")
        private var observer: SmsObserver? = null

        private fun getObserver(handler: Handler, context: Context): SmsObserver? {
            if (null == observer) {
                observer = SmsObserver(handler, context)
            }
            return observer
        }

        fun register(handler: Handler, context: Context) {
            unregister(context)
            getObserver(handler, context)?.let { o ->
                context.contentResolver.registerContentObserver(Telephony.Sms.CONTENT_URI, true, o)
            }
        }

        fun unregister(context: Context) {
            observer?.let { o ->
                context.contentResolver.unregisterContentObserver(o)
            }
        }
    }
}

/**
 * 消息广播接收器
 * @author xander
 */
class SmsReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context?, intent: Intent) {
        val intentAction = intent.action
        LogUtils.d("onReceive: $intent")
        if (intentAction == SMS_RECEIVER_ACTION) {
            // intent.getExtras()方法就是从过滤后的意图中获取携带的数据，
            // 这里携带的是以"pdus"为key、短信内容为value的键值对
            // android设备接收到的 SMS 是 pdu 形式的
            val format = intent.getStringExtra("format")
            intent.extras?.let { bundle ->
                var fullContent = ""
                var from = ""
                var msgKey = ""
                val pdus: Array<ByteArray> = bundle["pdus"] as? Array<ByteArray> ?: return@let
                for (pdu in pdus) {
                    val msg = SmsMessage.createFromPdu(pdu, format)
                    fullContent += msg.displayMessageBody
                    from = msg.originatingAddress ?: ""
                    msgKey = storeFlow.value.msgContentKey
                }
                Toast.makeText(context, "收到信息: $fullContent", Toast.LENGTH_LONG).show()
                LogUtils.d("onReceive content: $fullContent ,from:$from")
                if (fullContent.contains(msgKey) || fullContent.lowercase().contains(msgKey.lowercase())) {
                    // startAlarmService()
                }
            }
        }

    }

    companion object {


        private val SMS_RECEIVER by lazy { SmsReceiver() }

        private const val SMS_RECEIVER_ACTION = "android.provider.Telephony.SMS_RECEIVED"
        private const val SMS_DELIVER_ACTION = "android.provider.Telephony.SMS_DELIVER"

        fun register(context: Context) {
            unregister(context)
            LogUtils.d("register")
            val smsFilter = IntentFilter()
            smsFilter.addAction(SMS_RECEIVER_ACTION)
            smsFilter.addAction(SMS_DELIVER_ACTION)
            kotlin.runCatching {
                context.registerReceiver(SMS_RECEIVER, smsFilter)
            }
        }

        fun unregister(context: Context) {
            LogUtils.d("unregister")
            kotlin.runCatching {
                context.unregisterReceiver(SMS_RECEIVER)
            }
        }

    }

}

/**
 * 短信信息
 */
data class Msg(val id: String, val from: String, val content: String)

/**
 * 读取短信
 * @param context 上下文
 * @param limit 读取条数
 * @return 短信列表
 */
fun readMsg(context: Context, limit: Int = 1): List<Msg> {
    val list = mutableListOf<Msg>()
    runCatching {
        // 定义短信 URI 和列
        val smsUri: Uri = Telephony.Sms.CONTENT_URI
        val projection = arrayOf(Telephony.Sms._ID, Telephony.Sms.ADDRESS, Telephony.Sms.DATE, Telephony.Sms.BODY)
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
                    list += Msg(id, from, content)
                } while (cursor.moveToNext() && list.size < limit)
            }
            it.close()
        }
    }.onFailure {
        LogUtils.e("readMsg error", it)
    }

    return list
}
