package com.github.warnastrophy.core.util

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import java.io.File
import java.text.SimpleDateFormat
import java.util.Locale

/**
 * Appends a message to a debug.txt file for debugging purposes.
 *
 * @param msg The message to write to the debug file.
 */
fun debugPrint(msg: String) {
  val file = File("debug.txt")
  file.appendText(msg + "\n")
}

/**
 * Validates a phone number using a simple regex.
 *
 * The regex checks for an optional '+' at the start, followed by 10 to 15 digits.
 *
 * @param phone The phone number string to validate.
 * @return `true` if the phone number is valid, `false` otherwise.
 */
fun isValidPhoneNumber(phone: String): Boolean {
  // Regex for basic validation: optional '+' at start, followed by 10-15 digits
  return phone.matches(Regex("^\\+?[0-9]{10,15}\$"))
}

/**
 * Traverses the context wrapper to find the base activity.
 *
 * @return The [Activity] if found, otherwise `null`.
 */
fun Context.findActivity(): Activity? {
  var context = this
  while (context is ContextWrapper) {
    if (context is Activity) return context

    context = context.baseContext
  }
  return null
}

/**
 * Open Application's internal settings
 *
 * @param context The context to use for starting the activity.
 */
fun openAppSettings(context: Context) {
  val intent =
      Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
        data = Uri.fromParts("package", context.packageName, null)
      }
  context.startActivity(intent)
}

/**
 * Formats a date string from "yyyy-MM-dd" to "dd MMMM".
 *
 * For example, "2023-10-27" becomes "27 October".
 *
 * @param date The date string in "yyyy-MM-dd" format.
 * @return The formatted date string, or an empty string if parsing fails.
 */
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
