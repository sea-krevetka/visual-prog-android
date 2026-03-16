package com.example.calc.ui

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.example.calc.R
import org.zeromq.SocketType
import org.zeromq.ZContext
import org.zeromq.ZMQ

class ZMQActivity : AppCompatActivity() {

    private lateinit var tvZmqLog: TextView
    private lateinit var btnStartServer: Button
    private lateinit var btnStartClient: Button
    private lateinit var btnStopAll: Button

    private val handler = Handler(Looper.getMainLooper())
    private val TAG = "ZMQActivity"

    @Volatile
    private var serverRunning = false
    @Volatile
    private var clientRunning = false

    private var serverThread: Thread? = null
    private var clientThread: Thread? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_zmq)

        tvZmqLog = findViewById(R.id.tvZmqLog)
        btnStartServer = findViewById(R.id.btnStartServer)
        btnStartClient = findViewById(R.id.btnStartClient)
        btnStopAll = findViewById(R.id.btnStopAll)

        btnStartServer.setOnClickListener { startServer() }
        btnStartClient.setOnClickListener { startClient() }
        btnStopAll.setOnClickListener { stopAll() }
    }

    private fun appendLog(message: String) {
        handler.post {
            tvZmqLog.append(message + "\n")
        }
        Log.d(TAG, message)
    }

    private fun startServer() {
        if (serverRunning) {
            appendLog("Server already running")
            return
        }
        serverRunning = true
        serverThread = Thread {
            ZContext().use { context ->
                context.createSocket(SocketType.REP).use { socket ->
                    try {
                        socket.bind("tcp://*:2222")
                        appendLog("[SERVER] Bound to tcp://*:2222")

                        var counter = 0
                        while (serverRunning) {
                            val requestBytes = socket.recv(0) ?: break
                            val request = String(requestBytes, ZMQ.CHARSET)
                            counter++
                            appendLog("[SERVER] Received request (#$counter): $request")
                            Thread.sleep(1000)
                            val response = "Hello from Android ZMQ Server (#$counter)"
                            socket.send(response.toByteArray(ZMQ.CHARSET), 0)
                            appendLog("[SERVER] Sent reply (#$counter): $response")
                        }
                    } catch (t: Throwable) {
                        if (serverRunning) appendLog("[SERVER] Error: ${t.message}")
                    } finally {
                        appendLog("[SERVER] Stopped")
                        serverRunning = false
                        updateButtons()
                    }
                }
            }
        }
        serverThread?.start()
        updateButtons()
    }

    private fun startClient() {
        if (clientRunning) {
            appendLog("Client already running")
            return
        }
        clientRunning = true
        clientThread = Thread {
            ZContext().use { context ->
                context.createSocket(SocketType.REQ).use { socket ->
                    try {
                        socket.connect("tcp://127.0.0.1:2222")
                        appendLog("[CLIENT] Connected to tcp://127.0.0.1:2222")

                        var counter = 0
                        while (clientRunning && counter < 1000) {
                            val request = "Hello from Android client! #${counter}"
                            socket.send(request.toByteArray(ZMQ.CHARSET), 0)
                            appendLog("[CLIENT] Sent (#$counter): $request")
                            val reply = socket.recv(0) ?: break
                            appendLog("[CLIENT] Received (#$counter): ${String(reply, ZMQ.CHARSET)}")
                            counter++
                            Thread.sleep(1000)
                        }
                    } catch (t: Throwable) {
                        if (clientRunning) appendLog("[CLIENT] Error: ${t.message}")
                    } finally {
                        appendLog("[CLIENT] Stopped")
                        clientRunning = false
                        updateButtons()
                    }
                }
            }
        }
        clientThread?.start()
        updateButtons()
    }

    private fun stopAll() {
        appendLog("Stopping all ZMQ threads...")
        serverRunning = false
        clientRunning = false

        serverThread?.interrupt()
        clientThread?.interrupt()

        try {
            serverThread?.join(2000)
            clientThread?.join(2000)
        } catch (e: InterruptedException) {
            Thread.currentThread().interrupt()
        }

        appendLog("All stopped")
        updateButtons()
    }

    private fun updateButtons() {
        handler.post {
            btnStartServer.isEnabled = !serverRunning
            btnStartClient.isEnabled = !clientRunning
            btnStopAll.isEnabled = serverRunning || clientRunning
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        stopAll()
    }
}
