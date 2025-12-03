package com.github.warnastrophy.core.ui.features.health

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.viewmodel.compose.viewModel
import com.github.warnastrophy.R
import com.github.warnastrophy.core.ui.theme.extendedColors

object HealthCardPopUpTestTags {
  const val ROOT_CARD = "healthCardRootCard"
  const val TITLE = "healthCardTitle"
  const val EDIT_BUTTON = "healthCardEditButton"
  const val CONTENT_CARD = "healthCardContentCard"
  const val EMPTY_STATE_TEXT = "healthCardEmptyStateText"

  // Fields
  const val FULL_NAME_FIELD = "FullNameField"
  const val BIRTH_DATE_FIELD = "BirthDateField"
  const val SEX_FIELD = "SexField"
  const val BLOOD_TYPE_FIELD = "BloodTypeField"
  const val ALLERGIES_FIELD = "AllergiesField"
  const val MEDICATIONS_FIELD = "MedicationsField"
  const val ORGAN_DONOR_FIELD = "OrganDonorSwitch"
  const val NOTES_FIELD = "NotesField"
}

/**
 * Represents the state of the Health Card form.
 *
 * This class manages all form fields and their validation states. It uses a "touched" pattern to
 * track user interaction with required fields, enabling validation feedback only after the user has
 * interacted with a field.
 *
 * @property fullName The full name of the card holder (required)
 * @property fullNameTouched Whether the full name field has been interacted with
 * @property birthDate The birth date in dd/MM/yyyy format (required)
 * @property birthDateTouched Whether the birth date field has been interacted with
 * @property socialSecurityNumber The social security number (required)
 * @property ssnTouched Whether the SSN field has been interacted with
 * @property sex The biological sex (optional)
 * @property bloodType The blood type (optional)
 * @property heightCm Height in centimeters (optional)
 * @property weightKg Weight in kilograms (optional)
 * @property chronicConditions Comma-separated list of chronic conditions (optional)
 * @property allergies Comma-separated list of allergies (optional)
 * @property medications Comma-separated list of medications (optional)
 * @property onGoingTreatments Comma-separated list of ongoing treatments (optional)
 * @property medicalHistory Comma-separated list of medical history items (optional)
 * @property organDonor Whether the person is an organ donor
 * @property notes Additional notes (optional)
 */
@Stable
data class HealthCardPreviewState(
    val fullName: String = "",
    val birthDate: String = "",
    val sex: String = "",
    val bloodType: String = "",
    val allergies: String = "",
    val medications: String = "",
    val organDonor: Boolean = false,
    val notes: String = ""
)

@Composable
fun HealthCardPopUp(
    userId: String,
    onDismissRequest: () -> Unit = {},
    onClick: () -> Unit = {},
    viewModel: HealthCardViewModel = viewModel()
) {
  val healthCardColors = MaterialTheme.extendedColors.healthCardPopUp
  val context = LocalContext.current
  val currentCard by viewModel.currentCard.collectAsState()

  LaunchedEffect(Unit) { viewModel.loadHealthCard(context, userId) }

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
              if (currentCard != null) {
                HealthCardDetails(card = currentCard!!.toPreviewState())
              } else {
                EmptyHealthCardPopUp()
              }
            }
          }
        }
  }
}

@Composable
private fun HealthCardDetails(card: HealthCardPreviewState) {
  Column(modifier = Modifier.padding(16.dp).verticalScroll(rememberScrollState())) {
    HealthInfoEntry(label = "Name", value = card.fullName)
    HealthInfoEntry(label = "Date of birth", value = card.birthDate)
    HealthInfoEntry(label = "Gender", value = card.sex)
    HealthInfoEntry(label = "Blood Type", value = card.bloodType)
    HealthInfoEntry(label = "Allergies", value = card.allergies)
    HealthInfoEntry(label = "Organ Donor", value = if (card.organDonor) "Yes" else "No")
    HealthInfoEntry(label = "Medication", value = card.medications)
    HealthInfoEntry(label = "Notes", value = card.notes)
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

@Composable
private fun HealthInfoEntry(label: String, value: String) {
  if (value.isNotBlank()) {
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
}

// TODO remove preview
@Preview
@Composable
fun EmergencyCardPreview() {
  HealthCardPopUp("user1234")
}
