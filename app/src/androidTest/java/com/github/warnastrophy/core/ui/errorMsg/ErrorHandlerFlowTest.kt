// kotlin
package com.github.warnastrophy.core.ui.errorMsg

import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import com.github.warnastrophy.MainActivity
import com.github.warnastrophy.core.ui.util.BaseAndroidComposeTest
import org.junit.Rule
import org.junit.Test

class ErrorHandlerFlowTest : BaseAndroidComposeTest() {
  @get:Rule val composeRule = createAndroidComposeRule(MainActivity::class.java)

  @Test fun whenHandlerHasError_errorScreenIsShown_withCorrectMessage() {}
}
