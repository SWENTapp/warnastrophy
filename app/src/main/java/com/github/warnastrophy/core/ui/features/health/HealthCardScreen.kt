package com.github.warnastrophy.core.ui.features.health

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.github.warnastrophy.R
import com.github.warnastrophy.core.model.HealthCard
import com.github.warnastrophy.core.ui.components.Loading
import com.github.warnastrophy.core.ui.components.LoadingTestTags
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.ResolverStyle

/**
 * Constants for UI testing tags used throughout the Health Card screen. These tags enable automated
 * testing by providing stable identifiers for UI components.
 */
object HealthCardTestTags {
  const val FULL_NAME_FIELD = "FullNameField"
  const val BIRTH_DATE_FIELD = "BirthDateField"
  const val SSN_FIELD = "SSNField"
  const val SEX_FIELD = "SexField"
  const val BLOOD_TYPE_FIELD = "BloodTypeField"
  const val HEIGHT_FIELD = "HeightField"
  const val WEIGHT_FIELD = "WeightField"
  const val CHRONIC_CONDITIONS_FIELD = "ChronicField"
  const val ALLERGIES_FIELD = "AllergiesField"
  const val MEDICATIONS_FIELD = "MedicationsField"
  const val TREATMENTS_FIELD = "TreatmentsField"
  const val HISTORY_FIELD = "HistoryField"
  const val ORGAN_DONOR_FIELD = "OrganDonorSwitch"
  const val NOTES_FIELD = "NotesField"

  // Action button tags
  const val ADD_BUTTON = "AddButton"
  const val UPDATE_BUTTON = "UpdateButton"
  const val DELETE_BUTTON = "DeleteButton"
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
data class HealthCardFormState(
    val fullName: String = "",
    val fullNameTouched: Boolean = false,
    val birthDate: String = "",
    val birthDateTouched: Boolean = false,
    val socialSecurityNumber: String = "",
    val ssnTouched: Boolean = false,
    val sex: String = "",
    val bloodType: String = "",
    val heightCm: String = "",
    val weightKg: String = "",
    val chronicConditions: String = "",
    val allergies: String = "",
    val medications: String = "",
    val onGoingTreatments: String = "",
    val medicalHistory: String = "",
    val organDonor: Boolean = false,
    val notes: String = ""
) {
  private val dateFormatter =
      DateTimeFormatter.ofPattern("dd/MM/uuuu").withResolverStyle(ResolverStyle.STRICT)

  /**
   * Validates the birth date format and ensures it represents a valid date.
   *
   * @return true if the date is valid and parseable, false otherwise
   */
  fun isDateValid(): Boolean {
    try {
      val date = LocalDate.parse(birthDate, dateFormatter)
      return birthDate.isNotBlank() && !date.isAfter(LocalDate.now())
    } catch (_: Exception) {
      return false
    }
  }

  /**
   * Validates that all required fields are filled and properly formatted.
   *
   * @return true if all required fields are valid, false otherwise
   */
  fun isValid(): Boolean =
      fullName.isNotBlank() && isDateValid() && socialSecurityNumber.isNotBlank()

  /**
   * Marks all required fields as touched to trigger validation display. Used when attempting to
   * submit the form.
   *
   * @return A new form state with all required fields marked as touched
   */
  fun markAllTouched(): HealthCardFormState =
      copy(fullNameTouched = true, birthDateTouched = true, ssnTouched = true)
}

/**
 * Encapsulates the actions that can be performed on the health card form.
 *
 * @property onSave Callback for saving a new health card.
 * @property onUpdate Callback for updating an existing health card.
 * @property onDelete Callback for deleting the health card.
 */
@Stable
data class HealthCardActions(
    val onSave: () -> Unit,
    val onUpdate: () -> Unit,
    val onDelete: () -> Unit
)

/**
 * Main screen composable for managing health card information.
 *
 * This screen allows users to view, create, update, and delete their health card. It handles form
 * validation, state management, and navigation.
 *
 * @param userId The ID of the user whose health card is being managed
 * @param viewModel The ViewModel managing the health card data and operations
 */
@Composable
fun HealthCardScreen(
    userId: String,
    viewModel: HealthCardViewModel = viewModel(),
    onDone: () -> Unit = {}
) {
  val context = LocalContext.current
  val uiState by viewModel.uiState.collectAsState()
  val currentCard by viewModel.currentCard.collectAsState()

  LaunchedEffect(uiState) {
    val successState = uiState as? HealthCardUiState.Success
    val isOperationSuccessful =
        successState?.message == context.getString(R.string.health_card_saved_message) ||
            successState?.message == context.getString(R.string.health_card_deleted_message)

    if (isOperationSuccessful) {
      onDone()
      viewModel.resetUiState()
    }
  }

  var formState by remember { mutableStateOf(HealthCardFormState()) }

  LaunchedEffect(Unit) { viewModel.loadHealthCard(context, userId) }
  LaunchedEffect(currentCard) { currentCard?.let { formState = it.toFormState() } }

  Scaffold { paddingValues ->
    val actions =
        HealthCardActions(
            onSave = {
              val validatedState = formState.markAllTouched()
              formState = validatedState
              if (validatedState.isValid()) {
                viewModel.saveHealthCard(context, userId, validatedState.toDomain())
                viewModel.saveHealthCardDB(validatedState.toDomain())
              }
            },
            onUpdate = {
              val validatedState = formState.markAllTouched()
              formState = validatedState
              if (validatedState.isValid()) {
                viewModel.updateHealthCard(context, userId, validatedState.toDomain())
                viewModel.saveHealthCardDB(validatedState.toDomain())
              }
            },
            onDelete = {
              viewModel.deleteHealthCard(context, userId)
              viewModel.deleteHealthCardDB()
              formState = HealthCardFormState()
            })
    HealthCardContent(
        formState = formState,
        onFormStateChange = { formState = it },
        currentCard = currentCard,
        uiState = uiState,
        actions = actions,
        modifier = Modifier.padding(paddingValues))
  }
}

/**
 * Main content area of the Health Card screen.
 *
 * Contains all form fields, validation, and action buttons organized in a scrollable layout.
 *
 * @param formState Current state of the form
 * @param onFormStateChange Callback to update form state
 * @param currentCard The existing health card (null if creating new)
 * @param uiState Current UI state (loading, success, error)
 * @param actions The actions that can be performed on the form.
 * @param modifier Optional modifier for customization
 */
@Composable
private fun HealthCardContent(
    formState: HealthCardFormState,
    onFormStateChange: (HealthCardFormState) -> Unit,
    currentCard: HealthCard?,
    uiState: HealthCardUiState,
    actions: HealthCardActions,
    modifier: Modifier = Modifier
) {
  Column(
      modifier = modifier.padding(16.dp).fillMaxSize().verticalScroll(rememberScrollState()),
      verticalArrangement = Arrangement.spacedBy(12.dp)) {
        RequiredFieldsSection(formState = formState, onFormStateChange = onFormStateChange)

        OptionalFieldsSection(formState = formState, onFormStateChange = onFormStateChange)

        Spacer(modifier = Modifier.height(16.dp))

        ActionButtons(
            currentCard = currentCard,
            isFormValid = formState.isValid(),
            onSave = actions.onSave,
            onUpdate = actions.onUpdate,
            onDelete = actions.onDelete)

        if (uiState is HealthCardUiState.Loading) {
          Loading(modifier = Modifier.fillMaxSize().testTag(LoadingTestTags.LOADING_INDICATOR))
        }
      }
}

/**
 * Action buttons for creating, updating, or deleting the health card.
 *
 * Displays different buttons based on whether a health card already exists.
 *
 * @param currentCard The existing health card (null if creating new)
 * @param isFormValid Whether the form is valid for submission
 * @param onSave Callback for saving a new health card
 * @param onUpdate Callback for updating an existing health card
 * @param onDelete Callback for deleting the health card
 * @param modifier Optional modifier for customization
 */
@Composable
private fun ActionButtons(
    currentCard: HealthCard?,
    isFormValid: Boolean,
    onSave: () -> Unit,
    onUpdate: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
  Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
    if (currentCard == null) {
      Button(
          onClick = onSave,
          enabled = isFormValid,
          modifier = Modifier.fillMaxWidth().testTag(HealthCardTestTags.ADD_BUTTON)) {
            Text(stringResource(R.string.add_button))
          }
    } else {
      Button(
          onClick = onUpdate,
          enabled = isFormValid,
          modifier = Modifier.fillMaxWidth().testTag(HealthCardTestTags.UPDATE_BUTTON)) {
            Text(stringResource(R.string.update_button))
          }

      OutlinedButton(
          onClick = onDelete,
          modifier = Modifier.fillMaxWidth().testTag(HealthCardTestTags.DELETE_BUTTON)) {
            Text(stringResource(R.string.delete_button))
          }
    }
  }
}
