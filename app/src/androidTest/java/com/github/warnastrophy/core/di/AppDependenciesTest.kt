package com.github.warnastrophy.core.di

import android.content.Context
import io.mockk.mockk
import org.junit.Assert.assertThrows
import org.junit.Test

class AppDependenciesTest {
  @Test
  fun errorHandler_notInitialized_throws() {
    assertThrows(IllegalStateException::class.java) { AppDependencies.errorHandler }
  }

  @Test
  fun gpsService_notInitialized_throws() {
    assertThrows(IllegalStateException::class.java) { AppDependencies.gpsService }
  }

  @Test
  fun hazardsService_notInitialized_throws() {
    assertThrows(IllegalStateException::class.java) { AppDependencies.hazardsService }
  }

  @Test
  fun permissionManager_notInitialized_throws() {
    assertThrows(IllegalStateException::class.java) { AppDependencies.permissionManager }
  }

  @Test
  fun smokeTest_init() { // for coverage
    val mockContext = mockk<Context>(relaxed = true)
    AppDependencies.init(mockContext)
  }
}
