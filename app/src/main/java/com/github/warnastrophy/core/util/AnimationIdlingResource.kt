package com.github.warnastrophy.core.util

import androidx.test.espresso.IdlingResource

/**
 * An [IdlingResource] for Espresso tests that waits for animations or other asynchronous operations
 * to complete.
 *
 * This resource can be manually controlled to signal the start and end of a long-running operation.
 * When an operation starts, call [setBusy] to mark the resource as not idle. When the operation
 * completes, call [setIdle] to signal that Espresso can continue with its test assertions.
 *
 * Example Usage:
 * ```
 * // In your test setup:
 * val idlingResource = AnimationIdlingResource()
 * IdlingRegistry.getInstance().register(idlingResource)
 *
 * // In your application code, before starting an animation:
 * idlingResource.setBusy()
 *
 * // In the animation's onEnd listener:
 * a.addListener(object : AnimatorListenerAdapter() {
 *     override fun onAnimationEnd(animation: Animator) {
 *         idlingResource.setIdle()
 *     }
 * })
 *
 * // In your test teardown:
 * IdlingRegistry.getInstance().unregister(idlingResource)
 * ```
 *
 * _Note: Check `onTrackLocationClicked` in MapViewModel for an example of how to use this resource.
 * Check `trackLocationButtonAnimatesOnClick` in MapScreenTest for an example of how to use this
 * resource in tests._
 */
class AnimationIdlingResource : IdlingResource {
  /**
   * A flag to indicate the idle state of the resource.
   *
   * It is marked as `@Volatile` to ensure that changes made by one thread are immediately visible
   * to other threads, which is crucial for synchronization between the application thread and the
   * test thread.
   */
  @Volatile private var isIdle = true
  private var resourceCallback: IdlingResource.ResourceCallback? = null

  /**
   * Sets the resource to be busy. This should be called when an animation or long-running operation
   * starts.
   */
  fun setBusy() {
    isIdle = false
  }

  /**
   * Sets the resource to an idle state.
   *
   * This function marks the resource as no longer busy and notifies the registered
   * [IdlingResource.ResourceCallback] that the resource has transitioned to idle. This allows
   * Espresso to continue with the test execution.
   */
  fun setIdle() {
    isIdle = true
    resourceCallback?.onTransitionToIdle()
  }

  /**
   * Returns the name of the [IdlingResource], which is used for logging and debugging. This
   * implementation returns the class name of [AnimationIdlingResource].
   */
  override fun getName(): String = AnimationIdlingResource::class.java.name

  /**
   * Returns true if the resource is currently idle. Espresso polls this method to check if the
   * application is currently busy.
   */
  override fun isIdleNow() = isIdle

  /**
   * Registers the given [callback] with the resource.
   *
   * This method is called by the Espresso framework to register a callback that will be invoked
   * when the resource transitions to the idle state. The resource should hold a reference to this
   * callback and call [IdlingResource.ResourceCallback.onTransitionToIdle] on it when it becomes
   * idle.
   *
   * @param callback The callback to be registered.
   */
  override fun registerIdleTransitionCallback(callback: IdlingResource.ResourceCallback) {
    resourceCallback = callback
  }
}
