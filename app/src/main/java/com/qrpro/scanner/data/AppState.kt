package com.qrpro.scanner.data

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import org.json.JSONArray
import org.json.JSONObject

/** Settings + scan history, persisted in SharedPreferences. */
class AppState(context: Context) {

    private val prefs = context.applicationContext
        .getSharedPreferences("qr_pro", Context.MODE_PRIVATE)

    var vibrate by mutableStateOf(prefs.getBoolean(KEY_VIBRATE, true))
        private set
    var beep by mutableStateOf(prefs.getBoolean(KEY_BEEP, true))
        private set
    var saveHistory by mutableStateOf(prefs.getBoolean(KEY_SAVE_HISTORY, true))
        private set

    val history = mutableStateListOf<HistoryItem>()

    init {
        history.addAll(loadHistory())
    }

    fun updateVibrate(value: Boolean) {
        vibrate = value
        prefs.edit().putBoolean(KEY_VIBRATE, value).apply()
    }

    fun updateBeep(value: Boolean) {
        beep = value
        prefs.edit().putBoolean(KEY_BEEP, value).apply()
    }

    fun updateSaveHistory(value: Boolean) {
        saveHistory = value
        prefs.edit().putBoolean(KEY_SAVE_HISTORY, value).apply()
    }

    fun addHistory(raw: String, format: String) {
        if (!saveHistory) return
        history.removeAll { it.raw == raw }
        history.add(0, HistoryItem(raw, format, System.currentTimeMillis()))
        while (history.size > MAX_ITEMS) history.removeAt(history.lastIndex)
        persist()
    }

    fun remove(item: HistoryItem) {
        history.remove(item)
        persist()
    }

    fun clearHistory() {
        history.clear()
        persist()
    }

    private fun persist() {
        val arr = JSONArray()
        history.forEach {
            arr.put(
                JSONObject()
                    .put("raw", it.raw)
                    .put("format", it.format)
                    .put("time", it.time)
            )
        }
        prefs.edit().putString(KEY_HISTORY, arr.toString()).apply()
    }

    private fun loadHistory(): List<HistoryItem> {
        val json = prefs.getString(KEY_HISTORY, null) ?: return emptyList()
        return try {
            val arr = JSONArray(json)
            (0 until arr.length()).map { i ->
                val o = arr.getJSONObject(i)
                HistoryItem(o.getString("raw"), o.optString("format", "QR Code"), o.getLong("time"))
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    private companion object {
        const val KEY_VIBRATE = "vibrate"
        const val KEY_BEEP = "beep"
        const val KEY_SAVE_HISTORY = "save_history"
        const val KEY_HISTORY = "history"
        const val MAX_ITEMS = 300
    }
}
