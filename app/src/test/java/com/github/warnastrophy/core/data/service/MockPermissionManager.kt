package com.github.warnastrophy.core.data.service

import android.app.Activity
import com.github.warnastrophy.core.permissions.AppPermissions
import com.github.warnastrophy.core.permissions.PermissionManagerInterface
import com.github.warnastrophy.core.permissions.PermissionResult

/**
 * TODO: REMOVE THIS LATER BECAUSE ITS A DUPLICATE OF MockPermissionManager in androidTest A
 *   test-friendly subclass of [PermissionManager] that allows manually controlling the permission
 *   state without requiring Android APIs.
 */
class MockPermissionManager(
    private var currentResult: PermissionResult = PermissionResult.Denied(listOf("FAKE_PERMISSION"))
) : PermissionManagerInterface {

  /** Sets what result should be returned for permission checks. */
  fun setPermissionResult(result: PermissionResult) {
    currentResult = result
  }

  /** Returns the injected permission result instead of checking Android APIs. */
  override fun getPermissionResult(permissionType: AppPermissions): PermissionResult {
    return currentResult
  }

  /** Also override the Activity-based version (so tests can call either). */
  override fun getPermissionResult(
      permissionType: AppPermissions,
      activity: Activity
  ): PermissionResult {
    return currentResult
  }

  override fun markPermissionsAsAsked(permissionType: AppPermissions) {
    // no-op for tests
  }

  override fun isPermissionAskedBefore(permissionType: AppPermissions): Boolean {
    // Optional: you can simulate “has been asked” logic
    return true
  }
}
