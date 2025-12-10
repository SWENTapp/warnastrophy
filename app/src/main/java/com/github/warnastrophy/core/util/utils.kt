package com.github.warnastrophy.core.util

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.widget.Toast
import androidx.browser.customtabs.CustomTabsIntent
import androidx.core.net.toUri
import com.github.warnastrophy.R
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
 * Opens a URL in a Chrome Custom Tab with a fallback to a standard browser. It validates the URL,
 * displays user-friendly error messages for invalid URLs or if no browser is found.
 *
 * @param context The context to use for launching the intent and displaying Toasts.
 * @param url The URL string to open.
 */
fun openWebPage(context: Context, url: String?) {
  if (url.isNullOrBlank()) {
    Toast.makeText(context, R.string.invalid_url, Toast.LENGTH_SHORT).show()
    return
  }

  val webPageUri =
      try {
        url.toUri()
      } catch (_: Exception) {
        Toast.makeText(context, R.string.invalid_url, Toast.LENGTH_SHORT).show()
        return
      }

  // Try Custom Tabs first, then browser, then toast
  try {
    val customTabsIntent = CustomTabsIntent.Builder().setShowTitle(true).build()
    customTabsIntent.launchUrl(context, webPageUri)
  } catch (_: Exception) {
    try {
      val browserIntent = Intent(Intent.ACTION_VIEW, webPageUri)
      context.startActivity(browserIntent)
    } catch (_: Exception) {
      Toast.makeText(context, R.string.no_browser_found, Toast.LENGTH_LONG).show()
    }
  }
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

/**
 * Checks if the current list is a subsequence of another list.
 *
 * A list is considered a subsequence of another if all its elements appear in the same order within
 * the other list, but not necessarily consecutively.
 *
 * @param other The list to check against.
 * @return `true` if the current list is a subsequence of the other list, `false` otherwise.
 */
fun <T> List<T>.isSubsequenceOf(other: List<T>): Boolean {
  if (this.isEmpty()) return true
  var i = 0
  for (item in other) {
    if (item == this[i]) {
      i++
      if (i == this.size) return true
    }
  }
  return false
}
