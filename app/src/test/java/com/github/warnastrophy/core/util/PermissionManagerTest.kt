package com.github.warnastrophy.core.util

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.SharedPreferences
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import com.github.warnastrophy.core.model.AppPermissions
import com.github.warnastrophy.core.model.PermissionManager
import com.github.warnastrophy.core.model.PermissionResult
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.*
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class PermissionManagerTest {

  private lateinit var activity: Activity
  private lateinit var context: Context
  private lateinit var prefs: SharedPreferences
  private lateinit var manager: PermissionManager

  @Before
  fun setup() {
    activity = Robolectric.buildActivity(Activity::class.java).get()
    context = activity.applicationContext
    prefs = context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
    prefs.edit().clear().commit()
    manager = PermissionManager(activity)
  }

  @Test
  fun returns_Granted_when_all_permissions_are_granted() {
    // Mock permission check to always grant
    mockStatic(ContextCompat::class.java).use { mocked ->
      mocked
          .`when`<Int> {
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)
          }
          .thenReturn(PackageManager.PERMISSION_GRANTED)

      val result = manager.getPermissionResult(AppPermissions.LocationFine)
      assertTrue(result is PermissionResult.Granted)
    }
  }

  @Test
  fun returns_Denied_when_permission_temporarily_denied() {
    // Mark permission as asked before so it can be considered "temporarily denied"
    manager.markPermissionsAsAsked(AppPermissions.LocationFine)

    mockStatic(ContextCompat::class.java).use { mocked ->
      mocked
          .`when`<Int> {
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)
          }
          .thenReturn(PackageManager.PERMISSION_DENIED)

      // Mock shouldShowRequestPermissionRationale to true (temporarily denied)
      val spyActivity = spy(activity)
      `when`(spyActivity.shouldShowRequestPermissionRationale(anyString())).thenReturn(true)
      val result = PermissionManager(spyActivity).getPermissionResult(AppPermissions.LocationFine)
      assertTrue(result is PermissionResult.Denied)
    }
  }

  @Test
  fun returns_PermanentlyDenied_when_permission_permanently_denied() {
    // Mark permission as asked before
    manager.markPermissionsAsAsked(AppPermissions.LocationFine)

    mockStatic(ContextCompat::class.java).use { mocked ->
      mocked
          .`when`<Int> {
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)
          }
          .thenReturn(PackageManager.PERMISSION_DENIED)

      // Mock rationale = false (means permanently denied)
      val spyActivity = spy(activity)
      `when`(spyActivity.shouldShowRequestPermissionRationale(anyString())).thenReturn(false)
      val result = PermissionManager(spyActivity).getPermissionResult(AppPermissions.LocationFine)

      assertTrue(result is PermissionResult.PermanentlyDenied)
      val denied = result as PermissionResult.PermanentlyDenied
      assertTrue(
          denied.permanentlyDeniedPermissions.contains(Manifest.permission.ACCESS_FINE_LOCATION))
    }
  }

  @Test
  fun markPermissionsAsAsked_and_isPermissionAskedBefore_work_correctly() {
    val type = AppPermissions.LocationFine
    assertFalse(manager.isPermissionAskedBefore(type))
    manager.markPermissionsAsAsked(type)
    assertTrue(manager.isPermissionAskedBefore(type))
  }
}
