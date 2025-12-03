package com.github.warnastrophy.core.data.service

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import androidx.core.net.toUri

/** Abstraction for placing phone calls so tests can be mocked. */
fun interface CallSender {
  /** Place a phone call to the given number. Implementations must handle permission checks. */
  fun placeCall(phoneNumber: String)
}

/**
 * A production [CallSender] that uses an application Context to start an ACTION_CALL intent. The
 * provided context should be an application context to avoid leaking activities.
 */
class CallIntentCaller(private val appContext: Context, private val defaultNumber: String) :
    CallSender {
  override fun placeCall(phoneNumber: String) {
    val num = if (phoneNumber.isBlank()) defaultNumber else phoneNumber
    // Check CALL_PHONE permission before trying to start the call.
    val granted =
        ContextCompat.checkSelfPermission(appContext, android.Manifest.permission.CALL_PHONE) ==
            PackageManager.PERMISSION_GRANTED
    if (!granted) {
      // Silently return if permission missing — caller should ensure permission beforehand.
      return
    }

    val intent =
        Intent(Intent.ACTION_CALL).apply {
          data = ("tel:$num").toUri()
          flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }

    appContext.startActivity(intent)
  }
}
