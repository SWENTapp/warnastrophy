package com.github.warnastrophy.core.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.github.warnastrophy.core.ui.theme.MainAppTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen() {
  Column(
      verticalArrangement = Arrangement.Center,
      horizontalAlignment = Alignment.CenterHorizontally,
      modifier = Modifier.fillMaxSize()) {
        Text("Nothing here yet :(")
      }
}

@Preview
@Composable
fun HomeScreenPreview() {
  MainAppTheme { HomeScreen() }
}
