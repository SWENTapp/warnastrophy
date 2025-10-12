package com.github.warnastrophy.core.ui.profile

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.github.warnastrophy.core.ui.theme.MainAppTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(onContactListClick: () -> Unit = {}) {
  Column(
      verticalArrangement = Arrangement.Center,
      horizontalAlignment = Alignment.CenterHorizontally,
      modifier = Modifier.fillMaxSize()) {
        Text("Nothing yet here either :(")
        Spacer(modifier = Modifier.height(32.dp)) // Add some space
        Button(onClick = { onContactListClick() }) { Text("Contact List") }
      }
}

@Preview
@Composable
fun ProfileScreenPreview() {
  MainAppTheme { ProfileScreen() }
}
