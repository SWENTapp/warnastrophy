package com.github.warnastrophy.core.util

import androidx.test.espresso.IdlingResource
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class AnimationIdlingResourceTest {
  private lateinit var idlingResource: AnimationIdlingResource

  @Before
  fun setUp() {
    idlingResource = AnimationIdlingResource()
  }

  /** Verifies that the initial state of the `AnimationIdlingResource` is idle. */
  @Test
  fun initiallyIdle() {
    assertTrue(idlingResource.isIdleNow)
  }

  /**
   * Verifies that calling `setBusy()` on the `AnimationIdlingResource` correctly transitions its
   * state to busy
   */
  @Test
  fun setBusySetsIdleToFalse() {
    idlingResource.setBusy()
    assertFalse(idlingResource.isIdleNow)
  }

  /**
   * Verifies that calling `setIdle()` on the `AnimationIdlingResource` correctly transitions its
   * state to idle and triggers the registered resource callback.
   */
  @Test
  fun setIdleSetsIdleToTrueAndCallsCallback() {
    var callbackCalled = false
    val callback = IdlingResource.ResourceCallback { callbackCalled = true }
    idlingResource.registerIdleTransitionCallback(callback)

    idlingResource.setBusy() // set busy first
    idlingResource.setIdle() // then idle again

    assertTrue(idlingResource.isIdleNow)
    assertTrue(callbackCalled)
  }

  /**
   * Verifies that the `name` property of the `AnimationIdlingResource` correctly returns the fully
   * qualified class name.
   */
  @Test
  fun getNameReturnsClassName() {
    assertEquals(AnimationIdlingResource::class.java.name, idlingResource.name)
  }

  /**
   * Verifies that `registerIdleTransitionCallback` correctly stores the provided
   * [IdlingResource.ResourceCallback].
   *
   * This test uses reflection to access the private `resourceCallback` field within the
   * [AnimationIdlingResource] and asserts that it is the same instance as the one passed to the
   * registration method.
   */
  @Test
  fun registerIdleTransitionCallbackStoresCallback() {
    val dummyCallback = IdlingResource.ResourceCallback {}
    idlingResource.registerIdleTransitionCallback(dummyCallback)

    // Using reflection to verify private field is set
    val p0 = "resourceCallback"
    val field = AnimationIdlingResource::class.java.getDeclaredField(p0)
    field.isAccessible = true
    val storedCallback = field.get(idlingResource)
    assertSame(dummyCallback, storedCallback)
  }
}
