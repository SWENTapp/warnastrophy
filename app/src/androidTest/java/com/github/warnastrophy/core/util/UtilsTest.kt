package com.github.warnastrophy.core.util

import android.content.ContextWrapper
import android.content.Intent
import android.provider.Settings
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertNotNull
import org.junit.Test

class UtilsTest : BaseAndroidComposeTest() {

  @Test
  fun openAppSettings_sendsCorrectIntent() {
    val context = composeTestRule.activity.applicationContext
    val recordingContext =
        object : ContextWrapper(context) {
          var lastIntent: Intent? = null

          override fun startActivity(intent: Intent?) {
            lastIntent = intent
            // Not calling super to avoid real activity launch
          }
        }

    openAppSettings(recordingContext)

    val intent = recordingContext.lastIntent
    assertNotNull(intent)
    assertEquals(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, intent!!.action)
    assertEquals("package", intent.data?.scheme)
    assertEquals(recordingContext.packageName, intent.data?.schemeSpecificPart)
  }
}
