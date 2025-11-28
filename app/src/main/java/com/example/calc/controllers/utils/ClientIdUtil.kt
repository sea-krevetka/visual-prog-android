package com.example.calc.controller.utils

import android.content.Context
import java.util.UUID

object ClientIdUtil {
    private const val PREFS = "zmq_prefs"
    private const val KEY_CLIENT_ID = "client_id"

    fun getClientId(context: Context): String {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        var id = prefs.getString(KEY_CLIENT_ID, null)
        if (id == null) {
            id = UUID.randomUUID().toString()
            prefs.edit().putString(KEY_CLIENT_ID, id).apply()
        }
        return id
    }
}
