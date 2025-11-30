package com.github.warnastrophy.core.ui.features.health

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog

@Composable
fun HealthCardPopUp(onDismissRequest: () -> Unit = {}, onClick: () -> Unit = {}) {
  Dialog(onDismissRequest = onDismissRequest) {
    Card(
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth().aspectRatio(0.63f),
        colors =
            CardDefaults.cardColors(
                containerColor = Color(0xFF9D2A2C) // TODO: Use theme color
                )) {
          Column(modifier = Modifier.padding(16.dp).fillMaxHeight()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically) {
                  Text(
                      text = "HEALTH CARD",
                      color = Color.White,
                      fontWeight = FontWeight.Bold,
                      fontSize = 20.sp)
                  TextButton(onClick = onClick) {
                    Text(
                        text = "Edit/Show Full",
                        color = Color.White,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 12.sp)
                    Spacer(Modifier.width(3.dp))
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = "Go to full health card",
                        tint = Color.White,
                        modifier = Modifier.size(12.dp))
                  }
                }

            Spacer(modifier = Modifier.height(16.dp))

            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                modifier = Modifier.fillMaxWidth().weight(1f)) {
                  EmptyHealthCardPopUp()
                }
          }
        }
  }
}

@Composable
private fun HealthInfoEntry(label: String, value: String) {
  Column(modifier = Modifier.padding(bottom = 12.dp)) {
    Text(text = label, fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Color(0xFF9D2A2C))
    Text(text = value, fontSize = 16.sp, color = Color.Black)
  }
}

@Composable
private fun EmptyHealthCardPopUp() {
  Column(
      modifier = Modifier.fillMaxSize(),
      verticalArrangement = Arrangement.Center,
      horizontalAlignment = Alignment.CenterHorizontally) {
        Text("Empty Health Card")
      }
}

@Preview
@Composable
fun EmergencyCardPreview() {
  HealthCardPopUp()
}
