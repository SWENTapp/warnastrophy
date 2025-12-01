package com.github.warnastrophy.core.ui.features.health

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.github.warnastrophy.R
import com.github.warnastrophy.core.ui.theme.extendedColors

@Composable
fun HealthCardPopUp(onDismissRequest: () -> Unit = {}, onClick: () -> Unit = {}) {
  val healthCardColors = MaterialTheme.extendedColors.healthCardPopUp
  Dialog(onDismissRequest = onDismissRequest) {
    Card(
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth(1f).fillMaxHeight(0.8f),
        colors = CardDefaults.cardColors(containerColor = healthCardColors.primary)) {
          Column(modifier = Modifier.padding(16.dp).fillMaxHeight()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically) {
                  Text(
                      text = stringResource(id = R.string.health_card_popup_title),
                      color = healthCardColors.secondary,
                      fontWeight = FontWeight.Bold,
                      fontSize = 20.sp)
                  TextButton(onClick = onClick) {
                    Text(
                        text = stringResource(id = R.string.health_card_popup_edit_button),
                        color = healthCardColors.secondary,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 12.sp)
                    Spacer(Modifier.width(3.dp))
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription =
                            stringResource(id = R.string.health_card_popup_edit_button_cd),
                        tint = healthCardColors.secondary,
                        modifier = Modifier.size(12.dp))
                  }
                }

            Spacer(modifier = Modifier.height(6.dp))

            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = healthCardColors.secondary),
                modifier = Modifier.fillMaxWidth().weight(1f),
            ) {
              EmptyHealthCardPopUp()
            }
          }
        }
  }
}

@Composable
private fun HealthInfoEntry(label: String, value: String) {
  Column(modifier = Modifier.padding(bottom = 12.dp)) {
    Text(
        text = label,
        fontWeight = FontWeight.Bold,
        fontSize = 18.sp,
        color = MaterialTheme.extendedColors.healthCardPopUp.primary)
    Text(
        text = value,
        fontSize = 16.sp,
        color = MaterialTheme.extendedColors.healthCardPopUp.fieldText)
  }
}

@Composable
private fun EmptyHealthCardPopUp() {
  Column(
      modifier = Modifier.fillMaxSize(),
      verticalArrangement = Arrangement.Center,
      horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = stringResource(id = R.string.health_card_popup_empty_text),
            color = MaterialTheme.extendedColors.healthCardPopUp.fieldText)
      }
}

@Preview
@Composable
fun EmergencyCardPreview() {
  HealthCardPopUp()
}
