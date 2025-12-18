package com.github.warnastrophy.core.ui.features.contact

import android.widget.Toast
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.github.warnastrophy.R

object EditContactTestTags {
  const val INPUT_FULL_NAME = "inputFullName"
  const val INPUT_PHONE_NUMBER = "inputPhoneNumber"
  const val ERROR_MESSAGE = "errorMessage"
  const val INPUT_RELATIONSHIP = "inputRelationship"
  const val SAVE_BUTTON = "contactSave"
  const val DELETE_BUTTON = "contactDelete"
}

/**
 * A screen composable used for editing an existing emergency contact.
 *
 * This screen is responsible for:
 * 1. Loading the contact data chosen by user in Contact List Screen.
 * 5. Providing buttons for saving and deleting the contact.
 *
 * @param contactID The unique identifier of the contact to be loaded and edited.
 * @param editContactViewModel The ViewModel instance responsible for handling all business logic,
 *   data persistence, and UI state for the contact editing process.
 * @param onDone A callback lambda executed when a successful action (Save or Delete) is completed.
 *   This is typically used by the Navigation Host to navigate back to the previous screen (e.g.,
 *   the contact list).
 */
@Composable
fun EditContactScreen(
    contactID: String = "1", // just for testing purpose
    editContactViewModel: EditContactViewModel = viewModel(),
    onDone: () -> Unit = {},
) {
  LaunchedEffect(contactID) { editContactViewModel.loadContact(contactID) }

  val contactUIState by editContactViewModel.uiState.collectAsState()
  val errorMsg = contactUIState.errorMsg
  val isSaveButtonValid = contactUIState.isValid

  val context = LocalContext.current

  LaunchedEffect(errorMsg) {
    if (errorMsg != null) {
      Toast.makeText(context, errorMsg, Toast.LENGTH_SHORT).show()
      editContactViewModel.clearErrorMsg()
    }
  }

  LaunchedEffect(Unit) {
    // 1. Collect the flow of navigation events
    editContactViewModel.navigateBack.collect { onDone() }
  }

  Column(
      modifier = Modifier.fillMaxSize().padding(16.dp),
      horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = stringResource(R.string.emergency_contact_edit_contact_title),
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(bottom = 32.dp))

        // --- Input Field: Full Name ---
        OutlinedTextField(
            value = contactUIState.fullName,
            onValueChange = { editContactViewModel.setFullName(it) },
            label = { Text(stringResource(R.string.emergency_contact_name)) },
            isError = contactUIState.invalidFullNameMsg != null,
            supportingText = {
              contactUIState.invalidFullNameMsg?.let {
                Text(it, modifier = Modifier.testTag(EditContactTestTags.ERROR_MESSAGE))
              }
            },
            modifier =
                Modifier.fillMaxWidth()
                    .padding(bottom = 16.dp)
                    .testTag(EditContactTestTags.INPUT_FULL_NAME))

        // --- Input Field: Phone Number ---
        OutlinedTextField(
            value = contactUIState.phoneNumber,
            onValueChange = { editContactViewModel.setPhoneNumber(it) },
            label = { Text(stringResource(R.string.emergency_contact_phone_number)) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
            isError = contactUIState.invalidPhoneNumberMsg != null,
            supportingText = {
              contactUIState.invalidPhoneNumberMsg?.let {
                Text(it, modifier = Modifier.testTag(EditContactTestTags.ERROR_MESSAGE))
              }
            },
            modifier =
                Modifier.fillMaxWidth()
                    .padding(bottom = 16.dp)
                    .testTag(EditContactTestTags.INPUT_PHONE_NUMBER))

        // --- Input Field: Relationship ---
        OutlinedTextField(
            value = contactUIState.relationship,
            onValueChange = { editContactViewModel.setRelationship(it) },
            label = { Text(stringResource(R.string.emergency_contact_relationship_with_examples)) },
            isError = contactUIState.invalidRelationshipMsg != null,
            supportingText = {
              contactUIState.invalidRelationshipMsg?.let {
                Text(it, modifier = Modifier.testTag(EditContactTestTags.ERROR_MESSAGE))
              }
            },
            modifier =
                Modifier.fillMaxWidth()
                    .padding(bottom = 32.dp)
                    .testTag(EditContactTestTags.INPUT_RELATIONSHIP))

        // --- Save Button with Validation ---
        Button(
            onClick = { editContactViewModel.editContact(contactID) },
            enabled = isSaveButtonValid,
            modifier =
                Modifier.fillMaxWidth().height(50.dp).testTag(EditContactTestTags.SAVE_BUTTON)) {
              Text(stringResource(R.string.emergency_contact_save_contact_button))
            }

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = { editContactViewModel.deleteContact(contactID) },
            colors =
                ButtonColors(
                    containerColor = Color.Red,
                    contentColor = Color.White,
                    disabledContainerColor = Color.Gray,
                    disabledContentColor = Color.DarkGray),
            modifier =
                Modifier.fillMaxWidth().height(50.dp).testTag(EditContactTestTags.DELETE_BUTTON)) {
              Text(stringResource(R.string.delete_button), color = Color.White)
            }
      }
}
