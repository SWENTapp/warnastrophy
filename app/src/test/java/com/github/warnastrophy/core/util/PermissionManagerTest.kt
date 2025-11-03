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
    prefs.edit().clear().apply() // use apply() for async operation in tests
    // Initialize the manager with the application context, as designed
    manager = PermissionManager(context)
  }
  /**
   * Verifies that `getPermissionResult` returns [PermissionResult.Granted] when the requested
   * permission has already been granted by the user.
   *
   * It mocks the [ContextCompat.checkSelfPermission] to return [PackageManager.PERMISSION_GRANTED]
   * and then asserts that the result of calling `getPermissionResult` is of the type
   * [PermissionResult.Granted].
   */
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

  /**
   * Tests the overloaded `getPermissionResult(permissionType)` function. Verifies that it returns
   * [PermissionResult.Granted] when all permissions are granted, without needing an Activity.
   */
  @Test
  fun getPermissionResult_returns_Granted_when_all_permissions_are_granted() {
    mockStatic(ContextCompat::class.java).use { mocked ->
      mocked
          .`when`<Int> {
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)
          }
          .thenReturn(PackageManager.PERMISSION_GRANTED)

      // Call the function under test (the one without the Activity)
      val result = manager.getPermissionResult(AppPermissions.LocationFine)

      assertTrue(result is PermissionResult.Granted)
    }
  }

  /**
   * Tests the overloaded `getPermissionResult(permissionType)` function. Verifies that it returns
   * [PermissionResult.Denied] when a permission is not granted. This version of the function cannot
   * distinguish between temporary and permanent denial, so it should always return a simple
   * `Denied` result.
   */
  @Test
  fun getPermissionResult_returns_Denied_when_a_permission_is_not_granted() {
    mockStatic(ContextCompat::class.java).use { mocked ->
      mocked
          .`when`<Int> {
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)
          }
          .thenReturn(PackageManager.PERMISSION_DENIED)

      // Call the function under test (the one without the Activity)
      val result = manager.getPermissionResult(AppPermissions.LocationFine)

      assertTrue(result is PermissionResult.Denied)
      val denied = result as PermissionResult.Denied
      assertTrue(denied.deniedPermissions.contains(Manifest.permission.ACCESS_FINE_LOCATION))
    }
  }

  /**
   * Tests that `getPermissionResult` returns [PermissionResult.Denied] when the permission has been
   * previously requested but denied by the user, and the system indicates a rationale should be
   * shown. This state is considered a "temporary denial".
   *
   * The test flow is as follows:
   * 1. Mark the fine location permission as having been asked before. This is a prerequisite for
   *    the permission to be considered temporarily or permanently denied.
   * 2. Mock `ContextCompat.checkSelfPermission` to return `PackageManager.PERMISSION_DENIED`,
   *    simulating that the app does not currently have the permission.
   * 3. Mock `Activity.shouldShowRequestPermissionRationale` to return `true`. This is the key
   *    condition that differentiates a temporary denial from a permanent one. A `true` return value
   *    means the user has denied the permission before, but not with the "Don't ask again" option.
   * 4. Call `getPermissionResult` and assert that the returned result is an instance of
   *    [PermissionResult.Denied].
   */
  @Test
  fun returns_Denied_when_permission_temporarily_denied() {
    // Mark permission as asked before so it can be considered "temporarily denied"
    manager.markPermissionsAsAsked(AppPermissions.LocationFine)

    mockStatic(ContextCompat::class.java).use { mocked ->
      // Mock checkSelfPermission to return DENIED
      mocked
          .`when`<Int> {
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)
          }
          .thenReturn(PackageManager.PERMISSION_DENIED)

      val spyActivity = spy(activity)

      // Mock shouldShowRequestPermissionRationale to return true (simulate show rationale)
      `when`(spyActivity.shouldShowRequestPermissionRationale(anyString())).thenReturn(true)

      val result =
          PermissionManager(spyActivity.applicationContext)
              .getPermissionResult(AppPermissions.LocationFine, spyActivity)
      assertTrue(result is PermissionResult.Denied)
    }
  }

  /**
   * Verifies that [PermissionManager.getPermissionResult] returns
   * [PermissionResult.PermanentlyDenied] when a permission has been denied and the user has
   * selected "Don't ask again".
   *
   * This test case simulates the following scenario:
   * 1. The permission has been requested at least once before.
   * 2. The current permission status is `PERMISSION_DENIED`.
   * 3. The system indicates that a rationale should not be shown
   *    (`shouldShowRequestPermissionRationale` is false), which signifies a permanent denial.
   *
   * It asserts that the result is indeed [PermissionResult.PermanentlyDenied] and that the list of
   * permanently denied permissions contains the expected permission.
   */
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
      val result =
          PermissionManager(spyActivity.applicationContext)
              .getPermissionResult(AppPermissions.LocationFine, spyActivity)

      assertTrue(result is PermissionResult.PermanentlyDenied)
      val denied = result as PermissionResult.PermanentlyDenied
      assertTrue(
          denied.permanentlyDeniedPermissions.contains(Manifest.permission.ACCESS_FINE_LOCATION))
    }
  }

  /**
   * Tests the functionality of [PermissionManager.markPermissionsAsAsked] and
   * [PermissionManager.isPermissionAskedBefore].
   *
   * It verifies that a permission is not marked as "asked before" by default. After calling
   * `markPermissionsAsAsked`, it asserts that `isPermissionAskedBefore` correctly returns true for
   * that same permission.
   */
  @Test
  fun markPermissionsAsAsked_and_isPermissionAskedBefore_work_correctly() {
    val type = AppPermissions.LocationFine
    assertFalse(manager.isPermissionAskedBefore(type))
    manager.markPermissionsAsAsked(type)
    assertTrue(manager.isPermissionAskedBefore(type))
  }
}
