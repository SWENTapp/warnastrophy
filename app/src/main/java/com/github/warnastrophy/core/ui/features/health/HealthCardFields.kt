package com.github.warnastrophy.core.ui.features.health

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.github.warnastrophy.R

/**
 * Data class to hold the state for a text field. This helps to reduce the number of parameters in
 * composable functions.
 */
data class TextFieldState(
    val value: String,
    val onValueChange: (String) -> Unit,
    val label: String,
    val testTag: String,
    val keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
)

/** Data class for required text fields, including validation state. */
data class RequiredTextFieldState(
    val baseState: TextFieldState,
    val touched: Boolean,
    val onTouched: () -> Unit,
    val error: Boolean,
    val errorText: String?
)

/**
 * Section containing required form fields with validation.
 *
 * @param formState Current state of the form
 * @param onFormStateChange Callback to update form state
 * @param modifier Optional modifier for customization
 */
@Composable
fun RequiredFieldsSection(
    formState: HealthCardFormState,
    onFormStateChange: (HealthCardFormState) -> Unit,
    modifier: Modifier = Modifier
) {
  Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(12.dp)) {
    val fullNameState =
        RequiredTextFieldState(
            baseState =
                TextFieldState(
                    value = formState.fullName,
                    onValueChange = { onFormStateChange(formState.copy(fullName = it)) },
                    label = stringResource(R.string.health_card_full_name),
                    testTag = HealthCardTestTags.FULL_NAME_FIELD),
            touched = formState.fullNameTouched,
            onTouched = { onFormStateChange(formState.copy(fullNameTouched = true)) },
            error = formState.fullName.isBlank(),
            errorText =
                if (formState.fullName.isBlank()) stringResource(R.string.error_mandatory_field)
                else null)
    LabeledRequiredTextField(state = fullNameState)

    val birthDateErrorText =
        when {
          formState.birthDate.isBlank() && formState.birthDateTouched ->
              stringResource(R.string.error_mandatory_field)
          formState.birthDate.isNotBlank() &&
              !formState.isDateValid() &&
              formState.birthDateTouched -> stringResource(R.string.error_invalid_date)
          else -> null
        }

    val birthDateState =
        RequiredTextFieldState(
            baseState =
                TextFieldState(
                    value = formState.birthDate,
                    onValueChange = { onFormStateChange(formState.copy(birthDate = it)) },
                    label = stringResource(R.string.health_card_birth_date),
                    testTag = HealthCardTestTags.BIRTH_DATE_FIELD),
            touched = formState.birthDateTouched,
            onTouched = { onFormStateChange(formState.copy(birthDateTouched = true)) },
            error = birthDateErrorText != null,
            errorText = birthDateErrorText)
    LabeledRequiredTextField(state = birthDateState)

    val ssnState =
        RequiredTextFieldState(
            baseState =
                TextFieldState(
                    value = formState.socialSecurityNumber,
                    onValueChange = {
                      onFormStateChange(formState.copy(socialSecurityNumber = it))
                    },
                    label = stringResource(R.string.health_card_ssn),
                    testTag = HealthCardTestTags.SSN_FIELD),
            touched = formState.ssnTouched,
            onTouched = { onFormStateChange(formState.copy(ssnTouched = true)) },
            error = formState.socialSecurityNumber.isBlank(),
            errorText =
                if (formState.socialSecurityNumber.isBlank())
                    stringResource(R.string.error_mandatory_field)
                else null)
    LabeledRequiredTextField(state = ssnState)
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
fun OptionalFieldsSection(
    formState: HealthCardFormState,
    onFormStateChange: (HealthCardFormState) -> Unit,
    modifier: Modifier = Modifier
) {
  Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(12.dp)) {
    // Sex
    val sexState =
        TextFieldState(
            value = formState.sex,
            onValueChange = { onFormStateChange(formState.copy(sex = it)) },
            label = stringResource(R.string.health_card_sex),
            testTag = HealthCardTestTags.SEX_FIELD)
    LabeledTextField(state = sexState)

    // Blood Type
    val bloodTypeState =
        TextFieldState(
            value = formState.bloodType,
            onValueChange = { onFormStateChange(formState.copy(bloodType = it)) },
            label = stringResource(R.string.health_card_blood_type),
            testTag = HealthCardTestTags.BLOOD_TYPE_FIELD)
    LabeledTextField(state = bloodTypeState)

    // Height
    val heightState =
        TextFieldState(
            value = formState.heightCm,
            onValueChange = { onFormStateChange(formState.copy(heightCm = it)) },
            label = stringResource(R.string.health_card_height),
            testTag = HealthCardTestTags.HEIGHT_FIELD,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
    LabeledTextField(state = heightState)

    // Weight
    val weightState =
        TextFieldState(
            value = formState.weightKg,
            onValueChange = { onFormStateChange(formState.copy(weightKg = it)) },
            label = stringResource(R.string.health_card_weight),
            testTag = HealthCardTestTags.WEIGHT_FIELD,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
    LabeledTextField(state = weightState)

    // Chronic Conditions
    val chronicConditionsState =
        TextFieldState(
            value = formState.chronicConditions,
            onValueChange = { onFormStateChange(formState.copy(chronicConditions = it)) },
            label = stringResource(R.string.health_card_chronic_conditions),
            testTag = HealthCardTestTags.CHRONIC_CONDITIONS_FIELD)
    LabeledTextField(state = chronicConditionsState)

    // Allergies
    val allergiesState =
        TextFieldState(
            value = formState.allergies,
            onValueChange = { onFormStateChange(formState.copy(allergies = it)) },
            label = stringResource(R.string.health_card_allergies),
            testTag = HealthCardTestTags.ALLERGIES_FIELD)
    LabeledTextField(state = allergiesState)

    // Medications
    val medicationsState =
        TextFieldState(
            value = formState.medications,
            onValueChange = { onFormStateChange(formState.copy(medications = it)) },
            label = stringResource(R.string.health_card_medications),
            testTag = HealthCardTestTags.MEDICATIONS_FIELD)
    LabeledTextField(state = medicationsState)

    // On-going Treatments
    val treatmentsState =
        TextFieldState(
            value = formState.onGoingTreatments,
            onValueChange = { onFormStateChange(formState.copy(onGoingTreatments = it)) },
            label = stringResource(R.string.health_card_treatments),
            testTag = HealthCardTestTags.TREATMENTS_FIELD)
    LabeledTextField(state = treatmentsState)

    // Medical History
    val historyState =
        TextFieldState(
            value = formState.medicalHistory,
            onValueChange = { onFormStateChange(formState.copy(medicalHistory = it)) },
            label = stringResource(R.string.health_card_history),
            testTag = HealthCardTestTags.HISTORY_FIELD)
    LabeledTextField(state = historyState)

    OrganDonorSwitch(
        checked = formState.organDonor,
        onCheckedChange = { onFormStateChange(formState.copy(organDonor = it)) })

    // Notes
    val notesState =
        TextFieldState(
            value = formState.notes,
            onValueChange = { onFormStateChange(formState.copy(notes = it)) },
            label = stringResource(R.string.health_card_notes),
            testTag = HealthCardTestTags.NOTES_FIELD)
    LabeledTextField(state = notesState)
  }
}

/**
 * Text field component with required field validation and touch tracking.
 *
 * This component implements the "touched" pattern where validation errors are only shown after the
 * user has interacted with the field.
 *
 * @param state The state object for the required text field.
 * @param modifier Optional modifier for customization
 */
@Composable
private fun LabeledRequiredTextField(state: RequiredTextFieldState, modifier: Modifier = Modifier) {
  Column(modifier = modifier) {
    OutlinedTextField(
        value = state.baseState.value,
        onValueChange = {
          state.baseState.onValueChange(it)
          if (!state.touched) state.onTouched()
        },
        label = { Text(state.baseState.label) },
        isError = state.error && state.touched,
        keyboardOptions = state.baseState.keyboardOptions,
        modifier =
            Modifier.fillMaxWidth().testTag(state.baseState.testTag).onFocusChanged { focusState ->
              if (focusState.isFocused) state.onTouched()
            })

    if (state.error && state.touched && state.errorText != null) {
      Text(
          text = state.errorText,
          color = MaterialTheme.colorScheme.error,
          style = MaterialTheme.typography.bodySmall,
          modifier = Modifier.padding(start = 16.dp, top = 4.dp))
    }
  }
}

/**
 * Basic text field component without required field validation.
 *
 * @param state The state object containing all necessary information for the text field.
 * @param modifier Optional modifier for customization
 */
@Composable
private fun LabeledTextField(
    state: TextFieldState,
    modifier: Modifier = Modifier,
    isError: Boolean = false,
    errorText: String? = null
) {
  Column(modifier = modifier) {
    OutlinedTextField(
        value = state.value,
        onValueChange = state.onValueChange,
        label = { Text(state.label + " " + stringResource(R.string.optional_label_suffix)) },
        isError = isError,
        keyboardOptions = state.keyboardOptions,
        modifier = Modifier.fillMaxWidth().testTag(state.testTag))

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
        Text(stringResource(R.string.health_card_organ_donor))
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            modifier = Modifier.testTag(HealthCardTestTags.ORGAN_DONOR_FIELD))
      }
}
