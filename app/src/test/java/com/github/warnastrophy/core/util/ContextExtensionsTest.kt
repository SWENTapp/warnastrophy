package com.github.warnastrophy.core.util

import android.app.Activity
import android.content.ContextWrapper
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class ContextExtensionsTest {

  @Test
  fun returns_activity_when_context_IS_an_activity() {
    val activity = Robolectric.buildActivity(Activity::class.java).get()
    assertEquals(activity, activity.findActivity())
  }

  @Test
  fun returns_wrapped_activity_when_context_is_ContextWrapper() {
    val activity = Robolectric.buildActivity(Activity::class.java).get()
    val wrapped = ContextWrapper(activity)
    assertEquals(activity, wrapped.findActivity())
  }

  @Test
  fun returns_nested_wrapped_activity_when_context_is_multiple_wrappers() {
    val activity = Robolectric.buildActivity(Activity::class.java).get()
    val deepWrapper = ContextWrapper(ContextWrapper(ContextWrapper(activity)))
    assertEquals(activity, deepWrapper.findActivity())
  }

  @Test
  fun returns_null_when_context_chain_has_no_activity() {
    val plainContext = Robolectric.buildActivity(Activity::class.java).get().applicationContext
    val wrapper = ContextWrapper(plainContext)
    assertNull(wrapper.findActivity())
  }
}
