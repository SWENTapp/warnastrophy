package com.github.warnastrophy.core.util

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.junit4.createComposeRule
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
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
   * @throws AssertionError if the timeout is exceeded while waiting for the Compose UI to become
   *   idle.
   */
  protected fun ComposeContentTestRule.waitForIdleWithTimeout(
      timeoutMillis: Long = defaultTimeout
  ) {
    try {
      runBlocking { withTimeout(timeoutMillis) { waitForIdle() } }
    } catch (e: TimeoutCancellationException) {
      throw AssertionError("Timed out waiting for Compose to be idle after $timeoutMillis ms", e)
    }
  }

  /**
   * Waits for a condition to be met within a specified timeout.
   *
   * This suspended function periodically checks a condition until it is met or the specified
   * timeout is exceeded. It uses a limited execution context to avoid unnecessary thread overhead.
   *
   * @param timeoutMillis The maximum time in milliseconds to wait for the condition to be met.
   *   Default is 2000 ms.
   * @param condition A lambda function representing the condition to check. It should return `true`
   *   when the condition is met.
   * @throws TimeoutCancellationException If the specified timeout is exceeded before the condition
   *   is met.
   */
  protected suspend fun awaitCondition(timeoutMillis: Long = 2000L, condition: () -> Boolean) {
    withContext(Dispatchers.Default) {
      withTimeout(timeoutMillis) {
        while (!condition()) {
          delay(10)
        }
      }
    }
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
