package com.github.warnastrophy.core.domain.model

import android.content.Context
import android.os.Build
import android.telephony.SmsManager

fun interface SmsSender {
  /**
   * A functional interface for sending SMS messages.
   *
   * @param phoneNumber The destination phone number.
   * @param message The [EmergencyMessage] to be sent.
   */
  fun sendSms(phoneNumber: String, message: EmergencyMessage)
}

/** An implementation of [SmsSender] that uses Android's [SmsManager]. */
class SmsManagerSender(context: Context) : SmsSender {
  private val smsManager =
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        context.getSystemService(SmsManager::class.java)
      } else {
        @Suppress("DEPRECATION") SmsManager.getDefault() // For API versions ≤ 31
      }

  override fun sendSms(phoneNumber: String, message: EmergencyMessage) {
    smsManager.sendTextMessage(phoneNumber, null, message.toStringMessage(), null, null)
  }
}
