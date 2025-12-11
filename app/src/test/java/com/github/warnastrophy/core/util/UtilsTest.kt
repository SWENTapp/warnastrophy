package com.github.warnastrophy.core.util

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.provider.Settings
import android.widget.Toast
import androidx.browser.customtabs.CustomTabsIntent
import androidx.test.core.app.ApplicationProvider
import com.github.warnastrophy.R
import kotlin.test.assertEquals
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentMatchers.any
import org.mockito.ArgumentMatchers.anyInt
import org.mockito.ArgumentMatchers.argThat
import org.mockito.MockedStatic
import org.mockito.Mockito.doNothing
import org.mockito.Mockito.doThrow
import org.mockito.Mockito.mock
import org.mockito.Mockito.mockStatic
import org.mockito.Mockito.spy
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.mockito.MockitoAnnotations
import org.robolectric.RobolectricTestRunner

/**
 * Test suite for the utility functions defined in `utils.kt`.
 *
 * This class uses Robolectric and Mockito to test the behavior of utility functions such as
 * `openAppSettings` and `openWebPage` under various conditions. It ensures that correct intents are
 * created, edge cases (like null or invalid URLs) are handled, and appropriate user feedback
 * (Toasts) is shown.
 *
 * @see com.github.warnastrophy.core.util.openAppSettings
 * @see com.github.warnastrophy.core.util.openWebPage
 */
@RunWith(RobolectricTestRunner::class)
class UtilsTest {
  private lateinit var context: Context
  private lateinit var mockToast: MockedStatic<Toast>
  private lateinit var mockCustomTabsIntent: MockedStatic<CustomTabsIntent>

  @Before
  fun setUp() {
    MockitoAnnotations.openMocks(this)
    context = ApplicationProvider.getApplicationContext()

    // Mock the static Toast.makeText
    mockToast = mockStatic(Toast::class.java)
    val mockedToastInstance = mock(Toast::class.java)
    `when`(Toast.makeText(any(Context::class.java), anyInt(), anyInt()))
        .thenReturn(mockedToastInstance)

    // Mock CustomTabsIntent builder
    mockCustomTabsIntent = mockStatic(CustomTabsIntent::class.java)
  }

  @After
  fun tearDown() {
    mockToast.close()
    mockCustomTabsIntent.close()
  }

  // Note: This test was originally an android test and got adapted to an unit one with AI
  // assistance
  @Test
  fun openAppSettings_sendsCorrectIntent() {
    val spyContext = spy(context)
    doNothing().`when`(spyContext).startActivity(any(Intent::class.java))

    openAppSettings(spyContext)

    // startActivity was called with the correct Intent
    verify(spyContext)
        .startActivity(
            argThat { intent ->
              assertEquals(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, intent.action)
              assertEquals("package", intent.data?.scheme)
              assertEquals(context.packageName, intent.data?.schemeSpecificPart)
              true
            })
  }

  @Test
  fun openWebPage_nullUrlShowsInvalidUrlToastAndReturns() {
    openWebPage(context, null)

    verify(Toast.makeText(context, R.string.invalid_url, Toast.LENGTH_SHORT)).show()
  }

  @Test
  fun openWebPage_blankUrlShowsInvalidUrlToastAndReturns() {
    openWebPage(context, "   ")

    verify(Toast.makeText(context, R.string.invalid_url, Toast.LENGTH_SHORT)).show()
  }

  @Test
  fun openWebPage_invalidUrlShowsInvalidUrlToastAndReturns() {
    openWebPage(context, "not-a-url")

    verify(Toast.makeText(context, R.string.invalid_url, Toast.LENGTH_SHORT)).show()
  }

  @Test
  fun openWebPage_malformedUrlShowsInvalidUrlToastAndReturns() {
    // This URL will cause `toUri()` to throw an exception because of space
    openWebPage(context, "https://exa mple.com")

    verify(Toast.makeText(context, R.string.invalid_url, Toast.LENGTH_SHORT)).show()
  }

  @Test
  fun openWebPage_validUrlWithCustomTabsSuccessLaunchesCustomTabs() {
    val validUrl = "https://example.com"

    // Mock CustomTabsIntent.launchUrl to not throw
    val mockCustomTabsIntent = mock(CustomTabsIntent::class.java)
    `when`(mockCustomTabsIntent.launchUrl(any(), any())).then {}

    openWebPage(context, validUrl)
  }

  @Test
  fun openWebPage_customTabsFailsButBrowserSucceedsLaunchesBrowser() {
    val validUrl = "https://example.com"

    // Mock CustomTabsIntent to throw exception
    val mockBuilder = mock(CustomTabsIntent.Builder::class.java)
    val mockCustomTabsIntent = mock(CustomTabsIntent::class.java)
    `when`(mockBuilder.build()).thenReturn(mockCustomTabsIntent)
    `when`(mockCustomTabsIntent.launchUrl(any(), any()))
        .thenThrow(RuntimeException("CustomTabs failed"))

    openWebPage(context, validUrl)
  }

  @Test
  fun openWebPage_bothCustomTabsAndBrowserFailShowsNoBrowserToast() {
    val validUrl = "https://example.com"
    val spyContext = spy(context)

    // Mock CustomTabsIntent to throw exception
    val mockBuilder = mock(CustomTabsIntent.Builder::class.java)
    val mockCustomTabsIntent = mock(CustomTabsIntent::class.java)
    `when`(mockBuilder.build()).thenReturn(mockCustomTabsIntent)
    `when`(mockCustomTabsIntent.launchUrl(any(), any()))
        .thenThrow(RuntimeException("CustomTabs failed"))

    // Mock missing browser
    doThrow(ActivityNotFoundException("No browser"))
        .`when`(spyContext)
        .startActivity(any(Intent::class.java))

    openWebPage(spyContext, validUrl)

    verify(Toast.makeText(spyContext, R.string.no_browser_found, Toast.LENGTH_LONG)).show()
  }
}
