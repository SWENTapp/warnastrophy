package com.github.warnastrophy.core.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.tooling.preview.Preview

object LoadingTestTags {
  const val LOADING_INDICATOR = "loadingIndicator"
}

@Composable
fun Loading(modifier: Modifier = Modifier) {
  Box(
      modifier = modifier.fillMaxSize().testTag(LoadingTestTags.LOADING_INDICATOR),
      contentAlignment = Alignment.Center) {
        CircularProgressIndicator()
      }
}

@Preview(showBackground = true)
@Composable
fun LoadingPreview() {
  Loading()
}
