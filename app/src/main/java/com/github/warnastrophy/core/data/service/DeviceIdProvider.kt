package com.github.warnastrophy.core.data.service

import android.content.Context
import java.util.UUID

object DeviceIdProvider {
  private const val PREFS = "warnastrophy_prefs"
  private const val KEY = "device_id"
  @Volatile private var cached: String? = null

  fun get(context: Context): String {
    cached?.let {
      return it
    }
    val prefs = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
    val id =
        prefs.getString(KEY, null)
            ?: UUID.randomUUID().toString().also { prefs.edit().putString(KEY, it).apply() }
    cached = id
    return id
  }
}
