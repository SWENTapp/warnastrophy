package com.github.warnastrophy.core.domain.model

import android.content.Context
import android.os.Build
import android.telephony.SmsManager

interface SmsSender {
  fun sendSms(phoneNumber: String, message: EmergencyMessage)
}

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
