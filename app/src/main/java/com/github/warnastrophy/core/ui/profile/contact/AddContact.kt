package com.github.warnastrophy.core.ui.profile.contact

import android.widget.Toast
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.github.warnastrophy.core.ui.viewModel.AddContactViewModel

object AddContactTestTags {
  // TODO: define testTags
}
// Utility function for basic phone number format validation
fun isValidPhoneNumber(phone: String): Boolean {
  // Regex for basic validation: optional '+' at start, followed by 10-15 digits
  return phone.matches(Regex("^\\+?[0-9]{10,15}\$"))
}

@OptIn(ExperimentalMaterial3Api::class)
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

  // 1. State variables to hold the user input
  var fullName by remember { mutableStateOf("") }
  var phoneNumber by remember { mutableStateOf("") }
  var relationship by remember { mutableStateOf("") }

  // 2. State variables for error messages (Form Validation)
  var fullNameError by remember { mutableStateOf(false) }
  var phoneError by remember { mutableStateOf(false) }

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
            supportingText = { contactUIState.invalidFullNameMsg?.let { Text(it) } },
            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp))

        // --- Input Field: Phone Number ---
        OutlinedTextField(
            value = contactUIState.phoneNumber,
            onValueChange = { addContactViewModel.setPhoneNumber(it) },
            label = { Text("Phone number") },
            // Note: Use KeyboardOptions to hint at numeric input
            // keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
            isError = contactUIState.invalidPhoneNumberMsg != null,
            supportingText = { contactUIState.invalidPhoneNumberMsg?.let { Text(it) } },
            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp))

        // --- Input Field: Relationship ---
        OutlinedTextField(
            value = contactUIState.relationship,
            onValueChange = { addContactViewModel.setRelationShip(it) },
            label = { Text("Relationship (e.g., family, friend, doctor, etc.)") },
            isError = contactUIState.invalidRelationshipMsg != null,
            supportingText = { contactUIState.invalidRelationshipMsg?.let { Text(it) } },
            modifier = Modifier.fillMaxWidth().padding(bottom = 32.dp))

        // --- Save Button with Validation ---
        Button(
            onClick = {
              if (addContactViewModel.addContact()) {
                onDone()
                // TODO: Add navigate back here

              }
            },
            modifier = Modifier.fillMaxWidth().height(50.dp)) {
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
