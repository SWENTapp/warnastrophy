package com.github.warnastrophy.core.data.service

import android.content.Context
import android.os.Build
import android.telephony.SmsManager
import com.github.warnastrophy.core.domain.model.EmergencyMessage

fun interface SmsSender {
  /**
   * A functional interface for sending SMS messages.
   *
   * @param phoneNumber The destination phone number.
   * @param message The [com.github.warnastrophy.core.domain.model.EmergencyMessage] to be sent.
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
    if (phoneNumber.isBlank()) {
      throw IllegalArgumentException(
          "Phone number cannot be empty. Please add an emergency contact.")
    }
    // Basic phone number validation - must contain at least some digits
    if (!phoneNumber.any { it.isDigit() }) {
      throw IllegalArgumentException("Invalid phone number format: $phoneNumber")
    }
    smsManager.sendTextMessage(phoneNumber, null, message.toStringMessage(), null, null)
  }
}
