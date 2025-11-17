package com.github.warnastrophy.core.domain.model

import android.content.Context
import android.os.Build
import android.telephony.SmsManager

/** Defines the contract for a service that sends SMS messages. */
interface SmsSender {
  /**
   * Sends an SMS message to the specified phone number.
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
        SmsManager.getDefault() // For API versions ≤ 31
      }

  override fun sendSms(phoneNumber: String, message: EmergencyMessage) {
    smsManager.sendTextMessage(phoneNumber, null, message.toStringMessage(), null, null)
  }
}
