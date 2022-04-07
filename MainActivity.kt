package com.example.socketapp

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.telecom.Call
import android.telecom.CallScreeningService
import android.telephony.TelephonyManager
import android.util.Log
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import okhttp3.*
import okio.ByteString
import java.time.LocalDateTime


internal var CALLER_ID: String = ""

internal var prevState: String = ""

class MainActivity : AppCompatActivity() {

    private lateinit var client: OkHttpClient

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        client = OkHttpClient()

        findViewById<TextView>(R.id.callerIdText).setOnClickListener {
            start()
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (applicationContext.checkSelfPermission(android.Manifest.permission.READ_PHONE_STATE)
                != PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(
                        this,
                        arrayOf(android.Manifest.permission.READ_PHONE_STATE),
                        1
                    )
                }
            }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (applicationContext.checkSelfPermission(android.Manifest.permission.PROCESS_OUTGOING_CALLS)
                != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(android.Manifest.permission.READ_PHONE_STATE),
                    1
                )
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (applicationContext.checkSelfPermission(android.Manifest.permission.BIND_SCREENING_SERVICE)
                != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(android.Manifest.permission.READ_PHONE_STATE),
                    1
                )
            }
        }

        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.READ_CALL_LOG)
            != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(android.Manifest.permission.READ_PHONE_STATE),
                1
            )
        }
    }

    private fun start() {
        val request = Request.Builder().url("wss://websocket-echo.glitch.me").build()
        val listener = WebSocket()
        val ws = client.newWebSocket(request, listener)

        client.dispatcher.executorService.shutdown()
    }




    inner class WebSocket : WebSocketListener() {

        private var status = 1000

        override fun onOpen(webSocket: okhttp3.WebSocket, response: Response) {
            super.onOpen(webSocket, response)
            webSocket.send("87475605287")
            webSocket.close(status, "CLOSE")
        }

        override fun onMessage(webSocket: okhttp3.WebSocket, text: String) {
            super.onMessage(webSocket, text)

            findViewById<TextView>(R.id.callerIdText).text = text
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

        @RequiresApi(Build.VERSION_CODES.O)
        override fun onReceive(ctx: Context?, intent: Intent?) {

            when {
                intent?.getStringExtra(TelephonyManager.EXTRA_STATE).equals(TelephonyManager.EXTRA_STATE_OFFHOOK) -> {
                    prevState = TelephonyManager.EXTRA_STATE_OFFHOOK
                    Toast.makeText(ctx, "Трубку подняли: ${LocalDateTime.now()}", Toast.LENGTH_SHORT).show()
                }

                intent?.getStringExtra(TelephonyManager.EXTRA_STATE).equals(TelephonyManager.EXTRA_STATE_IDLE) -> {
                    if (prevState != "") {
                        Toast.makeText(ctx, "Звонок завершился: ${LocalDateTime.now()}", Toast.LENGTH_SHORT).show()
                    }
                }

                intent?.getStringExtra(TelephonyManager.EXTRA_STATE).equals(TelephonyManager.EXTRA_STATE_RINGING) -> {
                    Toast.makeText(ctx, "Позвонили: ${LocalDateTime.now()}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}