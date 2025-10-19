package com.github.warnastrophy.core.ui.healthcard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.github.warnastrophy.core.model.HealthCard
import com.github.warnastrophy.core.ui.components.Loading
import com.github.warnastrophy.core.ui.components.LoadingTestTags
import com.github.warnastrophy.core.ui.navigation.Screen
import com.github.warnastrophy.core.ui.theme.MainAppTheme
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/**
 * Constants for UI testing tags used throughout the Health Card screen. These tags enable automated
 * testing by providing stable identifiers for UI components.
 */
object HealthCardTestTags {
  const val BACK_BUTTON = "BackButton"

  // Input field tags
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
  /** Regex pattern for date validation in dd/MM/yyyy format */
  private val dateRegex = Regex("""^([0-2]\d|3[01])/(0\d|1[0-2])/(\d{4})$""")

  /**
   * Validates the birth date format and ensures it represents a valid date.
   *
   * @return true if the date is valid and parseable, false otherwise
   */
  fun isDateValid(): Boolean {
    if (birthDate.isBlank()) return false
    return try {
      val formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")
      LocalDate.parse(birthDate, formatter)
      true
    } catch (e: Exception) {
      false
    }
  }

  /**
   * Validates that all required fields are filled and properly formatted.
   *
   * @return true if all required fields are valid, false otherwise
   */
  fun isValid(): Boolean =
      fullName.isNotBlank() &&
          birthDate.isNotBlank() &&
          dateRegex.matches(birthDate) &&
          socialSecurityNumber.isNotBlank()

  /**
   * Converts the birth date from dd/MM/yyyy format to ISO format (yyyy-MM-dd).
   *
   * @return The birth date in ISO format
   */
  private fun formatDateToIso(): String {
    val inputFormat = DateTimeFormatter.ofPattern("dd/MM/yyyy")
    val outputFormat = DateTimeFormatter.ofPattern("yyyy-MM-dd")
    val date = LocalDate.parse(birthDate, inputFormat)
    return date.format(outputFormat)
  }

  /**
   * Converts the form state to a HealthCard domain model.
   *
   * This method handles type conversions and transforms comma-separated strings into lists for
   * multi-value fields.
   *
   * @return A HealthCard instance populated with form data
   */
  fun toHealthCard(): HealthCard =
      HealthCard(
          fullName = fullName,
          birthDate = formatDateToIso(),
          socialSecurityNumber = socialSecurityNumber,
          sex = sex.ifEmpty { null },
          bloodType = bloodType.ifEmpty { null },
          heightCm = heightCm.toIntOrNull(),
          weightKg = weightKg.toDoubleOrNull(),
          chronicConditions = chronicConditions.splitToList(),
          allergies = allergies.splitToList(),
          medications = medications.splitToList(),
          onGoingTreatments = onGoingTreatments.splitToList(),
          medicalHistory = medicalHistory.splitToList(),
          organDonor = organDonor,
          notes = notes.ifEmpty { null })

  /**
   * Marks all required fields as touched to trigger validation display. Used when attempting to
   * submit the form.
   *
   * @return A new form state with all required fields marked as touched
   */
  fun markAllTouched(): HealthCardFormState =
      copy(fullNameTouched = true, birthDateTouched = true, ssnTouched = true)

  /**
   * Splits a comma-separated string into a list of trimmed, non-empty strings.
   *
   * @return A list of trimmed strings, excluding empty values
   */
  private fun String.splitToList(): List<String> =
      split(",").map { it.trim() }.filter { it.isNotEmpty() }

  companion object {
    /**
     * Creates a form state from an existing HealthCard model.
     *
     * This method converts the ISO date format back to dd/MM/yyyy and transforms lists into
     * comma-separated strings for display in text fields.
     *
     * @param card The HealthCard to convert
     * @return A HealthCardFormState populated with the card's data
     */
    fun fromHealthCard(card: HealthCard): HealthCardFormState {
      val inputFormat = DateTimeFormatter.ofPattern("yyyy-MM-dd")
      val outputFormat = DateTimeFormatter.ofPattern("dd/MM/yyyy")
      val date = LocalDate.parse(card.birthDate, inputFormat)

      return HealthCardFormState(
          fullName = card.fullName,
          birthDate = date.format(outputFormat),
          socialSecurityNumber = card.socialSecurityNumber,
          sex = card.sex ?: "",
          bloodType = card.bloodType ?: "",
          heightCm = card.heightCm?.toString() ?: "",
          weightKg = card.weightKg?.toString() ?: "",
          chronicConditions = card.chronicConditions.joinToString(", "),
          allergies = card.allergies.joinToString(", "),
          medications = card.medications.joinToString(", "),
          onGoingTreatments = card.onGoingTreatments.joinToString(", "),
          medicalHistory = card.medicalHistory.joinToString(", "),
          organDonor = card.organDonor,
          notes = card.notes ?: "")
    }
  }
}

/**
 * Main screen composable for managing health card information.
 *
 * This screen allows users to view, create, update, and delete their health card. It handles form
 * validation, state management, and navigation.
 *
 * @param userId The ID of the user whose health card is being managed
 * @param viewModel The ViewModel managing the health card data and operations
 * @param navController Navigation controller for screen transitions
 */
@Composable
fun HealthCardScreen(
    userId: String = "John Doe",
    viewModel: HealthCardViewModel = viewModel(),
    navController: NavController = rememberNavController()
) {
  val context = LocalContext.current
  val uiState by viewModel.uiState.collectAsState()
  val currentCard by viewModel.currentCard.collectAsState()

  var formState by remember { mutableStateOf(HealthCardFormState()) }

  // Load the health card when the screen is first displayed
  LaunchedEffect(userId) { viewModel.loadHealthCard(context, userId) }

  // Update the form when a health card is loaded
  LaunchedEffect(currentCard) {
    currentCard?.let { card -> formState = HealthCardFormState.fromHealthCard(card) }
  }

  Scaffold(
      topBar = {
        HealthCardTopBar(onNavigateBack = { navController.navigate(Screen.PROFILE.name) })
      }) { paddingValues ->
        HealthCardContent(
            formState = formState,
            onFormStateChange = { formState = it },
            currentCard = currentCard,
            uiState = uiState,
            onSave = {
              val validatedState = formState.markAllTouched()
              formState = validatedState
              if (validatedState.isValid()) {
                viewModel.saveHealthCard(context, userId, validatedState.toHealthCard())
              }
            },
            onUpdate = {
              val validatedState = formState.markAllTouched()
              formState = validatedState
              if (validatedState.isValid()) {
                viewModel.updateHealthCard(context, userId, validatedState.toHealthCard())
              }
            },
            onDelete = { viewModel.deleteHealthCard(context, userId) },
            modifier = Modifier.padding(paddingValues))
      }
}

/**
 * Top app bar for the Health Card screen with navigation.
 *
 * @param onNavigateBack Callback invoked when the back button is pressed
 * @param modifier Optional modifier for customization
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HealthCardTopBar(onNavigateBack: () -> Unit, modifier: Modifier = Modifier) {
  TopAppBar(
      title = { Text("Health card") },
      navigationIcon = {
        IconButton(
            onClick = onNavigateBack, modifier = Modifier.testTag(HealthCardTestTags.BACK_BUTTON)) {
              Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Return")
            }
      },
      modifier = modifier)
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
 * @param onSave Callback for saving a new health card
 * @param onUpdate Callback for updating an existing health card
 * @param onDelete Callback for deleting the health card
 * @param modifier Optional modifier for customization
 */
@Composable
private fun HealthCardContent(
    formState: HealthCardFormState,
    onFormStateChange: (HealthCardFormState) -> Unit,
    currentCard: HealthCard?,
    uiState: HealthCardUiState,
    onSave: () -> Unit,
    onUpdate: () -> Unit,
    onDelete: () -> Unit,
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
            onSave = onSave,
            onUpdate = onUpdate,
            onDelete = onDelete)

        // Show loading indicator when performing operations
        if (uiState is HealthCardUiState.Loading) {
          Loading(modifier = Modifier.fillMaxSize().testTag(LoadingTestTags.LOADING_INDICATOR))
        }
      }
}

/**
 * Section containing required form fields with validation.
 *
 * @param formState Current state of the form
 * @param onFormStateChange Callback to update form state
 * @param modifier Optional modifier for customization
 */
@Composable
private fun RequiredFieldsSection(
    formState: HealthCardFormState,
    onFormStateChange: (HealthCardFormState) -> Unit,
    modifier: Modifier = Modifier
) {
  Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(12.dp)) {
    LabeledRequiredTextField(
        value = formState.fullName,
        onValueChange = { onFormStateChange(formState.copy(fullName = it)) },
        label = "Full name",
        testTag = HealthCardTestTags.FULL_NAME_FIELD,
        touched = formState.fullNameTouched,
        onTouched = { onFormStateChange(formState.copy(fullNameTouched = true)) },
        error = formState.fullName.isBlank(),
        errorText = if (formState.fullName.isBlank()) "Mandatory field" else null)

    val birthDateErrorText =
        when {
          formState.birthDate.isBlank() && formState.birthDateTouched -> "Mandatory field"
          formState.birthDate.isNotBlank() &&
              !formState.isDateValid() &&
              formState.birthDateTouched -> "Invalid birth date"
          else -> null
        }

    LabeledRequiredTextField(
        value = formState.birthDate,
        onValueChange = { onFormStateChange(formState.copy(birthDate = it)) },
        label = "Birth date",
        testTag = HealthCardTestTags.BIRTH_DATE_FIELD,
        touched = formState.birthDateTouched,
        onTouched = { onFormStateChange(formState.copy(birthDateTouched = true)) },
        error = birthDateErrorText != null,
        errorText = birthDateErrorText)

    LabeledRequiredTextField(
        value = formState.socialSecurityNumber,
        onValueChange = { onFormStateChange(formState.copy(socialSecurityNumber = it)) },
        label = "Social security number",
        testTag = HealthCardTestTags.SSN_FIELD,
        touched = formState.ssnTouched,
        onTouched = { onFormStateChange(formState.copy(ssnTouched = true)) },
        error = formState.socialSecurityNumber.isBlank(),
        errorText = if (formState.socialSecurityNumber.isBlank()) "Mandatory field" else null)
  }
}

/**
 * Section containing optional form fields.
 *
 * @param formState Current state of the form
 * @param onFormStateChange Callback to update form state
 * @param modifier Optional modifier for customization
 */
@Composable
private fun OptionalFieldsSection(
    formState: HealthCardFormState,
    onFormStateChange: (HealthCardFormState) -> Unit,
    modifier: Modifier = Modifier
) {
  Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(12.dp)) {
    LabeledTextField(
        value = formState.sex,
        onValueChange = { onFormStateChange(formState.copy(sex = it)) },
        label = "Sex",
        testTag = HealthCardTestTags.SEX_FIELD)

    LabeledTextField(
        value = formState.bloodType,
        onValueChange = { onFormStateChange(formState.copy(bloodType = it)) },
        label = "Blood type",
        testTag = HealthCardTestTags.BLOOD_TYPE_FIELD)

    LabeledTextField(
        value = formState.heightCm,
        onValueChange = { onFormStateChange(formState.copy(heightCm = it)) },
        label = "Height (cm)",
        testTag = HealthCardTestTags.HEIGHT_FIELD,
        keyboardType = KeyboardType.Number)

    LabeledTextField(
        value = formState.weightKg,
        onValueChange = { onFormStateChange(formState.copy(weightKg = it)) },
        label = "Weight (kg)",
        testTag = HealthCardTestTags.WEIGHT_FIELD,
        keyboardType = KeyboardType.Number)

    LabeledTextField(
        value = formState.chronicConditions,
        onValueChange = { onFormStateChange(formState.copy(chronicConditions = it)) },
        label = "Chronic conditions",
        testTag = HealthCardTestTags.CHRONIC_CONDITIONS_FIELD)

    LabeledTextField(
        value = formState.allergies,
        onValueChange = { onFormStateChange(formState.copy(allergies = it)) },
        label = "Allergies",
        testTag = HealthCardTestTags.ALLERGIES_FIELD)

    LabeledTextField(
        value = formState.medications,
        onValueChange = { onFormStateChange(formState.copy(medications = it)) },
        label = "Medications",
        testTag = HealthCardTestTags.MEDICATIONS_FIELD)

    LabeledTextField(
        value = formState.onGoingTreatments,
        onValueChange = { onFormStateChange(formState.copy(onGoingTreatments = it)) },
        label = "On going treatments",
        testTag = HealthCardTestTags.TREATMENTS_FIELD)

    LabeledTextField(
        value = formState.medicalHistory,
        onValueChange = { onFormStateChange(formState.copy(medicalHistory = it)) },
        label = "Medical history",
        testTag = HealthCardTestTags.HISTORY_FIELD)

    OrganDonorSwitch(
        checked = formState.organDonor,
        onCheckedChange = { onFormStateChange(formState.copy(organDonor = it)) })

    LabeledTextField(
        value = formState.notes,
        onValueChange = { onFormStateChange(formState.copy(notes = it)) },
        label = "Notes",
        testTag = HealthCardTestTags.NOTES_FIELD)
  }
}

/**
 * Switch component for organ donor preference.
 *
 * @param checked Current checked state
 * @param onCheckedChange Callback when the switch is toggled
 * @param modifier Optional modifier for customization
 */
@Composable
private fun OrganDonorSwitch(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
  Row(
      modifier = modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically) {
        Text("Organ donor")
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            modifier = Modifier.testTag(HealthCardTestTags.ORGAN_DONOR_FIELD))
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
      // Show Add button when creating new card
      Button(
          onClick = onSave,
          enabled = isFormValid,
          modifier = Modifier.fillMaxWidth().testTag(HealthCardTestTags.ADD_BUTTON)) {
            Text("Add")
          }
    } else {
      // Show Update and Delete buttons when editing existing card
      Button(
          onClick = onUpdate,
          enabled = isFormValid,
          modifier = Modifier.fillMaxWidth().testTag(HealthCardTestTags.UPDATE_BUTTON)) {
            Text("Update")
          }

      OutlinedButton(
          onClick = onDelete,
          modifier = Modifier.fillMaxWidth().testTag(HealthCardTestTags.DELETE_BUTTON)) {
            Text("Delete")
          }
    }
  }
}

/**
 * Basic text field component without required field validation.
 *
 * @param value Current value of the field
 * @param onValueChange Callback when the value changes
 * @param label Label text for the field
 * @param testTag Test tag for UI testing
 * @param modifier Optional modifier for customization
 * @param isError Whether the field is in an error state
 * @param errorText Error message to display
 * @param keyboardType Type of keyboard to display
 */
@Composable
private fun LabeledTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    testTag: String,
    modifier: Modifier = Modifier,
    isError: Boolean = false,
    errorText: String? = null,
    keyboardType: KeyboardType = KeyboardType.Text
) {
  Column(modifier = modifier) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        isError = isError,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        modifier = Modifier.fillMaxWidth().testTag(testTag))

    if (isError && errorText != null) {
      Text(
          text = errorText,
          color = MaterialTheme.colorScheme.error,
          style = MaterialTheme.typography.bodySmall,
          modifier = Modifier.padding(start = 16.dp, top = 4.dp))
    }
  }
}

/**
 * Text field component with required field validation and touch tracking.
 *
 * This component implements the "touched" pattern where validation errors are only shown after the
 * user has interacted with the field.
 *
 * @param value Current value of the field
 * @param onValueChange Callback when the value changes
 * @param label Label text for the field
 * @param testTag Test tag for UI testing
 * @param touched Whether the field has been interacted with
 * @param onTouched Callback to mark the field as touched
 * @param error Whether the field has a validation error
 * @param modifier Optional modifier for customization
 * @param errorText Error message to display
 * @param keyboardOptions Keyboard configuration options
 */
@Composable
private fun LabeledRequiredTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    testTag: String,
    touched: Boolean,
    onTouched: () -> Unit,
    error: Boolean,
    modifier: Modifier = Modifier,
    errorText: String? = null,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default
) {
  Column(modifier = modifier) {
    OutlinedTextField(
        value = value,
        onValueChange = {
          onValueChange(it)
          if (!touched) onTouched()
        },
        label = { Text(label) },
        isError = error && touched,
        keyboardOptions = keyboardOptions,
        modifier =
            Modifier.fillMaxWidth().testTag(testTag).onFocusChanged { focusState ->
              if (focusState.isFocused) onTouched()
            })

    if (error && touched && errorText != null) {
      Text(
          text = errorText,
          color = MaterialTheme.colorScheme.error,
          style = MaterialTheme.typography.bodySmall,
          modifier = Modifier.padding(start = 16.dp, top = 4.dp))
    }
  }
}

/** Preview function for the Health Card screen in Android Studio. */
@Preview(showBackground = true)
@Composable
private fun HealthCardScreenPreview() {
  MainAppTheme { HealthCardScreen() }
}
