package com.github.warnastrophy.core.ui.profile.contact

import android.widget.Toast
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel

object AddContactTestTags {
  const val INPUT_FULL_NAME = "inputFullName"
  const val INPUT_PHONE_NUMBER = "inputPhoneNumber"
  const val INPUT_RELATIONSHIP = "inputRelationship"
  const val ERROR_MESSAGE = "errorMessage"
  const val CONTACT_SAVE = "contactSave"
}

/**
 * The main screen composable for the Add Contact feature.
 *
 * This screen provides the form inputs (Full Name, Phone Number, Relationship) and observes the
 * state of [AddContactViewModel]. It handles user input by calling the ViewModel's update
 * functions, displays validation and error messages, and ultimately triggers the contact
 * persistence logic via [AddContactViewModel.addContact].
 *
 * @param addContactViewModel The ViewModel responsible for form state, input validation, and saving
 *   the new contact.
 * @param onDone A lambda invoked after a contact is successfully added, signaling to the navigation
 *   host that the screen should be closed or navigated away from (e.g., back to the contact list).
 */
@Composable
fun AddContactScreen(
    // Optional: Add a callback to handle the save action and pass the contact data
    addContactViewModel: AddContactViewModel = viewModel(),
    onDone: () -> Unit = {}
) {
  val contactUIState by addContactViewModel.uiState.collectAsState()
  val errorMsg = contactUIState.errorMsg

  val context = LocalContext.current
  LaunchedEffect(errorMsg) {
    if (errorMsg != null) {
      Toast.makeText(context, errorMsg, Toast.LENGTH_SHORT).show()
      addContactViewModel.clearErrorMsg()
    }
  }

  LaunchedEffect(Unit) {
    // 1. Collect the flow of navigation events
    addContactViewModel.navigateBack.collect { onDone() }
  }

  Column(
      modifier = Modifier.fillMaxSize().padding(16.dp),
      horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = "Add Contact Form",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(bottom = 32.dp))

        // --- Input Field: Full Name ---
        OutlinedTextField(
            value = contactUIState.fullName,
            onValueChange = { addContactViewModel.setFullName(it) },
            label = { Text("Full Name") },
            isError = contactUIState.invalidFullNameMsg != null,
            supportingText = {
              contactUIState.invalidFullNameMsg?.let {
                Text(it, modifier = Modifier.testTag(AddContactTestTags.ERROR_MESSAGE))
              }
            },
            modifier =
                Modifier.fillMaxWidth()
                    .padding(bottom = 16.dp)
                    .testTag(AddContactTestTags.INPUT_FULL_NAME))

        // --- Input Field: Phone Number ---
        OutlinedTextField(
            value = contactUIState.phoneNumber,
            onValueChange = { addContactViewModel.setPhoneNumber(it) },
            label = { Text("Phone number") },
            // Note: Use KeyboardOptions to hint at numeric input
            // keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
            isError = contactUIState.invalidPhoneNumberMsg != null,
            supportingText = {
              contactUIState.invalidPhoneNumberMsg?.let {
                Text(it, modifier = Modifier.testTag(AddContactTestTags.ERROR_MESSAGE))
              }
            },
            modifier =
                Modifier.fillMaxWidth()
                    .padding(bottom = 16.dp)
                    .testTag(AddContactTestTags.INPUT_PHONE_NUMBER))

        // --- Input Field: Relationship ---
        OutlinedTextField(
            value = contactUIState.relationship,
            onValueChange = { addContactViewModel.setRelationShip(it) },
            label = { Text("Relationship (e.g., family, friend, doctor, etc.)") },
            isError = contactUIState.invalidRelationshipMsg != null,
            supportingText = {
              contactUIState.invalidRelationshipMsg?.let {
                Text(it, modifier = Modifier.testTag(AddContactTestTags.ERROR_MESSAGE))
              }
            },
            modifier =
                Modifier.fillMaxWidth()
                    .padding(bottom = 32.dp)
                    .testTag(AddContactTestTags.INPUT_RELATIONSHIP))

        // --- Save Button with Validation ---
        Button(
            onClick = {
              addContactViewModel.addContact()
              // TODO: Add navigate back here

            },
            modifier =
                Modifier.fillMaxWidth().height(50.dp).testTag(AddContactTestTags.CONTACT_SAVE)) {
              Text("Save Contact")
            }
      }
}

@Preview(showBackground = true)
@Composable
fun AddContactScreenPreview() {
  // Assuming you have a MainAppTheme or just use the system default
  MaterialTheme { AddContactScreen() }
}
