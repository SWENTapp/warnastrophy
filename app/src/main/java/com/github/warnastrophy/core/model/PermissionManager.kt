package com.github.warnastrophy.core.model

import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
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

/**
 * Internal enum to categorize the status of a single permission. This is used within
 * [PermissionManager] to group permissions before determining the final, public [PermissionResult].
 */
private enum class PermissionStatus {
  GRANTED,
  PERMANENTLY_DENIED,
  TEMPORARILY_DENIED
}

/**
 * A utility class for managing Android runtime permissions within a specific [Activity].
 *
 * This class simplifies the process of checking the status of one or more permissions, handling the
 * nuances of permanently denied permissions (the "Don't ask again" scenario), and tracking whether
 * a permission group has been requested previously. It uses [android.content.SharedPreferences] to
 * persist the "asked" state across app sessions.
 *
 * It is designed to be instantiated with a reference to the current `Activity`, which is necessary
 * for checking rationales (`shouldShowRequestPermissionRationale`).
 *
 * @param activity The [Activity] context from which permissions are being checked or requested.
 *   This is crucial for correctly determining the rationale status.
 */
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

  /**
   * Checks if a permission request for a specific [AppPermissions] group has been made before. This
   * is determined by checking a flag in SharedPreferences. This flag should be set using
   * [markPermissionsAsAsked] after the first time the permission dialog is shown.
   *
   * @param permissionType The permission group to check.
   * @return `true` if the permission group has been requested before, `false` otherwise.
   */
  fun isPermissionAskedBefore(permissionType: AppPermissions): Boolean {
    val prefKey = "perm_asked_${permissionType::class.simpleName}"
    return prefs.getBoolean(prefKey, false)
  }

  /**
   * Marks a specific permission group as having been asked for at least once.
   *
   * This is crucial for differentiating between a simple denial and a "permanently denied" state
   * (where the user has checked "Don't ask again"). The flag is stored in SharedPreferences.
   *
   * @param permissionType The permission group (e.g., location, camera) to mark as asked.
   */
  fun markPermissionsAsAsked(permissionType: AppPermissions) {
    val prefKey = "perm_asked_${permissionType::class.simpleName}"
    prefs.edit { putBoolean(prefKey, true) }
  }
}
