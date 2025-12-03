package com.github.warnastrophy.core.ui.features.health

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.github.warnastrophy.R
import com.github.warnastrophy.core.ui.theme.extendedColors

object HealthCardPopUpTestTags {
  const val ROOT_CARD = "healthCardRootCard"
  const val TITLE = "healthCardTitle"
  const val EDIT_BUTTON = "healthCardEditButton"
  const val CONTENT_CARD = "healthCardContentCard"
  const val EMPTY_STATE_TEXT = "healthCardEmptyStateText"
}

@Composable
fun HealthCardPopUp(onDismissRequest: () -> Unit = {}, onClick: () -> Unit = {}) {
  val healthCardColors = MaterialTheme.extendedColors.healthCardPopUp
  Dialog(onDismissRequest = onDismissRequest) {
    Card(
        shape = RoundedCornerShape(16.dp),
        modifier =
            Modifier.fillMaxWidth(1f)
                .fillMaxHeight(0.8f)
                .testTag(HealthCardPopUpTestTags.ROOT_CARD),
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
                      fontSize = 20.sp,
                      modifier = Modifier.testTag(HealthCardPopUpTestTags.TITLE))
                  TextButton(
                      onClick = onClick,
                      modifier = Modifier.testTag(HealthCardPopUpTestTags.EDIT_BUTTON)) {
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
                modifier =
                    Modifier.fillMaxWidth()
                        .weight(1f)
                        .testTag(HealthCardPopUpTestTags.CONTENT_CARD),
            ) {
              EmptyHealthCardPopUp()
            }
          }
        }
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
            color = MaterialTheme.extendedColors.healthCardPopUp.fieldText,
            modifier = Modifier.testTag(HealthCardPopUpTestTags.EMPTY_STATE_TEXT))
      }
}

@Preview
@Composable
fun EmergencyCardPreview() {
  HealthCardPopUp()
}
