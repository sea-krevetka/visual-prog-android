package com.example.calc.controller.utils

import android.content.Context
import android.util.Log
import com.example.calc.data.repository.SendLogRepository
import com.google.gson.Gson
import org.zeromq.SocketType
import org.zeromq.ZContext
import org.zeromq.ZMQ
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean

class ZmqSender(private val context: Context, private val endpoint: String) {
    private val TAG = "ZmqSender"
    private var zmqContext: ZContext? = null
    private var socket: ZMQ.Socket? = null
    private val executor: ScheduledExecutorService = Executors.newSingleThreadScheduledExecutor()
    private val retrying = AtomicBoolean(false)
    private val gson = Gson()
    private val logRepo = SendLogRepository(context)

    private data class QueueEntry(val payload: String, var attempts: Int = 0)
    private val queue = java.util.concurrent.LinkedBlockingQueue<QueueEntry>()
    private var connected = false

    fun start() {
        if (connected) return
        zmqContext = ZContext()
        socket = zmqContext!!.createSocket(SocketType.REQ)
        socket!!.connect(endpoint)
        connected = true
        Log.d(TAG, "Connected to $endpoint")
        startWorker()
    }

    fun sendAsync(payload: String) {
        if (!connected) start()
        val entry = QueueEntry(payload, 0)
        queue.offer(entry)
        try {
            logRepo.addLog(status = "queued", payload = mapOf("payload" to gson.fromJson(payload, Map::class.java)), attempts = entry.attempts)
        } catch (_: Throwable) {}
    }

    private fun startWorker() {
        if (retrying.getAndSet(true)) return
        executor.submit {
            while (connected) {
                try {
                    val entry = queue.poll(1, TimeUnit.SECONDS)
                    if (entry == null) continue
                    trySendEntry(entry)
                } catch (t: Throwable) {
                    Log.w(TAG, "worker loop failure: ${t.message}")
                }
            }
            retrying.set(false)
        }
    }

    private fun trySendEntry(entry: QueueEntry) {
        try {
            socket?.send(entry.payload.toByteArray(ZMQ.CHARSET), 0)
            val reply = socket?.recv(0)
            val replyText = reply?.let { String(it, ZMQ.CHARSET) } ?: ""
            logRepo.addLog(status = "sent", payload = mapOf("reply" to replyText, "payload" to gson.fromJson(entry.payload, Map::class.java)), attempts = entry.attempts)
            Log.d(TAG, "ZMQ send ack: $replyText")
        } catch (t: Throwable) {
            entry.attempts += 1
            logRepo.addLog(status = "error", payload = mapOf("payload" to gson.fromJson(entry.payload, Map::class.java), "error" to t.message), attempts = entry.attempts)
            if (entry.attempts <= 5) {
                // Exponential backoff schedule
                val delay = (Math.pow(2.0, entry.attempts.toDouble()) * 1000L).toLong()
                Log.d(TAG, "retrying in ${delay}ms; attempt=${entry.attempts}")
                executor.schedule({ queue.offer(entry) }, delay, TimeUnit.MILLISECONDS)
            } else {
                Log.w(TAG, "Dropping entry after ${entry.attempts} attempts")
            }
        }
    }

    fun stop() {
        try {
            connected = false
            executor.shutdownNow()
            executor.awaitTermination(1, TimeUnit.SECONDS)
        } catch (t: Throwable) {
            Log.w(TAG, "shutdown executor failed: ${t.message}")
        }
        try { socket?.close() } catch (t: Throwable) {}
        try { zmqContext?.close() } catch (t: Throwable) {}
        connected = false
    }
}
