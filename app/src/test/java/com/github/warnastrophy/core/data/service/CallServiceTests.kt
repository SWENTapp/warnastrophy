package com.github.warnastrophy.core.data.service

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import androidx.core.content.ContextCompat
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentCaptor
import org.mockito.Mockito
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class CallServiceTests {
  @Test
  fun placeCall_withPermission_startsCallIntent() {
    val mockContext = Mockito.mock(Context::class.java)

    // Mock static ContextCompat.checkSelfPermission to return GRANTED
    val ctxCompatStatic = Mockito.mockStatic(ContextCompat::class.java)
    ctxCompatStatic
        .`when`<Int> {
          ContextCompat.checkSelfPermission(mockContext, Manifest.permission.CALL_PHONE)
        }
        .thenReturn(PackageManager.PERMISSION_GRANTED)

    val caller = CallIntentCaller(mockContext, "+0000000000")

    caller.placeCall("+123456789")

    // Capture the intent passed to startActivity
    val intentCaptor = ArgumentCaptor.forClass(Intent::class.java)
    Mockito.verify(mockContext).startActivity(intentCaptor.capture())

    val captured = intentCaptor.value
    assert(captured.action == Intent.ACTION_CALL)
    assert(captured.data == Uri.parse("tel:+123456789"))
    assert((captured.flags and Intent.FLAG_ACTIVITY_NEW_TASK) != 0)

    ctxCompatStatic.close()
  }

  @Test
  fun placeCall_withoutPermission_doesNotStartActivity() {
    val mockContext = Mockito.mock(Context::class.java)

    val ctxCompatStatic = Mockito.mockStatic(ContextCompat::class.java)
    ctxCompatStatic
        .`when`<Int> {
          ContextCompat.checkSelfPermission(mockContext, Manifest.permission.CALL_PHONE)
        }
        .thenReturn(PackageManager.PERMISSION_DENIED)

    val caller = CallIntentCaller(mockContext, "+0000000000")

    caller.placeCall("+123456789")

    // Verify startActivity was never called
    Mockito.verify(mockContext, Mockito.never()).startActivity(Mockito.any(Intent::class.java))

    ctxCompatStatic.close()
  }

  @Test
  fun placeCall_withPermission_andBlankNumber_usesDefaultNumber() {
    val mockContext = Mockito.mock(Context::class.java)

    val ctxCompatStatic = Mockito.mockStatic(ContextCompat::class.java)
    ctxCompatStatic
        .`when`<Int> {
          ContextCompat.checkSelfPermission(mockContext, Manifest.permission.CALL_PHONE)
        }
        .thenReturn(PackageManager.PERMISSION_GRANTED)

    val defaultNum = "+0000000000"
    val caller = CallIntentCaller(mockContext, defaultNum)

    caller.placeCall("") // blank -> should use defaultNum

    val intentCaptor = ArgumentCaptor.forClass(Intent::class.java)
    Mockito.verify(mockContext).startActivity(intentCaptor.capture())

    val captured = intentCaptor.value
    assert(captured.action == Intent.ACTION_CALL)
    assert(captured.data == Uri.parse("tel:$defaultNum"))
    assert((captured.flags and Intent.FLAG_ACTIVITY_NEW_TASK) != 0)

    ctxCompatStatic.close()
  }
}
