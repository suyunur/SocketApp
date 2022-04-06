package com.example.socketapp

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.telephony.PhoneStateListener
import android.telephony.TelephonyCallback
import android.telephony.TelephonyManager
import android.util.Log
import android.widget.TextView
import android.widget.Toast
import okhttp3.*
import okio.ByteString
import java.time.LocalDateTime


internal var CALLER_ID: String = ""

class MainActivity : AppCompatActivity() {

    private lateinit var client: OkHttpClient

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        client = OkHttpClient()

        findViewById<TextView>(R.id.callerIdText).setOnClickListener {
            start()
        }
    }

    private fun start() {
        val request = Request.Builder().url("ws://echo.websocket.org").build()
        val listener = WebSocket()
        val ws = client.newWebSocket(request, listener)

        client.dispatcher.executorService.shutdown()
    }

    inner class WebSocket : WebSocketListener() {

        private var status = 1000

        override fun onOpen(webSocket: okhttp3.WebSocket, response: Response) {
            super.onOpen(webSocket, response)
            webSocket.send("87071312133")
            Log.d("SEND", "SEND")
            webSocket.close(status, "CLOSE")
        }

        override fun onMessage(webSocket: okhttp3.WebSocket, text: String) {
            super.onMessage(webSocket, text)

            findViewById<TextView>(R.id.callerIdText).text = text
            Toast.makeText(this@MainActivity, "Номер с вэб сокета: $text", Toast.LENGTH_SHORT).show()
            CALLER_ID = text
        }

        override fun onMessage(webSocket: okhttp3.WebSocket, bytes: ByteString) {
            super.onMessage(webSocket, bytes)
            print(bytes.hex())
        }

        override fun onClosing(webSocket: okhttp3.WebSocket, code: Int, reason: String) {
            super.onClosing(webSocket, code, reason)
            webSocket.close(status, null)
            print("$code / $reason")
        }

        override fun onFailure(webSocket: okhttp3.WebSocket, t: Throwable, response: Response?) {
            super.onFailure(webSocket, t, response)
            print(t.message)
        }
    }

    class CallReceiver : BroadcastReceiver() {
        var prev_state: Int? = null

        override fun onReceive(p0: Context?, p1: Intent?) {
            val tm: TelephonyManager = p0?.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                tm.registerTelephonyCallback(
                    p0.mainExecutor,
                    object : TelephonyCallback(), TelephonyCallback.CallStateListener {
                        override fun onCallStateChanged(state: Int) {
                            when (state) {
                                TelephonyManager.CALL_STATE_RINGING -> {

                                    val number = p1?.extras?.getString("incoming_number")
                                    Toast.makeText(p0, "Номер: $number", Toast.LENGTH_SHORT).show()
                                    Toast.makeText(p0, "Звонок пришел: ${LocalDateTime.now()}", Toast.LENGTH_SHORT).show()

                                    if (number.equals(CALLER_ID)) {
                                        tm.javaClass.getMethod("answerRingingCall").invoke(tm)
                                        Toast.makeText(p0, "Трубку подняли: ${LocalDateTime.now()}", Toast.LENGTH_SHORT).show()
                                    }

                                    prev_state = state
                                }

                                TelephonyManager.CALL_STATE_OFFHOOK -> {
                                    prev_state = state

                                    Toast.makeText(p0, "Звонок завершился: ${LocalDateTime.now()}", Toast.LENGTH_SHORT).show()
                                }

                                TelephonyManager.CALL_STATE_IDLE -> {
                                    if (prev_state == TelephonyManager.CALL_STATE_OFFHOOK) {
                                        prev_state = state
                                        //CAll ended
                                    }
                                }
                            }
                        }
                    }
                )
            } else {
                tm.listen(object : PhoneStateListener() {
                    override fun onCallStateChanged(state: Int, phoneNumber: String?) {
                        super.onCallStateChanged(state, phoneNumber)
                    }
                }, PhoneStateListener.LISTEN_CALL_STATE)
            }
        }
    }
}