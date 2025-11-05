package com.github.warnastrophy.core.util

import androidx.test.espresso.IdlingResource
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class AnimationIdlingResourceTest {
  private lateinit var idlingResource: AnimationIdlingResource

  @Before
  fun setUp() {
    idlingResource = AnimationIdlingResource()
  }

  @Test
  fun initiallyIdle() {
    assertTrue(idlingResource.isIdleNow)
  }

  @Test
  fun setBusySetsIdleToFalse() {
    idlingResource.setBusy()
    assertFalse(idlingResource.isIdleNow)
  }

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

  @Test
  fun getNameReturnsClassName() {
    assertEquals(AnimationIdlingResource::class.java.name, idlingResource.name)
  }

  @Test
  fun registerIdleTransitionCallbackStoresCallback() {
    val dummyCallback = IdlingResource.ResourceCallback {}
    idlingResource.registerIdleTransitionCallback(dummyCallback)

    // Using reflection to verify private field is set (optional)
    val field = AnimationIdlingResource::class.java.getDeclaredField("resourceCallback")
    field.isAccessible = true
    val storedCallback = field.get(idlingResource)
    assertSame(dummyCallback, storedCallback)
  }
}
