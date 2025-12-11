package com.github.warnastrophy.core.util

import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
import com.github.warnastrophy.core.data.service.ForegroundService

fun startForegroundGpsService(context: Context) {
  val intent = Intent(context, ForegroundService::class.java)
  ContextCompat.startForegroundService(context, intent)
}

fun stopForegroundGpsService(context: Context) {
  val intent = Intent(context, ForegroundService::class.java)
  context.stopService(intent)
}
