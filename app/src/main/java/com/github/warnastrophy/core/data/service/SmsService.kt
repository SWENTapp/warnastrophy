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
    require(!(phoneNumber.isBlank())) {
      "Phone number cannot be empty. Please add an emergency contact."
    }
    require(phoneNumber.any { it.isDigit() }) { "Invalid phone number format: $phoneNumber" }

    val messageText = message.toStringMessage()
    if (messageText.length > 160) {
      val parts = smsManager.divideMessage(messageText)
      smsManager.sendMultipartTextMessage(phoneNumber, null, parts, null, null)
    } else {
      smsManager.sendTextMessage(phoneNumber, null, messageText, null, null)
    }
  }
}
