package com.github.warnastrophy.core.ui.util

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.junit4.createComposeRule
import org.junit.Rule

/**
 * Base class providing helper methods for testing Compose UI in Android. Defines common
 * functionality, such as managing timeouts and helper methods to wait for idle or custom
 * conditions.
 */
abstract class BaseComposeTest {

  companion object {
    /** Default timeout duration (5000 milliseconds = 5 seconds) */
    const val DEFAULT_TIMEOUT = 5000L
    /** Extended timeout duration (10000 milliseconds = 10 seconds) */
    const val EXTENDED_TIMEOUT = 10000L
  }

  /**
   * The default timeout duration for operations. Can be overridden in subclasses to set a custom
   * timeout
   */
  open val defaultTimeout: Long = DEFAULT_TIMEOUT

  /**
   * Waits for the Compose UI to be idle and then waits until the specified timeout is reached.
   *
   * @param timeoutMillis The maximum time (in milliseconds) to wait for idle and for the condition
   *   to complete. Default value is [defaultTimeout]
   */
  protected fun ComposeContentTestRule.waitForIdleWithTimeout(
      timeoutMillis: Long = defaultTimeout
  ) {
    waitForIdle()
    waitUntil(timeoutMillis = timeoutMillis) { true }
  }

  /**
   * Waits until a specific condition is met or the timeout is reached.
   *
   * @param timeoutMillis The maximum time (in milliseconds) to wait for the condition to be true.
   *   Default value is [defaultTimeout].
   * @param condition A lambda function that returns a [Boolean] indicating if the condition is met.
   */
  protected fun ComposeContentTestRule.waitUntilWithTimeout(
      timeoutMillis: Long = defaultTimeout,
      condition: () -> Boolean
  ) {
    waitUntil(timeoutMillis = timeoutMillis, condition = condition)
  }
}

/**
 * Base class for testing Compose UI in Android with a [ComponentActivity] context. Provides a
 * [composeTestRule] for interacting with Compose UI components in the context of an Android
 * [ComponentActivity].
 */
abstract class BaseAndroidComposeTest : BaseComposeTest() {
  /**
   * [Rule] that provides access to [ComposeTestRule] for Android-specific Compose UI tests. This
   * rule is responsible for launching and managing the lifecycle of a [ComponentActivity] for
   * testing.
   */
  @get:Rule val composeTestRule = createAndroidComposeRule<ComponentActivity>()

  /**
   * Setup method for initializing necessary state or resources before each test. Can be overridden
   * by subclasses.
   */
  open fun setUp() {
    // Override to implement custom setup behavior.
  }

  /**
   * Teardown method for cleaning up resources or performing actions after each test. Can be
   * overridden by subclasses.
   */
  open fun tearDown() {
    // Override to implement custom teardown behavior.
  }
}

/**
 * Base class for simple Compose UI tests that don't require an Android [ComponentActivity] context.
 * Uses [createComposeRule] for managing the Compose UI test lifecycle.
 */
abstract class BaseSimpleComposeTest : BaseComposeTest() {

  /**
   * [Rule] that provides access to [ComposeTestRule] for simple Compose UI tests. This rule is used
   * for tests that do not involve an Android [Activity] context.
   */
  @get:Rule val composeTestRule = createComposeRule()

  /**
   * Setup method for initializing necessary state or resources before each test. Can be overridden
   * by subclasses.
   */
  open fun setUp() {
    // Override to implement custom setup behavior.
  }

  /**
   * Teardown method for cleaning up resources or performing actions after each test. Can be
   * overridden by subclasses.
   */
  open fun tearDown() {
    // Override to implement custom tearDown behavior.
  }
}
