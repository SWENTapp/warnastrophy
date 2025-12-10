package com.github.warnastrophy.core.util

import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
import com.github.warnastrophy.core.data.service.ForegroundGpsService

fun startForegroundGpsService(context: Context) {
  val intent = Intent(context, ForegroundGpsService::class.java)
  ContextCompat.startForegroundService(context, intent)
}

fun stopForegroundGpsService(context: Context) {
  val intent = Intent(context, ForegroundGpsService::class.java)
  context.stopService(intent)
}
