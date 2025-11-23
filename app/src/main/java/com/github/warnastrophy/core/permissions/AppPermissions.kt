package com.github.warnastrophy.core.permissions

import android.Manifest
import android.os.Build
import androidx.annotation.ChecksSdkIntAtLeast

/**
 * A sealed class to define and manage different sets of permissions required by the app.
 *
 * The constructor uses 'vararg' to allow passing permissions as a comma-separated list. The
 * permissions are stored as an Array<String> to avoid later conversions, as Android's permission
 * APIs expect an array.
 *
 * @property permissions The array of Android permission strings.
 * @property key A stable, non-changing string identifier for this permission set, used for storage.
 *   This value MUST NOT be changed once defined.
 */
sealed class AppPermissions(val key: String, vararg permissionsWithNulls: String?) {
  // Filter out nulls from the incoming vararg and convert to an array.
  val permissions: Array<String> = permissionsWithNulls.filterNotNull().toTypedArray()

  /**
   * Permissions required for accessing the user's PRECISE (fine) location.
   * - From Android 12 (API 31) onwards, requesting FINE is sufficient. The system dialog will let
   *   the user choose between fine and coarse.
   * - Before Android 12, both must be requested to offer a choice.
   */
  object LocationFine :
      AppPermissions(
          key = "location_fine",
          // The common permission required for all versions
          Manifest.permission.ACCESS_FINE_LOCATION,

          // Conditionally add ACCESS_COARSE_LOCATION for versions below Android 12 (S)
          if (!isAtLeastSdkVersion(Build.VERSION_CODES.S)) {
            Manifest.permission.ACCESS_COARSE_LOCATION
          } else {
            // No extra permission needed for S+
            null
          })

  /** Permissions required for accessing the user's APPROXIMATE (coarse) location only. */
  object LocationCoarse :
      AppPermissions(key = "location_coarse", Manifest.permission.ACCESS_COARSE_LOCATION)

  /** Permissions required to enable alert mode */
  // !!! Add more permissions if necessary later (background, notifications, etc) !!!
  object AlertModePermission :
      AppPermissions(key = "alert_mode_permissions", Manifest.permission.ACCESS_FINE_LOCATION)

  // !!! Add permissions object for Inactivity Detection if necessary !!!

  /**
   * Foreground service location permission introduced on Android 14 (API 34). Request this
   * separately when starting a location foreground service on SDK 34+.
   */
  object ForegroundServiceLocation :
      AppPermissions(
          key = "foreground_service_location",
          if (isAtLeastSdkVersion(34)) {
            Manifest.permission.FOREGROUND_SERVICE_LOCATION
            Manifest.permission.POST_NOTIFICATIONS
          } else {
            null
          })

  /** Permissions required to send emergency messages */
  object SendEmergencySms :
      AppPermissions(
          key = "send_sms",
          Manifest.permission.SEND_SMS,
          Manifest.permission.ACCESS_FINE_LOCATION,
      )

  companion object {
    /** A reusable check for SDK versions, annotated to help the compiler with smart casting. */
    @ChecksSdkIntAtLeast(parameter = 0)
    private fun isAtLeastSdkVersion(sdkInt: Int): Boolean {
      return Build.VERSION.SDK_INT >= sdkInt
    }
  }

  // To add a new permission set, create a new 'object' that extends 'AppPermissions'.
  // Define the required permissions within its constructor.
  // Use 'Build.VERSION.SDK_INT' checks for permissions that are version-specific.
  // This allows for handling permissions that were added in newer Android versions.
  // For example, POST_NOTIFICATIONS was introduced in Android 13 (TIRAMISU).
  // By checking the SDK version, we only request the permission on devices where it's relevant.
  // This prevents the app from crashing on older versions when trying to request a non-existent
  // permission.

  // Example:
  // object Notifications : AppPermissions(
  //     if (isAtLeastSdkVersion(Build.VERSION_CODES.TIRAMISU)) {
  //         Manifest.permission.POST_NOTIFICATIONS
  //     } else {
  //         null // No permission needed for older versions
  //     }
  // )
}
