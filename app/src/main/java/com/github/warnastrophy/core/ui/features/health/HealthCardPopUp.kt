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

  // Fields titles
  const val FULL_NAME_TITLE = "FullNameTitle"
  const val BIRTH_DATE_TITLE = "BirthDateTitle"
  const val SEX_TITLE = "SexTitle"
  const val BLOOD_TYPE_TITLE = "BloodTypeTitle"
  const val ALLERGIES_TITLE = "AllergiesTitle"
  const val MEDICATIONS_TITLE = "MedicationsTitle"
  const val ORGAN_DONOR_TITLE = "OrganDonorSwitch"
  const val NOTES_TITLE = "NotesTitle"

  // Field Values
  const val FULL_NAME_VALUE = "FullNameValue"
  const val BIRTH_DATE_VALUE = "BirthDateValue"
  const val SEX_VALUE = "SexValue"
  const val BLOOD_TYPE_VALUE = "BloodTypeValue"
  const val ALLERGIES_VALUE = "AllergiesValue"
  const val MEDICATIONS_VALUE = "MedicationsValue"
  const val ORGAN_DONOR_VALUE = "OrganDonorValue"
  const val NOTES_VALUE = "NotesValue"
}

/**
 * Represents the state of the Health Card preview.
 *
 * @property fullName The full name of the card holder (required)
 * @property birthDate The birth date in dd/MM/yyyy format (required)
 * @property sex The biological sex (optional)
 * @property bloodType The blood type (optional)
 * @property allergies Comma-separated list of allergies (optional)
 * @property medications Comma-separated list of medications (optional)
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

/**
 * A composable function that displays a user's health card information in a dialog pop-up.
 *
 * This dialog fetches the health card data for a given [userId] using the [HealthCardViewModel]. It
 * shows a title and an "Edit" button that triggers the [onClick] lambda. The main content area
 * either displays the detailed health information using [HealthCardDetails] if data is available,
 * or an empty state message via [EmptyHealthCardPopUp] if no card data is found. The pop-up can be
 * dismissed by triggering [onDismissRequest].
 *
 * @param userId The unique identifier for the user whose health card is to be displayed.
 * @param onDismissRequest A lambda function to be invoked when the user requests to dismiss the
 *   dialog (e.g., by tapping outside the dialog or pressing the back button).
 * @param onClick A lambda function to be invoked when the "Edit" button is clicked. This is
 *   typically used to navigate to a screen where the health card can be modified.
 * @param viewModel An instance of [HealthCardViewModel] used to fetch and manage the state of the
 *   health card data. Defaults to the instance provided by `viewModel()`.
 */
@Composable
fun HealthCardPopUp(
    userId: String,
    onDismissRequest: () -> Unit = {},
    onClick: () -> Unit = {},
    viewModel: HealthCardViewModel = viewModel()
) {
  val healthCardColors = MaterialTheme.extendedColors.dashboardPopUp
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
                            fontSize = 14.sp)
                        Spacer(Modifier.width(3.dp))
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription =
                                stringResource(id = R.string.health_card_popup_edit_button_cd),
                            tint = healthCardColors.secondary,
                            modifier = Modifier.size(14.dp))
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

/**
 * A composable that displays the detailed information from a health card. It lays out the various
 * health entries vertically in a scrollable column.
 *
 * @param card The [HealthCardPreviewState] data object containing the details to be displayed.
 */
@Composable
private fun HealthCardDetails(card: HealthCardPreviewState) {
  Column(modifier = Modifier.padding(16.dp).verticalScroll(rememberScrollState())) {
    HealthInfoEntry(
        label = stringResource(id = R.string.health_card_full_name),
        value = card.fullName,
        titleTestTag = HealthCardPopUpTestTags.FULL_NAME_TITLE,
        valueTestTag = HealthCardPopUpTestTags.FULL_NAME_VALUE)
    HealthInfoEntry(
        label = stringResource(id = R.string.health_card_birth_date),
        value = card.birthDate,
        titleTestTag = HealthCardPopUpTestTags.BIRTH_DATE_TITLE,
        valueTestTag = HealthCardPopUpTestTags.BIRTH_DATE_VALUE)
    HealthInfoEntry(
        label = stringResource(id = R.string.health_card_sex),
        value = card.sex,
        titleTestTag = HealthCardPopUpTestTags.SEX_TITLE,
        valueTestTag = HealthCardPopUpTestTags.SEX_VALUE)
    HealthInfoEntry(
        label = stringResource(id = R.string.health_card_blood_type),
        value = card.bloodType,
        titleTestTag = HealthCardPopUpTestTags.BLOOD_TYPE_TITLE,
        valueTestTag = HealthCardPopUpTestTags.BLOOD_TYPE_VALUE)
    HealthInfoEntry(
        label = stringResource(id = R.string.health_card_allergies),
        value = card.allergies,
        titleTestTag = HealthCardPopUpTestTags.ALLERGIES_TITLE,
        valueTestTag = HealthCardPopUpTestTags.ALLERGIES_VALUE)
    HealthInfoEntry(
        label = stringResource(id = R.string.health_card_organ_donor),
        value = if (card.organDonor) "Yes" else "No",
        titleTestTag = HealthCardPopUpTestTags.ORGAN_DONOR_TITLE,
        valueTestTag = HealthCardPopUpTestTags.ORGAN_DONOR_VALUE)
    HealthInfoEntry(
        label = stringResource(id = R.string.health_card_medications),
        value = card.medications,
        titleTestTag = HealthCardPopUpTestTags.MEDICATIONS_TITLE,
        valueTestTag = HealthCardPopUpTestTags.MEDICATIONS_VALUE)
    HealthInfoEntry(
        label = stringResource(id = R.string.health_card_notes),
        value = card.notes,
        titleTestTag = HealthCardPopUpTestTags.NOTES_TITLE,
        valueTestTag = HealthCardPopUpTestTags.NOTES_VALUE)
  }
}

/**
 * A composable that displays a message indicating that the health card is empty. This is shown
 * within the `HealthCardPopUp` when no health card data is available to display.
 */
@Composable
private fun EmptyHealthCardPopUp() {
  Column(
      modifier = Modifier.fillMaxSize(),
      verticalArrangement = Arrangement.Center,
      horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = stringResource(id = R.string.health_card_popup_empty_text),
            color = MaterialTheme.extendedColors.dashboardPopUp.fieldText,
            modifier = Modifier.testTag(HealthCardPopUpTestTags.EMPTY_STATE_TEXT))
      }
}

/**
 * A composable that displays a labeled piece of health information. It shows a bolded label and its
 * corresponding value underneath. The entry is only rendered if the provided [value] is not blank.
 *
 * @param label The text to display as the title for the information (e.g., "Blood Type").
 * @param value The text to display as the value for the information (e.g., "O+").
 * @param titleTestTag The test tag for the label's [Text] composable, used for UI testing.
 * @param valueTestTag The test tag for the value's [Text] composable, used for UI testing.
 */
@Composable
private fun HealthInfoEntry(
    label: String,
    value: String,
    titleTestTag: String,
    valueTestTag: String
) {
  if (value.isNotBlank()) {
    Column(modifier = Modifier.padding(bottom = 12.dp)) {
      Text(
          text = label,
          fontWeight = FontWeight.Bold,
          fontSize = 18.sp,
          color = MaterialTheme.extendedColors.dashboardPopUp.primary,
          modifier = Modifier.testTag(titleTestTag))
      Text(
          text = value,
          fontSize = 16.sp,
          color = MaterialTheme.extendedColors.dashboardPopUp.fieldText,
          modifier = Modifier.testTag(valueTestTag))
    }
  }
}
