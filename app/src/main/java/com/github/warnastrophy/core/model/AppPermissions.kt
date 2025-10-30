package com.github.warnastrophy.core.model

import android.Manifest
import android.os.Build
import androidx.annotation.ChecksSdkIntAtLeast

/**
 * A sealed class to define and manage different sets of permissions required by the app. This
 * structure is scalable and allows for easy addition of new permission groups.
 *
 * The constructor uses 'vararg' to allow passing permissions as a comma-separated list. The
 * permissions are stored as an Array<String> to avoid later conversions, as Android's permission
 * APIs expect an array.
 */
sealed class AppPermissions(vararg permissionsWithNulls: String?) {
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
  object LocationCoarse : AppPermissions(Manifest.permission.ACCESS_COARSE_LOCATION)

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
