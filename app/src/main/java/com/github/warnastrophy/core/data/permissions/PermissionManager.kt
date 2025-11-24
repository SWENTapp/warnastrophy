package com.github.warnastrophy.core.data.permissions

import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import androidx.core.content.edit
import com.github.warnastrophy.core.util.AppConfig

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

interface PermissionManagerInterface {
  /**
   * Gets the permission result without checking the rationale. This is safe to call during
   * ViewModel initialization as it doesn't require an `Activity` context.
   *
   * This method can distinguish between [PermissionResult.Granted] and [PermissionResult.Denied],
   * but it **cannot** detect the [PermissionResult.PermanentlyDenied] state because that requires
   * checking the rationale with an `Activity`. For that, use the overloaded version of
   * [getPermissionResult] that takes an `Activity`.
   *
   * @param permissionType The group of permissions to check.
   * @return [PermissionResult.Granted] if all permissions are granted, otherwise
   *   [PermissionResult.Denied].
   */
  fun getPermissionResult(permissionType: AppPermissions): PermissionResult

  /**
   * Analyzes the state of a group of permissions and returns a detailed PermissionResult. This
   * correctly handles mixed states (some granted, some denied, some permanently denied).
   *
   * @param permissionType The [AppPermissions] group to analyze.
   * @param activity The [Activity] required to check for rationale.
   * @return A [PermissionResult] sealed class instance describing the collective status.
   */
  fun getPermissionResult(permissionType: AppPermissions, activity: Activity): PermissionResult

  /**
   * Marks a specific permission group as having been asked for at least once.
   *
   * This is crucial for differentiating between a simple denial and a "permanently denied" state
   * (where the user has checked "Don't ask again"). The flag is stored in SharedPreferences.
   *
   * @param permissionType The permission group (e.g., location, camera) to mark as asked.
   */
  fun markPermissionsAsAsked(permissionType: AppPermissions)

  /**
   * Checks if a permission request for a specific [AppPermissions] group has been made before. This
   * is determined by checking a flag in SharedPreferences. This flag should be set using
   * [markPermissionsAsAsked] after the first time the permission dialog is shown.
   *
   * @param permissionType The permission group to check.
   * @return `true` if the permission group has been requested before, `false` otherwise.
   */
  fun isPermissionAskedBefore(permissionType: AppPermissions): Boolean
}

/**
 * A utility class for managing Android runtime permissions.
 *
 * This class simplifies the process of checking the status of one or more permissions, handling the
 * nuances of permanently denied permissions (the "Don't ask again" scenario), and tracking whether
 * a permission group has been requested previously. It uses [android.content.SharedPreferences] to
 * persist the "asked" state across app sessions.
 *
 * @param context The [Context] used to access SharedPreferences for tracking the "asked" state of
 *   permissions.
 */
class PermissionManager(private val context: Context) : PermissionManagerInterface {
  private val prefs = context.getSharedPreferences(AppConfig.PREF_FILE_NAME, Context.MODE_PRIVATE)

  companion object {
    @Volatile private var instance: PermissionManager? = null

    fun getInstance(context: Context): PermissionManager {
      return instance
          ?: synchronized(this) {
            instance ?: PermissionManager(context.applicationContext).also { instance = it }
          }
    }
  }

  override fun getPermissionResult(permissionType: AppPermissions): PermissionResult {
    val permissions = permissionType.permissions
    if (permissions.isEmpty()) return PermissionResult.Granted

    val deniedPermissions =
        permissions.filter {
          ContextCompat.checkSelfPermission(context, it) != PackageManager.PERMISSION_GRANTED
        }

    return if (deniedPermissions.isEmpty()) {
      PermissionResult.Granted
    } else {
      PermissionResult.Denied(deniedPermissions)
    }
  }

  override fun getPermissionResult(
      permissionType: AppPermissions,
      activity: Activity
  ): PermissionResult {
    if (permissionType.permissions.isEmpty()) {
      return PermissionResult.Granted // No permissions to check, so they are considered granted.
    }

    val permissionsState =
        permissionType.permissions.groupBy { permission ->
          when {
            ContextCompat.checkSelfPermission(context, permission) ==
                PackageManager.PERMISSION_GRANTED -> PermissionStatus.GRANTED
            isPermissionAskedBefore(permissionType) &&
                !activity.shouldShowRequestPermissionRationale(permission) ->
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

  override fun isPermissionAskedBefore(permissionType: AppPermissions): Boolean {
    val prefKey = "perm_asked_${permissionType.key}"
    return prefs.getBoolean(prefKey, false)
  }

  override fun markPermissionsAsAsked(permissionType: AppPermissions) {
    val prefKey = "perm_asked_${permissionType.key}"
    prefs.edit { putBoolean(prefKey, true) }
  }
}
