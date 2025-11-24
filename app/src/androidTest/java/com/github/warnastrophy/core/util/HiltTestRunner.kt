package com.github.warnastrophy.core.util

import android.app.Application
import android.content.Context
import androidx.test.runner.AndroidJUnitRunner
import dagger.hilt.android.testing.HiltTestApplication

/**
 * Custom test runner required by Hilt to inject dependencies into instrumented tests.
 * * It ensures that the HiltTestApplication is used instead of the production Application class,
 *   allowing the Hilt test graph to be built correctly.
 */
class HiltTestRunner : AndroidJUnitRunner() {
  override fun newApplication(
      cl: ClassLoader?,
      className: String?,
      context: Context?
  ): Application? {
    return super.newApplication(cl, HiltTestApplication::class.java.name, context)
  }
}