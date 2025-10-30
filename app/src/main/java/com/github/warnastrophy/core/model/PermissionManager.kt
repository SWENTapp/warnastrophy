package com.github.warnastrophy.core.model

import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import androidx.compose.ui.geometry.isEmpty
import androidx.core.content.ContextCompat
import androidx.core.content.edit

/**
 * Represents the possible outcomes of a permission request. A sealed class is used to allow states
 * to carry data.
 */
sealed class PermissionResult {
  /** The requested permissions have been granted by the user. */
  object Granted : PermissionResult()

  /**
   * Some or all requested permissions were denied.
   *
   * @property deniedPermissions A list of permissions that were denied.
   */
  data class Denied(val deniedPermissions: List<String>) : PermissionResult()

  /**
   * Some or all requested permissions were permanently denied. The user must go to app settings to
   * grant them.
   *
   * @property permanentlyDeniedPermissions A list of permissions that were permanently denied.
   */
  data class PermanentlyDenied(val permanentlyDeniedPermissions: List<String>) : PermissionResult()
}

private enum class PermissionStatus {
  GRANTED,
  PERMANENTLY_DENIED,
  TEMPORARILY_DENIED
}

class PermissionManager(private val activity: Activity) {
  private val context: Context = activity.applicationContext
  private val prefs = context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)

  /**
   * Analyzes the state of a group of permissions and returns a detailed PermissionResult. This
   * correctly handles mixed states (some granted, some denied, some permanently denied).
   *
   * @param permissionType The group of permissions to analyze.
   * @return A [PermissionResult] sealed class instance describing the collective status.
   */
  fun getPermissionResult(permissionType: AppPermissions): PermissionResult {
    if (permissionType.permissions.isEmpty()) {
      return PermissionResult.Granted // No permissions to check, so they are considered granted.
    }

    val prefKey = "perm_asked_${permissionType::class.simpleName}"
    val hasBeenAsked = prefs.getBoolean(prefKey, false)

    val permissionsState =
        permissionType.permissions.groupBy { permission ->
          when {
            ContextCompat.checkSelfPermission(context, permission) ==
                PackageManager.PERMISSION_GRANTED -> PermissionStatus.GRANTED
            hasBeenAsked && !activity.shouldShowRequestPermissionRationale(permission) ->
                PermissionStatus.PERMANENTLY_DENIED
            else -> PermissionStatus.TEMPORARILY_DENIED
          }
        }

    val permanentlyDenied = permissionsState[PermissionStatus.PERMANENTLY_DENIED] ?: emptyList()
    val temporarilyDenied = permissionsState[PermissionStatus.TEMPORARILY_DENIED] ?: emptyList()

    // Determine the final, prioritized result
    return when {
      // If any permission is permanently denied, that is the most critical state to report.
      permanentlyDenied.isNotEmpty() -> PermissionResult.PermanentlyDenied(permanentlyDenied)

      // If none are permanently denied, but some are temporarily denied, report that.
      temporarilyDenied.isNotEmpty() -> PermissionResult.Denied(temporarilyDenied)

      // If all permissions have been evaluated and none are denied, they must all be granted.
      else -> PermissionResult.Granted
    }
  }

  fun isGranted(permissionResult: PermissionResult): Boolean {
    return permissionResult is PermissionResult.Granted
  }

  fun isPermissionAskedBefore(permissionType: AppPermissions): Boolean {
    val prefKey = "perm_asked_${permissionType::class.simpleName}"
    return prefs.getBoolean(prefKey, false)
  }

  fun markPermissionsAsAsked(permissionType: AppPermissions) {
    val prefKey = "perm_asked_${permissionType::class.simpleName}"
    prefs.edit { putBoolean(prefKey, true) }
  }
}
