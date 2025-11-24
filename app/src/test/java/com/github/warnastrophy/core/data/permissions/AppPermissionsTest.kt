package com.github.warnastrophy.core.data.permissions

import android.Manifest
import android.os.Build
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertTrue
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
class AppPermissionsTest {

  @Test
  fun LocationCoarse_should_contain_only_coarse_location_permission() {
    val perms = AppPermissions.LocationCoarse.permissions
    Assert.assertEquals(1, perms.size)
    Assert.assertEquals(Manifest.permission.ACCESS_COARSE_LOCATION, perms.first())
  }

  /**
   * Test to verify that LocationFine permission set includes both fine and coarse location
   * permissions on Android versions below 12 (S).
   */
  @Test
  @Config(sdk = [Build.VERSION_CODES.R]) // Android 11
  fun LocationFine_should_include_coarse_location_below_Android_12() {
    val perms = AppPermissions.LocationFine.permissions
    Assert.assertTrue(perms.contains(Manifest.permission.ACCESS_FINE_LOCATION))
    Assert.assertTrue(perms.contains(Manifest.permission.ACCESS_COARSE_LOCATION))
  }

  /**
   * Test to verify that LocationFine permission set includes only fine location permission on
   * Android 12 (S) and above.
   */
  @Test
  @Config(sdk = [Build.VERSION_CODES.S]) // Android 12+
  fun LocationFine_should_NOT_include_coarse_location_on_Android_12_and_above() {
    val perms = AppPermissions.LocationFine.permissions
    Assert.assertTrue(perms.contains(Manifest.permission.ACCESS_FINE_LOCATION))
    Assert.assertFalse(perms.contains(Manifest.permission.ACCESS_COARSE_LOCATION))
  }

  @Test
  fun SendSms_should_contain_correct_permission() {
    val perms = AppPermissions.SendEmergencySms.permissions
    assertEquals(2, perms.size)
    assertTrue(perms.contains(Manifest.permission.SEND_SMS))
    assertTrue(perms.contains(Manifest.permission.ACCESS_FINE_LOCATION))
  }
}
