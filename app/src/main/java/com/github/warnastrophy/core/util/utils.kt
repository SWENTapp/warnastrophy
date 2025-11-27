package com.github.warnastrophy.core.util

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import java.io.File
import java.text.SimpleDateFormat
import java.util.Locale

fun debugPrint(msg: String) {
  val file = File("debug.txt")
  file.appendText(msg + "\n")
}

fun isValidPhoneNumber(phone: String): Boolean {
  // Regex for basic validation: optional '+' at start, followed by 10-15 digits
  return phone.matches(Regex("^\\+?[0-9]{10,15}\$"))
}

fun Context.findActivity(): Activity? {
  var context = this
  while (context is ContextWrapper) {
    if (context is Activity) return context

    context = context.baseContext
  }
  return null
}

fun formatDate(date: String): String {
  val inputFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
  val outputFormat = SimpleDateFormat("dd MMMM", Locale.getDefault())
  return try {
    val parsedDate = inputFormat.parse(date.substring(0, 10))
    outputFormat.format(parsedDate ?: "")
  } catch (e: Exception) {
    ""
  }
}
