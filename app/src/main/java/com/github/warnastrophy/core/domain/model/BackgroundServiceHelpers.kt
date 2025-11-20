package com.github.warnastrophy.core.domain.model

import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat

fun startBackgroundLocationService(context: Context) {
  val intent = Intent(context, BackgroundService::class.java)
  ContextCompat.startForegroundService(context, intent)
}

fun stopBackgroundLocationService(context: Context) {
  val intent = Intent(context, BackgroundService::class.java)
  context.stopService(intent)
}
